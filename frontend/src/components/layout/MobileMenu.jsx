import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { Link, NavLink } from 'react-router-dom';
import { ChevronRight, X } from 'lucide-react';
import Logo from './Logo.jsx';
import { NAV_LINKS } from '../../utils/constants.js';
import { useAsync } from '../../hooks/useAsync.js';
import { brandService, categoryService } from '../../services/catalogService.js';

const TABS = ['Menu', 'Categories', 'Brands'];

/** Slide-in drawer for phones/tablets with Menu, Categories and Brands tabs. */
export default function MobileMenu({ open, onClose }) {
  const [tab, setTab] = useState('Menu');
  const categories = useAsync(() => categoryService.getAll(), []);
  const brands = useAsync(() => brandService.getAll(), []);

  // Close with Escape and stop the page behind from scrolling while open.
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = previous;
    };
  }, [open, onClose]);

  const list = tab === 'Categories' ? categories : brands;
  const basePath = tab === 'Categories' ? '/categories' : '/brands';

  // Rendered into <body> through a portal: the header uses backdrop-blur, which would
  // otherwise trap this fixed-position drawer inside the header's height.
  return createPortal(
    <div className={`fixed inset-0 z-50 lg:hidden ${open ? '' : 'pointer-events-none'}`} inert={!open}>
      <div
        className={`absolute inset-0 bg-navy-950/60 transition-opacity duration-300 ${open ? 'opacity-100' : 'opacity-0'}`}
        onClick={onClose}
      />
      <aside
        role="dialog"
        aria-modal="true"
        aria-label="Site menu"
        className={`absolute inset-y-0 left-0 flex w-[86%] max-w-sm flex-col bg-navy-900 shadow-2xl transition-transform duration-300 ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex items-center justify-between border-b border-white/10 px-4 py-4">
          <Logo />
          <button type="button" onClick={onClose} className="rounded-lg p-2 text-white hover:bg-white/10" aria-label="Close menu">
            <X className="size-6" aria-hidden="true" />
          </button>
        </div>

        <div className="flex gap-1 border-b border-white/10 px-3 pt-3" role="tablist">
          {TABS.map((name) => (
            <button
              key={name}
              type="button"
              role="tab"
              aria-selected={tab === name}
              onClick={() => setTab(name)}
              className={`flex-1 rounded-t-lg px-3 py-2.5 text-sm font-semibold transition ${
                tab === name ? 'bg-gold-400 text-navy-900' : 'text-white/70 hover:text-white'
              }`}
            >
              {name}
            </button>
          ))}
        </div>

        <div className="flex-1 overflow-y-auto px-3 py-3">
          {tab === 'Menu' ? (
            <ul className="space-y-1">
              {NAV_LINKS.map((link) => (
                <li key={link.to}>
                  <NavLink
                    to={link.to}
                    end={link.to === '/'}
                    onClick={onClose}
                    className={({ isActive }) =>
                      `flex items-center justify-between rounded-lg px-3 py-3 text-[0.95rem] font-medium ${
                        isActive ? 'bg-white/10 text-gold-400' : 'text-white/85 hover:bg-white/5'
                      }`
                    }
                  >
                    {link.label}
                    <ChevronRight className="size-4 opacity-50" aria-hidden="true" />
                  </NavLink>
                </li>
              ))}
              <li className="pt-2">
                <NavLink to="/estimate" onClick={onClose} className="flex rounded-lg px-3 py-3 text-[0.95rem] font-medium text-white/85 hover:bg-white/5">
                  My Estimate List
                </NavLink>
              </li>
            </ul>
          ) : (
            <ul className="space-y-1">
              {list.loading && <li className="px-3 py-3 text-sm text-white/60">Loading…</li>}
              {list.error && <li className="px-3 py-3 text-sm text-red-300">Could not load {tab.toLowerCase()}.</li>}
              {list.data?.map((item) => (
                <li key={item.id}>
                  <Link
                    to={`${basePath}/${item.slug}`}
                    onClick={onClose}
                    className="flex items-center justify-between rounded-lg px-3 py-3 text-[0.95rem] text-white/85 hover:bg-white/5"
                  >
                    {item.name}
                    <span className="rounded-full bg-white/10 px-2 py-0.5 text-xs text-white/60">{item.productCount}</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      </aside>
    </div>,
    document.body,
  );
}
