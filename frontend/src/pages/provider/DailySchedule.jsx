import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import dayjs from "dayjs";
import appointmentService from "../../services/appointmentService";
import queueService from "../../services/queueService";
import { useAuth } from "../../hooks/useAuth";
import PageWrapper from "../../components/layout/PageWrapper";
import Card from "../../components/ui/Card";
import Badge from "../../components/ui/Badge";
import PriorityBadge from "../../components/features/PriorityBadge";
import Button from "../../components/ui/Button";
import Spinner from "../../components/ui/Spinner";
import Alert from "../../components/ui/Alert";
import { LuInbox } from "react-icons/lu";
import { fmtTime } from "../../utils/formatters";

export default function DailySchedule() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [selectedDate, setSelectedDate] = useState(
    dayjs().format("YYYY-MM-DD"),
  );
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [checkingIn, setCheckingIn] = useState(null);
  const isAppointmentToday = dayjs(selectedDate).isSame(dayjs(), "day");

  useEffect(() => {
    if (!user?.providerId) return;
    setLoading(true);
    setError(null);
    appointmentService
      .getByProviderDate(user.providerId, selectedDate)
      .then((r) => setAppointments(r.data.data ?? []))
      .catch(() => setError("Could not load schedule. Please try again."))
      .finally(() => setLoading(false));
  }, [selectedDate, user?.providerId]);

  const handleCheckIn = async (apptId) => {
    setCheckingIn(apptId);
    try {
      const { data } = await queueService.checkIn(apptId);
      const entry = data.data;
      toast.success(
        `Patient checked in at queue position ${entry?.queuePosition}`,
      );
      if (entry?.queueId) navigate(`/provider/queue/${entry.queueId}`);
    } catch (err) {
      toast.error(err.response?.data?.message ?? "Check-in failed.");
    } finally {
      setCheckingIn(null);
    }
  };

  return (
    <PageWrapper title="Today's Schedule">
      {/* Date picker */}
      <div className="flex items-center gap-3 mb-6">
        <label
          htmlFor="scheduleDate"
          className="text-sm font-medium text-gray-700 shrink-0"
        >
          Viewing date:
        </label>
        <input
          id="scheduleDate"
          type="date"
          value={selectedDate}
          onChange={(e) => setSelectedDate(e.target.value)}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-primary min-h-touch"
        />
      </div>

      {loading && <Spinner />}
      {error && <Alert variant="error" message={error} />}

      {!loading && appointments.length === 0 && !error && (
        <div className="text-center py-16 text-gray-500">
          <LuInbox
            className="mx-auto w-12 h-12 text-slate-300 mb-3"
            aria-hidden="true"
          />
          <p>
            No appointments scheduled for{" "}
            {dayjs(selectedDate).format("DD MMM YYYY")}.
          </p>
        </div>
      )}

      {appointments.length > 0 && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-x-auto">
          <table
            className="w-full text-sm"
            aria-label="Daily appointment schedule"
          >
            <thead className="bg-gray-50 text-gray-600 uppercase text-xs">
              <tr>
                <th className="px-4 py-3 text-left">Patient</th>
                <th className="px-4 py-3 text-left">Reason</th>
                <th className="px-4 py-3 text-left">Priority</th>
                <th className="px-4 py-3 text-left">Status</th>
                <th className="px-4 py-3 text-left">Action</th>
              </tr>
            </thead>
            <tbody>
              {appointments.map((a, i) => (
                <tr
                  key={a.appointmentId}
                  className={`border-b border-gray-100 hover:bg-gray-50 transition-colors ${
                    i % 2 === 0 ? "" : "bg-gray-50/40"
                  }`}
                >
                  <td className="px-4 py-3">
                    {a.patientFirstName} {a.patientLastName}
                  </td>
                  <td className="px-4 py-3 text-gray-500 max-w-xs truncate">
                    {a.reason}
                  </td>
                  <td className="px-4 py-3">
                    <PriorityBadge priority={a.appointmentPriority} />
                  </td>
                  <td className="px-4 py-3">
                    <Badge label={a.status} />
                  </td>
                  <td className="px-4 py-3">
                    {a.status === "SCHEDULED" && (
                      <Button
                        size="sm"
                        loading={checkingIn === a.appointmentId}
                        onClick={() => handleCheckIn(a.appointmentId)}
                        disabled={isAppointmentToday ? false : true}
                      >
                        Check In
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </PageWrapper>
  );
}
