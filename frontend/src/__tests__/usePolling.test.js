import { renderHook, act } from '@testing-library/react';
import { usePolling } from '../../hooks/usePolling';

jest.useFakeTimers();

describe('usePolling', () => {

  afterEach(() => {
    jest.clearAllTimers();
  });

  test('calls fetchFn immediately on mount', () => {
    const fn = jest.fn().mockResolvedValue(undefined);
    renderHook(() => usePolling(fn, 5000, true));
    expect(fn).toHaveBeenCalledTimes(1);
  });

  test('calls fetchFn again after each interval', () => {
    const fn = jest.fn().mockResolvedValue(undefined);
    renderHook(() => usePolling(fn, 5000, true));

    act(() => { jest.advanceTimersByTime(5000); });
    expect(fn).toHaveBeenCalledTimes(2);

    act(() => { jest.advanceTimersByTime(5000); });
    expect(fn).toHaveBeenCalledTimes(3);
  });

  test('does NOT call fetchFn when enabled is false', () => {
    const fn = jest.fn();
    renderHook(() => usePolling(fn, 5000, false));
    act(() => { jest.advanceTimersByTime(10000); });
    expect(fn).not.toHaveBeenCalled();
  });

  test('clears interval on unmount', () => {
    const fn = jest.fn().mockResolvedValue(undefined);
    const { unmount } = renderHook(() => usePolling(fn, 5000, true));
    unmount();
    act(() => { jest.advanceTimersByTime(15000); });
    // Only the initial call; no more after unmount
    expect(fn).toHaveBeenCalledTimes(1);
  });

  test('always uses latest fetchFn reference', () => {
    const fn1 = jest.fn().mockResolvedValue(undefined);
    const fn2 = jest.fn().mockResolvedValue(undefined);

    let currentFn = fn1;
    const { rerender } = renderHook(() => usePolling(currentFn, 5000, true));

    currentFn = fn2;
    rerender();

    act(() => { jest.advanceTimersByTime(5000); });
    expect(fn1).toHaveBeenCalledTimes(1); // initial call only
    expect(fn2).toHaveBeenCalledTimes(1); // interval tick uses updated ref
  });
});
