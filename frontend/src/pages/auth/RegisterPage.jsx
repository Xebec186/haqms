import { useForm } from "react-hook-form";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-toastify";
import authService from "../../services/authService";
import { extractError } from "../../services/api";
import Input from "../../components/ui/Input";
import Button from "../../components/ui/Button";
import { LuBuilding, LuTriangleAlert } from "react-icons/lu";

export default function RegisterPage() {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm();
  const navigate = useNavigate();

  const onSubmit = async (data) => {
    try {
      let phone = data.phoneNumber.trim();

      if (phone.startsWith("0")) {
        phone = "+233" + phone.slice(1);
      } else if (!phone.startsWith("+233")) {
        phone = "+233" + phone;
      }

      const formattedData = {
        ...data,
        phoneNumber: phone,
      };

      await authService.register(formattedData);

      toast.success("Account created successfully! Please sign in.");
      navigate("/login");
    } catch (err) {
      const extracted = extractError(
        err,
        "Registration failed. Please check your details.",
      );

      if (typeof extracted === "object" && extracted.fields) {
        Object.entries(extracted.fields).forEach(([field, message]) => {
          setError(field, { type: "server", message });
        });
        toast.error(extracted.summary);

        toast.error("Please fix the highlighted errors and try again.");
      } else {
        setError("root", { type: "server", message: extracted });
        toast.error(extracted);
      }
    }
  };

  return (
    <div
      className="min-h-screen bg-linear-to-br from-slate-900 via-indigo-950 to-blue-900
                    flex items-center justify-center p-4 sm:p-8 relative overflow-hidden"
    >
      {/* Decorative blurred blobs */}
      <div className="absolute top-[10%] left-[-5%] w-[30rem] h-[30rem] bg-indigo-600 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-pulse"></div>
      <div
        className="absolute bottom-[-10%] right-[-5%] w-[30rem] h-[30rem] bg-blue-500 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-pulse"
        style={{ animationDelay: "1.5s" }}
      ></div>

      <div className="w-full max-w-lg relative z-10 animate-in fade-in zoom-in-95 duration-700 py-6">
        {/* Header */}
        <div className="text-center mb-8">
          <div
            className="inline-flex h-16 w-16 items-center justify-center rounded-2xl
                          bg-linear-to-tr from-indigo-500 to-blue-400 shadow-xl shadow-blue-500/20 mb-4 transform hover:scale-105 transition-transform duration-300"
          >
            <LuBuilding className="w-8 h-8 text-white" aria-hidden="true" />
          </div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight drop-shadow-sm">
            HAQMS
          </h1>
          <p className="text-indigo-200 text-sm font-medium mt-1 tracking-wide">
            Welcome! Create your patient account
          </p>
        </div>

        {/* Card */}
        <div className="glass-card rounded-[2rem] p-6 sm:p-10 relative overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-1 bg-linear-to-r from-indigo-500 to-blue-400"></div>

          <h2 className="text-2xl font-bold text-slate-800 mb-6 tracking-tight">
            Register as a Patient
          </h2>

          {errors.root && (
            <div
              className="mb-6 flex items-start gap-3 rounded-xl bg-red-50 border
                            border-red-100 p-4 text-sm text-red-800"
              role="alert"
            >
              <LuTriangleAlert
                className="w-5 h-5 mt-0.5 text-red-700"
                aria-hidden="true"
              />
              <p className="leading-snug">{errors.root.message}</p>
            </div>
          )}

          <form
            onSubmit={handleSubmit(onSubmit)}
            noValidate
            className="flex flex-col gap-5"
          >
            <div className="grid grid-cols-2 gap-4">
              <Input
                id="firstName"
                label="First Name"
                required
                autoComplete="given-name"
                placeholder="Kwame"
                error={errors.firstName?.message}
                {...register("firstName", {
                  required: "First name is required",
                })}
              />
              <Input
                id="lastName"
                label="Last Name"
                required
                autoComplete="family-name"
                placeholder="Mensah"
                error={errors.lastName?.message}
                {...register("lastName", { required: "Last name is required" })}
              />
            </div>

            <Input
              id="phoneNumber"
              label="Phone Number"
              type="tel"
              required
              autoComplete="tel"
              prefix="+233"
              placeholder="591234567"
              helpText="Enter your 9-digit Ghana number after +233"
              error={errors.phoneNumber?.message}
              {...register("phoneNumber", {
                required: "Phone number is required",
                pattern: {
                  value: /^[0-9]{9}$/,
                  message: "Enter your 9-digit Ghana number after +233",
                },
              })}
            />

            <div className="grid grid-cols-2 gap-4">
              <Input
                id="dateOfBirth"
                label="Date of Birth"
                type="date"
                required
                error={errors.dateOfBirth?.message}
                {...register("dateOfBirth", {
                  required: "Date of birth is required",
                  validate: (v) => {
                    const birth = new Date(v).getTime();
                    if (Number.isNaN(birth))
                      return "Please enter a valid date of birth";
                    const nowTime = Date.now();
                    const age =
                      (nowTime - birth) / (365.25 * 24 * 60 * 60 * 1000);
                    if (age < 0) return "Date of birth cannot be in the future";
                    if (age > 150) return "Please enter a valid date of birth";
                    return true;
                  },
                })}
              />
              <div className="flex flex-col gap-1">
                <label
                  htmlFor="gender"
                  className="text-sm font-medium text-slate-700"
                >
                  Gender{" "}
                  <span className="text-red-500 font-bold" aria-hidden="true">
                    *
                  </span>
                </label>
                <select
                  id="gender"
                  className={`w-full rounded-xl border px-4 py-3 text-sm min-h-touch bg-white text-slate-800 backdrop-blur-sm
                    focus:outline-none focus:ring-4 focus:border-indigo-400 focus:bg-white transition-all duration-200
                    ${
                      errors.gender
                        ? "border-red-300 bg-red-50 focus:ring-red-500/20"
                        : "border-slate-200 hover:border-slate-300 focus:ring-indigo-500/20 shadow-sm"
                    }`}
                  {...register("gender", { required: "Gender is required" })}
                >
                  <option value="">Select…</option>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
                {errors.gender && (
                  <p
                    role="alert"
                    className="text-xs font-medium text-red-600 flex items-center gap-2 mt-1"
                  >
                    <LuTriangleAlert
                      className="w-4 h-4 text-red-600"
                      aria-hidden="true"
                    />{" "}
                    {errors.gender.message}
                  </p>
                )}
              </div>
            </div>

            <Input
              id="email"
              label="Email Address (optional)"
              type="email"
              autoComplete="email"
              placeholder="kwame@example.com"
              error={errors.email?.message}
              {...register("email", {
                pattern: {
                  value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                  message: "Enter a valid email address",
                },
              })}
            />

            <div className="grid grid-cols-2 gap-4">
              <Input
                id="ghanaCardNumber"
                label="Ghana Card Number"
                required
                autoComplete="off"
                placeholder="GHA-123456789-0"
                helpText="Format: GHA-XXXXXXXXX-X"
                error={errors.ghanaCardNumber?.message}
                {...register("ghanaCardNumber", {
                  required: "Ghana card number is required",
                  pattern: {
                    value: /^GHA-\d{9}-\d$/,
                    message: "Must be in format GHA-XXXXXXXXX-X",
                  },
                })}
              />
              <Input
                id="address"
                label="Address"
                required
                autoComplete="address-line1"
                placeholder="Enter your address"
                error={errors.address?.message}
                {...register("address", {
                  required: "Address is required",
                })}
              />
            </div>

            <div className="border-t border-indigo-50/50 pt-5 mt-2">
              <p className="text-xs font-bold text-indigo-500 uppercase tracking-wider mb-4">
                Login Credentials
              </p>
              <div className="flex flex-col gap-4">
                <Input
                  id="reg_username"
                  label="Username"
                  required
                  autoComplete="username"
                  placeholder="Choose a username"
                  helpText="3–100 characters, no spaces"
                  error={errors.username?.message}
                  {...register("username", {
                    required: "Username is required",
                    minLength: {
                      value: 3,
                      message: "Username must be at least 3 characters long",
                    },
                    maxLength: {
                      value: 100,
                      message: "Username must not exceed 100 characters",
                    },
                    pattern: {
                      value: /^\S+$/,
                      message: "Username must not contain spaces",
                    },
                  })}
                />
                <Input
                  id="reg_password"
                  label="Password"
                  type="password"
                  required
                  autoComplete="new-password"
                  placeholder="Create a strong password"
                  helpText="At least 8 characters"
                  error={errors.password?.message}
                  showPasswordToggle
                  {...register("password", {
                    required: "Password is required",
                    minLength: {
                      value: 8,
                      message: "Password must be at least 8 characters long",
                    },
                  })}
                />
              </div>
            </div>

            <Button
              type="submit"
              fullWidth
              loading={isSubmitting}
              size="lg"
              className="mt-4 shadow-xl"
            >
              Create Account
            </Button>
          </form>

          <div className="mt-8 pt-6 border-t border-slate-200/60 text-center">
            <p className="text-sm font-medium text-slate-600">
              Already have an account?{" "}
              <Link
                to="/login"
                className="text-indigo-600 font-bold hover:text-indigo-700 transition-colors"
              >
                Sign in
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
