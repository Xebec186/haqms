import { LuTriangleAlert, LuMegaphone, LuCircleCheck } from "react-icons/lu";

const styles = {
  // Appointment status
  SCHEDULED: "bg-blue-50 text-blue-700 ring-1 ring-blue-200",
  CONFIRMED: "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200",
  CANCELLED: "bg-slate-100 text-slate-500 ring-1 ring-slate-200",
  COMPLETED: "bg-slate-100 text-slate-600 ring-1 ring-slate-200",
  NO_SHOW: "bg-red-50 text-red-600 ring-1 ring-red-200",
  // Queue entry status
  WAITING: "bg-amber-50 text-amber-700 ring-1 ring-amber-200",
  CALLED: "bg-orange-50 text-orange-700 ring-1 ring-orange-200",
  SERVING: "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200",
  MISSED: "bg-red-50 text-red-600 ring-1 ring-red-200",
  // Priority
  EMERGENCY: "bg-red-600 text-white ring-1 ring-red-700",
  URGENT: "bg-amber-500 text-white ring-1 ring-amber-600",
  REGULAR: "bg-blue-50 text-blue-700 ring-1 ring-blue-200",
  // Queue status
  OPEN: "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200",
  PAUSED: "bg-amber-50 text-amber-700 ring-1 ring-amber-200",
  CLOSED: "bg-slate-100 text-slate-500 ring-1 ring-slate-200",
  // Roles
  ADMIN: "bg-purple-50 text-purple-700 ring-1 ring-purple-200",
  PROVIDER: "bg-blue-50 text-blue-700 ring-1 ring-blue-200",
  PATIENT: "bg-teal-50 text-teal-700 ring-1 ring-teal-200",
};

const icons = {
  EMERGENCY: LuTriangleAlert,
  URGENT: LuTriangleAlert,
  CALLED: LuMegaphone,
  SERVING: LuCircleCheck,
};
const variantStyles = {
  success: "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200",
  danger: "bg-red-50 text-red-600 ring-1 ring-red-200",
  warning: "bg-amber-50 text-amber-700 ring-1 ring-amber-200",
  info: "bg-blue-50 text-blue-700 ring-1 ring-blue-200",
  neutral: "bg-slate-100 text-slate-600 ring-1 ring-slate-200",
};

export default function Badge({ label, variant }) {
  const Icon = icons[label];
  const cls =
    styles[label] ??
    (variant ? variantStyles[variant] : null) ??
    variantStyles.neutral;

  return (
    <span
      title={label}
      className={`inline-flex items-center gap-2 px-2.5 py-0.5 rounded-full text-xs font-semibold ${cls}`}
    >
      {Icon && <Icon className="w-4 h-4" aria-hidden="true" />}
      <span className="leading-none">{label}</span>
    </span>
  );
}
