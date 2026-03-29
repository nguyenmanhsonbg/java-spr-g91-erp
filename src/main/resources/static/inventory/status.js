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
    pageSize: 10,
    totalPages: 1,
    totalItems: 0,
    user: null
};

const roleLabel = document.querySelector("[data-user-role]");
const pageMessage = document.querySelector("[data-page-message]");
const filterForm = document.querySelector("[data-filter-form]");
const productSelect = document.querySelector("#productId");
const statusBody = document.querySelector("[data-status-body]");
const paginationLabel = document.querySelector("[data-pagination-label]");
const totalLabel = document.querySelector("[data-total-label]");
const prevButton = document.querySelector("[data-page-prev]");
const nextButton = document.querySelector("[data-page-next]");

initTokenControls(document);
document.addEventListener("inventory-token-changed", boot);
document.querySelector("[data-apply-filter]").addEventListener("click", () => {
    state.page = 1;
    loadStatus();
});
document.querySelector("[data-reset-filter]").addEventListener("click", () => {
    filterForm.reset();
    state.page = 1;
    loadStatus();
});
prevButton.addEventListener("click", () => {
    if (state.page > 1) {
        state.page -= 1;
        loadStatus();
    }
});
nextButton.addEventListener("click", () => {
    if (state.page < state.totalPages) {
        state.page += 1;
        loadStatus();
    }
});

boot();

async function boot() {
    state.user = await loadCurrentUserOrNull();
    applyRoleLabel(roleLabel, state.user);
    await populateProducts();

    if (!canViewInventory(state.user)) {
        statusBody.innerHTML = `<tr><td colspan="5" class="empty-state">Paste a Warehouse or Owner token to load inventory status.</td></tr>`;
        setMessage(pageMessage, "info", "Inventory status requires Warehouse or Owner access.");
        updatePagination();
        return;
    }

    await loadStatus();
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

async function loadStatus() {
    if (!canViewInventory(state.user)) {
        return;
    }

    setMessage(pageMessage, "info", "");
    statusBody.innerHTML = `<tr><td colspan="5" class="empty-state">Loading inventory status...</td></tr>`;

    try {
        const params = new URLSearchParams({
            page: String(state.page),
            pageSize: String(state.pageSize)
        });
        const formData = new FormData(filterForm);
        const search = String(formData.get("search") || "").trim();
        const productId = String(formData.get("productId") || "").trim();
        if (search) {
            params.set("search", search);
        }
        if (productId) {
            params.set("productId", productId);
        }

        const payload = await apiRequest(`/api/inventory/status?${params.toString()}`);
        const data = payload.data;
        const items = Array.isArray(data?.items) ? data.items : [];
        const pagination = data?.pagination || {};

        state.page = pagination.page || 1;
        state.totalPages = pagination.totalPages || 1;
        state.totalItems = pagination.totalItems || 0;

        renderStatus(items);
        updatePagination();
    } catch (error) {
        statusBody.innerHTML = `<tr><td colspan="5" class="empty-state">Unable to load inventory status.</td></tr>`;
        updatePagination();
        setMessage(pageMessage, "error", error.message);
    }
}

function renderStatus(items) {
    if (!items.length) {
        statusBody.innerHTML = `<tr><td colspan="5" class="empty-state">No inventory status records matched the current filters.</td></tr>`;
        return;
    }

    statusBody.innerHTML = items.map((item) => `
        <tr>
            <td>
                <strong>${escapeHtml(item.productName)}</strong>
                <div class="muted">${escapeHtml(item.productCode)}</div>
            </td>
            <td>${escapeHtml(item.type || "N/A")}</td>
            <td>${escapeHtml(formatQuantity(item.currentQuantity))}</td>
            <td>${escapeHtml(item.unit || "N/A")}</td>
            <td>${escapeHtml(formatTimestamp(item.updatedAt))}</td>
        </tr>
    `).join("");
}

function updatePagination() {
    paginationLabel.textContent = `Page ${state.page} of ${Math.max(state.totalPages, 1)}`;
    totalLabel.textContent = `${state.totalItems} products`;
    prevButton.disabled = state.page <= 1;
    nextButton.disabled = state.page >= state.totalPages;
}
