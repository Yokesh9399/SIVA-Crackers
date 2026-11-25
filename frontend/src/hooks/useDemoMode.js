import { USE_MOCKS } from '../config/env.js';
import { useSettings } from './useSettings.js';

/**
 * True while the website shows demonstration data: always in mock mode, otherwise whenever the
 * backend reports that visible products are demo data (settings.demoMode).
 */
export function useDemoMode() {
  const { settings } = useSettings();
  return USE_MOCKS || Boolean(settings?.demoMode);
}
