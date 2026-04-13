export default function Card({
  children,
  className = "",
  title,
  action,
  noPad = false,
}) {
  return (
    <div
      className={`glass-card rounded-2xl ${noPad ? "" : "p-6 md:p-8"} ${className}`}
      role="region"
    >
      {(title || action) && (
        <div className="flex items-center justify-between mb-6 gap-3 flex-wrap border-b border-slate-100 pb-4">
          {title && (
            <h2 className="text-xl font-bold text-slate-800 tracking-tight">
              {title}
            </h2>
          )}
          {action && <div>{action}</div>}
        </div>
      )}
      {children}
    </div>
  );
}
