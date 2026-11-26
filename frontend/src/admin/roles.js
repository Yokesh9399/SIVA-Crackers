import {
  LayoutDashboard, Package, Tags, Award, Boxes, History, Truck, Wallet, Contact, BarChart3, Users, ScrollText,
  PanelsTopLeft, Settings,
} from 'lucide-react';

// Role table (Phase 0, confirmed for Phase 5/6). The server enforces the same rules on every request;
// this only decides what each person sees in the admin.
export const STAFF_ROLES = ['OWNER', 'MANAGER', 'CASHIER'];
const ALL = STAFF_ROLES;
const RUN_SHOP = ['OWNER', 'MANAGER'];
const OWNER = ['OWNER'];

export const ROLE_LABELS = { OWNER: 'Owner', MANAGER: 'Manager', CASHIER: 'Cashier', CUSTOMER: 'Customer' };

export const ROLE_DESCRIPTIONS = {
  MANAGER: 'Products, categories, brands, stock, suppliers, expenses, customers and reports. No staff, settings or homepage.',
  CASHIER: 'Views stock and looks up customers. Cannot change anything.',
};

/** Sidebar sections, in order, with who may open them. */
export const ADMIN_SECTIONS = [
  { to: '/admin', label: 'Dashboard', icon: LayoutDashboard, end: true, roles: RUN_SHOP },
  { to: '/admin/products', label: 'Products', icon: Package, roles: RUN_SHOP },
  { to: '/admin/categories', label: 'Categories', icon: Tags, roles: RUN_SHOP },
  { to: '/admin/brands', label: 'Brands', icon: Award, roles: RUN_SHOP },
  { to: '/admin/inventory', label: 'Stock', icon: Boxes, roles: ALL },
  { to: '/admin/stock-history', label: 'Stock history', icon: History, roles: ALL },
  { to: '/admin/suppliers', label: 'Suppliers', icon: Truck, roles: RUN_SHOP },
  { to: '/admin/expenses', label: 'Expenses', icon: Wallet, roles: RUN_SHOP },
  { to: '/admin/customers', label: 'Customers', icon: Contact, roles: ALL },
  { to: '/admin/reports', label: 'Reports', icon: BarChart3, roles: RUN_SHOP },
  { to: '/admin/staff', label: 'Staff', icon: Users, roles: OWNER },
  { to: '/admin/activity', label: 'Activity log', icon: ScrollText, roles: OWNER },
  { to: '/admin/content', label: 'Website Content', icon: PanelsTopLeft, roles: OWNER },
  { to: '/admin/settings', label: 'Business Settings', icon: Settings, roles: OWNER },
];

export const ROLES_FOR = {
  runShop: RUN_SHOP,
  owner: OWNER,
  all: ALL,
};

export function sectionsFor(role) {
  return ADMIN_SECTIONS.filter((s) => s.roles.includes(role));
}

/** Can this role change data in the "run the shop" areas (stock, customers)? */
export function canEdit(role) {
  return RUN_SHOP.includes(role);
}

/** Where each role starts after logging in. */
export function homeFor(role) {
  return RUN_SHOP.includes(role) ? '/admin' : '/admin/inventory';
}
