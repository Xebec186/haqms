import { useState, useEffect, useCallback } from "react";
import { useParams, Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { toast } from "react-toastify";
import dayjs from "dayjs";
import appointmentService from "../../services/appointmentService";
import userService from "../../services/userService";
import { extractError } from "../../services/api";
import PageWrapper from "../../components/layout/PageWrapper";
import { LuPlus, LuCalendar, LuChevronLeft } from "react-icons/lu";
import Input from "../../components/ui/Input";
import Button from "../../components/ui/Button";
import Spinner from "../../components/ui/Spinner";
import Alert from "../../components/ui/Alert";
import { fmtDate } from "../../utils/formatters";

export default function ManageSchedules() {
  const { providerId } = useParams();
  const [provider, setProvider] = useState(null);
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [toggling, setToggling] = useState(null);

  const {
    register,
    handleSubmit,
    reset,
    setError: setFormError,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { maxSlots: 20 } });

  const loadSchedules = useCallback(() => {
    appointmentService
      .getSchedules(providerId)
      .then((r) => {
        setSchedules(r.data.data ?? []);
        setError(null);
      })
      .catch(() => setError("Could not load schedules."))
      .finally(() => setLoading(false));
  }, [providerId]);

  useEffect(() => {
    userService
      .getProviderById(providerId)
      .then((r) => setProvider(r.data.data))
      .catch(() => {});
    loadSchedules();
  }, [providerId, loadSchedules]);

  const onSubmit = async (data) => {
    try {
      await appointmentService.createSchedule(providerId, {
        scheduleDate: data.scheduleDate,
        startTime: data.startTime + ":00",
        endTime: data.endTime + ":00",
        maxSlots: Number(data.maxSlots),
      });
      toast.success("Schedule added successfully.");
      reset({ maxSlots: 20 });
      loadSchedules();
    } catch (err) {
      const extracted = extractError(err, "Failed to create schedule.");
      if (typeof extracted === "object" && extracted.fields) {
        Object.entries(extracted.fields).forEach(([field, msg]) =>
          setFormError(field, { type: "server", message: msg }),
        );
      } else {
        const msg =
          typeof extracted === "object" ? extracted.summary : extracted;
        toast.error(msg);
      }
    }
  };

  const toggleAvailability = async (scheduleId, current) => {
    setToggling(scheduleId);
    try {
      await appointmentService.updateSchedule(providerId, scheduleId, {
        isAvailable: !current,
      });
      toast.success(`Schedule ${current ? "blocked" : "unblocked"}.`);
      loadSchedules();
    } catch (err) {
      const msg = extractError(err, "Failed to update schedule.");
      toast.error(typeof msg === "object" ? msg.summary : msg);
    } finally {
      setToggling(null);
    }
  };

  const upcoming = schedules.filter((s) =>
    dayjs(s.scheduleDate).isAfter(dayjs().subtract(1, "day")),
  );
  const past = schedules.filter((s) =>
    dayjs(s.scheduleDate).isBefore(dayjs(), "day"),
  );

  const providerName = provider
    ? `Dr. ${provider.firstName} ${provider.lastName}`
    : "Provider";

  return (
    <PageWrapper
      title={`Schedules — ${providerName}`}
      subtitle={provider?.departmentName}
      action={
        <Link to="/admin/providers">
          <Button variant="ghost" size="sm" iconComponent={LuChevronLeft}>
            Back to Providers
          </Button>
        </Link>
      }
    >
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        {/* ── Add schedule form (left) ─────────────────────────────── */}
        <div className="lg:col-span-2">
          <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-5 sticky top-24">
            <h2 className="text-sm font-semibold text-slate-700 mb-4 flex items-center gap-2">
              <LuPlus className="w-4 h-4" aria-hidden="true" /> Add Working
              Schedule
            </h2>

            <form
              onSubmit={handleSubmit(onSubmit)}
              noValidate
              className="flex flex-col gap-4"
            >
              <Input
                id="scheduleDate"
                label="Date"
                type="date"
                required
                error={errors.scheduleDate?.message}
                {...register("scheduleDate", {
                  required: "Date is required",
                  validate: (v) =>
                    dayjs(v).isAfter(dayjs().subtract(1, "day")) ||
                    "Date cannot be in the past",
                })}
              />

              <div className="grid grid-cols-2 gap-3">
                <Input
                  id="startTime"
                  label="Start"
                  type="time"
                  required
                  error={errors.startTime?.message}
                  {...register("startTime", { required: "Required" })}
                />
                <Input
                  id="endTime"
                  label="End"
                  type="time"
                  required
                  error={errors.endTime?.message}
                  {...register("endTime", {
                    required: "Required",
                    validate: (v, vals) =>
                      v > vals.startTime || "End time must be after start time",
                  })}
                />
              </div>

              <Input
                id="maxSlots"
                label="Max Appointment Slots"
                type="number"
                required
                helpText="How many patients can book in this window"
                error={errors.maxSlots?.message}
                {...register("maxSlots", {
                  required: "Required",
                  valueAsNumber: true,
                  min: { value: 1, message: "Minimum 1 slot" },
                  max: { value: 100, message: "Maximum 100 slots" },
                })}
              />

              <Button
                type="submit"
                fullWidth
                loading={isSubmitting}
                iconComponent={LuPlus}
              >
                Add Schedule
              </Button>
            </form>
          </div>
        </div>

        {/* ── Existing schedules (right) ───────────────────────────── */}
        <div className="lg:col-span-3">
          {loading && <Spinner />}
          {error && <Alert variant="error" message={error} />}

          {!loading && schedules.length === 0 && (
            <div className="text-center py-16 bg-white rounded-2xl border border-dashed border-slate-200">
              <LuCalendar
                className="mx-auto w-12 h-12 text-slate-300 mb-3"
                aria-hidden="true"
              />
              <p className="text-slate-500 text-sm">
                No schedules yet. Add one using the form on the left.
              </p>
            </div>
          )}

          {upcoming.length > 0 && (
            <section className="mb-6">
              <h2 className="text-xs font-semibold text-slate-500 uppercase tracking-widest mb-3">
                Upcoming ({upcoming.length})
              </h2>
              <div className="flex flex-col gap-3">
                {upcoming.map((s) => (
                  <ScheduleRow
                    key={s.scheduleId}
                    schedule={s}
                    toggling={toggling === s.scheduleId}
                    onToggle={() =>
                      toggleAvailability(s.scheduleId, s.isAvailable)
                    }
                  />
                ))}
              </div>
            </section>
          )}

          {past.length > 0 && (
            <section>
              <h2 className="text-xs font-semibold text-slate-500 uppercase tracking-widest mb-3">
                Past ({past.length})
              </h2>
              <div className="flex flex-col gap-3 opacity-60">
                {past.slice(0, 5).map((s) => (
                  <ScheduleRow
                    key={s.scheduleId}
                    schedule={s}
                    toggling={false}
                    onToggle={null}
                    readonly
                  />
                ))}
                {past.length > 5 && (
                  <p className="text-xs text-slate-400 text-center">
                    + {past.length - 5} older schedules not shown
                  </p>
                )}
              </div>
            </section>
          )}
        </div>
      </div>
    </PageWrapper>
  );
}

function ScheduleRow({ schedule: s, toggling, onToggle, readonly }) {
  const isToday = dayjs(s.scheduleDate).isSame(dayjs(), "day");

  return (
    <div
      className={`bg-white rounded-xl border-2 p-4 flex items-center justify-between gap-3
      transition-all ${
        isToday
          ? "border-primary bg-primary-light"
          : s.isAvailable
            ? "border-slate-100 hover:border-slate-200"
            : "border-slate-100 bg-slate-50"
      }`}
    >
      <div className="flex items-center gap-3">
        <div
          className={`h-10 w-10 rounded-lg flex flex-col items-center justify-center shrink-0
          ${isToday ? "bg-primary text-white" : "bg-slate-100 text-slate-600"}`}
        >
          <span className="text-xs font-bold leading-none">
            {dayjs(s.scheduleDate).format("DD")}
          </span>
          <span className="text-xs leading-none opacity-80">
            {dayjs(s.scheduleDate).format("MMM")}
          </span>
        </div>
        <div>
          <p className="font-medium text-slate-800 text-sm">
            {fmtDate(s.scheduleDate)}
            {isToday && (
              <span className="ml-2 text-xs bg-primary text-white px-1.5 py-0.5 rounded-full">
                Today
              </span>
            )}
          </p>
          <p className="text-xs text-slate-500">
            {s.startTime?.slice(0, 5)} – {s.endTime?.slice(0, 5)} · {s.maxSlots}{" "}
            slots
          </p>
          <p className="text-xs mt-0.5">
            <span className={s.isAvailable ? "text-success" : "text-danger"}>
              {s.isAvailable ? "● Available" : "○ Blocked"}
            </span>
          </p>
        </div>
      </div>

      {!readonly && onToggle && (
        <Button
          size="sm"
          variant={s.isAvailable ? "danger" : "success"}
          loading={toggling}
          onClick={onToggle}
        >
          {s.isAvailable ? "Block" : "Unblock"}
        </Button>
      )}
    </div>
  );
}
