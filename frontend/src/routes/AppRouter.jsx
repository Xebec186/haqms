import { lazy, Suspense } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Spinner from "../components/ui/Spinner";
import ProtectedRoute from "./ProtectedRoute";
import { useAuth } from "../hooks/useAuth";

// ── Auth ──────────────────────────────────────────────────────────────────────
const LoginPage = lazy(() => import("../pages/auth/LoginPage"));
const RegisterPage = lazy(() => import("../pages/auth/RegisterPage"));

// ── Patient ───────────────────────────────────────────────────────────────────
const PatientDashboard = lazy(
  () => import("../pages/patient/PatientDashboard"),
);
const BookAppointment = lazy(() => import("../pages/patient/BookAppointment"));
const MyAppointments = lazy(() => import("../pages/patient/MyAppointments"));
const QueueStatus = lazy(() => import("../pages/patient/QueueStatus"));

// ── Provider ──────────────────────────────────────────────────────────────────
const ProviderDashboard = lazy(
  () => import("../pages/provider/ProviderDashboard"),
);
const DailySchedule = lazy(() => import("../pages/provider/DailySchedule"));
const QueueManagement = lazy(() => import("../pages/provider/QueueManagement"));

// ── Admin ─────────────────────────────────────────────────────────────────────
const AdminDashboard = lazy(() => import("../pages/admin/AdminDashboard"));
const UserManagement = lazy(() => import("../pages/admin/UserManagement"));
const CreateUser = lazy(() => import("../pages/admin/CreateUser"));
const ProviderList = lazy(() => import("../pages/admin/ProviderList"));
const CreateProvider = lazy(() => import("../pages/admin/CreateProvider"));
const CreatePatient = lazy(() => import("../pages/admin/CreatePatient"));
const ManageSchedules = lazy(() => import("../pages/admin/ManageSchedules"));

// ── Fallback ──────────────────────────────────────────────────────────────────
const NotFound = lazy(() => import("../pages/NotFound"));
const Unauthorized = lazy(() => import("../pages/Unauthorized"));
const Profile = lazy(() => import("../pages/Profile"));

// ── Role redirect ─────────────────────────────────────────────────────────────
function RoleRedirect() {
  const { user, isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (user?.role === "PATIENT") return <Navigate to="/patient" replace />;
  if (user?.role === "PROVIDER") return <Navigate to="/provider" replace />;
  if (user?.role === "ADMIN") return <Navigate to="/admin" replace />;
  return <Navigate to="/login" replace />;
}

function P({ roles, children }) {
  return <ProtectedRoute allowedRoles={roles}>{children}</ProtectedRoute>;
}

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Suspense fallback={<Spinner fullScreen />}>
        <Routes>
          <Route path="/" element={<RoleRedirect />} />

          {/* Public */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Patient */}
          <Route
            path="/patient"
            element={
              <P roles={["PATIENT"]}>
                <PatientDashboard />
              </P>
            }
          />
          <Route
            path="/patient/book"
            element={
              <P roles={["PATIENT"]}>
                <BookAppointment />
              </P>
            }
          />
          <Route
            path="/patient/appointments"
            element={
              <P roles={["PATIENT"]}>
                <MyAppointments />
              </P>
            }
          />
          <Route
            path="/patient/queue/:appointmentId"
            element={
              <P roles={["PATIENT"]}>
                <QueueStatus />
              </P>
            }
          />

          <Route
            path="/profile"
            element={
              <P roles={["PATIENT", "PROVIDER", "ADMIN"]}>
                <Profile />
              </P>
            }
          />

          {/* Provider */}
          <Route
            path="/provider"
            element={
              <P roles={["PROVIDER"]}>
                <ProviderDashboard />
              </P>
            }
          />
          <Route
            path="/provider/schedule"
            element={
              <P roles={["PROVIDER"]}>
                <DailySchedule />
              </P>
            }
          />
          <Route
            path="/provider/queue"
            element={
              <P roles={["PROVIDER"]}>
                <QueueManagement />
              </P>
            }
          />

          {/* Admin */}
          <Route
            path="/admin"
            element={
              <P roles={["ADMIN"]}>
                <AdminDashboard />
              </P>
            }
          />
          <Route
            path="/admin/users"
            element={
              <P roles={["ADMIN"]}>
                <UserManagement />
              </P>
            }
          />
          <Route
            path="/admin/users/new"
            element={
              <P roles={["ADMIN"]}>
                <CreateUser />
              </P>
            }
          />
          <Route
            path="/admin/patients/new"
            element={
              <P roles={["ADMIN"]}>
                <CreatePatient />
              </P>
            }
          />
          <Route
            path="/admin/providers"
            element={
              <P roles={["ADMIN"]}>
                <ProviderList />
              </P>
            }
          />
          <Route
            path="/admin/providers/new"
            element={
              <P roles={["ADMIN"]}>
                <CreateProvider />
              </P>
            }
          />
          <Route
            path="/admin/providers/:providerId/schedules"
            element={
              <P roles={["ADMIN"]}>
                <ManageSchedules />
              </P>
            }
          />

          {/* Fallback */}
          <Route path="/unauthorized" element={<Unauthorized />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
