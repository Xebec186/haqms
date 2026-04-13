import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-toastify";
import userService from "../../services/userService";
import appointmentService from "../../services/appointmentService";
import { extractError } from "../../services/api";
import PageWrapper from "../../components/layout/PageWrapper";
import Input from "../../components/ui/Input";
import Button from "../../components/ui/Button";
import Spinner from "../../components/ui/Spinner";
import { LuPlus, LuUser, LuTriangleAlert, LuChevronLeft } from "react-icons/lu";

export default function CreateProvider() {
  const navigate = useNavigate();
  const [departments, setDepartments] = useState([]);
  const [loadingDepts, setLoadingDepts] = useState(true);
  const [createAccount, setCreateAccount] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { maxSlots: 20 } });

  const watchUsername = watch("username");

  useEffect(() => {
    appointmentService
      .getDepartments()
      .then((r) => setDepartments(r.data.data ?? []))
      .catch(() => toast.error("Could not load departments."))
      .finally(() => setLoadingDepts(false));
  }, []);

  const onSubmit = async (data) => {
    try {
      const payload = {
        departmentId: Number(data.departmentId),
        firstName: data.firstName,
        lastName: data.lastName,
        specialisation: data.specialisation || undefined,
        licenseNumber: data.licenseNumber,
        phoneNumber: data.phoneNumber || undefined,
        email: data.email || undefined,
        username: createAccount ? data.username : undefined,
        password: createAccount ? data.password : undefined,
      };
      await userService.createProvider(payload);
      toast.success(
        createAccount
          ? "Provider registered and login account created."
          : "Provider registered. A login account can be added later from User Management.",
      );
      navigate("/admin/providers");
    } catch (err) {
      const extracted = extractError(err, "Failed to register provider.");
      if (typeof extracted === "object" && extracted.fields) {
        Object.entries(extracted.fields).forEach(([field, msg]) =>
          setError(field, { type: "server", message: msg }),
        );
        toast.error("Please fix the highlighted errors and try again.");
      } else {
        const msg =
          typeof extracted === "object" ? extracted.summary : extracted;
        setError("root", { type: "server", message: msg });
      }
    }
  };

  if (loadingDepts) {
    return (
      <PageWrapper title="Register Provider">
        <Spinner />
      </PageWrapper>
    );
  }

  return (
    <PageWrapper
      title="Register Healthcare Provider"
      subtitle="Add a new doctor or clinical staff member to the system"
      action={
        <Link to="/admin/providers">
          <Button variant="ghost" size="sm" iconComponent={LuChevronLeft}>
            Back to Providers
          </Button>
        </Link>
      }
    >
      <div className="max-w-lg mx-auto">
        {errors.root && (
          <div
            className="mb-4 flex items-center gap-2 rounded-xl bg-red-50 border
                            border-red-200 px-4 py-3 text-sm text-red-700"
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
          {/* ── Clinical details card ─────────────────────────────── */}
          <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-5">
            <h2 className="text-sm font-semibold text-slate-700 mb-4 flex items-center gap-2">
              <LuUser className="w-5 h-5" aria-hidden="true" /> Clinical Details
            </h2>

            <div className="flex flex-col gap-4">
              <div className="grid grid-cols-2 gap-3">
                <Input
                  id="firstName"
                  label="First Name"
                  required
                  placeholder="Kofi"
                  error={errors.firstName?.message}
                  {...register("firstName", {
                    required: "First name is required",
                  })}
                />
                <Input
                  id="lastName"
                  label="Last Name"
                  required
                  placeholder="Mensah"
                  error={errors.lastName?.message}
                  {...register("lastName", {
                    required: "Last name is required",
                  })}
                />
              </div>

              <div className="flex flex-col gap-1">
                <label
                  htmlFor="departmentId"
                  className="text-sm font-medium text-slate-700"
                >
                  Department{" "}
                  <span className="text-danger font-bold" aria-hidden="true">
                    *
                  </span>
                </label>
                <select
                  id="departmentId"
                  className={`w-full rounded-lg border px-4 py-2.5 text-sm min-h-touch bg-white
                    focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent
                    hover:border-slate-400 transition-all ${
                      errors.departmentId
                        ? "border-danger bg-red-50"
                        : "border-slate-300"
                    }`}
                  {...register("departmentId", {
                    required: "Department is required",
                  })}
                >
                  <option value="">Select department…</option>
                  {departments.map((d) => (
                    <option key={d.departmentId} value={d.departmentId}>
                      {d.name}
                    </option>
                  ))}
                </select>
                {errors.departmentId && (
                  <p
                    role="alert"
                    className="text-xs font-medium text-danger flex items-center gap-1"
                  >
                    <LuTriangleAlert className="w-4 h-4" aria-hidden="true" />{" "}
                    {errors.departmentId.message}
                  </p>
                )}
              </div>

              <Input
                id="specialisation"
                label="Specialisation (optional)"
                placeholder="e.g. Cardiologist, General Practitioner, Paediatrician"
                error={errors.specialisation?.message}
                {...register("specialisation")}
              />

              <Input
                id="licenseNumber"
                label="Medical License Number"
                required
                placeholder="GH-MED-123456"
                helpText="Must be unique — issued by the Ghana Medical and Dental Council"
                error={errors.licenseNumber?.message}
                {...register("licenseNumber", {
                  required: "License number is required",
                  pattern: {
                    value: /^[A-Z0-9\-]+$/i,
                    message: "License number contains invalid characters",
                  },
                })}
              />

              <div className="grid grid-cols-2 gap-3">
                <Input
                  id="phoneNumber"
                  label="Phone (optional)"
                  type="tel"
                  placeholder="+233XXXXXXXXX"
                  error={errors.phoneNumber?.message}
                  {...register("phoneNumber", {
                    pattern: {
                      value: /^\+233[0-9]{9}$/,
                      message: "Enter a valid Ghanaian number (+233XXXXXXXXX)",
                    },
                  })}
                />
                <Input
                  id="providerEmail"
                  label="Email (optional)"
                  type="email"
                  placeholder="doctor@hospital.gh"
                  error={errors.email?.message}
                  {...register("email", {
                    pattern: {
                      value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                      message: "Enter a valid email address",
                    },
                  })}
                />
              </div>
            </div>
          </div>

          {/* ── Account creation toggle ───────────────────────────── */}
          <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-5">
            <label className="flex items-start gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={createAccount}
                onChange={(e) => setCreateAccount(e.target.checked)}
                className="h-5 w-5 mt-0.5 rounded border-slate-300 text-primary
                           focus:ring-primary cursor-pointer shrink-0"
              />
              <div>
                <p className="text-sm font-medium text-slate-800">
                  Create a system login account for this provider
                </p>
                <p className="text-xs text-slate-400 mt-0.5">
                  If not checked now, you can create the login account later
                  from
                  <strong> User Management → Create Account</strong>, then link
                  it to this provider.
                </p>
              </div>
            </label>

            {createAccount && (
              <div className="mt-4 pt-4 border-t border-slate-100 flex flex-col gap-4">
                <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">
                  Login Credentials
                </p>
                <Input
                  id="username"
                  label="Username"
                  required
                  autoComplete="off"
                  placeholder="dr.kofi.mensah"
                  helpText="3–100 characters, no spaces"
                  error={errors.username?.message}
                  {...register("username", {
                    required: createAccount ? "Username is required" : false,
                    minLength: { value: 3, message: "Minimum 3 characters" },
                    pattern: {
                      value: /^\S+$/,
                      message: "Username must not contain spaces",
                    },
                  })}
                />
                <Input
                  id="password"
                  label="Temporary Password"
                  type="password"
                  required
                  autoComplete="new-password"
                  helpText="Minimum 8 characters — provider should change on first login"
                  showPasswordToggle={true}
                  error={errors.password?.message}
                  {...register("password", {
                    required: createAccount ? "Password is required" : false,
                    minLength: { value: 8, message: "Minimum 8 characters" },
                  })}
                />
              </div>
            )}
          </div>

          <div className="flex gap-3">
            <Button
              type="submit"
              fullWidth
              loading={isSubmitting}
              size="lg"
              iconComponent={LuPlus}
            >
              {createAccount
                ? "Register Provider & Create Account"
                : "Register Provider"}
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={() => navigate("/admin/providers")}
            >
              Cancel
            </Button>
          </div>
        </form>
      </div>
    </PageWrapper>
  );
}
