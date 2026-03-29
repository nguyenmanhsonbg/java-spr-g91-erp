import {
    apiRequest,
    applyRoleLabel,
    escapeHtml,
    fillMultiSelect,
    formatDate,
    formatMoney,
    formatTimestamp,
    initTokenControls,
    loadCurrentUser,
    loadProducts,
    setMessage,
    toMessages
} from "./shared.js";

const state = {
    page: 1,
    totalPages: 1,
    user: null
};

const pageMessage = document.querySelector("[data-page-message]");
const roleLabel = document.querySelector("[data-user-role]");
const createLink = document.querySelector("[data-create-link]");
const filterForm = document.querySelector("[data-filter-form]");
const body = document.querySelector("[data-promotion-body]");
const previewPanel = document.querySelector("[data-preview-panel]");
const paginationLabel = document.querySelector("[data-pagination-label]");
const productFilter = document.querySelector("#productId");

initTokenControls(document);
bindEvents();
bootstrap();

async function bootstrap() {
    setMessage(pageMessage, "info", "Loading promotion workspace...");
    try {
        state.user = await loadCurrentUser();
        applyRoleLabel(roleLabel, state.user);
        createLink.hidden = state.user.role !== "OWNER";
        await loadProductFilter();
        await loadPromotions();
        setMessage(pageMessage, "success", "Promotion directory loaded.");
    } catch (error) {
        state.user = null;
        createLink.hidden = true;
        applyRoleLabel(roleLabel, null);
        renderEmptyTable(error.message || "Unable to load promotions.");
        previewPanel.innerHTML = `<div class="empty-state">${escapeHtml(error.message || "Load a valid token to continue.")}</div>`;
        setMessage(pageMessage, "error", error.message || "Unable to load promotions.", toMessages(error.errors));
    }
}

function bindEvents() {
    document.addEventListener("promotion-token-changed", async () => {
        state.page = 1;
        await bootstrap();
    });

    document.querySelector("[data-apply-filter]").addEventListener("click", async () => {
        state.page = 1;
        await loadPromotions();
    });

    document.querySelector("[data-reset-filter]").addEventListener("click", async () => {
        filterForm.reset();
        if (productFilter) {
            productFilter.value = "";
        }
        state.page = 1;
        await loadPromotions();
    });

    document.querySelector("[data-page-prev]").addEventListener("click", async () => {
        if (state.page <= 1) {
            return;
        }
        state.page -= 1;
        await loadPromotions();
    });

    document.querySelector("[data-page-next]").addEventListener("click", async () => {
        if (state.page >= state.totalPages) {
            return;
        }
        state.page += 1;
        await loadPromotions();
    });

    body.addEventListener("click", async (event) => {
        const actionButton = event.target.closest("[data-action]");
        if (!actionButton) {
            return;
        }

        const { action, id } = actionButton.dataset;
        if (action === "preview") {
            await loadPreview(id);
            return;
        }
        if (action === "delete") {
            await deletePromotion(id);
        }
    });
}

async function loadProductFilter() {
    const products = await loadProducts();
    const currentValue = productFilter?.value || "";
    fillMultiSelect(productFilter, [
        { value: "", label: "All products" },
        ...products.map((product) => ({
            value: product.id,
            label: `${product.productCode} - ${product.productName}`
        }))
    ], [currentValue]);
    if (productFilter) {
        productFilter.multiple = false;
    }
}

async function loadPromotions() {
    if (!state.user) {
        return;
    }

    const params = new URLSearchParams({
        page: String(state.page),
        pageSize: "20"
    });
    for (const [key, value] of new FormData(filterForm).entries()) {
        if (value) {
            params.set(key, value.toString());
        }
    }

    setMessage(pageMessage, "info", "Refreshing promotion directory...");
    try {
        const payload = await apiRequest(`/api/promotions?${params.toString()}`);
        const data = payload.data;
        state.totalPages = Math.max(data.pagination.totalPages || 1, 1);
        state.page = data.pagination.page || 1;
        renderTable(data.items || []);
        paginationLabel.textContent = `Page ${state.page} of ${state.totalPages}`;
        setMessage(pageMessage, "success", "Promotion directory updated.");
    } catch (error) {
        renderEmptyTable(error.message || "Unable to load promotions.");
        setMessage(pageMessage, "error", error.message || "Unable to load promotions.", toMessages(error.errors));
    }
}

function renderTable(items) {
    if (!items.length) {
        renderEmptyTable("No promotions match the current filters.");
        return;
    }

    const ownerActions = state.user?.role === "OWNER";
    body.innerHTML = items.map((item) => `
        <tr>
            <td>
                <strong>${escapeHtml(item.name)}</strong>
                <div class="muted">${escapeHtml(item.code || "Pending code")}</div>
            </td>
            <td>${escapeHtml(item.promotionType)}</td>
            <td>${escapeHtml(formatMoney(item.discountValue))}</td>
            <td>${escapeHtml(`${formatDate(item.validFrom)} - ${formatDate(item.validTo)}`)}</td>
            <td><span class="pill status-pill ${escapeHtml(item.status.toLowerCase())}">${escapeHtml(item.status)}</span></td>
            <td>${escapeHtml(item.scopeSummary || "All products | All customer groups")}</td>
            <td>
                <div class="row-actions">
                    <button class="button secondary" type="button" data-action="preview" data-id="${escapeHtml(item.id)}">Preview</button>
                    <a class="button subtle" href="./detail.html?id=${encodeURIComponent(item.id)}">Open</a>
                    ${ownerActions ? `<a class="button subtle" href="./form.html?id=${encodeURIComponent(item.id)}">Edit</a>` : ""}
                    ${ownerActions ? `<button class="button danger" type="button" data-action="delete" data-id="${escapeHtml(item.id)}">Delete</button>` : ""}
                </div>
            </td>
        </tr>
    `).join("");
}

function renderEmptyTable(message) {
    body.innerHTML = `<tr><td colspan="7" class="empty-state">${escapeHtml(message)}</td></tr>`;
    paginationLabel.textContent = "Page 1";
}

async function loadPreview(id) {
    previewPanel.innerHTML = `<div class="empty-state">Loading promotion preview...</div>`;
    try {
        const payload = await apiRequest(`/api/promotions/${encodeURIComponent(id)}`);
        const detail = payload.data;
        previewPanel.innerHTML = `
            <div class="meta-list">
                <div class="meta-item">
                    <strong>Name</strong>
                    <span>${escapeHtml(detail.name)}</span>
                </div>
                <div class="meta-item">
                    <strong>Type</strong>
                    <span>${escapeHtml(detail.promotionType)}</span>
                </div>
                <div class="meta-item">
                    <strong>Discount</strong>
                    <span>${escapeHtml(formatMoney(detail.discountValue))}</span>
                </div>
                <div class="meta-item">
                    <strong>Priority</strong>
                    <span>${escapeHtml(String(detail.priority ?? 0))}</span>
                </div>
                <div class="meta-item">
                    <strong>Validity</strong>
                    <span>${escapeHtml(`${formatDate(detail.validFrom)} - ${formatDate(detail.validTo)}`)}</span>
                </div>
                <div class="meta-item">
                    <strong>Status</strong>
                    <span>${escapeHtml(detail.status)}</span>
                </div>
            </div>
            <div class="meta-item">
                <strong>Customer Groups</strong>
                <span>${escapeHtml((detail.customerGroups || []).join(", ") || "All customer groups")}</span>
            </div>
            <div class="meta-item">
                <strong>Products</strong>
                <span>${escapeHtml((detail.products || []).map((product) => product.productCode).join(", ") || "All products")}</span>
            </div>
            <div class="meta-item">
                <strong>Audit Trail</strong>
                <span>Created by ${escapeHtml(detail.createdBy || "System")} | Updated by ${escapeHtml(detail.updatedBy || "System")}</span>
                <div class="muted">Created ${escapeHtml(formatTimestamp(detail.createdAt))}</div>
                <div class="muted">Updated ${escapeHtml(formatTimestamp(detail.updatedAt))}</div>
            </div>
            <a class="button secondary" href="./detail.html?id=${encodeURIComponent(detail.id)}">Open full detail</a>
        `;
    } catch (error) {
        previewPanel.innerHTML = `<div class="message error">${escapeHtml(error.message || "Unable to load preview.")}</div>`;
    }
}

async function deletePromotion(id) {
    if (state.user?.role !== "OWNER") {
        return;
    }

    const confirmed = window.confirm("Delete this promotion? The system will soft-delete it and keep audit history.");
    if (!confirmed) {
        return;
    }

    try {
        await apiRequest(`/api/promotions/${encodeURIComponent(id)}`, { method: "DELETE" });
        previewPanel.innerHTML = `<div class="empty-state">Choose a promotion to preview the full configuration.</div>`;
        setMessage(pageMessage, "success", "Promotion deleted successfully.");
        await loadPromotions();
    } catch (error) {
        setMessage(pageMessage, "error", error.message || "Delete failed.", toMessages(error.errors));
    }
}
