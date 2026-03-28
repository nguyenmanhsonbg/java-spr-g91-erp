import {
    apiRequest,
    applyFieldErrors,
    applyRoleLabel,
    clearFieldErrors,
    fillMultiSelect,
    initTokenControls,
    loadCurrentUser,
    loadProducts,
    parseCsvList,
    setMessage,
    toMessages
} from "./shared.js";

const promotionId = new URLSearchParams(window.location.search).get("id");
const state = {
    user: null,
    products: []
};

const form = document.querySelector("[data-promotion-form]");
const formTitle = document.querySelector("[data-form-title]");
const formMessage = document.querySelector("[data-form-message]");
const roleLabel = document.querySelector("[data-user-role]");
const readonlyBanner = document.querySelector("[data-readonly-banner]");
const validationSummary = document.querySelector("[data-validation-summary]");
const submitButton = document.querySelector("[data-submit-button]");
const productSelect = document.querySelector("#productIds");

initTokenControls(document);
bindEvents();
bootstrap();

async function bootstrap() {
    clearFieldErrors(form);
    setMessage(formMessage, "info", "Loading promotion editor...");
    validationSummary.hidden = true;
    try {
        state.user = await loadCurrentUser();
        applyRoleLabel(roleLabel, state.user);
        state.products = await loadProducts();
        fillProductOptions([]);

        const editing = Boolean(promotionId);
        formTitle.textContent = editing ? "Edit Promotion" : "Create Promotion";
        submitButton.textContent = editing ? "Save Changes" : "Save Promotion";

        if (state.user.role !== "OWNER") {
            setReadonly(true, "Owner access is required to create, update, or delete promotions.");
            setMessage(formMessage, "warning", "Read-only mode enabled.");
            return;
        }

        setReadonly(false, "");
        if (editing) {
            await loadPromotion();
            setMessage(formMessage, "success", "Promotion data loaded.");
        } else {
            setMessage(formMessage, "success", "Promotion form ready.");
        }
    } catch (error) {
        setReadonly(true, error.message || "Unable to load the promotion editor.");
        applyRoleLabel(roleLabel, null);
        setMessage(formMessage, "error", error.message || "Unable to load the promotion editor.", toMessages(error.errors));
    }
}

function bindEvents() {
    document.addEventListener("promotion-token-changed", bootstrap);

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (state.user?.role !== "OWNER") {
            return;
        }

        const payload = buildPayload();
        const validationErrors = validatePayload(payload);
        if (validationErrors.length) {
            applyFieldErrors(form, validationErrors);
            validationSummary.hidden = false;
            validationSummary.className = "message error";
            validationSummary.innerHTML = `
                <div>Fix the highlighted fields before saving.</div>
                <ul class="summary-list">
                    ${validationErrors.map((error) => `<li>${error.field}: ${error.message}</li>`).join("")}
                </ul>
            `;
            return;
        }

        clearFieldErrors(form);
        validationSummary.hidden = true;
        setMessage(formMessage, "info", "Saving promotion...");
        submitButton.disabled = true;

        try {
            const method = promotionId ? "PUT" : "POST";
            const path = promotionId ? `/api/promotions/${encodeURIComponent(promotionId)}` : "/api/promotions";
            const payloadResponse = await apiRequest(path, { method, body: payload });
            const nextId = promotionId || payloadResponse.data?.id;
            setMessage(formMessage, "success", "Promotion saved successfully.");
            window.location.href = `./detail.html?id=${encodeURIComponent(nextId)}`;
        } catch (error) {
            applyFieldErrors(form, error.errors || []);
            validationSummary.hidden = false;
            validationSummary.className = "message error";
            validationSummary.innerHTML = `
                <div>${error.message || "Save failed."}</div>
                <ul class="summary-list">
                    ${toMessages(error.errors).map((message) => `<li>${message}</li>`).join("")}
                </ul>
            `;
            setMessage(formMessage, "error", error.message || "Save failed.", toMessages(error.errors));
        } finally {
            submitButton.disabled = false;
        }
    });
}

async function loadPromotion() {
    const payload = await apiRequest(`/api/promotions/${encodeURIComponent(promotionId)}`);
    const detail = payload.data;
    document.querySelector("#name").value = detail.name || "";
    document.querySelector("#promotionType").value = detail.promotionType || "PERCENT";
    document.querySelector("#discountValue").value = detail.discountValue ?? "";
    document.querySelector("#status").value = detail.status || "ACTIVE";
    document.querySelector("#priority").value = detail.priority ?? 0;
    document.querySelector("#validFrom").value = detail.validFrom || "";
    document.querySelector("#validTo").value = detail.validTo || "";
    document.querySelector("#description").value = detail.description || "";
    document.querySelector("#customerGroups").value = (detail.customerGroups || []).join(", ");
    fillProductOptions((detail.products || []).map((product) => product.productId));
}

function fillProductOptions(selectedValues) {
    fillMultiSelect(productSelect, state.products.map((product) => ({
        value: product.id,
        label: `${product.productCode} - ${product.productName}`
    })), selectedValues);
}

function buildPayload() {
    return {
        name: document.querySelector("#name").value.trim(),
        promotionType: document.querySelector("#promotionType").value,
        discountValue: normalizeNumber(document.querySelector("#discountValue").value),
        status: document.querySelector("#status").value,
        priority: normalizeInteger(document.querySelector("#priority").value),
        validFrom: document.querySelector("#validFrom").value || null,
        validTo: document.querySelector("#validTo").value || null,
        description: document.querySelector("#description").value.trim(),
        productIds: Array.from(productSelect.selectedOptions).map((option) => option.value).filter(Boolean),
        customerGroups: parseCsvList(document.querySelector("#customerGroups").value)
    };
}

function validatePayload(payload) {
    const errors = [];
    if (!payload.name) {
        errors.push({ field: "name", message: "Promotion name is required" });
    }
    if (!payload.promotionType) {
        errors.push({ field: "promotionType", message: "Promotion type is required" });
    }
    if (payload.discountValue === null || payload.discountValue <= 0) {
        errors.push({ field: "discountValue", message: "Discount value must be greater than 0" });
    }
    if (payload.promotionType === "PERCENT" && payload.discountValue > 100) {
        errors.push({ field: "discountValue", message: "Percent discount must not exceed 100" });
    }
    if (!payload.validFrom) {
        errors.push({ field: "validFrom", message: "Valid from is required" });
    }
    if (!payload.validTo) {
        errors.push({ field: "validTo", message: "Valid to is required" });
    }
    if (payload.validFrom && payload.validTo && payload.validFrom > payload.validTo) {
        errors.push({ field: "validFrom", message: "Valid from must be on or before valid to" });
    }
    if (payload.priority !== null && payload.priority < 0) {
        errors.push({ field: "priority", message: "Priority must be at least 0" });
    }
    return errors;
}

function setReadonly(readonly, message) {
    readonlyBanner.hidden = !readonly;
    readonlyBanner.textContent = message;
    form.querySelectorAll("input, select, textarea, button").forEach((element) => {
        if (element === submitButton) {
            element.disabled = readonly;
            return;
        }
        if (element.type === "button") {
            return;
        }
        element.disabled = readonly;
    });
}

function normalizeNumber(value) {
    const trimmed = String(value ?? "").trim();
    return trimmed ? Number(trimmed) : null;
}

function normalizeInteger(value) {
    const trimmed = String(value ?? "").trim();
    return trimmed ? Number.parseInt(trimmed, 10) : null;
}
