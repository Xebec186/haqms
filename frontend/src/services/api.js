import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: { "Content-Type": "application/json" },
  timeout: 15000,
});

// ── Request interceptor — attach JWT ─────────────────────────────────────────
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("haqms_token");
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  },
  (error) => Promise.reject(error),
);

// ── Response interceptor ──────────────────────────────
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;

    if (status === 401) {
      const token = localStorage.getItem("haqms_token");

      // Only redirect if user was previously logged in
      if (token) {
        localStorage.removeItem("haqms_token");
        localStorage.removeItem("haqms_user");
        window.location.href = "/login";
      }
    }

    return Promise.reject(error);
  },
);

/**
 * Extracts the most useful error message from a backend error response.
 *
 * The backend returns one of two shapes:
 *   { success: false, message: "string", data: null }          — single message
 *   { success: false, message: "Validation failed", data: {    — field map
 *       firstName: "First name is required",
 *       phoneNumber: "Enter a valid Ghanaian number (+233...)"
 *   }}
 *
 * Returns a single display string, or an object of field→message for forms.
 */
export function extractError(
  error,
  fallback = "Something went wrong. Please try again.",
) {
  const body = error?.response?.data;
  if (!body) return fallback;

  // Field validation map — e.g. from @Valid failures
  if (body.data && typeof body.data === "object" && !Array.isArray(body.data)) {
    const fields = Object.entries(body.data);
    if (fields.length > 0) {
      // Return a readable joined string for toast, plus the raw map for forms
      return {
        summary: fields
          .map(([field, msg]) => `${formatFieldName(field)}: ${msg}`)
          .join(". "),
        fields: body.data,
      };
    }
  }

  // Single message
  if (body.message) return body.message;

  return fallback;
}

function formatFieldName(field) {
  // camelCase → "First Name"
  return field
    .replace(/([A-Z])/g, " $1")
    .replace(/^./, (s) => s.toUpperCase())
    .trim();
}

export default api;
