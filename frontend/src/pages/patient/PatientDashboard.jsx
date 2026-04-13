import { Link } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";
import PageWrapper from "../../components/layout/PageWrapper";
import Button from "../../components/ui/Button";
import { LuCalendar, LuClipboard, LuInfo } from "react-icons/lu";

const actions = [
  {
    to: "/patient/book",
    iconComponent: LuCalendar,
    label: "Book Appointment",
    desc: "Schedule a visit with a specialist or general practitioner",
    variant: "primary",
    btnLabel: "Book Now",
    accent: "bg-blue-50 border-blue-100",
  },
  {
    to: "/patient/appointments",
    iconComponent: LuClipboard,
    label: "My Appointments",
    desc: "View upcoming appointments, check in when you arrive, or cancel",
    variant: "secondary",
    btnLabel: "View All",
    accent: "bg-slate-50 border-slate-100",
  },
];

export default function PatientDashboard() {
  const { user } = useAuth();

  return (
    <PageWrapper
      title="Patient Portal"
      subtitle="Manage your hospital appointments and track your queue status"
    >
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 mb-10">
        {actions.map((a) => (
          <div
            key={a.to}
            className={`glass-card rounded-[2rem] p-8 flex flex-col justify-between gap-6 transition-transform hover:-translate-y-1 shadow-lg hover:shadow-xl`}
          >
            <div className="flex items-start gap-4">
              <div className="p-4 bg-white/50 backdrop-blur-md rounded-2xl shadow-sm border border-white/60">
                <a.iconComponent
                  className="text-4xl drop-shadow-sm"
                  aria-hidden="true"
                />
              </div>
              <div className="mt-1">
                <h2 className="text-xl font-bold text-slate-800 tracking-tight">
                  {a.label}
                </h2>
                <p className="text-base text-slate-600 mt-2 leading-snug">
                  {a.desc}
                </p>
              </div>
            </div>
            <Link to={a.to} className="self-end mt-2">
              <Button variant={a.variant} size="lg" className="shadow-md">
                {a.btnLabel}
              </Button>
            </Link>
          </div>
        ))}
      </div>

      {/* Info panel */}
      <div className="relative overflow-hidden bg-gradient-to-r from-indigo-50 to-blue-50 border border-indigo-100/50 rounded-[2rem] p-8 shadow-sm">
        <div className="absolute -right-10 -top-10 w-40 h-40 bg-indigo-500 rounded-full mix-blend-multiply filter blur-3xl opacity-10"></div>
        <div className="relative z-10">
          <h3 className="text-lg font-bold text-indigo-900 mb-4 flex items-center gap-2">
            <LuInfo className="text-indigo-500 w-4 h-4" aria-hidden="true" />{" "}
            How it works
          </h3>
          <ol className="text-base text-slate-700 space-y-3 list-decimal list-outside ml-4 marker:text-indigo-400 marker:font-bold">
            <li className="pl-2">
              Book an appointment with your preferred doctor and department
            </li>
            <li className="pl-2">
              Arrive at the hospital on your appointment day
            </li>
            <li className="pl-2">
              Check in via the app to get your queue number
            </li>
            <li className="pl-2">
              Watch your queue status — you will be notified when it is your
              turn
            </li>
          </ol>
        </div>
      </div>
    </PageWrapper>
  );
}
