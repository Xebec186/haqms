import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { useAppointments } from "../../hooks/useAppointments";
import { extractError } from "../../services/api";
import queueService from "../../services/queueService";
import PageWrapper from "../../components/layout/PageWrapper";
import AppointmentCard from "../../components/features/AppointmentCard";
import Modal from "../../components/ui/Modal";
import Button from "../../components/ui/Button";
import { LuPlus, LuInbox } from "react-icons/lu";
import Spinner from "../../components/ui/Spinner";
import Alert from "../../components/ui/Alert";

export default function MyAppointments() {
  const { appointments, loading, error, fetchMyAppointments, cancel } =
    useAppointments();
  const [cancelId, setCancelId] = useState(null);
  const [cancelReason, setReason] = useState("");
  const [cancelling, setCancelling] = useState(false);
  const [checkingIn, setCheckingIn] = useState(null);
  const [checkedInEntries, setCheckedInEntries] = useState(new Map());
  const navigate = useNavigate();

  useEffect(() => {
    fetchMyAppointments();
  }, [fetchMyAppointments]);

  // Detect which upcoming appointments have already been checked in
  useEffect(() => {
    let mounted = true;
    const upcoming = appointments.filter((a) =>
      ["SCHEDULED", "CONFIRMED"].includes(a.status),
    );
    if (upcoming.length === 0) {
      setCheckedInEntries(new Map());
      return;
    }

    (async () => {
      const results = await Promise.all(
        upcoming.map(async (a) => {
          try {
            const res = await queueService.getQueueEntry(a.appointmentId);
            return [a.appointmentId, res.data.data];
          } catch (err) {
            return [a.appointmentId, null];
          }
        }),
      );
      if (!mounted) return;
      const map = new Map(
        results.filter(([, v]) => v).map(([id, v]) => [id, v]),
      );
      setCheckedInEntries(map);
    })();

    return () => (mounted = false);
  }, [appointments]);

  const handleCheckIn = async (apptId) => {
    setCheckingIn(apptId);
    try {
      const { data } = await queueService.checkIn(apptId);
      const entry = data.data;
      toast.success(`Checked in! Your queue number is ${entry?.queuePosition}`);
      navigate(`/patient/queue/${apptId}`);
    } catch (err) {
      const msg = extractError(err, "Check-in failed. Please try again.");
      toast.error(typeof msg === "object" ? msg.summary : msg);
    } finally {
      setCheckingIn(null);
    }
  };

  const handleConfirmCancel = async () => {
    if (!cancelReason.trim()) {
      toast.error("Please provide a reason for cancellation.");
      return;
    }
    setCancelling(true);
    try {
      await cancel(cancelId, cancelReason);
      setCancelId(null);
      setReason("");
    } catch (err) {
      const msg = extractError(err, "Cancellation failed.");
      toast.error(typeof msg === "object" ? msg.summary : msg);
    } finally {
      setCancelling(false);
    }
  };

  const upcoming = appointments.filter((a) =>
    ["SCHEDULED", "CONFIRMED"].includes(a.status),
  );
  const past = appointments.filter((a) =>
    ["COMPLETED", "CANCELLED", "NO_SHOW"].includes(a.status),
  );

  return (
    <PageWrapper
      title="My Appointments"
      subtitle="View and manage your bookings"
      action={
        <Link to="/patient/book">
          <Button iconComponent={LuPlus}>Book New</Button>
        </Link>
      }
    >
      {loading && <Spinner />}
      {error && <Alert variant="error" message={error} className="mb-4" />}

      {!loading && appointments.length === 0 && !error && (
        <div className="text-center py-20 bg-white rounded-2xl border border-slate-100">
          <LuInbox
            className="mx-auto w-16 h-16 text-slate-300 mb-4"
            aria-hidden="true"
          />
          <h2 className="text-lg font-semibold text-slate-700 mb-2">
            No appointments yet
          </h2>
          <p className="text-slate-500 text-sm mb-5">
            Book your first appointment to get started.
          </p>
          <Link to="/patient/book">
            <Button>Book an Appointment</Button>
          </Link>
        </div>
      )}

      {upcoming.length > 0 && (
        <section className="mb-8">
          <h2 className="text-xs font-semibold text-slate-500 uppercase tracking-widest mb-3 flex items-center gap-2">
            <span
              className="inline-block h-2 w-2 rounded-full bg-primary"
              aria-hidden="true"
            />
            Upcoming ({upcoming.length})
          </h2>
          <div className="grid gap-3 sm:grid-cols-2">
            {upcoming.map((a) => (
              <AppointmentCard
                key={a.appointmentId}
                appt={a}
                onCancel={() => setCancelId(a.appointmentId)}
                onCheckIn={handleCheckIn}
                checkedIn={checkedInEntries.has(a.appointmentId)}
                onViewQueue={() =>
                  navigate(`/patient/queue/${a.appointmentId}`)
                }
              />
            ))}
          </div>
        </section>
      )}

      {past.length > 0 && (
        <section>
          <h2 className="text-xs font-semibold text-slate-500 uppercase tracking-widest mb-3 flex items-center gap-2">
            <span
              className="inline-block h-2 w-2 rounded-full bg-slate-300"
              aria-hidden="true"
            />
            Past ({past.length})
          </h2>
          <div className="grid gap-3 sm:grid-cols-2">
            {past.map((a) => (
              <AppointmentCard
                key={a.appointmentId}
                appt={a}
                onCancel={() => {}}
                onCheckIn={() => {}}
              />
            ))}
          </div>
        </section>
      )}

      {/* Cancel modal */}
      <Modal
        open={!!cancelId}
        title="Cancel Appointment"
        onClose={() => {
          setCancelId(null);
          setReason("");
        }}
      >
        <p className="text-sm text-slate-600 mb-3">
          Please provide a reason so the hospital can update their schedule:
        </p>
        <textarea
          rows={3}
          value={cancelReason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="e.g. I have recovered, unable to travel, rescheduling…"
          aria-label="Cancellation reason"
          className="w-full border border-slate-300 rounded-lg px-3 py-2.5 text-sm mb-4
                     focus:ring-2 focus:ring-primary focus:border-transparent resize-none
                     hover:border-slate-400 transition-all"
        />
        <div className="flex gap-3">
          <Button
            variant="danger"
            fullWidth
            loading={cancelling}
            onClick={handleConfirmCancel}
          >
            Confirm Cancellation
          </Button>
          <Button
            variant="secondary"
            fullWidth
            onClick={() => {
              setCancelId(null);
              setReason("");
            }}
          >
            Keep Appointment
          </Button>
        </div>
      </Modal>
    </PageWrapper>
  );
}
