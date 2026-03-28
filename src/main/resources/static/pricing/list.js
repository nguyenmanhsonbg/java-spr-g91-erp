import {
    apiRequest,
    applyRoleLabel,
    escapeHtml,
    formatDate,
    formatMoney,
    formatTimestamp,
    initTokenControls,
    loadCurrentUser,
    setMessage
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
const body = document.querySelector("[data-price-list-body]");
const detailPanel = document.querySelector("[data-detail-panel]");
const paginationLabel = document.querySelector("[data-pagination-label]");
const selectedId = new URLSearchParams(window.location.search).get("selected");

initTokenControls(document);
bindEvents();
bootstrap();

async function bootstrap() {
    setMessage(pageMessage, "info", "Loading pricing workspace...");
    try {
        state.user = await loadCurrentUser();
        applyRoleLabel(roleLabel, state.user);
        createLink.hidden = state.user.role !== "OWNER";
        await loadPriceLists();
        if (selectedId) {
            await loadDetail(selectedId);
        }
        setMessage(pageMessage, "success", "Price list data loaded.");
    } catch (error) {
        createLink.hidden = true;
        applyRoleLabel(roleLabel, null);
        renderEmptyTable(error.message || "Unable to load price lists.");
        detailPanel.innerHTML = `<div class="empty-state">${escapeHtml(error.message || "Load a valid token to inspect details.")}</div>`;
        setMessage(pageMessage, "error", error.message || "Unable to load price lists.", toMessages(error.errors));
    }
}

function bindEvents() {
    document.addEventListener("pricing-token-changed", async () => {
        state.page = 1;
        await bootstrap();
    });

    document.querySelector("[data-apply-filter]").addEventListener("click", async () => {
        state.page = 1;
        await loadPriceLists();
    });

    document.querySelector("[data-reset-filter]").addEventListener("click", async () => {
        filterForm.reset();
        state.page = 1;
        await loadPriceLists();
    });

    document.querySelector("[data-page-prev]").addEventListener("click", async () => {
        if (state.page <= 1) {
            return;
        }
        state.page -= 1;
        await loadPriceLists();
    });

    document.querySelector("[data-page-next]").addEventListener("click", async () => {
        if (state.page >= state.totalPages) {
            return;
        }
        state.page += 1;
        await loadPriceLists();
    });

    body.addEventListener("click", async (event) => {
        const target = event.target.closest("[data-action]");
        if (!target) {
            return;
        }

        const id = target.dataset.id;
        const action = target.dataset.action;

        if (action === "view") {
            await loadDetail(id);
            return;
        }

        if (action === "delete") {
            await deletePriceList(id);
        }
    });
}

async function loadPriceLists() {
    if (!state.user) {
        return;
    }

    const params = new URLSearchParams({
        page: String(state.page),
        size: "20"
    });
    for (const [key, value] of new FormData(filterForm).entries()) {
        if (value) {
            params.set(key, value.toString());
        }
    }

    setMessage(pageMessage, "info", "Refreshing price list directory...");
    try {
        const payload = await apiRequest(`/api/price-lists?${params.toString()}`);
        const data = payload.data;
        state.totalPages = Math.max(data.pagination.totalPages || 1, 1);
        state.page = data.pagination.page || 1;
        renderTable(data.items || []);
        paginationLabel.textContent = `Page ${state.page} of ${state.totalPages}`;
        setMessage(pageMessage, "success", "Directory updated.");
    } catch (error) {
        renderEmptyTable(error.message || "Unable to load price lists.");
        setMessage(pageMessage, "error", error.message || "Unable to load price lists.", toMessages(error.errors));
    }
}

function renderTable(items) {
    if (!items.length) {
        renderEmptyTable("No price lists match the current filters.");
        return;
    }

    const ownerActions = state.user?.role === "OWNER";
    body.innerHTML = items.map((item) => `
        <tr>
            <td>
                <strong>${escapeHtml(item.name)}</strong>
                <div class="muted">Updated ${escapeHtml(formatTimestamp(item.updatedAt))}</div>
            </td>
            <td>${escapeHtml(item.customerGroup)}</td>
            <td>${escapeHtml(`${formatDate(item.validFrom)} - ${formatDate(item.validTo)}`)}</td>
            <td><span class="pill status-pill ${item.status.toLowerCase()}">${escapeHtml(item.status)}</span></td>
            <td>${escapeHtml(String(item.itemCount))}</td>
            <td>
                <div class="row-actions">
                    <button class="button secondary" type="button" data-action="view" data-id="${escapeHtml(item.id)}">View</button>
                    ${ownerActions ? `<a class="button subtle" href="./form.html?id=${encodeURIComponent(item.id)}">Edit</a>` : ""}
                    ${ownerActions ? `<button class="button danger" type="button" data-action="delete" data-id="${escapeHtml(item.id)}">Delete</button>` : ""}
                </div>
            </td>
        </tr>
    `).join("");
}

function renderEmptyTable(message) {
    body.innerHTML = `<tr><td colspan="6" class="empty-state">${escapeHtml(message)}</td></tr>`;
    paginationLabel.textContent = "Page 1";
}

async function loadDetail(id) {
    detailPanel.innerHTML = `<div class="empty-state">Loading price list detail...</div>`;
    try {
        const payload = await apiRequest(`/api/price-lists/${encodeURIComponent(id)}`);
        const detail = payload.data;
        detailPanel.innerHTML = `
            <div class="meta-list">
                <div class="meta-item">
                    <strong>Name</strong>
                    <span>${escapeHtml(detail.name)}</span>
                </div>
                <div class="meta-item">
                    <strong>Group</strong>
                    <span>${escapeHtml(detail.customerGroup)}</span>
                </div>
                <div class="meta-item">
                    <strong>Status</strong>
                    <span>${escapeHtml(detail.status)}</span>
                </div>
                <div class="meta-item">
                    <strong>Validity</strong>
                    <span>${escapeHtml(`${formatDate(detail.validFrom)} - ${formatDate(detail.validTo)}`)}</span>
                </div>
            </div>
            <div class="meta-item">
                <strong>Audit Trail</strong>
                <span>Created by ${escapeHtml(detail.createdBy || "System")} · Updated by ${escapeHtml(detail.updatedBy || "System")}</span>
                <div class="muted">Created ${escapeHtml(formatTimestamp(detail.createdAt))}</div>
                <div class="muted">Updated ${escapeHtml(formatTimestamp(detail.updatedAt))}</div>
            </div>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Product</th>
                        <th>Unit Price</th>
                        <th>Note</th>
                    </tr>
                    </thead>
                    <tbody>
                    ${(detail.items || []).map((item) => `
                        <tr>
                            <td>
                                <strong>${escapeHtml(item.productCode || item.productId)}</strong>
                                <div class="muted">${escapeHtml(item.productName || "")}</div>
                            </td>
                            <td>${escapeHtml(formatMoney(item.unitPriceVnd))}</td>
                            <td>${escapeHtml(item.note || "No note")}</td>
                        </tr>
                    `).join("") || `<tr><td colspan="3" class="empty-state">No active price items.</td></tr>`}
                    </tbody>
                </table>
            </div>
        `;
    } catch (error) {
        detailPanel.innerHTML = `<div class="message error">${escapeHtml(error.message || "Unable to load detail.")}</div>`;
    }
}

async function deletePriceList(id) {
    if (state.user?.role !== "OWNER") {
        return;
    }
    const confirmed = window.confirm("Delete this price list? The system will soft-delete it and keep audit history.");
    if (!confirmed) {
        return;
    }

    try {
        await apiRequest(`/api/price-lists/${encodeURIComponent(id)}`, { method: "DELETE" });
        setMessage(pageMessage, "success", "Price list deleted successfully.");
        detailPanel.innerHTML = `<div class="empty-state">Choose a price list to see item-level pricing.</div>`;
        await loadPriceLists();
    } catch (error) {
        setMessage(pageMessage, "error", error.message || "Delete failed.", toMessages(error.errors));
    }
}

function toMessages(errors = []) {
    return errors.map((error) => error.field ? `${error.field}: ${error.message}` : error.message);
}
