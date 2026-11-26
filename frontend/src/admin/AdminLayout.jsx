import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { LogOut, Menu, X, ExternalLink } from 'lucide-react';
import Logo from '../components/layout/Logo.jsx';
import { useAuth } from '../hooks/useAuth.js';
import { ROLE_LABELS, sectionsFor } from './roles.js';


function NavItems({ role, onNavigate }) {
  return (
    <ul className="space-y-1">
      {sectionsFor(role).map(({ to, label, icon: Icon, end }) => (
        <li key={to}>
          <NavLink
            to={to}
            end={end}
            onClick={onNavigate}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition ${
                isActive ? 'bg-gold-400 text-navy-950' : 'text-white/80 hover:bg-white/10 hover:text-white'
              }`
            }
          >
            <Icon className="size-5 shrink-0" aria-hidden="true" />
            {label}
          </NavLink>
        </li>
      ))}
    </ul>
  );
}

/** Admin shell: sidebar on large screens, slide-in menu on phones and tablets, top bar with the user's name and role.
 * The menu shows only the sections the user's role may open. */
export default function AdminLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);
  const [leaving, setLeaving] = useState(false);

  // Close the phone menu with Escape.
  useEffect(() => {
    if (!menuOpen) return undefined;
    const onKey = (e) => e.key === 'Escape' && setMenuOpen(false);
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [menuOpen]);

  async function signOut() {
    setLeaving(true);
    await logout().catch(() => {});
    navigate('/admin/login', { replace: true, state: { loggedOut: true } });
  }

  const sidebarFooter = (
    <div className="space-y-1 border-t border-white/10 pt-4">
      <a href="/" target="_blank" rel="noopener" className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm text-white/80 hover:bg-white/10 hover:text-white">
        <ExternalLink className="size-5" aria-hidden="true" /> View website
      </a>
      <button
        type="button"
        onClick={signOut}
        disabled={leaving}
        className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm text-white/80 hover:bg-white/10 hover:text-white"
      >
        <LogOut className="size-5" aria-hidden="true" /> {leaving ? 'Logging out…' : 'Log out'}
      </button>
    </div>
  );

  return (
    <div className="min-h-dvh bg-slate-50">
      <a href="#admin-main" className="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-50 focus:rounded focus:bg-white focus:px-3 focus:py-2">
        Skip to content
      </a>

      {/* Desktop sidebar */}
      <aside className="fixed inset-y-0 left-0 hidden w-64 flex-col bg-navy-900 p-4 lg:flex print:hidden" aria-label="Admin navigation">
        <div className="mb-6 px-2">
          <Logo />
          <p className="mt-2 text-xs font-semibold tracking-wider text-gold-400 uppercase">Admin dashboard</p>
        </div>
        <nav className="flex-1 overflow-y-auto">
          <NavItems role={user?.role} />
        </nav>
        {sidebarFooter}
      </aside>

      {/* Phone / tablet menu */}
      {menuOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <button type="button" className="absolute inset-0 bg-navy-950/60" aria-label="Close menu" onClick={() => setMenuOpen(false)} />
          <div className="relative flex h-full w-72 max-w-[85vw] flex-col bg-navy-900 p-4" role="dialog" aria-modal="true" aria-label="Admin menu">
            <div className="mb-6 flex items-center justify-between px-2">
              <Logo compact />
              <button type="button" onClick={() => setMenuOpen(false)} className="grid size-10 place-items-center rounded-full text-white hover:bg-white/10" aria-label="Close menu">
                <X className="size-5" aria-hidden="true" />
              </button>
            </div>
            <nav className="flex-1 overflow-y-auto" key={location.pathname}>
              <NavItems role={user?.role} onNavigate={() => setMenuOpen(false)} />
            </nav>
            {sidebarFooter}
          </div>
        </div>
      )}

      <div className="lg:pl-64 print:pl-0">
        <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-slate-200 bg-white/95 px-4 backdrop-blur sm:px-6 print:hidden">
          <button
            type="button"
            className="grid size-10 place-items-center rounded-full text-navy-900 hover:bg-slate-100 lg:hidden"
            onClick={() => setMenuOpen(true)}
            aria-label="Open menu"
            aria-expanded={menuOpen}
          >
            <Menu className="size-5" aria-hidden="true" />
          </button>
          <span className="font-display font-bold text-navy-900 lg:hidden">SIVA Admin</span>
          <div className="ml-auto flex min-w-0 items-center gap-3">
            <div className="min-w-0 text-right">
              <p className="truncate text-sm font-semibold text-navy-900">{user?.fullName}</p>
              <p className="text-xs text-slate-500">{ROLE_LABELS[user?.role] ?? 'Staff'}</p>
            </div>
            <span className="grid size-9 shrink-0 place-items-center rounded-full bg-gold-400 font-display font-bold text-navy-950" aria-hidden="true">
              {user?.fullName?.trim()?.[0]?.toUpperCase() ?? 'O'}
            </span>
          </div>
        </header>
        <main id="admin-main" className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
