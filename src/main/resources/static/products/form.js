import {
    apiRequest,
    applyFieldErrors,
    applyRoleLabel,
    canManageProducts,
    clearFieldErrors,
    initTokenControls,
    loadCurrentUserOrNull,
    setMessage
} from "./shared.js";

const params = new URLSearchParams(window.location.search);
const productId = params.get("id");
const editing = Boolean(productId);

const roleLabel = document.querySelector("[data-user-role]");
const formTitle = document.querySelector("[data-form-title]");
const formMessage = document.querySelector("[data-form-message]");
const readonlyBanner = document.querySelector("[data-readonly-banner]");
const validationSummary = document.querySelector("[data-validation-summary]");
const form = document.querySelector("[data-product-form]");
const submitButton = document.querySelector("[data-submit-button]");
const deleteButton = document.querySelector("[data-delete-button]");

let currentUser = null;
let archived = false;

initTokenControls(document);
document.addEventListener("product-token-changed", boot);
form.addEventListener("submit", submitForm);
deleteButton.addEventListener("click", archiveProduct);

boot();

async function boot() {
    formTitle.textContent = editing ? "Edit Product" : "Create Product";
    currentUser = await loadCurrentUserOrNull();
    applyRoleLabel(roleLabel, currentUser);

    if (editing) {
        await loadProduct();
    }

    applyPermissions();
}

async function loadProduct() {
    try {
        const payload = await apiRequest(`/api/products/${encodeURIComponent(productId)}`, { auth: "optional" });
        populateForm(payload.data);
        archived = Boolean(payload.data.deletedAt);
        if (archived) {
            setMessage(formMessage, "info", "This product has already been archived.");
        }
    } catch (error) {
        setMessage(formMessage, "error", error.message);
        form.reset();
    }
}

function populateForm(product) {
    form.productCode.value = product.productCode || "";
    form.productName.value = product.productName || "";
    form.type.value = product.type || "";
    form.size.value = product.size || "";
    form.thickness.value = product.thickness || "";
    form.unit.value = product.unit || "";
    form.weightConversion.value = product.weightConversion ?? "";
    form.referenceWeight.value = product.referenceWeight ?? "";
    form.status.value = product.status || "ACTIVE";
    form.description.value = product.description || "";
    form.imageUrls.value = (product.imageUrls || []).join("\n");
}

function applyPermissions() {
    const warehouseMode = canManageProducts(currentUser);
    const locked = !warehouseMode || archived;

    readonlyBanner.hidden = !locked;
    readonlyBanner.textContent = archived
        ? "Archived products are read-only."
        : "Warehouse access is required to save product changes.";

    Array.from(form.elements).forEach((element) => {
        if (element.tagName === "A" || element === deleteButton) {
            return;
        }
        element.disabled = locked;
    });

    submitButton.disabled = locked;
    deleteButton.hidden = !(editing && warehouseMode && !archived);
}

async function submitForm(event) {
    event.preventDefault();
    clearFieldErrors(form);
    setMessage(formMessage, "info", "");
    setMessage(validationSummary, "error", "");

    const clientErrors = validateForm();
    if (clientErrors.length) {
        showValidationSummary(clientErrors);
        return;
    }

    const body = buildRequestBody();
    const method = editing ? "PUT" : "POST";
    const path = editing ? `/api/products/${encodeURIComponent(productId)}` : "/api/products";

    try {
        await apiRequest(path, { method, body, auth: true });
        setMessage(formMessage, "success", editing ? "Product updated successfully." : "Product created successfully.");
        window.setTimeout(() => {
            window.location.href = "./index.html";
        }, 800);
    } catch (error) {
        const summary = applyFieldErrors(form, error.errors);
        if (summary.length) {
            showValidationSummary(summary);
        } else {
            setMessage(formMessage, "error", error.message);
        }
    }
}

function validateForm() {
    const errors = [];
    const requiredFields = [
        ["productCode", "Product code is required"],
        ["productName", "Product name is required"],
        ["type", "Type is required"],
        ["size", "Size is required"],
        ["thickness", "Thickness is required"],
        ["unit", "Unit is required"]
    ];

    requiredFields.forEach(([field, message]) => {
        if (!String(form[field].value || "").trim()) {
            const slot = form.querySelector(`[data-error-for="${field}"]`);
            if (slot) {
                slot.textContent = message;
            }
            errors.push(`${field}: ${message}`);
        }
    });

    validateDecimal("weightConversion", errors);
    validateDecimal("referenceWeight", errors);

    return errors;
}

function validateDecimal(fieldName, errors) {
    const rawValue = String(form[fieldName].value || "").trim();
    if (!rawValue) {
        return;
    }

    const numeric = Number(rawValue);
    if (Number.isNaN(numeric) || numeric < 0) {
        const slot = form.querySelector(`[data-error-for="${fieldName}"]`);
        if (slot) {
            slot.textContent = `${fieldName} must be a valid non-negative number`;
        }
        errors.push(`${fieldName}: must be a valid non-negative number`);
    }
}

function buildRequestBody() {
    return {
        productCode: form.productCode.value.trim(),
        productName: form.productName.value.trim(),
        type: form.type.value.trim(),
        size: form.size.value.trim(),
        thickness: form.thickness.value.trim(),
        unit: form.unit.value.trim(),
        weightConversion: parseDecimal(form.weightConversion.value),
        referenceWeight: parseDecimal(form.referenceWeight.value),
        status: form.status.value,
        description: form.description.value.trim() || null,
        imageUrls: form.imageUrls.value
            .split(/\r?\n/)
            .map((value) => value.trim())
            .filter(Boolean)
    };
}

function parseDecimal(value) {
    const trimmed = String(value || "").trim();
    return trimmed ? Number(trimmed) : null;
}

function showValidationSummary(lines) {
    setMessage(validationSummary, "error", "Fix the highlighted fields before submitting.", lines);
}

async function archiveProduct() {
    if (!editing) {
        return;
    }

    const confirmed = window.confirm("Archive this product? The product will remain in history but will be removed from the public catalog.");
    if (!confirmed) {
        return;
    }

    try {
        await apiRequest(`/api/products/${encodeURIComponent(productId)}`, { method: "DELETE", auth: true });
        setMessage(formMessage, "success", "Product archived successfully.");
        window.setTimeout(() => {
            window.location.href = "./index.html";
        }, 800);
    } catch (error) {
        setMessage(formMessage, "error", error.message);
    }
}
