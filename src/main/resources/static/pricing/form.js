import {
    apiRequest,
    applyRoleLabel,
    escapeHtml,
    initTokenControls,
    loadCurrentUser,
    loadProducts,
    setMessage
} from "./shared.js";

const params = new URLSearchParams(window.location.search);
const priceListId = params.get("id");
const mode = params.get("mode") || (priceListId ? "edit" : "create");

const formTitle = document.querySelector("[data-form-title]");
const form = document.querySelector("[data-price-list-form]");
const messageBox = document.querySelector("[data-form-message]");
const summaryBox = document.querySelector("[data-validation-summary]");
const readonlyBanner = document.querySelector("[data-readonly-banner]");
const roleLabel = document.querySelector("[data-user-role]");
const itemBody = document.querySelector("[data-item-body]");
const addRowButton = document.querySelector("[data-add-row]");
const submitButton = document.querySelector("[data-submit-button]");

const state = {
    user: null,
    products: [],
    editable: false
};

initTokenControls(document);
bindEvents();
bootstrap();

async function bootstrap() {
    formTitle.textContent = mode === "create" ? "Create Price List" : "Edit Price List";
    setMessage(messageBox, "info", "Loading pricing editor...");

    try {
        state.user = await loadCurrentUser();
        state.products = await loadProducts();
        state.editable = state.user.role === "OWNER" && mode !== "view";
        applyRoleLabel(roleLabel, state.user);
        applyEditMode();

        if (priceListId) {
            await loadPriceListDetail(priceListId);
        } else {
            renderRows([blankRow()]);
        }

        setMessage(messageBox, "success", "Editor ready.");
    } catch (error) {
        applyRoleLabel(roleLabel, null);
        applyEditMode(false);
        renderRows([blankRow()]);
        setMessage(messageBox, "error", error.message || "Unable to load editor.", toMessages(error.errors));
    }
}

function bindEvents() {
    document.addEventListener("pricing-token-changed", bootstrap);

    addRowButton.addEventListener("click", () => {
        const rows = collectRows();
        rows.push(blankRow());
        renderRows(rows);
    });

    itemBody.addEventListener("click", (event) => {
        const removeButton = event.target.closest("[data-remove-row]");
        if (!removeButton || !state.editable) {
            return;
        }
        const row = removeButton.closest("tr");
        row?.remove();
        if (!itemBody.querySelector("tr")) {
            renderRows([blankRow()]);
        }
    });

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearErrors();

        const payload = buildPayload();
        const clientErrors = validatePayload(payload);
        if (clientErrors.length) {
            setMessage(summaryBox, "error", "Fix the validation errors before submitting.", clientErrors);
            return;
        }

        try {
            const response = await apiRequest(
                priceListId ? `/api/price-lists/${encodeURIComponent(priceListId)}` : "/api/price-lists",
                {
                    method: priceListId ? "PUT" : "POST",
                    body: payload
                }
            );
            const redirectId = priceListId || response.data?.id;
            window.location.href = `./index.html${redirectId ? `?selected=${encodeURIComponent(redirectId)}` : ""}`;
        } catch (error) {
            applyServerErrors(error.errors || []);
            setMessage(summaryBox, "error", error.message || "Save failed.", toMessages(error.errors));
        }
    });
}

async function loadPriceListDetail(id) {
    const payload = await apiRequest(`/api/price-lists/${encodeURIComponent(id)}`);
    const detail = payload.data;
    document.querySelector("#name").value = detail.name || "";
    document.querySelector("#customerGroup").value = detail.customerGroup || "";
    document.querySelector("#status").value = detail.status || "ACTIVE";
    document.querySelector("#validFrom").value = detail.validFrom || "";
    document.querySelector("#validTo").value = detail.validTo || "";
    renderRows((detail.items || []).map((item) => ({
        id: item.id,
        productId: item.productId,
        unitPriceVnd: item.unitPriceVnd,
        note: item.note || ""
    })));
}

function applyEditMode(forceEditable = state.editable) {
    state.editable = Boolean(forceEditable);
    addRowButton.disabled = !state.editable;
    submitButton.disabled = !state.editable;
    submitButton.hidden = !state.editable;
    readonlyBanner.hidden = state.editable;
    readonlyBanner.textContent = state.user?.role === "ACCOUNTANT"
        ? "Read-only mode: accountants can inspect price lists but cannot create or update them."
        : "Owner access is required to save this price list.";

    for (const element of form.querySelectorAll("input, select, textarea")) {
        element.disabled = !state.editable;
    }
}

function renderRows(rows) {
    if (!state.products.length) {
        itemBody.innerHTML = `<tr><td colspan="4" class="empty-state">No active products available.</td></tr>`;
        return;
    }

    itemBody.innerHTML = rows.map((row, index) => `
        <tr>
            <td class="item-cell">
                <input type="hidden" class="item-id" value="${escapeHtml(row.id || "")}">
                <select class="item-product" ${state.editable ? "" : "disabled"}>
                    <option value="">Choose product</option>
                    ${state.products.map((product) => `
                        <option value="${escapeHtml(product.id)}" ${product.id === row.productId ? "selected" : ""}>
                            ${escapeHtml(product.productCode)} · ${escapeHtml(product.productName)}
                        </option>
                    `).join("")}
                </select>
                <div class="field-error" data-row-error="productId" data-row-index="${index}"></div>
            </td>
            <td class="item-cell">
                <input class="item-price" type="number" min="0.01" step="0.01" value="${escapeHtml(row.unitPriceVnd ?? "")}" ${state.editable ? "" : "disabled"}>
                <div class="field-error" data-row-error="unitPriceVnd" data-row-index="${index}"></div>
            </td>
            <td class="item-cell">
                <textarea class="item-note" ${state.editable ? "" : "disabled"}>${escapeHtml(row.note || "")}</textarea>
                <div class="field-error" data-row-error="note" data-row-index="${index}"></div>
            </td>
            <td>
                <button class="button danger" type="button" data-remove-row ${state.editable ? "" : "disabled"}>Remove</button>
            </td>
        </tr>
    `).join("");
}

function collectRows() {
    return Array.from(itemBody.querySelectorAll("tr")).map((row) => ({
        id: row.querySelector(".item-id")?.value || "",
        productId: row.querySelector(".item-product")?.value || "",
        unitPriceVnd: row.querySelector(".item-price")?.value || "",
        note: row.querySelector(".item-note")?.value || ""
    }));
}

function buildPayload() {
    const rows = collectRows();
    return {
        name: document.querySelector("#name").value.trim(),
        customerGroup: document.querySelector("#customerGroup").value.trim(),
        status: document.querySelector("#status").value,
        validFrom: document.querySelector("#validFrom").value,
        validTo: document.querySelector("#validTo").value,
        items: rows.map((row) => ({
            id: row.id || undefined,
            productId: row.productId,
            unitPriceVnd: row.unitPriceVnd ? Number(row.unitPriceVnd) : null,
            note: row.note?.trim() || undefined
        }))
    };
}

function validatePayload(payload) {
    const errors = [];

    if (!payload.name) {
        markFieldError("name", "Price list name is required.");
        errors.push("Price list name is required.");
    }

    if (!payload.customerGroup) {
        markFieldError("customerGroup", "Customer group is required.");
        errors.push("Customer group is required.");
    }

    if (!payload.validFrom) {
        markFieldError("validFrom", "Valid from is required.");
        errors.push("Valid from is required.");
    }

    if (!payload.validTo) {
        markFieldError("validTo", "Valid to is required.");
        errors.push("Valid to is required.");
    }

    if (payload.validFrom && payload.validTo && payload.validFrom > payload.validTo) {
        markFieldError("validFrom", "Valid from must be on or before valid to.");
        errors.push("Valid from must be on or before valid to.");
    }

    if (!payload.items.length) {
        markFieldError("items", "At least one pricing item is required.");
        errors.push("At least one pricing item is required.");
        return errors;
    }

    const uniqueProductIds = new Set();
    payload.items.forEach((item, index) => {
        if (!item.productId) {
            markRowError(index, "productId", "Product is required.");
            errors.push(`Row ${index + 1}: product is required.`);
        } else if (uniqueProductIds.has(item.productId)) {
            markRowError(index, "productId", "Duplicate product is not allowed.");
            errors.push(`Row ${index + 1}: duplicate product is not allowed.`);
        } else {
            uniqueProductIds.add(item.productId);
        }

        if (!(item.unitPriceVnd > 0)) {
            markRowError(index, "unitPriceVnd", "Unit price must be greater than 0.");
            errors.push(`Row ${index + 1}: unit price must be greater than 0.`);
        }
    });

    return errors;
}

function applyServerErrors(errors) {
    errors.forEach((error) => {
        const match = /items\[(\d+)]\.(.+)/.exec(error.field || "");
        if (match) {
            markRowError(Number(match[1]), match[2], error.message);
            return;
        }
        markFieldError(error.field, error.message);
    });
}

function clearErrors() {
    for (const element of document.querySelectorAll(".field-error")) {
        element.textContent = "";
    }
    for (const input of form.querySelectorAll("input, select, textarea")) {
        input.setAttribute("aria-invalid", "false");
    }
    setMessage(summaryBox, "error", "");
}

function markFieldError(field, message) {
    const errorElement = document.querySelector(`[data-error-for="${field}"]`);
    const input = document.querySelector(`#${CSS.escape(field)}`);
    if (errorElement) {
        errorElement.textContent = message;
    }
    if (input) {
        input.setAttribute("aria-invalid", "true");
    }
}

function markRowError(index, field, message) {
    const errorElement = document.querySelector(`[data-row-error="${field}"][data-row-index="${index}"]`);
    if (errorElement) {
        errorElement.textContent = message;
    }
    const row = itemBody.querySelectorAll("tr")[index];
    const input = row?.querySelector(field === "productId" ? ".item-product" : field === "unitPriceVnd" ? ".item-price" : ".item-note");
    input?.setAttribute("aria-invalid", "true");
}

function blankRow() {
    return { id: "", productId: "", unitPriceVnd: "", note: "" };
}

function toMessages(errors = []) {
    return errors.map((error) => error.field ? `${error.field}: ${error.message}` : error.message);
}
