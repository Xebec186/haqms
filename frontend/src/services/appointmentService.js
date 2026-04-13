import api from './api';

const appointmentService = {
  create:              (data)                 => api.post('/appointments', data),
  getById:             (id)                   => api.get(`/appointments/${id}`),
  getMyAppointments:   ()                     => api.get('/appointments/my'),
  getPatientAppointments: (patientId)         => api.get('/appointments/my', { params: { patientId } }),
  getByProviderDate:   (providerId, date)     => api.get('/appointments', { params: { providerId, date } }),
  updateStatus:        (id, data)             => api.patch(`/appointments/${id}/status`, data),
  updatePriority:      (id, data)             => api.patch(`/appointments/${id}/priority`, data),
  delete:              (id)                   => api.delete(`/appointments/${id}`),

  // Reference data
  getDepartments:      ()                     => api.get('/departments'),
  getProvidersByDept:  (deptId)               => api.get(`/departments/${deptId}/providers`),
  getSchedules:        (providerId)           => api.get(`/providers/${providerId}/schedules`),
  createSchedule:      (providerId, data)     => api.post(`/providers/${providerId}/schedules`, data),
  updateSchedule:      (providerId, scheduleId, data) =>
                         api.patch(`/providers/${providerId}/schedules/${scheduleId}`, data),
};

export default appointmentService;
