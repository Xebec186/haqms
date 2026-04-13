import { useEffect, useRef } from 'react';

/**
 * Polls fetchFn immediately, then every intervalMs milliseconds.
 * Clears the interval on unmount or when enabled becomes false.
 *
 * @param {Function} fetchFn   - async function to call on each tick
 * @param {number}   intervalMs - polling interval in milliseconds
 * @param {boolean}  enabled   - set to false to pause polling
 */
export function usePolling(fetchFn, intervalMs, enabled = true) {
  const savedFn = useRef(fetchFn);

  // Always call the latest version of fetchFn
  useEffect(() => {
    savedFn.current = fetchFn;
  }, [fetchFn]);

  useEffect(() => {
    if (!enabled) return;

    // Immediate first call
    savedFn.current();

    const id = setInterval(() => savedFn.current(), intervalMs);
    return () => clearInterval(id);
  }, [intervalMs, enabled]);
}
