const TOKEN_KEY = "g90.products.token";

export function getToken() {
    return localStorage.getItem(TOKEN_KEY)?.trim() || "";
}

export function setToken(token) {
    if (token && token.trim()) {
        localStorage.setItem(TOKEN_KEY, token.trim());
        return;
    }
    localStorage.removeItem(TOKEN_KEY);
}

export function initTokenControls(root = document) {
    const input = root.querySelector("[data-token-input]");
    const saveButton = root.querySelector("[data-token-save]");
    const clearButton = root.querySelector("[data-token-clear]");

    if (input) {
        input.value = getToken();
    }

    saveButton?.addEventListener("click", () => {
        setToken(input?.value || "");
        root.dispatchEvent(new CustomEvent("product-token-changed"));
    });

    clearButton?.addEventListener("click", () => {
        setToken("");
        if (input) {
            input.value = "";
        }
        root.dispatchEvent(new CustomEvent("product-token-changed"));
    });
}

export async function apiRequest(path, { method = "GET", body, auth = false } = {}) {
    const headers = {
        Accept: "application/json"
    };

    const token = getToken();
    if (auth === true) {
        if (!token) {
            throw {
                status: 401,
                code: "UNAUTHORIZED",
                message: "Paste an access token to continue.",
                errors: []
            };
        }
        headers.Authorization = `Bearer ${token}`;
    } else if (auth === "optional" && token) {
        headers.Authorization = `Bearer ${token}`;
    }

    if (body !== undefined) {
        headers["Content-Type"] = "application/json";
    }

    const response = await fetch(path, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body)
    });

    const payload = await safeJson(response);
    if (!response.ok) {
        throw normalizeError(response.status, payload);
    }

    return payload;
}

export async function loadCurrentUserOrNull() {
    if (!getToken()) {
        return null;
    }

    try {
        const payload = await apiRequest("/api/users/me", { auth: true });
        return payload.data || null;
    } catch {
        return null;
    }
}

export function canManageProducts(user) {
    return user?.role === "WAREHOUSE";
}

export function setMessage(element, type, message, details = []) {
    if (!element) {
        return;
    }

    if (!message) {
        element.hidden = true;
        element.textContent = "";
        return;
    }

    element.hidden = false;
    element.className = `message ${type}`;

    if (!details.length) {
        element.textContent = message;
        return;
    }

    element.innerHTML = `
        <div>${escapeHtml(message)}</div>
        <ul class="summary-list">
            ${details.map((detail) => `<li>${escapeHtml(detail)}</li>`).join("")}
        </ul>
    `;
}

export function applyRoleLabel(element, user) {
    if (!element) {
        return;
    }

    element.textContent = user ? `${user.role} | ${user.email}` : "Guest catalog access";
}

export function clearFieldErrors(root = document) {
    root.querySelectorAll("[data-error-for]").forEach((node) => {
        node.textContent = "";
    });
}

export function applyFieldErrors(root, errors = []) {
    const summary = [];
    errors.forEach((error) => {
        const normalizedField = normalizeErrorField(error.field);
        const slot = root.querySelector(`[data-error-for="${normalizedField}"]`);
        if (slot && !slot.textContent) {
            slot.textContent = error.message;
            return;
        }
        summary.push(`${normalizedField}: ${error.message}`);
    });
    return summary;
}

export function statusLabel(product) {
    if (product?.deletedAt) {
        return "ARCHIVED";
    }
    return product?.status || "ACTIVE";
}

export function statusText(product) {
    const value = statusLabel(product);
    if (value === "ARCHIVED") {
        return "Archived";
    }
    return value === "ACTIVE" ? "Active" : "Inactive";
}

export function formatTimestamp(value) {
    if (!value) {
        return "N/A";
    }
    return new Date(value).toLocaleString("vi-VN");
}

export function formatDecimal(value) {
    if (value === null || value === undefined || value === "") {
        return "N/A";
    }
    const numeric = Number(value);
    if (Number.isNaN(numeric)) {
        return "N/A";
    }
    return numeric.toLocaleString("vi-VN", { maximumFractionDigits: 4 });
}

export function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function normalizeError(status, payload) {
    return {
        status,
        code: payload?.code || "ERROR",
        message: payload?.message || "Request failed.",
        errors: Array.isArray(payload?.errors) ? payload.errors : []
    };
}

function normalizeErrorField(field) {
    if (!field) {
        return "request";
    }
    return field.replace(/\[\d+\]/g, "");
}

async function safeJson(response) {
    const text = await response.text();
    if (!text) {
        return null;
    }

    try {
        return JSON.parse(text);
    } catch {
        return null;
    }
}
