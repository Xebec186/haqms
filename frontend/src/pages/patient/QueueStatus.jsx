import { useState, useCallback } from "react";
import { useParams, Link } from "react-router-dom";
import { usePolling } from "../../hooks/usePolling";
import queueService from "../../services/queueService";
import { POLL_INTERVAL_MS } from "../../utils/constants";
import PageWrapper from "../../components/layout/PageWrapper";
import QueueStatusCard from "../../components/features/QueueStatusCard";
import { LuTriangleAlert } from "react-icons/lu";
import Spinner from "../../components/ui/Spinner";
import Button from "../../components/ui/Button";

export default function QueueStatus() {
  const { appointmentId } = useParams();

  const [entry, setEntry] = useState(null);
  const [estimatedWait, setWait] = useState(0);
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [fetchError, setFetchError] = useState(false);

  const fetchStatus = useCallback(async () => {
    try {
      const { data } = await queueService.getQueueEntry(appointmentId);
      const myEntry = data?.data;
      console.log("Fetched queue status:", myEntry);
      setEntry(myEntry);
      setWait(myEntry?.estimatedWaitMinutes ?? null);
      setLastUpdated(new Date());
      setFetchError(false);
    } catch (err) {
      // Don't show error if appointment not checked in (404)
      if (err.response?.status !== 404) {
        setFetchError(true);
      }
    } finally {
      setLoading(false);
    }
  }, [appointmentId]);

  // Stop polling once the entry reaches a terminal state
  const isTerminal = entry && ["COMPLETED", "MISSED"].includes(entry.status);
  usePolling(fetchStatus, POLL_INTERVAL_MS, !isTerminal);

  return (
    <PageWrapper
      title="Queue Status"
      subtitle="Your position updates automatically every 30 seconds"
    >
      {loading ? (
        <Spinner />
      ) : (
        <div className="max-w-sm mx-auto flex flex-col gap-4">
          <QueueStatusCard entry={entry} estimatedWait={estimatedWait} />

          {/* Live update footer */}
          {lastUpdated && !isTerminal && (
            <div className="flex items-center justify-center gap-2 text-xs text-slate-400">
              <span
                className="inline-block h-2 w-2 rounded-full bg-emerald-400 animate-pulse"
                aria-hidden="true"
              />
              <span aria-live="polite">
                Updated {lastUpdated.toLocaleTimeString()} · refreshes every{" "}
                {POLL_INTERVAL_MS / 1000}s
              </span>
            </div>
          )}

          {fetchError && (
            <div className="text-center text-xs text-danger bg-red-50 rounded-lg py-2 px-3 flex items-center justify-center gap-2">
              <LuTriangleAlert className="w-4 h-4 text-red-700" />
              <span>Could not refresh — check your connection</span>
            </div>
          )}

          {/* Navigation after terminal state */}
          {isTerminal && (
            <div className="text-center pt-2 flex flex-col gap-2">
              <Link to="/patient/appointments">
                <Button variant="secondary" fullWidth>
                  Back to My Appointments
                </Button>
              </Link>
              <Link to="/patient">
                <Button variant="ghost" fullWidth>
                  Go to Dashboard
                </Button>
              </Link>
            </div>
          )}
        </div>
      )}
    </PageWrapper>
  );
}
