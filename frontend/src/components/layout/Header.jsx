import { useEffect, useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { Menu, Search, UserRound, ClipboardList } from 'lucide-react';
import Logo from './Logo.jsx';
import MobileMenu from './MobileMenu.jsx';
import { NAV_LINKS } from '../../utils/constants.js';

/** Sticky site header: logo, search, account links, desktop nav and the mobile menu trigger. */
export default function Header() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [menuOpen, setMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const submitSearch = (event) => {
    event.preventDefault();
    const term = query.trim();
    navigate(term ? `/products?search=${encodeURIComponent(term)}` : '/products');
  };

  return (
    <header className={`sticky top-0 z-40 bg-navy-900/95 backdrop-blur transition-shadow ${scrolled ? 'shadow-lg shadow-navy-950/30' : ''}`}>
      <div className="container-page flex h-16 items-center gap-2 sm:gap-3 lg:h-[4.5rem]">
        <button
          type="button"
          onClick={() => setMenuOpen(true)}
          className="-ml-2 rounded-lg p-2 text-white hover:bg-white/10 lg:hidden"
          aria-label="Open menu"
          aria-expanded={menuOpen}
        >
          <Menu className="size-6" aria-hidden="true" />
        </button>

        <Logo />

        <form onSubmit={submitSearch} role="search" className="mx-auto hidden w-full max-w-md md:block">
          <label htmlFor="header-search" className="sr-only">
            Search products
          </label>
          <div className="relative">
            <Search className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-slate-400" aria-hidden="true" />
            <input
              id="header-search"
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search sparklers, rockets, gift boxes…"
              className="w-full rounded-full border border-white/10 bg-white/10 py-2.5 pr-4 pl-10 text-sm text-white placeholder:text-white/50 focus:border-gold-400 focus:bg-white/15 focus:outline-none"
            />
          </div>
        </form>

        <div className="ml-auto flex shrink-0 items-center gap-1 md:ml-0">
          <NavLink
            to="/estimate"
            className="hidden items-center gap-1.5 rounded-full px-3 py-2 text-sm font-medium text-white/80 hover:bg-white/10 hover:text-white sm:inline-flex"
            title="My Estimate List (for your reference only)"
          >
            <ClipboardList className="size-5" aria-hidden="true" />
            <span className="hidden xl:inline">Estimate</span>
          </NavLink>
          <NavLink to="/login" className="btn-gold px-3 py-2 sm:px-5">
            <UserRound className="size-4" aria-hidden="true" />
            <span className="hidden sm:inline">Login / Signup</span>
            <span className="sm:hidden">Login</span>
          </NavLink>
        </div>
      </div>

      {/* Mobile search */}
      <form onSubmit={submitSearch} role="search" className="container-page pb-3 md:hidden">
        <label htmlFor="mobile-search" className="sr-only">
          Search products
        </label>
        <div className="relative">
          <Search className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-slate-400" aria-hidden="true" />
          <input
            id="mobile-search"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search products…"
            className="w-full rounded-full border border-white/10 bg-white/10 py-2.5 pr-4 pl-10 text-sm text-white placeholder:text-white/50 focus:border-gold-400 focus:outline-none"
          />
        </div>
      </form>

      {/* Desktop navigation */}
      <nav aria-label="Main" className="hidden border-t border-white/10 lg:block">
        <ul className="container-page flex items-center gap-1">
          {NAV_LINKS.map((link) => (
            <li key={link.to}>
              <NavLink
                to={link.to}
                end={link.to === '/'}
                className={({ isActive }) =>
                  `relative block px-4 py-3 text-sm font-medium transition ${
                    isActive ? 'text-gold-400' : 'text-white/80 hover:text-white'
                  } after:absolute after:inset-x-4 after:bottom-0 after:h-0.5 after:rounded-full after:bg-gold-400 after:transition-transform ${
                    isActive ? 'after:scale-x-100' : 'after:scale-x-0 hover:after:scale-x-100'
                  }`
                }
              >
                {link.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      <MobileMenu open={menuOpen} onClose={() => setMenuOpen(false)} />
    </header>
  );
}
