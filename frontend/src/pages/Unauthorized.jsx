import { Link } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import Button from "../components/ui/Button";
import { LuLock } from "react-icons/lu";

export default function Unauthorized() {
  const { user } = useAuth();

  const homeLink =
    user?.role === "PATIENT"
      ? "/patient"
      : user?.role === "PROVIDER"
        ? "/provider"
        : "/admin";

  return (
    <div className="min-h-screen bg-surface flex items-center justify-center p-4">
      <div className="text-center">
        <LuLock
          className="mx-auto w-12 h-12 text-slate-300 mb-4"
          aria-hidden="true"
        />
        <h1 className="text-2xl font-bold text-gray-800 mb-2">Access Denied</h1>
        <p className="text-gray-500 mb-6">
          You do not have permission to view this page.
        </p>
        <Link to={homeLink}>
          <Button>Back to Dashboard</Button>
        </Link>
      </div>
    </div>
  );
}
