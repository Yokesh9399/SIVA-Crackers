import { useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import AnnouncementBar from '../components/layout/AnnouncementBar.jsx';
import Header from '../components/layout/Header.jsx';
import LegalNotice from '../components/layout/LegalNotice.jsx';
import DemoDataNotice from '../components/layout/DemoDataNotice.jsx';
import Footer from '../components/layout/Footer.jsx';

/** Shared page frame: announcement bar, header, page content, legal notice, footer. */
export default function MainLayout() {
  const { pathname } = useLocation();

  // Start each new page at the top.
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  return (
    <div className="flex min-h-screen flex-col">
      <a href="#main" className="sr-only z-50 rounded bg-gold-400 px-4 py-2 text-navy-900 focus:not-sr-only focus:fixed focus:top-2 focus:left-2">
        Skip to content
      </a>
      {/* Site chrome is hidden on paper; printable pages render their own PrintHeader. */}
      <div className="print:hidden">
        <AnnouncementBar />
      </div>
      <Header />
      <div className="print:hidden">
        <DemoDataNotice />
      </div>
      <main id="main" className="flex-1">
        <Outlet />
      </main>
      <div className="print:hidden">
        <LegalNotice />
        <Footer />
      </div>
    </div>
  );
}
