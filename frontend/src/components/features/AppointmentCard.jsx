import { fmtDate, fmtTime } from "../../utils/formatters";
import Badge from "../ui/Badge";
import PriorityBadge from "./PriorityBadge";
import Button from "../ui/Button";
import {
  LuCalendar,
  LuClock,
  LuCircleCheck,
  LuX,
  LuInbox,
} from "react-icons/lu";

export default function AppointmentCard({
  appt,
  onCancel,
  onCheckIn,
  compact = false,
  checkedIn = false,
  onViewQueue,
}) {
  const canCancel = ["SCHEDULED", "CONFIRMED"].includes(appt.status);
  const canCheckIn = appt.status === "SCHEDULED" || appt.status === "CONFIRMED";
  const isAppointmentToday =
    fmtDate(appt.appointmentDate) === fmtDate(new Date());
  const isTerminal = ["COMPLETED", "CANCELLED", "NO_SHOW"].includes(
    appt.status,
  );

  return (
    <article
      className={`bg-white rounded-2xl border shadow-sm transition-shadow hover:shadow-md
      ${isTerminal ? "border-slate-100 opacity-75" : "border-slate-100"}`}
    >
      {/* Top accent bar — colour by priority */}
      {!isTerminal && (
        <div
          className={`h-1 rounded-t-2xl ${
            appt.appointmentPriority === "EMERGENCY"
              ? "bg-danger"
              : appt.appointmentPriority === "URGENT"
                ? "bg-warning"
                : "bg-primary"
          }`}
        />
      )}

      <div className="p-4 md:p-5">
        {/* Header row */}
        <div className="flex items-start justify-between gap-3 mb-3 flex-wrap">
          <div>
            <p className="font-semibold text-slate-800">
              {appt.departmentName}
            </p>
            <p className="text-sm text-slate-500 mt-0.5">
              Dr. {appt.providerFirstName} {appt.providerLastName}
            </p>
          </div>
          <div className="flex gap-2 items-center flex-wrap">
            <PriorityBadge priority={appt.appointmentPriority} />
            <Badge label={appt.status} />
          </div>
        </div>

        {/* Details row */}
        <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm mb-3">
          <div className="flex items-center gap-2 text-slate-600">
            <LuCalendar className="w-4 h-4 text-slate-400" aria-hidden="true" />
            <span>{fmtDate(appt.appointmentDate)}</span>
          </div>
          <div className="flex items-center gap-2 text-slate-600 justify-self-end">
            <LuClock className="w-4 h-4 text-slate-400" aria-hidden="true" />
            <span>
              {fmtTime(appt.appointmentStartTime)} -{" "}
              {fmtTime(appt.appointmentEndTime)}
            </span>
          </div>
        </div>

        {appt.reason && (
          <p className="text-sm text-slate-500 italic line-clamp-1 mb-3 border-t border-slate-50 pt-3">
            "{appt.reason}"
          </p>
        )}

        {/* Actions */}
        {(canCancel || canCheckIn) && !compact && (
          <div className="flex gap-2 pt-1 border-t border-slate-50">
            {canCheckIn &&
              (checkedIn ? (
                <Button
                  variant="secondary"
                  size="sm"
                  iconComponent={LuInbox}
                  onClick={onViewQueue}
                >
                  View Queue
                </Button>
              ) : (
                <Button
                  variant="primary"
                  size="sm"
                  iconComponent={LuCircleCheck}
                  onClick={() => onCheckIn(appt.appointmentId)}
                  disabled={isAppointmentToday ? false : true}
                >
                  Check In
                </Button>
              ))}
            {canCancel && (
              <Button
                variant="ghost"
                size="sm"
                iconComponent={LuX}
                onClick={() => onCancel(appt.appointmentId)}
                className="text-danger hover:bg-red-50"
              >
                Cancel
              </Button>
            )}
          </div>
        )}
      </div>
    </article>
  );
}
