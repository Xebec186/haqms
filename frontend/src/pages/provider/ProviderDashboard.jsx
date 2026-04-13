import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import userService from "../../services/userService";
import PageWrapper from "../../components/layout/PageWrapper";
import Button from "../../components/ui/Button";
import Spinner from "../../components/ui/Spinner";
import { LuClipboard, LuList, LuZap, LuLightbulb } from "react-icons/lu";

export default function ProviderDashboard() {
  const [provider, setProvider] = useState(null);
  const [loadingProvider, setLoadingProvider] = useState(true);

  useEffect(() => {
    userService
      .getMyProviderProfile()
      .then((r) => setProvider(r.data.data))
      .catch(() => toast.error("Could not load your provider profile."))
      .finally(() => setLoadingProvider(false));
  }, []);

  if (loadingProvider) {
    return (
      <PageWrapper title="Provider Dashboard">
        <Spinner />
      </PageWrapper>
    );
  }

  const today = new Date().toLocaleDateString("en-GB", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });

  return (
    <PageWrapper
      title="Provider Dashboard"
      subtitle={
        provider
          ? `Dr. ${provider.firstName} ${provider.lastName} · ${provider.departmentName}`
          : today
      }
    >
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
        {/* Schedule card */}
        <div
          className="bg-white rounded-2xl border-2 border-slate-100 p-6 flex flex-col gap-4
                        hover:border-primary-light hover:shadow-md transition-all"
        >
          <div className="flex items-center gap-3">
            <div
              className="h-12 w-12 bg-primary-light rounded-xl flex items-center
                            justify-center shrink-0"
            >
              <LuClipboard
                className="w-6 h-6 text-slate-700"
                aria-hidden="true"
              />
            </div>
            <div>
              <h2 className="text-base font-semibold text-slate-800">
                Today's Schedule
              </h2>
              <p className="text-sm text-slate-500">{today}</p>
            </div>
          </div>
          <p className="text-sm text-slate-500">
            View all appointments booked for today, check patients in, and see
            their details.
          </p>
          <Link to="/provider/schedule" className="self-start">
            <Button iconComponent={LuClipboard} className="cursor-pointer">
              View Schedule
            </Button>
          </Link>
        </div>

        {/* Queue card */}
        <div
          className="bg-white rounded-2xl border-2 border-slate-100 p-6 flex flex-col gap-4
                        hover:border-primary-light hover:shadow-md transition-all"
        >
          <div className="flex items-center gap-3">
            <div
              className="h-12 w-12 bg-emerald-50 rounded-xl flex items-center
                            justify-center shrink-0"
            >
              <LuList className="w-6 h-6 text-slate-700" aria-hidden="true" />
            </div>
            <div>
              <h2 className="text-base font-semibold text-slate-800">
                Patient Queue
              </h2>
              <p className="text-sm text-slate-500">
                {provider?.departmentName ?? "Your department"}
              </p>
            </div>
          </div>
          <p className="text-sm text-slate-500">
            Open the live queue to call patients in priority order and manage
            consultations.
          </p>
          <Link to="/provider/queue" className="self-start">
            <Button iconComponent={LuZap} className="cursor-pointer">
              Open Queue
            </Button>
          </Link>
        </div>
      </div>

      {/* Info tip */}
      <div className="bg-amber-50 border border-amber-200 rounded-2xl p-4 text-sm text-amber-800">
        <p className="font-semibold mb-1 flex items-center gap-1.5">
          <LuLightbulb className="w-4 h-4" /> Queue Priority
        </p>
        <p>
          The queue automatically orders patients by priority —{" "}
          <strong>EMERGENCY</strong> first, then <strong>URGENT</strong>, then{" "}
          <strong>REGULAR</strong>. Within the same priority, patients are
          called in the order they checked in.
        </p>
      </div>
    </PageWrapper>
  );
}
