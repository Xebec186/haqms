import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import dayjs from "dayjs";
import appointmentService from "../../services/appointmentService";
import { useAuth } from "../../hooks/useAuth";
import PageWrapper from "../../components/layout/PageWrapper";
import Button from "../../components/ui/Button";
import Spinner from "../../components/ui/Spinner";
import Alert from "../../components/ui/Alert";

export default function BookAppointment() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [departments, setDepartments] = useState([]);
  const [providers, setProviders] = useState([]);
  const [schedules, setSchedules] = useState([]);
  const [loadingDepts, setLoadingDepts] = useState(true);
  const [loadingProviders, setLoadingProviders] = useState(false);
  const [loadingSchedules, setLoadingSchedules] = useState(false);
  const [pageError, setPageError] = useState(null);

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm();

  const watchDeptId = watch("departmentId");
  const watchProviderId = watch("providerId");

  // Load departments on mount
  useEffect(() => {
    appointmentService
      .getDepartments()
      .then((r) => setDepartments(r.data.data ?? []))
      .catch(() => setPageError("Could not load departments. Please refresh."))
      .finally(() => setLoadingDepts(false));
  }, []);

  // Load providers when department changes
  useEffect(() => {
    if (!watchDeptId) return;
    setValue("providerId", "");
    setValue("scheduleId", "");
    setProviders([]);
    setSchedules([]);
    setLoadingProviders(true);
    appointmentService
      .getProvidersByDept(watchDeptId)
      .then((r) => setProviders(r.data.data ?? []))
      .catch(() => toast.error("Could not load doctors for this department."))
      .finally(() => setLoadingProviders(false));
  }, [watchDeptId, setValue]);

  // Load schedules when provider changes
  useEffect(() => {
    if (!watchProviderId) return;
    setValue("scheduleId", "");
    setSchedules([]);
    setLoadingSchedules(true);
    appointmentService
      .getSchedules(watchProviderId)
      .then((r) => setSchedules(r.data.data ?? []))
      .catch(() => toast.error("Could not load available slots."))
      .finally(() => setLoadingSchedules(false));
  }, [watchProviderId, setValue]);

  const onSubmit = async (data) => {
    try {
      await appointmentService.create({
        patientId: user.patientId,
        providerId: Number(data.providerId),
        departmentId: Number(data.departmentId),
        scheduleId: Number(data.scheduleId),
        reason: data.reason,
        priority: "REGULAR",
      });
      toast.success("Appointment booked successfully!");
      navigate("/patient/appointments");
    } catch (err) {
      toast.error(
        err.response?.data?.message ?? "Booking failed. Please try again.",
      );
    }
  };

  if (loadingDepts) {
    return (
      <PageWrapper title="Book an Appointment">
        <Spinner />
      </PageWrapper>
    );
  }

  return (
    <PageWrapper title="Book an Appointment">
      <div className="max-w-lg mx-auto bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
        {pageError && (
          <Alert variant="error" message={pageError} className="mb-4" />
        )}

        <form
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          className="flex flex-col gap-5"
        >
          {/* Department */}
          <div className="flex flex-col gap-1">
            <label
              htmlFor="departmentId"
              className="text-sm font-medium text-gray-700"
            >
              Department{" "}
              <span className="text-danger" aria-hidden="true">
                *
              </span>
            </label>
            <select
              id="departmentId"
              className="w-full rounded-lg border border-gray-300 px-4 py-2.5 min-h-touch focus:outline-none focus:ring-2 focus:ring-primary bg-white"
              {...register("departmentId", {
                required: "Please select a department",
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
              <p role="alert" className="text-sm text-danger">
                {errors.departmentId.message}
              </p>
            )}
          </div>

          {/* Provider */}
          {(loadingProviders || providers.length > 0) && (
            <div className="flex flex-col gap-1">
              <label
                htmlFor="providerId"
                className="text-sm font-medium text-gray-700"
              >
                Doctor{" "}
                <span className="text-danger" aria-hidden="true">
                  *
                </span>
              </label>
              {loadingProviders ? (
                <Spinner size="sm" />
              ) : (
                <select
                  id="providerId"
                  className="w-full rounded-lg border border-gray-300 px-4 py-2.5 min-h-touch focus:outline-none focus:ring-2 focus:ring-primary bg-white"
                  {...register("providerId", {
                    required: "Please select a doctor",
                  })}
                >
                  <option value="">Select doctor…</option>
                  {providers.map((p) => (
                    <option key={p.providerId} value={p.providerId}>
                      Dr. {p.firstName} {p.lastName}
                      {p.specialisation ? ` — ${p.specialisation}` : ""}
                    </option>
                  ))}
                </select>
              )}
              {errors.providerId && (
                <p role="alert" className="text-sm text-danger">
                  {errors.providerId.message}
                </p>
              )}
            </div>
          )}

          {/* Schedule / Time Slot */}
          {(loadingSchedules || schedules.length > 0) && (
            <div className="flex flex-col gap-1">
              <label
                htmlFor="scheduleId"
                className="text-sm font-medium text-gray-700"
              >
                Available Date &amp; Time Slot{" "}
                <span className="text-danger" aria-hidden="true">
                  *
                </span>
              </label>
              {loadingSchedules ? (
                <Spinner size="sm" />
              ) : (
                <select
                  id="scheduleId"
                  className="w-full rounded-lg border border-gray-300 px-4 py-2.5 min-h-touch focus:outline-none focus:ring-2 focus:ring-primary bg-white"
                  {...register("scheduleId", {
                    required: "Please select a time slot",
                  })}
                >
                  <option value="">Select slot…</option>
                  {schedules.map((s) => (
                    <option key={s.scheduleId} value={s.scheduleId}>
                      {dayjs(s.scheduleDate).format("DD MMM YYYY")} —{" "}
                      {s.startTime?.slice(0, 5)} to {s.endTime?.slice(0, 5)}
                    </option>
                  ))}
                </select>
              )}
              {errors.scheduleId && (
                <p role="alert" className="text-sm text-danger">
                  {errors.scheduleId.message}
                </p>
              )}
            </div>
          )}

          {/* Reason */}
          <div className="flex flex-col gap-1">
            <label
              htmlFor="reason"
              className="text-sm font-medium text-gray-700"
            >
              Reason for Visit{" "}
              <span className="text-danger" aria-hidden="true">
                *
              </span>
            </label>
            <textarea
              id="reason"
              rows={3}
              placeholder="Briefly describe your symptoms or reason for visit…"
              className={`w-full rounded-lg border px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-primary resize-none ${
                errors.reason
                  ? "border-danger bg-danger-light"
                  : "border-gray-300"
              }`}
              {...register("reason", {
                required: "Please describe your reason for visit",
                maxLength: { value: 500, message: "Maximum 500 characters" },
              })}
            />
            {errors.reason && (
              <p role="alert" className="text-sm text-danger">
                {errors.reason.message}
              </p>
            )}
          </div>

          <div className="flex gap-3 pt-1">
            <Button type="submit" fullWidth loading={isSubmitting}>
              Confirm Booking
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={() => navigate("/patient")}
            >
              Cancel
            </Button>
          </div>
        </form>
      </div>
    </PageWrapper>
  );
}
