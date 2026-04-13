import api from "./api";

const userService = {
  // Patient
  getMe: () => api.get("/patients/me"),

  // Provider
  getMyProviderProfile: () => api.get("/providers/me"),

  // Account
  getAccount: () => api.get("/users/me"),
  getPatient: (id) => api.get(`/patients/${id}`),
  updatePatient: (id, data) => api.patch(`/patients/${id}`, data),
  listPatients: (params) => api.get("/patients", { params }),

  // Admin — users
  listUsers: (params) => api.get("/admin/users", { params }),
  getUserById: (id) => api.get(`/admin/users/${id}`),
  updateUserStatus: (id, isActive) =>
    api.patch(`/admin/users/${id}/status`, { isActive }),
  createAdminUser: (data) => api.post("/admin/users", data),

  // Admin — analytics
  getSummary: () => api.get("/admin/analytics/summary"),
  getDeptAnalytics: (from, to) =>
    api.get("/admin/analytics/departments", { params: { from, to } }),

  // Admin — providers
  listProviders: (departmentId) =>
    api.get("/admin/providers", { params: { departmentId } }),
  getProviderById: (id) => api.get(`/admin/providers/${id}`),
  createProvider: (data) => api.post("/admin/providers", data),
  updateProvider: (id, data) => api.patch(`/admin/providers/${id}`, data),
};

export default userService;
