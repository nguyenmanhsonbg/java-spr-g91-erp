import {
    apiRequest,
    applyRoleLabel,
    escapeHtml,
    formatDate,
    formatMoney,
    formatTimestamp,
    initTokenControls,
    loadCurrentUser,
    setMessage,
    toMessages
} from "./shared.js";

const promotionId = new URLSearchParams(window.location.search).get("id");
const pageMessage = document.querySelector("[data-page-message]");
const roleLabel = document.querySelector("[data-user-role]");
const detailPanel = document.querySelector("[data-detail-panel]");
const editLink = document.querySelector("[data-edit-link]");

initTokenControls(document);
document.addEventListener("promotion-token-changed", bootstrap);
bootstrap();

async function bootstrap() {
    if (!promotionId) {
        detailPanel.innerHTML = `<div class="empty-state">Missing promotion id.</div>`;
        return;
    }

    setMessage(pageMessage, "info", "Loading promotion detail...");
    try {
        const user = await loadCurrentUser();
        applyRoleLabel(roleLabel, user);
        editLink.hidden = user.role !== "OWNER";
        editLink.href = `./form.html?id=${encodeURIComponent(promotionId)}`;

        const payload = await apiRequest(`/api/promotions/${encodeURIComponent(promotionId)}`);
        renderDetail(payload.data);
        setMessage(pageMessage, "success", "Promotion detail loaded.");
    } catch (error) {
        editLink.hidden = true;
        applyRoleLabel(roleLabel, null);
        detailPanel.innerHTML = `<div class="message error">${escapeHtml(error.message || "Unable to load detail.")}</div>`;
        setMessage(pageMessage, "error", error.message || "Unable to load detail.", toMessages(error.errors));
    }
}

function renderDetail(detail) {
    detailPanel.innerHTML = `
        <div class="meta-list">
            <div class="meta-item">
                <strong>Code</strong>
                <span>${escapeHtml(detail.code || "Pending code")}</span>
            </div>
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
                <strong>Status</strong>
                <span>${escapeHtml(detail.status)}</span>
            </div>
            <div class="meta-item">
                <strong>Validity</strong>
                <span>${escapeHtml(`${formatDate(detail.validFrom)} - ${formatDate(detail.validTo)}`)}</span>
            </div>
        </div>
        <div class="meta-item">
            <strong>Description</strong>
            <span>${escapeHtml(detail.description || "No description")}</span>
        </div>
        <div class="meta-item">
            <strong>Customer Groups</strong>
            <span>${escapeHtml((detail.customerGroups || []).join(", ") || "All customer groups")}</span>
        </div>
        <div class="table-wrap">
            <table>
                <thead>
                <tr>
                    <th>Product Code</th>
                    <th>Product Name</th>
                </tr>
                </thead>
                <tbody>
                ${(detail.products || []).map((product) => `
                    <tr>
                        <td>${escapeHtml(product.productCode || "")}</td>
                        <td>${escapeHtml(product.productName || "")}</td>
                    </tr>
                `).join("") || `<tr><td colspan="2" class="empty-state">All active products are eligible for this promotion.</td></tr>`}
                </tbody>
            </table>
        </div>
        <div class="meta-item">
            <strong>Audit Trail</strong>
            <span>Created by ${escapeHtml(detail.createdBy || "System")} | Updated by ${escapeHtml(detail.updatedBy || "System")}</span>
            <div class="muted">Created ${escapeHtml(formatTimestamp(detail.createdAt))}</div>
            <div class="muted">Updated ${escapeHtml(formatTimestamp(detail.updatedAt))}</div>
        </div>
    `;
}
