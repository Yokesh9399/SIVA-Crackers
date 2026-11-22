import { useCallback, useEffect, useState } from 'react';

/**
 * Runs an async function (usually a service call) and tracks loading / error / data.
 *
 *   const { data, loading, error, retry } = useAsync(() => productService.getFeatured(8), []);
 *
 * `deps` works like a useEffect dependency list: when it changes, the call runs again.
 * Results from an older call are ignored if a newer one has started.
 */
export function useAsync(asyncFn, deps = []) {
  const [attempt, setAttempt] = useState(0);
  const key = JSON.stringify([deps, attempt]);
  const [state, setState] = useState({ key: null, data: undefined, error: null });

  useEffect(() => {
    let cancelled = false;
    asyncFn().then(
      (data) => !cancelled && setState({ key, data, error: null }),
      (error) => !cancelled && setState({ key, data: undefined, error }),
    );
    return () => {
      cancelled = true;
    };
    // asyncFn is usually an inline arrow; re-running is controlled by `deps` instead.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  const retry = useCallback(() => setAttempt((n) => n + 1), []);

  return {
    data: state.data,
    error: state.key === key ? state.error : null,
    loading: state.key !== key,
    retry,
  };
}
