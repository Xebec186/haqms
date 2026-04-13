import { forwardRef, useState } from "react";
import { FiEye, FiEyeOff } from "react-icons/fi";
import { LuInfo, LuTriangleAlert } from "react-icons/lu";

const Input = forwardRef(function Input(
  {
    label,
    id,
    error,
    helpText,
    required,
    className = "",
    prefix,
    type = "text",
    showPasswordToggle = false,
    ...props
  },
  ref,
) {
  const [showPassword, setShowPassword] = useState(false);
  const resolvedType =
    showPasswordToggle && type === "password"
      ? showPassword
        ? "text"
        : "password"
      : type;

  return (
    <div className="flex flex-col gap-1">
      {label && (
        <label htmlFor={id} className="text-sm font-medium text-slate-700">
          {label}
          {required && (
            <span className="ml-1 text-danger font-bold" aria-hidden="true">
              *
            </span>
          )}
        </label>
      )}
      <div className="relative">
        {prefix && (
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm select-none">
            {prefix}
          </span>
        )}
        <input
          id={id}
          ref={ref}
          type={resolvedType}
          aria-describedby={
            error ? `${id}-error` : helpText ? `${id}-help` : undefined
          }
          aria-invalid={!!error}
          aria-required={required}
          className={[
            "w-full rounded-xl border px-4 py-3 text-sm min-h-touch bg-white text-slate-800 backdrop-blur-sm",
            "transition-all duration-200",
            "focus:outline-none focus:ring-4 focus:border-indigo-400 focus:bg-white",
            prefix ? "pl-8" : "",
            showPasswordToggle && type === "password" ? "pr-10" : "",
            error
              ? "border-red-300 bg-red-50 text-slate-900 focus:ring-red-500/20"
              : "border-slate-200 hover:border-slate-300 focus:ring-indigo-500/20 shadow-sm",
            className,
          ]
            .filter(Boolean)
            .join(" ")}
          {...props}
        />

        {showPasswordToggle && type === "password" && (
          <button
            type="button"
            onClick={() => setShowPassword((prev) => !prev)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-indigo-600 focus:outline-none rounded-lg cursor-pointer"
            aria-label={showPassword ? "Hide password" : "Show password"}
          >
            {showPassword ? (
              <FiEyeOff className="h-5 w-5" aria-hidden="true" />
            ) : (
              <FiEye className="h-5 w-5" aria-hidden="true" />
            )}
          </button>
        )}
      </div>
      {helpText && !error && (
        <p
          id={`${id}-help`}
          className="text-xs text-slate-500 flex items-center gap-2"
        >
          <LuInfo className="w-4 h-4 text-slate-400" aria-hidden="true" />{" "}
          {helpText}
        </p>
      )}
      {error && (
        <p
          id={`${id}-error`}
          role="alert"
          className="text-xs font-medium text-danger flex items-center gap-2"
        >
          <LuTriangleAlert
            className="w-4 h-4 text-red-600"
            aria-hidden="true"
          />{" "}
          {error}
        </p>
      )}
    </div>
  );
});

export default Input;
