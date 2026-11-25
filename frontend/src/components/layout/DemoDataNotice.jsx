import { FlaskConical } from 'lucide-react';
import { useDemoMode } from '../../hooks/useDemoMode.js';

/** Site-wide notice shown while the website runs on demonstration data. */
export default function DemoDataNotice() {
  const isDemo = useDemoMode();
  if (!isDemo) return null;
  return (
    <div role="note" className="border-b border-violet-200 bg-violet-50 text-violet-900">
      <div className="container-page flex items-start gap-2 py-2 text-xs sm:items-center sm:text-sm">
        <FlaskConical className="mt-0.5 size-4 shrink-0 sm:mt-0" aria-hidden="true" />
        <p>
          <strong className="font-semibold">Demonstration data:</strong> all products, brands, prices and stock levels shown
          are samples for testing. They are not SIVA Crackers&apos; actual range or prices.
        </p>
      </div>
    </div>
  );
}
