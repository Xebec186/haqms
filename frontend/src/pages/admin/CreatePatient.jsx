import { useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-toastify";
import patientService from "../../services/userService"; // We can use userService for admin patient creation
import { extractError } from "../../services/api";
import PageWrapper from "../../components/layout/PageWrapper";
import Input from "../../components/ui/Input";
import Button from "../../components/ui/Button";
import { LuPlus, LuUser, LuTriangleAlert, LuChevronLeft } from "react-icons/lu";

export default function CreatePatient() {
  const navigate = useNavigate();
  const [createAccount, setCreateAccount] = useState(true);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm();

  const onSubmit = async (data) => {
    try {
      const payload = {
        firstName: data.firstName,
        lastName: data.lastName,
        dateOfBirth: data.dateOfBirth,
        gender: data.gender,
        phoneNumber: data.phoneNumber,
        email: data.email || undefined,
        ghanaCardNumber: data.ghanaCardNumber || undefined,
        address: data.address || undefined,
        username: createAccount ? data.username : undefined,
        password: createAccount ? data.password : undefined,
      };
      
      // We need to ensure userService has a method for this
      await patientService.createPatient(payload);
      
      toast.success(
        createAccount
          ? "Patient registered and login account created."
          : "Patient registered successfully.",
      );
      navigate("/admin/users"); // Or to a patient list if we have one
    } catch (err) {
      const extracted = extractError(err, "Failed to register patient.");
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

  return (
    <PageWrapper
      title="Register New Patient"
      subtitle="Add a new patient record to the system"
      action={
        <Link to="/admin/users">
          <Button variant="ghost" size="sm" iconComponent={LuChevronLeft}>
            Back to Users
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
          {/* Patient Details Card */}
          <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-5">
            <h2 className="text-sm font-semibold text-slate-700 mb-4 flex items-center gap-2">
              <LuUser className="w-5 h-5" aria-hidden="true" /> Patient Information
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

              <div className="grid grid-cols-2 gap-3">
                <Input
                  id="dateOfBirth"
                  label="Date of Birth"
                  type="date"
                  required
                  error={errors.dateOfBirth?.message}
                  {...register("dateOfBirth", {
                    required: "Date of birth is required",
                  })}
                />
                <div className="flex flex-col gap-1">
                  <label htmlFor="gender" className="text-sm font-medium text-slate-700">
                    Gender <span className="text-danger font-bold">*</span>
                  </label>
                  <select
                    id="gender"
                    className={`w-full rounded-lg border px-4 py-2.5 text-sm min-h-touch bg-white
                      focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent
                      hover:border-slate-400 transition-all ${
                        errors.gender ? "border-danger bg-red-50" : "border-slate-300"
                      }`}
                    {...register("gender", { required: "Gender is required" })}
                  >
                    <option value="">Select gender…</option>
                    <option value="MALE">Male</option>
                    <option value="FEMALE">Female</option>
                    <option value="OTHER">Other</option>
                  </select>
                  {errors.gender && (
                    <p className="text-xs font-medium text-danger mt-1">{errors.gender.message}</p>
                  )}
                </div>
              </div>

              <Input
                id="phoneNumber"
                label="Phone Number"
                required
                placeholder="+233XXXXXXXXX"
                error={errors.phoneNumber?.message}
                {...register("phoneNumber", {
                  required: "Phone number is required",
                  pattern: {
                    value: /^\+233[0-9]{9}$/,
                    message: "Enter a valid Ghanaian number (+233XXXXXXXXX)",
                  },
                })}
              />

              <Input
                id="email"
                label="Email Address (optional)"
                type="email"
                placeholder="patient@example.com"
                error={errors.email?.message}
                {...register("email", {
                  pattern: {
                    value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                    message: "Enter a valid email address",
                  },
                })}
              />

              <Input
                id="ghanaCardNumber"
                label="Ghana Card Number (optional)"
                placeholder="GHA-123456789-0"
                error={errors.ghanaCardNumber?.message}
                {...register("ghanaCardNumber", {
                  pattern: {
                    value: /^GHA-[0-9]{9}-[0-9]$/,
                    message: "Format: GHA-XXXXXXXXX-X",
                  },
                })}
              />

              <Input
                id="address"
                label="Residential Address (optional)"
                placeholder="123 Hospital Lane, Accra"
                error={errors.address?.message}
                {...register("address")}
              />
            </div>
          </div>

          {/* Account Creation Toggle */}
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
                  Create a system login account for this patient
                </p>
                <p className="text-xs text-slate-400 mt-0.5">
                  Allows the patient to book appointments and track their queue status online.
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
                  placeholder="patient.username"
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
                  helpText="Minimum 8 characters — patient should change on first login"
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
                ? "Register Patient & Create Account"
                : "Register Patient"}
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
