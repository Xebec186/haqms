const variants = {
  primary:
    "bg-gradient-to-r from-indigo-600 to-blue-600 text-white shadow-lg shadow-indigo-500/30 hover:shadow-indigo-500/50 hover:-translate-y-0.5 focus:ring-indigo-500 disabled:opacity-50 disabled:hover:translate-y-0 disabled:shadow-none",
  secondary:
    "bg-white text-indigo-700 border border-indigo-100 shadow-sm hover:bg-slate-50 hover:border-indigo-200 hover:shadow-md hover:-translate-y-0.5 focus:ring-indigo-500 disabled:opacity-50",
  danger:
    "bg-gradient-to-r from-red-600 to-rose-600 text-white shadow-lg shadow-red-500/30 hover:shadow-red-500/40 hover:-translate-y-0.5 focus:ring-red-500 disabled:opacity-50",
  success:
    "bg-gradient-to-r from-emerald-500 to-teal-500 text-white shadow-lg shadow-emerald-500/30 hover:shadow-emerald-500/40 hover:-translate-y-0.5 focus:ring-emerald-500 disabled:opacity-50",
  ghost:
    "bg-transparent text-slate-600 hover:bg-slate-100/80 hover:text-slate-900 focus:ring-slate-400",
  warning:
    "bg-gradient-to-r from-amber-500 to-orange-500 text-white shadow-lg shadow-amber-500/30 hover:shadow-amber-500/40 hover:-translate-y-0.5 focus:ring-amber-500 disabled:opacity-50",
};

const sizes = {
  sm: "px-4 py-2 text-sm font-medium gap-1.5 rounded-lg",
  md: "px-6 py-2.5 text-sm font-semibold gap-2 rounded-xl",
  lg: "px-8 py-3.5 text-base font-semibold gap-2.5 rounded-2xl",
};

export default function Button({
  children,
  variant = "primary",
  size = "md",
  disabled,
  loading,
  fullWidth,
  type = "button",
  onClick,
  className = "",
  icon,
  iconComponent,
  ...props
}) {
  const Icon = iconComponent;

  return (
    <button
      type={type}
      disabled={disabled || loading}
      onClick={onClick}
      aria-busy={loading || undefined}
      className={[
        "inline-flex items-center justify-center rounded-lg transition-all duration-150",
        "focus:outline-none focus:ring-2 focus:ring-offset-2",
        "min-h-touch disabled:cursor-not-allowed select-none",
        sizes[size],
        variants[variant],
        fullWidth ? "w-full" : "",
        className,
      ]
        .filter(Boolean)
        .join(" ")}
      {...props}
    >
      {loading ? (
        <span
          aria-hidden="true"
          className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent shrink-0"
        />
      ) : Icon ? (
        <Icon className="w-4 h-4 shrink-0" aria-hidden="true" />
      ) : icon ? (
        <span aria-hidden="true" className="shrink-0">
          {icon}
        </span>
      ) : null}

      {children}
    </button>
  );
}
