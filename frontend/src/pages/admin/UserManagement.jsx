import { useEffect, useState, useCallback } from "react";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import userService from "../../services/userService";
import { extractError } from "../../services/api";
import PageWrapper from "../../components/layout/PageWrapper";
import Badge from "../../components/ui/Badge";
import Button from "../../components/ui/Button";
import { LuPlus, LuUsers, LuChevronLeft, LuChevronRight } from "react-icons/lu";
import Spinner from "../../components/ui/Spinner";
import Alert from "../../components/ui/Alert";
import { fmtDateTime } from "../../utils/formatters";

export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [roleFilter, setRoleFilter] = useState("");
  const [toggling, setToggling] = useState(null);

  const fetchUsers = useCallback(() => {
    setLoading(true);
    setError(null);
    userService
      .listUsers({ page, size: 20, role: roleFilter || undefined })
      .then((r) => {
        const pd = r.data.data;
        setUsers(pd.content ?? []);
        setTotalPages(pd.totalPages ?? 0);
        setTotal(pd.totalElements ?? 0);
      })
      .catch(() => setError("Could not load users. Please try again."))
      .finally(() => setLoading(false));
  }, [page, roleFilter]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleToggle = async (userId, isActive) => {
    setToggling(userId);
    try {
      await userService.updateUserStatus(userId, !isActive);
      toast.success(
        `Account ${isActive ? "deactivated" : "activated"} successfully.`,
      );
      fetchUsers();
    } catch (err) {
      const msg = extractError(err, "Failed to update account status.");
      toast.error(typeof msg === "object" ? msg.summary : msg);
    } finally {
      setToggling(null);
    }
  };

  const roles = ["PATIENT", "PROVIDER", "ADMIN", "RECEPTIONIST"];

  return (
    <PageWrapper
      title="User Management"
      subtitle="View and manage all system accounts"
      action={
        <Link to="/admin/users/new">
          <Button iconComponent={LuPlus}>Create Account</Button>
        </Link>
      }
    >
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-4 mb-4">
        <div className="flex flex-wrap gap-2 items-center">
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mr-2">
            Filter:
          </p>
          <button
            onClick={() => {
              setRoleFilter("");
              setPage(0);
            }}
            className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-colors ${
              roleFilter === ""
                ? "bg-primary text-white"
                : "bg-slate-100 text-slate-600 hover:bg-slate-200"
            }`}
          >
            All ({loading ? "…" : total})
          </button>
          {roles.map((r) => (
            <button
              key={r}
              onClick={() => {
                setRoleFilter(r);
                setPage(0);
              }}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-colors ${
                roleFilter === r
                  ? "bg-primary text-white"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              {r.charAt(0) + r.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
      </div>

      {loading && <Spinner />}
      {error && <Alert variant="error" message={error} />}

      {!loading && users.length === 0 && !error && (
        <div className="text-center py-20 bg-white rounded-2xl border border-slate-100">
          <LuUsers
            className="mx-auto w-12 h-12 text-slate-300 mb-4"
            aria-hidden="true"
          />
          <p className="text-slate-500">No accounts found for this filter.</p>
        </div>
      )}

      {users.length > 0 && (
        <>
          <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-x-auto mb-4">
            <table className="w-full text-sm" aria-label="System user accounts">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-100">
                  {["Account", "Role", "Status", "Last Login", "Actions"].map(
                    (h) => (
                      <th
                        key={h}
                        className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide"
                      >
                        {h}
                      </th>
                    ),
                  )}
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr
                    key={u.userId}
                    className={`border-b border-slate-50 hover:bg-slate-50 transition-colors ${!u.isActive ? "opacity-60" : ""}`}
                  >
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2.5">
                        <div
                          className={`h-8 w-8 rounded-full flex items-center justify-center shrink-0 text-xs font-bold ${
                            u.roleName === "ADMIN"
                              ? "bg-purple-100 text-purple-700"
                              : u.roleName === "PROVIDER"
                                ? "bg-blue-100 text-blue-700"
                                : u.roleName === "RECEPTIONIST"
                                  ? "bg-indigo-100 text-indigo-700"
                                  : "bg-teal-100 text-teal-700"
                          }`}
                        >
                          {u.username?.charAt(0).toUpperCase()}
                        </div>
                        <div>
                          <p className="font-medium text-slate-800">
                            {u.username}
                          </p>
                          <p className="text-xs text-slate-400">{u.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <Badge label={u.roleName} />
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex items-center gap-1 text-xs font-semibold ${u.isActive ? "text-success" : "text-slate-400"}`}
                      >
                        <span aria-hidden="true">{u.isActive ? "●" : "○"}</span>
                        {u.isActive ? "Active" : "Inactive"}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-400 whitespace-nowrap">
                      {fmtDateTime(u.lastLogin) === "—"
                        ? "Never"
                        : fmtDateTime(u.lastLogin)}
                    </td>
                    <td className="px-4 py-3">
                      <Button
                        size="sm"
                        variant={u.isActive ? "danger" : "success"}
                        loading={toggling === u.userId}
                        onClick={() => handleToggle(u.userId, u.isActive)}
                      >
                        {u.isActive ? "Deactivate" : "Activate"}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-3">
              <Button
                variant="secondary"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
                iconComponent={LuChevronLeft}
              >
                Previous
              </Button>
              <span className="text-sm text-slate-500">
                Page <strong>{page + 1}</strong> of{" "}
                <strong>{totalPages}</strong>
              </span>
              <Button
                variant="secondary"
                size="sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                iconComponent={LuChevronRight}
              >
                Next
              </Button>
            </div>
          )}
        </>
      )}
    </PageWrapper>
  );
}
