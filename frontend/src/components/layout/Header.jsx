import { useState } from "react";
import { NavLink, Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";
import Button from "../ui/Button";
import {
  LuCalendar,
  LuClipboard,
  LuUser,
  LuUsers,
  LuLogOut,
  LuMenu,
  LuX,
  LuBuilding,
  LuCircleCheck,
  LuList,
} from "react-icons/lu";
import { IoHomeOutline } from "react-icons/io5";

const navLinks = {
  PATIENT: [
    { to: "/patient", label: "Dashboard", iconComponent: IoHomeOutline },
    { to: "/patient/book", label: "Book", iconComponent: LuCalendar },
    {
      to: "/patient/appointments",
      label: "Appointments",
      iconComponent: LuClipboard,
    },
  ],
  PROVIDER: [
    { to: "/provider", label: "Dashboard", iconComponent: IoHomeOutline },
    { to: "/provider/schedule", label: "Schedule", iconComponent: LuClipboard },
    { to: "/provider/queue", label: "Queue", iconComponent: LuList },
  ],
  ADMIN: [
    { to: "/admin", label: "Dashboard", iconComponent: IoHomeOutline },
    { to: "/admin/users", label: "Users", iconComponent: LuUsers },
    { to: "/admin/providers", label: "Providers", iconComponent: LuUser },
  ],
};

export default function Header() {
  const { user, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);

  const links = navLinks[user?.role] ?? [];

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <header className="glass sticky top-4 z-40 mx-4 sm:mx-6 lg:mx-8 rounded-2xl shadow-lg shadow-indigo-500/5">
      <div className="max-w-7xl mx-auto px-6">
        <div className="h-16 flex items-center justify-between gap-6">
          {/* Logo */}
          <Link
            to="/"
            className="flex items-center gap-3 shrink-0 group"
            aria-label="HAQMS Home"
          >
            <div className="h-10 w-10 bg-linear-to-tr from-indigo-600 to-blue-500 rounded-xl flex items-center justify-center shadow-md shadow-indigo-500/20 group-hover:scale-105 transition-transform">
              <LuBuilding className="text-white w-6 h-6" aria-hidden="true" />
            </div>
            <div className="hidden sm:block">
              <p className="text-lg font-black text-slate-800 leading-none tracking-tight">
                HAQMS
              </p>
              <p className="text-xs font-semibold text-indigo-500 leading-tight uppercase tracking-wider mt-1">
                Hospital System
              </p>
            </div>
          </Link>

          {/* Desktop nav */}
          {isAuthenticated && (
            <nav
              aria-label="Main navigation"
              className="hidden md:flex items-center gap-1"
            >
              {links.map((link) => (
                <NavLink
                  key={link.to}
                  to={link.to}
                  end
                  className={({ isActive }) =>
                    `flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                      isActive
                        ? "bg-primary-light text-primary"
                        : "text-slate-600 hover:bg-slate-100 hover:text-slate-800"
                    }`
                  }
                >
                  {link.iconComponent &&
                    (() => {
                      const Icon = link.iconComponent;
                      return (
                        <Icon
                          className="w-4 h-4 text-slate-600"
                          aria-hidden="true"
                        />
                      );
                    })()}
                  {link.label}
                </NavLink>
              ))}
            </nav>
          )}

          {/* Right side */}
          <div className="flex items-center gap-2 shrink-0">
            {isAuthenticated && user && (
              <div className="hidden sm:flex items-center gap-2 mr-1">
                <div className="h-8 w-8 bg-primary-light rounded-full flex items-center justify-center">
                  <span className="text-primary text-xs font-bold">
                    {user.role?.charAt(0)}
                  </span>
                </div>
                <div className="hidden lg:block">
                  <p className="text-xs font-medium text-slate-700 leading-tight capitalize">
                    {user.role?.toLowerCase()}
                  </p>
                </div>
              </div>
            )}

            {isAuthenticated && (
              <div className="flex items-center gap-2">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => navigate("/profile")}
                  className="text-slate-600 hover:bg-slate-100 !min-h-0 h-9"
                >
                  Account
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleLogout}
                  className="text-slate-600 hover:text-danger hover:bg-red-50 !min-h-0 h-9"
                >
                  Sign out
                </Button>
              </div>
            )}

            {/* Mobile menu toggle */}
            {isAuthenticated && (
              <button
                className="md:hidden p-2 rounded-lg hover:bg-slate-100 text-slate-600"
                onClick={() => setMobileOpen((v) => !v)}
                aria-label="Toggle menu"
                aria-expanded={mobileOpen}
              >
                {mobileOpen ? (
                  <LuX className="w-5 h-5" />
                ) : (
                  <LuMenu className="w-5 h-5" />
                )}
              </button>
            )}
          </div>
        </div>

        {/* Mobile nav */}
        {mobileOpen && isAuthenticated && (
          <nav
            aria-label="Mobile navigation"
            className="md:hidden pb-3 flex flex-col gap-1 border-t border-slate-100 pt-3"
          >
            {links.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end
                onClick={() => setMobileOpen(false)}
                className={({ isActive }) =>
                  `flex items-center gap-2 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                    isActive
                      ? "bg-primary-light text-primary"
                      : "text-slate-600 hover:bg-slate-100"
                  }`
                }
              >
                {link.iconComponent &&
                  (() => {
                    const Icon = link.iconComponent;
                    return (
                      <Icon
                        className="w-4 h-4 text-slate-600"
                        aria-hidden="true"
                      />
                    );
                  })()}
                {link.label}
              </NavLink>
            ))}
            <button
              onClick={handleLogout}
              className="flex items-center gap-2 px-3 py-2.5 rounded-lg text-sm font-medium
                text-danger hover:bg-red-50 transition-colors w-full text-left mt-1"
            >
              <LuLogOut className="w-4 h-4" />
              Sign out
            </button>
          </nav>
        )}
      </div>
    </header>
  );
}
