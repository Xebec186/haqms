import { LuUser, LuClock, LuZap, LuCircleCheck } from "react-icons/lu";

const statusConfig = {
  WAITING: {
    bg: "bg-amber-50",
    border: "border-amber-300",
    label: "Waiting",
    pulse: false,
  },
  CALLED: {
    bg: "bg-orange-50",
    border: "border-orange-400",
    label: "Called!",
    pulse: true,
  },
  SERVING: {
    bg: "bg-emerald-50",
    border: "border-emerald-400",
    label: "In Session",
    pulse: false,
  },
  COMPLETED: {
    bg: "bg-slate-50",
    border: "border-slate-200",
    label: "Completed",
    pulse: false,
  },
  MISSED: {
    bg: "bg-red-50",
    border: "border-red-200",
    label: "Missed",
    pulse: false,
  },
};

export default function QueueStatusCard({ entry, estimatedWait }) {
  if (!entry) {
    return (
      <div className="rounded-2xl border-2 border-dashed border-slate-200 bg-white p-10 text-center">
        <LuUser
          className="mx-auto w-10 h-10 text-slate-400 mb-3"
          aria-hidden="true"
        />
        <p className="font-medium text-slate-600">Not checked in yet</p>
        <p className="text-sm text-slate-400 mt-1">
          Check in at the reception desk or via the app to receive your queue
          number.
        </p>
      </div>
    );
  }

  const cfg = statusConfig[entry.status] ?? statusConfig.WAITING;

  return (
    <div
      role="status"
      aria-live="polite"
      aria-atomic="true"
      className={`rounded-2xl border-2 p-6 text-center transition-all ${cfg.bg} ${cfg.border}
        ${cfg.pulse ? "animate-pulse" : ""}`}
    >
      <p className="text-xs font-semibold text-slate-500 uppercase tracking-widest mb-2">
        Your Queue Number
      </p>

      <div className="relative inline-block">
        <p
          className="text-8xl font-black text-primary leading-none"
          aria-label={`Queue number ${entry.queuePosition}`}
        >
          {entry.queuePosition}
        </p>
      </div>

      <p className="text-lg font-bold text-slate-700 mt-3">{cfg.label}</p>

      {entry.status === "WAITING" && estimatedWait > 0 && (
        <div
          className="mt-3 inline-flex items-center gap-2 bg-white/80 rounded-full
                        px-4 py-1.5 text-sm text-slate-600 border border-amber-200"
        >
          <LuClock className="w-4 h-4 text-slate-400" aria-hidden="true" />
          Estimated wait: <strong>{estimatedWait} min</strong>
        </div>
      )}

      {entry.status === "WAITING" && estimatedWait == null && (
        <p className="text-sm text-slate-400 mt-2">Calculating wait time…</p>
      )}

      {entry.status === "CALLED" && (
        <div className="mt-4">
          <div
            className="bg-orange-500 text-white rounded-xl px-4 py-3 inline-block"
            role="alert"
          >
            <p className="font-bold flex items-center gap-2">
              <LuZap className="w-5 h-5" />
              <span>Please proceed to the consultation room now</span>
            </p>
            <p className="text-sm opacity-90 mt-0.5">
              Your doctor is ready for you
            </p>
          </div>
        </div>
      )}

      {entry.status === "SERVING" && (
        <div
          className="mt-3 bg-emerald-100 text-emerald-800 rounded-xl px-4 py-2
                        inline-block text-sm font-medium flex items-center gap-2 justify-center"
        >
          <LuCircleCheck className="w-4 h-4" />
          <span>You are currently being seen by the doctor</span>
        </div>
      )}

      {entry.status === "COMPLETED" && (
        <div className="mt-3 text-slate-500 text-sm">
          Your consultation is complete. Thank you for your visit.
          {entry.waitMinutes != null && (
            <p className="mt-1">
              Total wait time: <strong>{entry.waitMinutes} min</strong>
            </p>
          )}
        </div>
      )}
    </div>
  );
}
