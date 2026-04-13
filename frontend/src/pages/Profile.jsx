import { useEffect, useState } from "react";
import PageWrapper from "../components/layout/PageWrapper";
import Card from "../components/ui/Card";
import Input from "../components/ui/Input";
import Button from "../components/ui/Button";
import Spinner from "../components/ui/Spinner";
import { useAuth } from "../hooks/useAuth";
import userService from "../services/userService";
import authService from "../services/authService";
import { extractError } from "../services/api";
import { toast } from "react-toastify";
import { fmtDate, fmtDateTime, fmtName } from "../utils/formatters";

export default function Profile() {
  const { user } = useAuth();
  console.log("User in Profile:", user); // Debugging line
  const [loading, setLoading] = useState(true);
  const [account, setAccount] = useState(null);
  const [patient, setPatient] = useState(null);
  const [provider, setProvider] = useState(null);

  // Password form state
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [savingPassword, setSavingPassword] = useState(false);

  useEffect(() => {
    async function load() {
      setLoading(true);
      try {
        if (user?.role === "PATIENT") {
          const { data } = await userService.getMe();
          setPatient(data.data ?? null);
        }
        if (user?.role === "PROVIDER") {
          const { data } = await userService.getMyProviderProfile();
          setProvider(data.data ?? null);
        }

        const { data } = await userService.getAccount();
        setAccount(data.data ?? null);
      } catch (err) {
        const msg = extractError(err, "Could not load profile.");
        toast.error(typeof msg === "object" ? msg.summary : msg);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [user]);

  const handleChangePassword = async (e) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      toast.error("New passwords do not match.");
      return;
    }
    setSavingPassword(true);
    try {
      await authService.changePassword({ oldPassword, newPassword });
      toast.success("Password updated successfully.");
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err) {
      const msg = extractError(err, "Failed to update password.");
      toast.error(typeof msg === "object" ? msg.summary : msg);
    } finally {
      setSavingPassword(false);
    }
  };

  if (loading) return <Spinner fullScreen />;

  return (
    <PageWrapper title="Profile" subtitle="Your account and personal details">
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card title="Account">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <p className="text-xs text-slate-500">Username</p>
              <p className="font-medium text-slate-800 mb-4">
                {account?.username || "—"}
              </p>
              <p className="text-xs text-slate-500 mt-2">Email</p>
              <p className="font-medium text-slate-800">
                {account?.email || "—"}
              </p>
            </div>

            <div>
              <p className="text-xs text-slate-500">Created</p>
              <p className="font-medium text-slate-800">
                {account ? fmtDateTime(account?.createdAt) : "—"}
              </p>
            </div>
          </div>
        </Card>

        <Card title="Personal">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {user?.role === "PROVIDER" && (
              <>
                <div>
                  <p className="text-xs text-slate-500">Full name</p>
                  <p className="font-medium text-slate-800">
                    {fmtName(provider?.firstName, provider?.lastName)}
                  </p>
                </div>
                <div>
                  <p className="text-xs text-slate-500">Specialisation</p>
                  <p className="font-medium text-slate-800">
                    {provider?.specialisation || "—"}
                  </p>
                </div>
                <div>
                  <p className="text-xs text-slate-500">License number</p>
                  <p className="font-medium text-slate-800">
                    {provider?.licenseNumber || "—"}
                  </p>
                </div>
                <div>
                  <p className="text-xs text-slate-500">Phone</p>
                  <p className="font-medium text-slate-800">
                    {provider?.phoneNumber || "—"}
                  </p>
                </div>
              </>
            )}

            {user?.role === "PATIENT" && (
              <>
                <div>
                  <p className="text-xs text-slate-500">Full name</p>
                  <p className="font-medium text-slate-800">
                    {fmtName(patient?.firstName, patient?.lastName)}
                  </p>
                </div>
                <div>
                  <p className="text-xs text-slate-500">Date of birth</p>
                  <p className="font-medium text-slate-800">
                    {fmtDate(patient?.dateOfBirth)}
                  </p>
                </div>
                <div>
                  <p className="text-xs text-slate-500">Gender</p>
                  <p className="font-medium text-slate-800">
                    {patient?.gender || "—"}
                  </p>
                </div>
                <div>
                  <p className="text-xs text-slate-500">Phone</p>
                  <p className="font-medium text-slate-800">
                    {patient?.phoneNumber || "—"}
                  </p>
                </div>
                <div className="sm:col-span-2">
                  <p className="text-xs text-slate-500">Address</p>
                  <p className="font-medium text-slate-800">
                    {patient?.address || "—"}
                  </p>
                </div>
              </>
            )}
          </div>
        </Card>
      </div>

      <div className="mt-6">
        <Card title="Change password">
          <form
            onSubmit={handleChangePassword}
            className="grid grid-cols-1 sm:grid-cols-2 gap-4"
          >
            <div className="sm:col-span-2">
              <Input
                id="oldPassword"
                label="Current password"
                type="password"
                required
                showPasswordToggle
                value={oldPassword}
                onChange={(e) => setOldPassword(e.target.value)}
              />
            </div>

            <div>
              <Input
                id="newPassword"
                label="New password"
                type="password"
                required
                showPasswordToggle
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                helpText="Minimum 8 characters"
              />
            </div>

            <div>
              <Input
                id="confirmPassword"
                label="Confirm new password"
                type="password"
                required
                showPasswordToggle
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
            </div>

            <div className="sm:col-span-2 flex justify-end">
              <Button type="submit" loading={savingPassword} size="md">
                Update password
              </Button>
            </div>
          </form>
        </Card>
      </div>
    </PageWrapper>
  );
}
