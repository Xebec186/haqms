export default function StatCard({
  label,
  value,
  icon,
  iconComponent,
  colour = "text-primary",
  bg = "bg-primary-light",
  trend,
}) {
  return (
    <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-5">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-medium text-slate-500 uppercase tracking-wide">
            {label}
          </p>
          <p className={`text-3xl font-bold mt-1 ${colour}`}>{value ?? "—"}</p>
          {trend && <p className="text-xs text-slate-400 mt-1">{trend}</p>}
        </div>
        <div className={`${bg} rounded-xl p-2.5`}>
          {iconComponent ? (
            (() => {
              const Icon = iconComponent;
              return <Icon className="w-7 h-7" aria-hidden="true" />;
            })()
          ) : (
            <span className="text-2xl" aria-hidden="true">
              {icon}
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
