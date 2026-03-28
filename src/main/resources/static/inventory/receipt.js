import {
    apiRequest,
    applyFieldErrors,
    applyRoleLabel,
    canOperateInventory,
    clearFieldErrors,
    initTokenControls,
    loadCurrentUserOrNull,
    loadProducts,
    setMessage
} from "./shared.js";

const roleLabel = document.querySelector("[data-user-role]");
const formMessage = document.querySelector("[data-form-message]");
const readonlyBanner = document.querySelector("[data-readonly-banner]");
const validationSummary = document.querySelector("[data-validation-summary]");
const form = document.querySelector("[data-receipt-form]");
const submitButton = document.querySelector("[data-submit-button]");
const productSelect = document.querySelector("#productId");

let currentUser = null;

initTokenControls(document);
document.addEventListener("inventory-token-changed", boot);
form.addEventListener("submit", submitForm);

boot();

async function boot() {
    currentUser = await loadCurrentUserOrNull();
    applyRoleLabel(roleLabel, currentUser);
    await populateProducts();
    applyPermissions();
}

async function populateProducts() {
    try {
        const products = await loadProducts();
        productSelect.innerHTML = `<option value="">Select product</option>${products.map((product) => `
            <option value="${product.id}">${product.productCode} | ${product.productName}</option>
        `).join("")}`;
    } catch {
        productSelect.innerHTML = `<option value="">Unable to load products</option>`;
    }
}

function applyPermissions() {
    const warehouseMode = canOperateInventory(currentUser);
    readonlyBanner.hidden = warehouseMode;
    readonlyBanner.textContent = "Warehouse access is required to create inventory receipts.";

    Array.from(form.elements).forEach((element) => {
        element.disabled = !warehouseMode;
    });
    submitButton.disabled = !warehouseMode;
}

async function submitForm(event) {
    event.preventDefault();
    clearFieldErrors(form);
    setMessage(formMessage, "info", "");
    setMessage(validationSummary, "error", "");

    const clientErrors = validateForm();
    if (clientErrors.length) {
        setMessage(validationSummary, "error", "Fix the highlighted fields before submitting.", clientErrors);
        return;
    }

    try {
        await apiRequest("/api/inventory/receipts", {
            method: "POST",
            body: {
                productId: form.productId.value,
                quantity: Number(form.quantity.value),
                receiptDate: form.receiptDate.value,
                supplierName: form.supplierName.value.trim() || null,
                reason: form.reason.value.trim() || null,
                note: form.note.value.trim() || null
            }
        });
        setMessage(formMessage, "success", "Inventory receipt created successfully.");
        window.setTimeout(() => {
            window.location.href = "./history.html";
        }, 800);
    } catch (error) {
        const summary = applyFieldErrors(form, error.errors);
        if (summary.length) {
            setMessage(validationSummary, "error", "Fix the highlighted fields before submitting.", summary);
        } else {
            setMessage(formMessage, "error", error.message);
        }
    }
}

function validateForm() {
    const errors = [];

    if (!form.productId.value) {
        form.querySelector('[data-error-for="productId"]').textContent = "Product is required";
        errors.push("productId: Product is required");
    }
    if (!form.quantity.value || Number(form.quantity.value) <= 0) {
        form.querySelector('[data-error-for="quantity"]').textContent = "Quantity must be greater than 0";
        errors.push("quantity: Quantity must be greater than 0");
    }
    if (!form.receiptDate.value) {
        form.querySelector('[data-error-for="receiptDate"]').textContent = "Receipt date is required";
        errors.push("receiptDate: Receipt date is required");
    }
    if (!form.reason.value.trim() && !form.note.value.trim()) {
        form.querySelector('[data-error-for="reason"]').textContent = "Reason or note is required";
        errors.push("reason: Reason or note is required");
    }

    return errors;
}
