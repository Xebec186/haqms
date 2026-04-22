import { createContext, useState, useEffect, useCallback } from "react";
import authService from "../services/authService";

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => localStorage.getItem("haqms_token"));
  const [loading, setLoading] = useState(true);

  // Restore user from localStorage on mount
  useEffect(() => {
    const stored = localStorage.getItem("haqms_user");
    if (stored && token) {
      try {
        setUser(JSON.parse(stored));
      } catch {
        logout();
      }
    }
    setLoading(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const login = useCallback(async (credentials) => {
    const { data } = await authService.login(credentials);
    const { token: jwt, role, userId, patientId, providerId } = data.data;

    localStorage.setItem("haqms_token", jwt);
    const userData = { userId, role, patientId, providerId };
    localStorage.setItem("haqms_user", JSON.stringify(userData));

    setToken(jwt);
    setUser(userData);
    return userData;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem("haqms_token");
    localStorage.removeItem("haqms_user");
    setToken(null);
    setUser(null);
  }, []);

  const value = {
    user,
    token,
    loading,
    login,
    logout,
    isAuthenticated: !!token,
    isPatient: user?.role === "PATIENT",
    isProvider: user?.role === "PROVIDER",
    isAdmin: user?.role === "ADMIN",
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
