import { useState } from 'react';
import { Sparkles, X } from 'lucide-react';
import { useSettings } from '../../hooks/useSettings.js';

/** Thin promotional strip at the very top. Text and on/off come from settings (admin-managed). */
export default function AnnouncementBar() {
  const { settings } = useSettings();
  const [dismissed, setDismissed] = useState(false);
  const announcement = settings?.announcement;

  if (dismissed || !announcement?.active || !announcement.text) return null;

  return (
    <div className="relative bg-gradient-to-r from-orange-600 via-orange-500 to-gold-400 text-navy-950">
      <div className="container-page flex items-center justify-center gap-2 py-2 pr-10 text-center text-xs font-semibold sm:text-sm">
        <Sparkles className="hidden size-4 shrink-0 sm:block" aria-hidden="true" />
        <p>{announcement.text}</p>
      </div>
      <button
        type="button"
        onClick={() => setDismissed(true)}
        className="absolute top-1/2 right-2 -translate-y-1/2 rounded-full p-1.5 hover:bg-black/10"
        aria-label="Dismiss announcement"
      >
        <X className="size-4" aria-hidden="true" />
      </button>
    </div>
  );
}
