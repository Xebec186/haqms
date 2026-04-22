import Badge from "../ui/Badge";
import PriorityBadge from "./PriorityBadge";
import Button from "../ui/Button";
import { LuPlay, LuCircleCheck, LuX } from "react-icons/lu";
import { fmtName, fmtTime } from "../../utils/formatters";

export default function QueueEntryRow({
  entry,
  onServing,
  onComplete,
  onMissed,
}) {
  const priorityBg =
    entry.appointmentPriority === "EMERGENCY"
      ? "bg-red-50"
      : entry.appointmentPriority === "URGENT"
        ? "bg-amber-50"
        : "";

  return (
    <tr
      className={`border-b border-slate-100 hover:bg-slate-50 transition-colors ${priorityBg}`}
    >
      <td className="px-4 py-3">
        <span
          className="inline-flex items-center justify-center h-8 w-8 rounded-full
                         bg-primary text-white text-sm font-bold"
        >
          {entry.queuePosition}
        </span>
      </td>
      <td className="px-4 py-3">
        <p className="font-medium text-slate-800 text-sm">
          {fmtName(entry.patientFirstName, entry.patientLastName)}
        </p>
        {entry.appointmentTime && (
          <p className="text-xs text-slate-400">
            {fmtTime(entry.appointmentTime)}
          </p>
        )}
      </td>
      <td className="px-4 py-3">
        <Badge label={entry.status} />
      </td>
      <td className="px-4 py-3">
        <PriorityBadge priority={entry.appointmentPriority} />
      </td>
      {entry.waitMinutes != null && (
        <td className="px-4 py-3 text-sm text-slate-500">
          {entry.waitMinutes}m
        </td>
      )}
      <td className="px-4 py-3">
        <div className="flex gap-2 flex-wrap">
          {/* CALLED → SERVING: provider only */}
          {entry.status === "CALLED" && onServing && (
            <Button
              size="sm"
              iconComponent={LuPlay}
              onClick={() => onServing(entry.entryId)}
            >
              Start
            </Button>
          )}
          {/* SERVING → COMPLETED: provider only */}
          {entry.status === "SERVING" && onComplete && (
            <Button
              size="sm"
              variant="success"
              iconComponent={LuCircleCheck}
              onClick={() => onComplete(entry.entryId)}
            >
              Complete
            </Button>
          )}
          {/* CALLED → MISSED: provider only */}
          {entry.status === "CALLED" && onMissed && (
            <Button
              size="sm"
              variant="danger"
              iconComponent={LuX}
              onClick={() => onMissed(entry.entryId)}
            >
              Missed
            </Button>
          )}
        </div>
      </td>
    </tr>
  );
}
