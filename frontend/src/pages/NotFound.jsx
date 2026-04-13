import { Link } from "react-router-dom";
import Button from "../components/ui/Button";
import { LuSearch } from "react-icons/lu";

export default function NotFound() {
  return (
    <div className="min-h-screen bg-surface flex items-center justify-center p-4">
      <div className="text-center">
        <p className="text-7xl font-bold text-gray-200 mb-2">404</p>
        <LuSearch
          className="mx-auto w-12 h-12 text-slate-300 mb-4"
          aria-hidden="true"
        />
        <h1 className="text-2xl font-bold text-gray-800 mb-2">
          Page Not Found
        </h1>
        <p className="text-gray-500 mb-6">
          The page you are looking for does not exist or has been moved.
        </p>
        <Link to="/">
          <Button>Go to Home</Button>
        </Link>
      </div>
    </div>
  );
}
