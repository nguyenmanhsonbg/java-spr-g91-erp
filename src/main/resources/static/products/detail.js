import {
    apiRequest,
    applyRoleLabel,
    canManageProducts,
    escapeHtml,
    formatDecimal,
    formatTimestamp,
    initTokenControls,
    loadCurrentUserOrNull,
    setMessage,
    statusLabel,
    statusText
} from "./shared.js";

const params = new URLSearchParams(window.location.search);
const productId = params.get("id");

const roleLabel = document.querySelector("[data-user-role]");
const pageMessage = document.querySelector("[data-page-message]");
const detailShell = document.querySelector("[data-detail-shell]");
const detailHeading = document.querySelector("[data-detail-heading]");
const editLink = document.querySelector("[data-edit-link]");
const deleteButton = document.querySelector("[data-delete-button]");

let currentUser = null;
let currentProduct = null;

initTokenControls(document);
document.addEventListener("product-token-changed", boot);
deleteButton.addEventListener("click", archiveProduct);

boot();

async function boot() {
    currentUser = await loadCurrentUserOrNull();
    applyRoleLabel(roleLabel, currentUser);

    if (!productId) {
        detailShell.innerHTML = `<div class="empty-state">Product ID is missing from the URL.</div>`;
        setMessage(pageMessage, "error", "Product ID is required.");
        editLink.hidden = true;
        deleteButton.hidden = true;
        return;
    }

    await loadProduct();
}

async function loadProduct() {
    try {
        const payload = await apiRequest(`/api/products/${encodeURIComponent(productId)}`, { auth: "optional" });
        currentProduct = payload.data;
        renderProduct(currentProduct);
        setMessage(pageMessage, "info", "");
    } catch (error) {
        currentProduct = null;
        detailHeading.textContent = "Product Details";
        detailShell.innerHTML = `<div class="empty-state">Unable to load the requested product.</div>`;
        editLink.hidden = true;
        deleteButton.hidden = true;
        setMessage(pageMessage, "error", error.message);
    }
}

function renderProduct(product) {
    const warehouseMode = canManageProducts(currentUser);
    const tag = statusLabel(product);

    detailHeading.textContent = product.productName;
    editLink.hidden = !(warehouseMode && !product.deletedAt);
    editLink.href = `./form.html?id=${encodeURIComponent(product.id)}`;
    deleteButton.hidden = !(warehouseMode && !product.deletedAt);

    detailShell.innerHTML = `
        <div class="hero-actions">
            <div class="pill status-pill" data-status="${tag}">${statusText(product)}</div>
            <div class="muted">Last updated ${escapeHtml(formatTimestamp(product.updatedAt || product.createdAt))}</div>
        </div>

        <div class="detail-grid">
            ${detailCard("Product Code", product.productCode)}
            ${detailCard("Type", product.type)}
            ${detailCard("Size", product.size)}
            ${detailCard("Thickness", product.thickness)}
            ${detailCard("Unit", product.unit)}
            ${detailCard("Weight Conversion", formatDecimal(product.weightConversion))}
            ${detailCard("Reference Weight", formatDecimal(product.referenceWeight))}
            ${detailCard("Created", formatTimestamp(product.createdAt))}
        </div>

        <div class="description">${escapeHtml(product.description || "No technical description available.")}</div>

        ${renderImages(product.imageUrls || [])}
    `;
}

function detailCard(label, value) {
    return `
        <div class="detail-card">
            <div class="detail-label">${escapeHtml(label)}</div>
            <div class="detail-value">${escapeHtml(value || "N/A")}</div>
        </div>
    `;
}

function renderImages(imageUrls) {
    if (!imageUrls.length) {
        return `<div class="empty-state">No product images available.</div>`;
    }

    return `
        <div class="gallery">
            ${imageUrls.map((imageUrl, index) => `
                <div class="image-card">
                    <img src="${escapeHtml(imageUrl)}" alt="Product image ${index + 1}">
                    <div class="image-caption">${escapeHtml(imageUrl)}</div>
                </div>
            `).join("")}
        </div>
    `;
}

async function archiveProduct() {
    if (!currentProduct) {
        return;
    }

    const confirmed = window.confirm("Archive this product? The record stays in the system for history but disappears from the public catalog.");
    if (!confirmed) {
        return;
    }

    try {
        await apiRequest(`/api/products/${encodeURIComponent(currentProduct.id)}`, { method: "DELETE", auth: true });
        setMessage(pageMessage, "success", "Product archived successfully.");
        await loadProduct();
    } catch (error) {
        setMessage(pageMessage, "error", error.message);
    }
}
