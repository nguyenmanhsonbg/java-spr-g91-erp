import {
    apiRequest,
    applyRoleLabel,
    canManageProducts,
    escapeHtml,
    formatTimestamp,
    getToken,
    initTokenControls,
    loadCurrentUserOrNull,
    setMessage,
    statusLabel,
    statusText
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
const productBody = document.querySelector("[data-product-body]");
const createLink = document.querySelector("[data-create-link]");
const statusField = document.querySelector("[data-status-field]");
const paginationLabel = document.querySelector("[data-pagination-label]");
const totalLabel = document.querySelector("[data-total-label]");
const prevButton = document.querySelector("[data-page-prev]");
const nextButton = document.querySelector("[data-page-next]");

initTokenControls(document);
document.addEventListener("product-token-changed", boot);
document.querySelector("[data-apply-filter]").addEventListener("click", () => {
    state.page = 1;
    loadProducts();
});
document.querySelector("[data-reset-filter]").addEventListener("click", () => {
    filterForm.reset();
    state.page = 1;
    loadProducts();
});
prevButton.addEventListener("click", () => {
    if (state.page > 1) {
        state.page -= 1;
        loadProducts();
    }
});
nextButton.addEventListener("click", () => {
    if (state.page < state.totalPages) {
        state.page += 1;
        loadProducts();
    }
});
productBody.addEventListener("click", handleBodyClick);

boot();

async function boot() {
    state.user = await loadCurrentUserOrNull();
    applyRoleLabel(roleLabel, state.user);

    const warehouseMode = canManageProducts(state.user);
    createLink.hidden = !warehouseMode;
    statusField.hidden = !warehouseMode;

    if (!warehouseMode) {
        filterForm.status.value = "";
    }

    await loadProducts();
}

async function loadProducts() {
    setMessage(pageMessage, "info", "");
    productBody.innerHTML = `<tr><td colspan="7" class="empty-state">Loading products...</td></tr>`;

    try {
        const params = new URLSearchParams({
            page: String(state.page),
            pageSize: String(state.pageSize)
        });

        const formData = new FormData(filterForm);
        const search = String(formData.get("search") || "").trim();
        const type = String(formData.get("type") || "").trim();
        const sizeValue = String(formData.get("sizeValue") || "").trim();
        const thickness = String(formData.get("thickness") || "").trim();
        const status = String(formData.get("status") || "").trim();

        if (search) {
            params.set("search", search);
        }
        if (type) {
            params.set("type", type);
        }
        if (sizeValue) {
            params.set("sizeValue", sizeValue);
        }
        if (thickness) {
            params.set("thickness", thickness);
        }
        if (canManageProducts(state.user) && status) {
            params.set("status", status);
        }

        const payload = await apiRequest(`/api/products?${params.toString()}`, { auth: "optional" });
        const data = payload.data;
        const products = Array.isArray(data?.items) ? data.items : [];
        const pagination = data?.pagination || {};

        state.page = pagination.page || 1;
        state.totalPages = pagination.totalPages || 1;
        state.totalItems = pagination.totalItems || 0;

        renderProducts(products);
        updatePagination();
    } catch (error) {
        productBody.innerHTML = `<tr><td colspan="7" class="empty-state">Unable to load products.</td></tr>`;
        updatePagination();
        setMessage(pageMessage, "error", error.message);
    }
}

function renderProducts(products) {
    if (!products.length) {
        productBody.innerHTML = `<tr><td colspan="7" class="empty-state">No products matched the current filters.</td></tr>`;
        return;
    }

    const warehouseMode = canManageProducts(state.user);
    productBody.innerHTML = products.map((product) => {
        const tag = statusLabel(product);
        const actions = [
            `<a class="button subtle" href="./detail.html?id=${encodeURIComponent(product.id)}">View</a>`
        ];

        if (warehouseMode && !product.deletedAt) {
            actions.push(`<a class="button secondary" href="./form.html?id=${encodeURIComponent(product.id)}">Edit</a>`);
            actions.push(`<button class="button danger" type="button" data-archive-id="${escapeHtml(product.id)}">Archive</button>`);
        }

        return `
            <tr>
                <td>
                    <strong>${escapeHtml(product.productName)}</strong>
                    <div class="muted">${escapeHtml(product.productCode)}</div>
                </td>
                <td>${escapeHtml(product.type)}</td>
                <td>${escapeHtml(product.size)}</td>
                <td>${escapeHtml(product.thickness)}</td>
                <td>${escapeHtml(product.unit)}</td>
                <td>
                    <span class="pill status-pill" data-status="${tag}">${statusText(product)}</span>
                    <div class="muted" style="margin-top: 6px;">Updated ${escapeHtml(formatTimestamp(product.updatedAt || product.createdAt))}</div>
                </td>
                <td>
                    <div class="toolbar-inline">
                        ${actions.join("")}
                    </div>
                </td>
            </tr>
        `;
    }).join("");
}

function updatePagination() {
    paginationLabel.textContent = `Page ${state.page} of ${Math.max(state.totalPages, 1)}`;
    totalLabel.textContent = `${state.totalItems} products`;
    prevButton.disabled = state.page <= 1;
    nextButton.disabled = state.page >= state.totalPages;
}

async function handleBodyClick(event) {
    const button = event.target.closest("[data-archive-id]");
    if (!button) {
        return;
    }

    if (!getToken()) {
        setMessage(pageMessage, "error", "Paste a warehouse access token before archiving a product.");
        return;
    }

    const productId = button.getAttribute("data-archive-id");
    const confirmed = window.confirm("Archive this product? The product will be kept for history and removed from the public catalog.");
    if (!confirmed) {
        return;
    }

    try {
        await apiRequest(`/api/products/${productId}`, { method: "DELETE", auth: true });
        setMessage(pageMessage, "success", "Product archived successfully.");
        await loadProducts();
    } catch (error) {
        setMessage(pageMessage, "error", error.message);
    }
}
