import api from "./api";

const queueService = {
  checkIn: (appointmentId) => api.post("/queue/check-in", { appointmentId }),
  getQueueEntry: (appointmentId) =>
    api.get(`/queue/entries/appointment/${appointmentId}`),
  getEntries: (queueId) => api.get(`/queue/${queueId}/entries`),
  getQueue: () => api.get("/providers/queue"),
  callNext: (queueId) => api.post(`/queue/${queueId}/call-next`),
  startServing: (entryId) => api.patch(`/queue/entries/${entryId}/serving`),
  complete: (entryId) => api.patch(`/queue/entries/${entryId}/complete`),
  markMissed: (entryId) => api.patch(`/queue/entries/${entryId}/missed`),
  updateStatus: (queueId, data) => api.patch(`/queue/${queueId}/status`, data),
};

export default queueService;
