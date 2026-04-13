export default function Spinner({
  fullScreen = false,
  size = "md",
  label = "Loading…",
}) {
  const sizes = {
    sm: "h-5 w-5 border-2",
    md: "h-9 w-9 border-[3px]",
    lg: "h-14 w-14 border-4",
  };

  const spinner = (
    <div
      role="status"
      aria-label={label}
      className="flex flex-col items-center justify-center gap-3"
    >
      <div
        className={`${sizes[size]} animate-spin rounded-full border-primary border-t-transparent`}
      />
      {size !== "sm" && <p className="text-sm text-slate-500">{label}</p>}
      <span className="sr-only">{label}</span>
    </div>
  );

  if (fullScreen) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-white/90 backdrop-blur-sm">
        {spinner}
      </div>
    );
  }

  return (
    <div className="flex items-center justify-center py-12">{spinner}</div>
  );
}
