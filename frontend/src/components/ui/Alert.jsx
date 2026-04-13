import { LuTriangleAlert, LuCheck, LuInfo, LuX } from "react-icons/lu";

const variants = {
  error: {
    bg: "bg-red-50 border-red-200",
    text: "text-red-800",
    iconComponent: LuX,
  },
  warning: {
    bg: "bg-amber-50 border-amber-200",
    text: "text-amber-800",
    iconComponent: LuTriangleAlert,
  },
  success: {
    bg: "bg-emerald-50 border-emerald-200",
    text: "text-emerald-800",
    iconComponent: LuCheck,
  },
  info: {
    bg: "bg-blue-50 border-blue-200",
    text: "text-blue-800",
    iconComponent: LuInfo,
  },
};

export default function Alert({ variant = "info", message, onDismiss, title }) {
  if (!message) return null;
  const v = variants[variant];

  return (
    <div
      role="alert"
      className={`flex items-start gap-3 rounded-xl border px-4 py-3 text-sm ${v.bg} ${v.text}`}
    >
      {v.iconComponent &&
        (() => {
          const Icon = v.iconComponent;
          return (
            <Icon className="mt-0.5 shrink-0 w-5 h-5" aria-hidden="true" />
          );
        })()}
      <div className="flex-1 min-w-0">
        {title && <p className="font-semibold mb-0.5">{title}</p>}
        <p className="leading-relaxed">{message}</p>
      </div>
      {onDismiss && (
        <button
          onClick={onDismiss}
          aria-label="Dismiss alert"
          className="shrink-0 opacity-60 hover:opacity-100 transition-opacity p-0.5"
        >
          <LuX className="w-4 h-4" />
        </button>
      )}
    </div>
  );
}
