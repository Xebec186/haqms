import { useState, useCallback, useEffect } from "react";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import { usePolling } from "../../hooks/usePolling";
import { useAuth } from "../../hooks/useAuth";
import queueService from "../../services/queueService";
import { extractError } from "../../services/api";
import { POLL_INTERVAL_MS } from "../../utils/constants";
import PageWrapper from "../../components/layout/PageWrapper";
import Button from "../../components/ui/Button";
import QueueEntryRow from "../../components/features/QueueEntryRow";
import Spinner from "../../components/ui/Spinner";
import Alert from "../../components/ui/Alert";
import {
  LuMegaphone,
  LuPause,
  LuPlay,
  LuUser,
  LuChevronLeft,
} from "react-icons/lu";

export default function QueueManagement() {
  const { isProvider } = useAuth();
  const [queueId, setQueueId] = useState(null);
  const [queue, setQueue] = useState(null);
  const [entries, setEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState(false);
  const [calling, setCalling] = useState(false);

  const refresh = useCallback(async (isInitial = false) => {
    if (isInitial) setLoading(true);
    try {
      const qRes = await queueService.getQueue();
      const queueData = qRes.data.data;

      setQueue(queueData);
      const currentId = queueData?.queueId ?? null;
      setQueueId(currentId);

      if (currentId) {
        const eRes = await queueService.getEntries(currentId);
        setEntries(eRes.data.data ?? []);
      } else {
        setEntries([]);
      }
      setFetchError(false);
    } catch (err) {
      console.error(err);
      setFetchError(true);
    } finally {
      if (isInitial) setLoading(false);
    }
  }, []);

  usePolling(() => refresh(false), POLL_INTERVAL_MS, true);

  useEffect(() => {
    refresh(true);
  }, [refresh]);

  const callNext = async () => {
    setCalling(true);
    try {
      const { data } = await queueService.callNext(queueId);
      toast.success(`Called patient #${data.data?.queuePosition}`);
      await refresh();
    } catch (err) {
      const msg = extractError(err, "No patients currently waiting.");
      toast.error(typeof msg === "object" ? msg.summary : msg);
    } finally {
      setCalling(false);
    }
  };

  const entryAction = async (fn, successMsg) => {
    try {
      await fn();
      toast.success(successMsg);
      await refresh();
    } catch (err) {
      const msg = extractError(err, "Action failed.");
      toast.error(typeof msg === "object" ? msg.summary : msg);
    }
  };

  const waiting = entries.filter((e) => e.status === "WAITING").length;
  const called = entries.filter((e) => e.status === "CALLED").length;
  const serving = entries.filter((e) => e.status === "SERVING").length;
  const emergency = entries.filter(
    (e) => e.status === "WAITING" && e.appointmentPriority === "EMERGENCY",
  ).length;

  if (loading)
    return (
      <PageWrapper title="Queue Management">
        <Spinner />
      </PageWrapper>
    );

  const backLink = isProvider ? "/provider" : "/reception";

  return (
    <PageWrapper
      title="Queue Management"
      subtitle={
        queue
          ? `${new Date().toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "long" })}`
          : ""
      }
      action={
        <Link to={backLink}>
          <Button variant="ghost" size="sm" iconComponent={LuChevronLeft}>
            Dashboard
          </Button>
        </Link>
      }
    >
      {fetchError && (
        <Alert variant="warning" message="Could not refresh queue data" />
      )}

      {emergency > 0 && (
        <Alert
          variant="error"
          title={`${emergency} EMERGENCY patient${emergency > 1 ? "s" : ""} waiting`}
          message="Call the emergency patient immediately using the button below."
          className="mb-4"
        />
      )}

      {/* Stats + actions bar */}
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-4 mb-4">
        <div className="flex flex-wrap gap-4 items-center">
          {/* Live counters */}
          <div className="flex gap-3">
            {[
              {
                label: "Waiting",
                value: waiting,
                colour: "text-amber-600",
                bg: "bg-amber-50",
              },
              {
                label: "Called",
                value: called,
                colour: "text-orange-600",
                bg: "bg-orange-50",
              },
              {
                label: "Serving",
                value: serving,
                colour: "text-emerald-600",
                bg: "bg-emerald-50",
              },
            ].map((s) => (
              <div
                key={s.label}
                className={`${s.bg} rounded-xl px-4 py-2 text-center min-w-[70px]`}
              >
                <p className="text-xs text-slate-500">{s.label}</p>
                <p
                  className={`text-2xl font-bold ${s.colour}`}
                  aria-live="polite"
                >
                  {s.value}
                </p>
              </div>
            ))}
          </div>

          {/* Actions */}
          <div className="flex gap-2 flex-wrap ml-auto">
            {isProvider && (
              <Button
                onClick={callNext}
                loading={calling}
                size="lg"
                iconComponent={LuMegaphone}
                disabled={!queueId}
              >
                Call Next
              </Button>
            )}
            {isProvider && queue?.status === "OPEN" && (
              <Button
                variant="secondary"
                size="sm"
                onClick={() =>
                  entryAction(
                    () =>
                      queueService.updateStatus(queueId, {
                        status: "PAUSED",
                      }),
                    "Queue paused",
                  )
                }
                iconComponent={LuPause}
              >
                Pause
              </Button>
            )}
            {isProvider && queue?.status === "PAUSED" && (
              <Button
                variant="success"
                size="sm"
                onClick={() =>
                  entryAction(
                    () =>
                      queueService.updateStatus(queueId, {
                        status: "OPEN",
                      }),
                    "Queue resumed",
                  )
                }
                iconComponent={LuPlay}
              >
                Resume
              </Button>
            )}
          </div>
        </div>

        {queue && (
          <p className="text-xs text-slate-400 mt-3 border-t border-slate-50 pt-3">
            Status:{" "}
            <strong className="capitalize">
              {queue.status?.toLowerCase()}
            </strong>
            {" · "}Total registered: <strong>{queue.totalRegistered}</strong>
            {" · "}Current position: <strong>{queue.currentPosition}</strong>
          </p>
        )}
      </div>

      {/* Entries table */}
      {entries.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-slate-100">
          <LuUser
            className="mx-auto w-12 h-12 text-slate-300 mb-3"
            aria-hidden="true"
          />
          <p className="text-slate-500">
            No patients have checked in yet today.
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-x-auto">
          <table className="w-full text-sm" aria-label="Queue entries">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-100">
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">
                  #
                </th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">
                  Patient
                </th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">
                  Status
                </th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">
                  Priority
                </th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {entries.map((e) => (
                <QueueEntryRow
                  key={e.entryId}
                  entry={e}
                  onServing={
                    isProvider
                      ? (id) =>
                          entryAction(
                            () => queueService.startServing(id),
                            "Serving started",
                          )
                      : null
                  }
                  onComplete={
                    isProvider
                      ? (id) =>
                          entryAction(
                            () => queueService.complete(id),
                            "Consultation completed",
                          )
                      : null
                  }
                  onMissed={(id) =>
                    entryAction(
                      () => queueService.markMissed(id),
                      "Marked as missed",
                    )
                  }
                />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </PageWrapper>
  );
}
