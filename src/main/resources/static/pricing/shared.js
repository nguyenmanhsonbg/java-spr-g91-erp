const TOKEN_KEY = "g90.pricing.token";
const VND_FORMATTER = new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0
});

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
    const roleLabel = root.querySelector("[data-user-role]");

    if (input) {
        input.value = getToken();
    }

    saveButton?.addEventListener("click", () => {
        setToken(input?.value || "");
        root.dispatchEvent(new CustomEvent("pricing-token-changed"));
    });

    clearButton?.addEventListener("click", () => {
        setToken("");
        if (input) {
            input.value = "";
        }
        if (roleLabel) {
            roleLabel.textContent = "Role unknown";
        }
        root.dispatchEvent(new CustomEvent("pricing-token-changed"));
    });
}

export async function apiRequest(path, { method = "GET", body, auth = true } = {}) {
    const headers = {
        Accept: "application/json"
    };

    if (auth) {
        const token = getToken();
        if (!token) {
            throw { status: 401, message: "Paste an access token to continue." };
        }
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

export async function loadCurrentUser() {
    const payload = await apiRequest("/api/users/me");
    return payload.data;
}

export async function loadProducts() {
    const payload = await apiRequest("/api/products?page=1&pageSize=100&status=ACTIVE", { auth: false });
    return payload.data?.items || [];
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

export function formatDate(value) {
    if (!value) {
        return "Open";
    }
    return new Date(`${value}T00:00:00`).toLocaleDateString("vi-VN");
}

export function formatTimestamp(value) {
    if (!value) {
        return "N/A";
    }
    return new Date(value).toLocaleString("vi-VN");
}

export function formatMoney(value) {
    if (value === null || value === undefined || value === "") {
        return "N/A";
    }
    return VND_FORMATTER.format(Number(value));
}

export function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

export function applyRoleLabel(element, user) {
    if (!element) {
        return;
    }
    element.textContent = user ? `${user.role} · ${user.email}` : "Role unknown";
}

function normalizeError(status, payload) {
    const errors = Array.isArray(payload?.errors) ? payload.errors : [];
    return {
        status,
        code: payload?.code || "ERROR",
        message: payload?.message || "Request failed.",
        errors
    };
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
