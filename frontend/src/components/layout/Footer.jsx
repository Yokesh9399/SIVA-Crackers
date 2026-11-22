import { Link } from 'react-router-dom';
import { Clock, Mail, MapPin, Phone } from 'lucide-react';
import Logo from './Logo.jsx';
import PlaceholderValue from '../ui/PlaceholderValue.jsx';
import { useSettings } from '../../hooks/useSettings.js';
import { useAsync } from '../../hooks/useAsync.js';
import { categoryService } from '../../services/catalogService.js';
import { formatAddress, telHref } from '../../utils/format.js';
import { LEGAL_LINKS, NAV_LINKS } from '../../utils/constants.js';

function FooterHeading({ children }) {
  return <h3 className="mb-4 font-display text-sm font-semibold tracking-wider text-gold-400 uppercase">{children}</h3>;
}

export default function Footer() {
  const { settings } = useSettings();
  const { data: categories } = useAsync(() => categoryService.getAll(), []);
  const year = new Date().getFullYear();

  return (
    <footer className="bg-navy-950 text-white/70">
      <div className="container-page grid gap-10 py-12 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <Logo />
          <p className="mt-4 text-sm leading-relaxed">{settings?.tagline}</p>
          <p className="mt-3 text-xs leading-relaxed text-white/50">{settings?.priceDisclaimer}</p>
        </div>

        <div>
          <FooterHeading>Quick links</FooterHeading>
          <ul className="space-y-2 text-sm">
            {NAV_LINKS.map((link) => (
              <li key={link.to}>
                <Link to={link.to} className="hover:text-gold-400">
                  {link.label}
                </Link>
              </li>
            ))}
          </ul>
        </div>

        <div>
          <FooterHeading>Categories</FooterHeading>
          <ul className="grid grid-cols-1 gap-2 text-sm">
            {categories?.slice(0, 8).map((c) => (
              <li key={c.id}>
                <Link to={`/categories/${c.slug}`} className="hover:text-gold-400">
                  {c.name}
                </Link>
              </li>
            ))}
            <li>
              <Link to="/categories" className="font-semibold text-gold-400 hover:underline">
                All categories →
              </Link>
            </li>
          </ul>
        </div>

        <div>
          <FooterHeading>Visit the shop</FooterHeading>
          <ul className="space-y-3 text-sm">
            <li className="flex gap-3">
              <MapPin className="mt-0.5 size-4 shrink-0 text-gold-400" aria-hidden="true" />
              <span>
                {formatAddress(settings?.address)}
                {!settings?.address?.line1 && (
                  <span className="block text-xs">
                    Street address: <PlaceholderValue value={null} />
                  </span>
                )}
              </span>
            </li>
            <li className="flex gap-3">
              <Phone className="mt-0.5 size-4 shrink-0 text-gold-400" aria-hidden="true" />
              {settings?.phone ? (
                <a href={telHref(settings.phone)} className="hover:text-gold-400">
                  {settings.phone}
                </a>
              ) : (
                <span>
                  Phone: <PlaceholderValue value={null} />
                </span>
              )}
            </li>
            <li className="flex gap-3">
              <Mail className="mt-0.5 size-4 shrink-0 text-gold-400" aria-hidden="true" />
              <span>
                Email: <PlaceholderValue value={settings?.email} />
              </span>
            </li>
            <li className="flex gap-3">
              <Clock className="mt-0.5 size-4 shrink-0 text-gold-400" aria-hidden="true" />
              <span>
                Hours: <PlaceholderValue value={settings?.businessHours} />
              </span>
            </li>
          </ul>
        </div>
      </div>

      <div className="border-t border-white/10">
        <div className="container-page flex flex-col gap-3 py-5 text-xs sm:flex-row sm:items-center sm:justify-between">
          <p>
            © {year} {settings?.shopName ?? 'SIVA Crackers'}, Eachanari, Coimbatore. All rights reserved.
          </p>
          <ul className="flex flex-wrap gap-x-4 gap-y-2">
            {LEGAL_LINKS.map((link) => (
              <li key={link.to}>
                <Link to={link.to} className="hover:text-gold-400">
                  {link.label}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </footer>
  );
}
