import { LuPlus, LuTriangleAlert, LuInfo, LuChevronLeft } from "react-icons/lu";
import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-toastify";
import userService from "../../services/userService";
import { extractError } from "../../services/api";
import PageWrapper from "../../components/layout/PageWrapper";
import Input from "../../components/ui/Input";
import Button from "../../components/ui/Button";
import Spinner from "../../components/ui/Spinner";

export default function CreateUser() {
  const navigate = useNavigate();
  const [providers, setProviders] = useState([]);
  const [loadingProviders, setLoadingProviders] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { roleName: "RECEPTIONIST" } });

  const watchRole = watch("roleName");

  useEffect(() => {
    if (watchRole === "PROVIDER") {
      setLoadingProviders(true);
      userService
        .listProviders()
        .then((r) => setProviders(r.data.data ?? []))
        .catch(() => toast.error("Could not load providers."))
        .finally(() => setLoadingProviders(false));
    }
  }, [watchRole]);

  const onSubmit = async (data) => {
    try {
      await userService.createAdminUser({
        username: data.username,
        password: data.password,
        email: data.email,
        roleName: data.roleName,
        providerId:
          data.roleName === "PROVIDER" && data.providerId
            ? Number(data.providerId)
            : undefined,
      });
      toast.success("User account created successfully.");
      navigate("/admin/users");
    } catch (err) {
      const extracted = extractError(err, "Failed to create account.");
      if (typeof extracted === "object" && extracted.fields) {
        Object.entries(extracted.fields).forEach(([field, msg]) =>
          setError(field, { type: "server", message: msg }),
        );
        toast.error("Please fix the errors and try again.");
      } else {
        setError("root", {
          type: "server",
          message:
            typeof extracted === "string" ? extracted : extracted.summary,
        });
      }
    }
  };

  const roleDescriptions = {
    RECEPTIONIST:
      "Can register patients, book appointments, and manage check-ins",
    PROVIDER:
      "Links to an existing clinical provider record. Can view schedule and manage queue",
    ADMIN: "Full system access including user management and analytics",
  };

  return (
    <PageWrapper
      title="Create User Account"
      subtitle="Add a new staff account to the system"
      action={
        <Link to="/admin/users">
          <Button variant="ghost" size="sm" iconComponent={LuChevronLeft}>
            Back to Users
          </Button>
        </Link>
      }
    >
      <div className="max-w-md mx-auto bg-white rounded-2xl shadow-sm border border-slate-100 p-6">
        {errors.root && (
          <div
            className="mb-4 flex items-center gap-2 rounded-lg bg-red-50 border
                            border-red-200 px-3 py-2.5 text-sm text-red-700"
            role="alert"
          >
            <LuTriangleAlert className="w-4 h-4" aria-hidden="true" />{" "}
            {errors.root.message}
          </div>
        )}

        <form
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          className="flex flex-col gap-5"
        >
          {/* Role selector */}
          <div className="flex flex-col gap-1">
            <label
              htmlFor="roleName"
              className="text-sm font-medium text-slate-700"
            >
              Role{" "}
              <span className="text-danger font-bold" aria-hidden="true">
                *
              </span>
            </label>
            <select
              id="roleName"
              className="w-full rounded-lg border border-slate-300 px-4 py-2.5 text-sm
                         min-h-touch bg-white focus:outline-none focus:ring-2 focus:ring-primary
                         focus:border-transparent hover:border-slate-400 transition-all"
              {...register("roleName", { required: "Role is required" })}
            >
              <option value="RECEPTIONIST">Receptionist</option>
              <option value="PROVIDER">
                Provider (links to clinical record)
              </option>
              <option value="ADMIN">Admin</option>
            </select>
            {watchRole && (
              <p className="text-xs text-slate-500 flex items-center gap-1 mt-0.5">
                <LuInfo className="w-4 h-4" aria-hidden="true" />
                {roleDescriptions[watchRole]}
              </p>
            )}
          </div>

          {/* Provider link */}
          {watchRole === "PROVIDER" && (
            <div className="flex flex-col gap-1">
              <label
                htmlFor="providerId"
                className="text-sm font-medium text-slate-700"
              >
                Link to Provider Record{" "}
                <span className="text-danger font-bold" aria-hidden="true">
                  *
                </span>
              </label>
              {loadingProviders ? (
                <Spinner size="sm" />
              ) : (
                <select
                  id="providerId"
                  className={`w-full rounded-lg border px-4 py-2.5 text-sm min-h-touch bg-white
                    focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent
                    transition-all ${
                      errors.providerId
                        ? "border-danger bg-red-50"
                        : "border-slate-300 hover:border-slate-400"
                    }`}
                  {...register("providerId", {
                    required:
                      watchRole === "PROVIDER"
                        ? "Please select the provider record"
                        : false,
                  })}
                >
                  <option value="">Select provider record…</option>
                  {providers.map((p) => (
                    <option key={p.providerId} value={p.providerId}>
                      Dr. {p.firstName} {p.lastName} — {p.departmentName}
                    </option>
                  ))}
                </select>
              )}
              {errors.providerId && (
                <p
                  role="alert"
                  className="text-xs font-medium text-danger flex items-center gap-1"
                >
                  <LuTriangleAlert className="w-4 h-4" aria-hidden="true" />{" "}
                  {errors.providerId.message}
                </p>
              )}
            </div>
          )}

          <div className="border-t border-slate-100 pt-4">
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-4">
              Account Credentials
            </p>
            <div className="flex flex-col gap-4">
              <Input
                id="username"
                label="Username"
                required
                autoComplete="off"
                helpText="3–100 characters"
                error={errors.username?.message}
                {...register("username", {
                  required: "Username is required",
                  minLength: {
                    value: 3,
                    message: "Username must be at least 3 characters",
                  },
                })}
              />
              <Input
                id="email"
                label="Email Address"
                type="email"
                required
                autoComplete="off"
                error={errors.email?.message}
                {...register("email", {
                  required: "Email is required",
                  pattern: {
                    value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                    message: "Enter a valid email address",
                  },
                })}
              />
              <Input
                id="password"
                label="Temporary Password"
                type="password"
                required
                autoComplete="new-password"
                helpText="Minimum 8 characters — staff should change this on first login"
                showPasswordToggle={true}
                error={errors.password?.message}
                {...register("password", {
                  required: "Password is required",
                  minLength: {
                    value: 8,
                    message: "Password must be at least 8 characters",
                  },
                })}
              />
            </div>
          </div>

          <div className="flex gap-3 pt-1">
            <Button
              type="submit"
              fullWidth
              loading={isSubmitting}
              iconComponent={LuPlus}
            >
              Create Account
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={() => navigate("/admin/users")}
            >
              Cancel
            </Button>
          </div>
        </form>
      </div>
    </PageWrapper>
  );
}
