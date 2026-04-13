import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import userService from "../../services/userService";
import PageWrapper from "../../components/layout/PageWrapper";
import StatCard from "../../components/ui/StatCard";
import Button from "../../components/ui/Button";
import {
  LuCalendar,
  LuCircleCheck,
  LuList,
  LuUsers,
  LuMapPin,
  LuX,
  LuSlash,
  LuTriangleAlert,
  LuPlus,
  LuUser,
} from "react-icons/lu";
import Spinner from "../../components/ui/Spinner";
import Alert from "../../components/ui/Alert";

export default function AdminDashboard() {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    userService
      .getSummary()
      .then((r) => setSummary(r.data.data))
      .catch(() => setError("Could not load analytics. Please refresh."))
      .finally(() => setLoading(false));
  }, []);

  const today = new Date().toLocaleDateString("en-GB", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  });

  return (
    <PageWrapper title="Admin Dashboard" subtitle={today}>
      {loading && <Spinner />}
      {error && <Alert variant="error" message={error} />}

      {!loading && summary && (
        <>
          {/* Emergency alert if any */}
          {summary.emergencyWaiting > 0 && (
            <Alert
              variant="error"
              title={`${summary.emergencyWaiting} EMERGENCY patient${summary.emergencyWaiting > 1 ? "s" : ""} waiting`}
              message="Emergency patients are in the queue and require immediate attention."
              className="mb-5"
            />
          )}

          {/* Primary stats */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <StatCard
              label="Total Today"
              value={summary.totalAppointments}
              iconComponent={LuCalendar}
              colour="text-primary"
              bg="bg-primary-light"
            />
            <StatCard
              label="Completed"
              value={summary.completed}
              iconComponent={LuCircleCheck}
              colour="text-success"
              bg="bg-success-light"
            />
            <StatCard
              label="Active Queues"
              value={summary.activeQueues}
              iconComponent={LuList}
              colour="text-primary"
              bg="bg-primary-light"
            />
            <StatCard
              label="Waiting Now"
              value={summary.patientsWaiting}
              iconComponent={LuUsers}
              colour="text-warning"
              bg="bg-warning-light"
            />
          </div>

          {/* Secondary stats */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-8">
            <StatCard
              label="Scheduled"
              value={summary.scheduled}
              iconComponent={LuMapPin}
              colour="text-blue-600"
              bg="bg-blue-50"
            />
            <StatCard
              label="Cancelled"
              value={summary.cancelled}
              iconComponent={LuX}
              colour="text-danger"
              bg="bg-danger-light"
            />
            <StatCard
              label="No Shows"
              value={summary.noShow}
              iconComponent={LuSlash}
              colour="text-slate-600"
              bg="bg-slate-100"
            />
            <StatCard
              label="Emergency"
              value={summary.emergencyWaiting}
              iconComponent={LuTriangleAlert}
              colour="text-danger"
              bg="bg-danger-light"
            />
          </div>
        </>
      )}

      {/* Quick actions */}
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-5">
        <h2 className="text-sm font-semibold text-slate-700 mb-4">
          Quick Actions
        </h2>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <Link to="/admin/users">
            <Button variant="secondary" fullWidth iconComponent={LuUsers}>
              Users
            </Button>
          </Link>
          <Link to="/admin/providers">
            <Button variant="secondary" fullWidth iconComponent={LuUser}>
              Providers
            </Button>
          </Link>
          <Link to="/admin/users/new">
            <Button variant="secondary" fullWidth iconComponent={LuPlus}>
              New Account
            </Button>
          </Link>
          <Link to="/admin/providers/new">
            <Button variant="secondary" fullWidth iconComponent={LuPlus}>
              New Provider
            </Button>
          </Link>
        </div>
      </div>
    </PageWrapper>
  );
}
