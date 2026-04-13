import { useForm } from "react-hook-form";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-toastify";
import { useAuth } from "../../hooks/useAuth";
import { extractError } from "../../services/api";
import Input from "../../components/ui/Input";
import Button from "../../components/ui/Button";
import { LuBuilding, LuTriangleAlert } from "react-icons/lu";

export default function LoginPage() {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm();
  const { login } = useAuth();
  const navigate = useNavigate();

  const onSubmit = async (data) => {
    try {
      const user = await login(data);
      toast.success("Welcome back!");

      if (user.role === "PATIENT") navigate("/patient");
      else if (user.role === "PROVIDER") navigate("/provider");
      else navigate("/admin");
    } catch (err) {
      const extracted = extractError(err, "Invalid username or password.");
      if (typeof extracted === "object" && extracted.fields) {
        // Set individual field errors from backend validation map
        Object.entries(extracted.fields).forEach(([field, message]) => {
          setError(field, { type: "server", message });
        });
      } else {
        // Single message — show under the form as a general error
        setError("root", { type: "server", message: extracted });
      }
    }
  };

  return (
    <div
      className="min-h-screen bg-linear-to-br from-slate-900 via-indigo-950 to-blue-900
                    flex items-center justify-center p-4 sm:p-8 relative overflow-hidden"
    >
      {/* Decorative blurred blobs */}
      <div className="absolute top-[-10%] left-[-10%] w-96 h-96 bg-indigo-600 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-pulse"></div>
      <div
        className="absolute bottom-[-10%] right-[-10%] w-96 h-96 bg-blue-500 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-pulse"
        style={{ animationDelay: "2s" }}
      ></div>

      <div className="w-full max-w-md relative z-10 animate-in fade-in zoom-in-95 duration-700">
        {/* Logo Header */}
        <div className="text-center mb-8">
          <div
            className="inline-flex h-20 w-20 items-center justify-center rounded-2xl
                          bg-linear-to-tr from-indigo-500 to-blue-400 shadow-xl shadow-blue-500/20 mb-6 transform -rotate-6 hover:rotate-0 transition-transform duration-300"
          >
            <LuBuilding className="w-10 h-10 text-white" aria-hidden="true" />
          </div>
          <h1 className="text-4xl font-extrabold text-white tracking-tight drop-shadow-sm">
            HAQMS
          </h1>
          <p className="text-indigo-200 text-base font-medium mt-2 tracking-wide">
            Hospital Appointment &amp; Queue System
          </p>
          <div className="h-1 w-12 bg-indigo-500 rounded-full mx-auto mt-4 opacity-50"></div>
        </div>

        {/* Form card */}
        <div className="glass-card rounded-3xl p-8 sm:p-10 relative overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-1 bg-linear-to-r from-indigo-500 to-blue-400"></div>

          <h2 className="text-2xl font-bold text-slate-800 mb-6 tracking-tight">
            Welcome back
          </h2>

          {/* Root / server error banner */}
          {errors.root && (
            <div
              className="mb-6 flex items-center gap-3 rounded-xl bg-red-50 border
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
            <Input
              id="username"
              label="Username"
              required
              autoComplete="username"
              autoFocus
              placeholder="Enter your username"
              error={errors.username?.message}
              {...register("username", { required: "Username is required" })}
            />
            <div className="relative">
              <Input
                id="password"
                label="Password"
                type="password"
                required
                autoComplete="current-password"
                placeholder="Enter your password"
                error={errors.password?.message}
                showPasswordToggle
                {...register("password", { required: "Password is required" })}
              />
            </div>

            <Button
              type="submit"
              fullWidth
              loading={isSubmitting}
              size="lg"
              className="mt-4 shadow-xl"
            >
              Sign In to Portal
            </Button>
          </form>

          <div className="mt-8 pt-6 border-t border-slate-200/60 text-center">
            <p className="text-sm font-medium text-slate-600">
              New patient?{" "}
              <Link
                to="/register"
                className="text-indigo-600 font-bold hover:text-indigo-700 transition-colors"
              >
                Create an account
              </Link>
            </p>
          </div>
        </div>

        <p className="text-center text-indigo-200/60 text-sm mt-8 font-medium tracking-wide">
          Secure healthcare management system
        </p>
      </div>
    </div>
  );
}
