import { useContext } from 'react';
import { SettingsContext } from '../context/settings-context.js';

/** Returns { settings, loading, error }. `settings` is null until loaded. */
export function useSettings() {
  return useContext(SettingsContext);
}
