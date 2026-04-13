import { useState, useCallback } from 'react';
import { toast } from 'react-toastify';
import appointmentService from '../services/appointmentService';

export function useAppointments() {
  const [appointments, setAppointments] = useState([]);
  const [loading,      setLoading]      = useState(false);
  const [error,        setError]        = useState(null);

  const fetchMyAppointments = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await appointmentService.getMyAppointments();
      setAppointments(data.data ?? []);
      setError(null);
    } catch {
      setError('Could not load appointments. Please try again.');
    } finally {
      setLoading(false);
    }
  }, []);

  const cancel = useCallback(async (id, reason) => {
    await appointmentService.updateStatus(id, {
      status: 'CANCELLED',
      cancellationReason: reason,
    });
    toast.success('Appointment cancelled successfully.');
    await fetchMyAppointments();
  }, [fetchMyAppointments]);

  return { appointments, loading, error, fetchMyAppointments, cancel };
}
