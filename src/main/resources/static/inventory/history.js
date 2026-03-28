import {
    apiRequest,
    applyRoleLabel,
    canViewInventory,
    escapeHtml,
    formatQuantity,
    formatTimestamp,
    initTokenControls,
    loadCurrentUserOrNull,
    loadProducts,
    setMessage
} from "./shared.js";

const state = {
    page: 1,
    size: 10,
    totalPages: 1,
    totalItems: 0,
    user: null
};

const roleLabel = document.querySelector("[data-user-role]");
const pageMessage = document.querySelector("[data-page-message]");
const filterForm = document.querySelector("[data-filter-form]");
const productSelect = document.querySelector("#productId");
const historyBody = document.querySelector("[data-history-body]");
const paginationLabel = document.querySelector("[data-pagination-label]");
const totalLabel = document.querySelector("[data-total-label]");
const prevButton = document.querySelector("[data-page-prev]");
const nextButton = document.querySelector("[data-page-next]");

initTokenControls(document);
document.addEventListener("inventory-token-changed", boot);
document.querySelector("[data-apply-filter]").addEventListener("click", () => {
    state.page = 1;
    loadHistory();
});
document.querySelector("[data-reset-filter]").addEventListener("click", () => {
    filterForm.reset();
    state.page = 1;
    loadHistory();
});
prevButton.addEventListener("click", () => {
    if (state.page > 1) {
        state.page -= 1;
        loadHistory();
    }
});
nextButton.addEventListener("click", () => {
    if (state.page < state.totalPages) {
        state.page += 1;
        loadHistory();
    }
});

boot();

async function boot() {
    state.user = await loadCurrentUserOrNull();
    applyRoleLabel(roleLabel, state.user);
    await populateProducts();

    if (!canViewInventory(state.user)) {
        historyBody.innerHTML = `<tr><td colspan="8" class="empty-state">Paste a Warehouse or Owner token to load inventory history.</td></tr>`;
        setMessage(pageMessage, "info", "Inventory history requires Warehouse or Owner access.");
        updatePagination();
        return;
    }

    await loadHistory();
}

async function populateProducts() {
    try {
        const products = await loadProducts();
        const currentValue = productSelect.value;
        productSelect.innerHTML = `<option value="">All products</option>${products.map((product) => `
            <option value="${escapeHtml(product.id)}">${escapeHtml(product.productCode)} | ${escapeHtml(product.productName)}</option>
        `).join("")}`;
        productSelect.value = currentValue;
    } catch {
        productSelect.innerHTML = `<option value="">Unable to load products</option>`;
    }
}

async function loadHistory() {
    if (!canViewInventory(state.user)) {
        return;
    }

    setMessage(pageMessage, "info", "");
    historyBody.innerHTML = `<tr><td colspan="8" class="empty-state">Loading inventory history...</td></tr>`;

    try {
        const params = new URLSearchParams({
            page: String(state.page),
            size: String(state.size)
        });
        const formData = new FormData(filterForm);
        const productId = String(formData.get("productId") || "").trim();
        const transactionType = String(formData.get("transactionType") || "").trim();
        const fromDate = String(formData.get("fromDate") || "").trim();
        const toDate = String(formData.get("toDate") || "").trim();

        if (productId) {
            params.set("productId", productId);
        }
        if (transactionType) {
            params.set("transactionType", transactionType);
        }
        if (fromDate) {
            params.set("fromDate", fromDate);
        }
        if (toDate) {
            params.set("toDate", toDate);
        }

        const payload = await apiRequest(`/api/inventory/history?${params.toString()}`);
        const data = payload.data;
        const items = Array.isArray(data?.items) ? data.items : [];
        const pagination = data?.pagination || {};

        state.page = pagination.page || 1;
        state.totalPages = pagination.totalPages || 1;
        state.totalItems = pagination.totalItems || 0;

        renderHistory(items);
        updatePagination();
    } catch (error) {
        historyBody.innerHTML = `<tr><td colspan="8" class="empty-state">Unable to load inventory history.</td></tr>`;
        updatePagination();
        setMessage(pageMessage, "error", error.message);
    }
}

function renderHistory(items) {
    if (!items.length) {
        historyBody.innerHTML = `<tr><td colspan="8" class="empty-state">No inventory transactions matched the current filters.</td></tr>`;
        return;
    }

    historyBody.innerHTML = items.map((item) => `
        <tr>
            <td>${escapeHtml(item.transactionType)}</td>
            <td>
                <strong>${escapeHtml(item.productName)}</strong>
                <div class="muted">${escapeHtml(item.productCode)}</div>
            </td>
            <td>${escapeHtml(formatQuantity(item.quantity))}</td>
            <td>${escapeHtml(formatQuantity(item.quantityBefore))}</td>
            <td>${escapeHtml(formatQuantity(item.quantityAfter))}</td>
            <td>${escapeHtml(formatTimestamp(item.transactionDate))}</td>
            <td>
                <div>${escapeHtml(item.operatorEmail || item.operatorId || "N/A")}</div>
                <div class="muted">${escapeHtml(item.transactionCode || "")}</div>
            </td>
            <td>
                <div>${escapeHtml(item.reason || "N/A")}</div>
                <div class="muted">${escapeHtml(item.note || "")}</div>
            </td>
        </tr>
    `).join("");
}

function updatePagination() {
    paginationLabel.textContent = `Page ${state.page} of ${Math.max(state.totalPages, 1)}`;
    totalLabel.textContent = `${state.totalItems} transactions`;
    prevButton.disabled = state.page <= 1;
    nextButton.disabled = state.page >= state.totalPages;
}
