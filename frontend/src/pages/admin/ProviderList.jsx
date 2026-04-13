import { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import userService from "../../services/userService";
import appointmentService from "../../services/appointmentService";
import { extractError } from "../../services/api";
import PageWrapper from "../../components/layout/PageWrapper";
import Button from "../../components/ui/Button";
import { LuUser, LuCalendar, LuPlus } from "react-icons/lu";
import Spinner from "../../components/ui/Spinner";
import Alert from "../../components/ui/Alert";

export default function ProviderList() {
  const [providers, setProviders] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [deptFilter, setDeptFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [toggling, setToggling] = useState(null);

  const fetchProviders = useCallback(() => {
    setLoading(true);
    setError(null);
    userService
      .listProviders(deptFilter || undefined)
      .then((r) => setProviders(r.data.data ?? []))
      .catch(() => setError("Could not load providers. Please try again."))
      .finally(() => setLoading(false));
  }, [deptFilter]);

  useEffect(() => {
    fetchProviders();
  }, [fetchProviders]);

  useEffect(() => {
    appointmentService
      .getDepartments()
      .then((r) => setDepartments(r.data.data ?? []));
  }, []);

  const handleToggle = async (id, isActive) => {
    setToggling(id);
    try {
      await userService.updateProvider(id, { isActive: !isActive });
      toast.success(
        `Provider ${isActive ? "deactivated" : "activated"} successfully.`,
      );
      fetchProviders();
    } catch (err) {
      const msg = extractError(err, "Failed to update provider status.");
      toast.error(typeof msg === "object" ? msg.summary : msg);
    } finally {
      setToggling(null);
    }
  };

  return (
    <PageWrapper
      title="Healthcare Providers"
      subtitle="Manage provider records and their working schedules"
      action={
        <Link to="/admin/providers/new">
          <Button iconComponent={LuPlus}>Register Provider</Button>
        </Link>
      }
    >
      {/* Filter row */}
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-4 mb-4">
        <div className="flex flex-wrap gap-3 items-end">
          <div className="flex flex-col gap-1">
            <label
              htmlFor="deptFilter"
              className="text-xs font-semibold text-slate-500 uppercase tracking-wide"
            >
              Filter by department
            </label>
            <select
              id="deptFilter"
              value={deptFilter}
              onChange={(e) => setDeptFilter(e.target.value)}
              className="rounded-lg border border-slate-300 px-3 py-2 text-sm min-h-touch
                         bg-white focus:ring-2 focus:ring-primary focus:border-transparent
                         hover:border-slate-400 transition-all"
            >
              <option value="">All departments</option>
              {departments.map((d) => (
                <option key={d.departmentId} value={d.departmentId}>
                  {d.name}
                </option>
              ))}
            </select>
          </div>
          <p className="text-sm text-slate-500 self-center">
            {loading
              ? ""
              : `${providers.length} provider${providers.length !== 1 ? "s" : ""}`}
          </p>
        </div>
      </div>

      {loading && <Spinner />}
      {error && <Alert variant="error" message={error} />}

      {!loading && providers.length === 0 && !error && (
        <div className="text-center py-20 bg-white rounded-2xl border border-slate-100">
          <LuUser
            className="mx-auto w-12 h-12 text-slate-300 mb-4"
            aria-hidden="true"
          />
          <h2 className="text-lg font-semibold text-slate-700 mb-2">
            No providers found
          </h2>
          <p className="text-slate-500 text-sm mb-5">
            {deptFilter
              ? "No providers in this department."
              : "Register your first provider to get started."}
          </p>
          <Link to="/admin/providers/new">
            <Button iconComponent={LuPlus}>Register Provider</Button>
          </Link>
        </div>
      )}

      {providers.length > 0 && (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-x-auto">
          <table className="w-full text-sm" aria-label="Healthcare providers">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-100">
                {[
                  "Name",
                  "Department",
                  "Specialisation",
                  "License",
                  "Status",
                  "Actions",
                ].map((h) => (
                  <th
                    key={h}
                    className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide"
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {providers.map((p, i) => (
                <tr
                  key={p.providerId}
                  className={`border-b border-slate-50 hover:bg-slate-50 transition-colors
                    ${!p.isActive ? "opacity-60" : ""}`}
                >
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2.5">
                      <div
                        className="h-8 w-8 rounded-full bg-primary-light flex items-center
                                      justify-center shrink-0"
                      >
                        <span className="text-primary text-xs font-bold">
                          {p.firstName?.charAt(0)}
                          {p.lastName?.charAt(0)}
                        </span>
                      </div>
                      <div>
                        <p className="font-medium text-slate-800">
                          Dr. {p.firstName} {p.lastName}
                        </p>
                        {p.email && (
                          <p className="text-xs text-slate-400">{p.email}</p>
                        )}
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-slate-600">
                    {p.departmentName}
                  </td>
                  <td className="px-4 py-3 text-slate-500">
                    {p.specialisation || "—"}
                  </td>
                  <td className="px-4 py-3">
                    <span className="font-mono text-xs bg-slate-100 px-2 py-0.5 rounded text-slate-600">
                      {p.licenseNumber}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-flex items-center gap-1 text-xs font-semibold ${
                        p.isActive ? "text-success" : "text-danger"
                      }`}
                    >
                      <span aria-hidden="true">{p.isActive ? "●" : "○"}</span>
                      {p.isActive ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2 flex-wrap">
                      <Link to={`/admin/providers/${p.providerId}/schedules`}>
                        <Button
                          size="sm"
                          variant="secondary"
                          iconComponent={LuCalendar}
                        >
                          Schedules
                        </Button>
                      </Link>
                      <Button
                        size="sm"
                        variant={p.isActive ? "danger" : "success"}
                        loading={toggling === p.providerId}
                        onClick={() => handleToggle(p.providerId, p.isActive)}
                      >
                        {p.isActive ? "Deactivate" : "Activate"}
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </PageWrapper>
  );
}
