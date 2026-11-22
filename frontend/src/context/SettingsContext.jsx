import { useMemo } from 'react';
import { SettingsContext } from './settings-context.js';
import { useAsync } from '../hooks/useAsync.js';
import { settingsService } from '../services/settingsService.js';

// Loads shop settings (name, contact details, disclaimers, announcement) once
// and shares them with every component through useSettings().
export function SettingsProvider({ children }) {
  const { data, loading, error } = useAsync(() => settingsService.get(), []);
  const value = useMemo(() => ({ settings: data ?? null, loading, error }), [data, loading, error]);
  return <SettingsContext.Provider value={value}>{children}</SettingsContext.Provider>;
}
