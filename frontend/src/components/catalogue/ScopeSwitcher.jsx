import { NavLink } from 'react-router-dom';

/**
 * Horizontal row of category (or brand) links shown on a category/brand page,
 * so visitors can jump between them without going back.
 */
export default function ScopeSwitcher({ items, basePath, label }) {
  if (!items?.length) return null;
  return (
    <nav aria-label={label} className="no-scrollbar -mx-4 mt-6 flex gap-2 overflow-x-auto px-4 pb-1 sm:mx-0 sm:flex-wrap sm:px-0">
      {items.map((item) => (
        <NavLink
          key={item.id}
          to={`${basePath}/${item.slug}`}
          className={({ isActive }) =>
            `shrink-0 rounded-full px-3.5 py-1.5 text-xs font-semibold whitespace-nowrap ring-1 transition ${
              isActive ? 'bg-gold-400 text-navy-900 ring-gold-400' : 'text-white/80 ring-white/25 hover:text-white hover:ring-white/60'
            }`
          }
        >
          {item.name}
        </NavLink>
      ))}
    </nav>
  );
}
