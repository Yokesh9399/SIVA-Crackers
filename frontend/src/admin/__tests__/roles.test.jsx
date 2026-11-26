import { describe, expect, it } from 'vitest';
import { screen, within } from '@testing-library/react';
import { Route } from 'react-router-dom';
import RequireRole from '../RequireRole.jsx';
import AdminLayout from '../AdminLayout.jsx';
import { ROLES_FOR, canVoid, homeFor, sectionsFor } from '../roles.js';
import { renderAdmin } from '../../test/renderAdmin.jsx';

const user = (role) => ({ user: { id: 9, fullName: 'Ravi Kumar', email: 'r@example.test', role } });

describe('Role table in the admin', () => {
  it('menu sections follow the role table', () => {
    const labels = (role) => sectionsFor(role).map((s) => s.label);
    // The owner sees every section, in the order the shop asked for
    expect(labels('OWNER')).toEqual([
      'Dashboard', 'Products', 'Categories', 'Brands', 'Homepage Management', 'Business Settings', 'Staff Management',
      'Inventory', 'Stock Movements', 'Suppliers', 'Billing / POS', 'Bill History', 'Customers', 'Expenses', 'Reports', 'Activity Log',
    ]);
    expect(labels('MANAGER')).toEqual(expect.arrayContaining(['Dashboard', 'Products', 'Inventory', 'Suppliers', 'Billing / POS', 'Expenses', 'Reports', 'Customers']));
    expect(labels('MANAGER')).not.toContain('Staff Management');
    expect(labels('MANAGER')).not.toContain('Business Settings');
    expect(labels('MANAGER')).not.toContain('Homepage Management');
    expect(labels('CASHIER')).toEqual(['Inventory', 'Stock Movements', 'Billing / POS', 'Bill History', 'Customers']);
    expect(homeFor('CASHIER')).toBe('/admin/billing');
    expect(canVoid('OWNER')).toBe(true);
    expect(canVoid('MANAGER')).toBe(true);
    expect(canVoid('CASHIER')).toBe(false);
    expect(homeFor('MANAGER')).toBe('/admin');
  });

  it('the sidebar shows only the allowed sections and the role name', () => {
    renderAdmin({ '/admin': <AdminLayout /> }, { at: '/admin', auth: user('CASHIER') });
    const nav = screen.getByRole('complementary', { name: 'Admin navigation' });
    expect(within(nav).getByRole('link', { name: 'Billing / POS' })).toBeInTheDocument();
    expect(within(nav).queryByRole('link', { name: 'Products' })).not.toBeInTheDocument();
    expect(within(nav).queryByRole('link', { name: 'Staff Management' })).not.toBeInTheDocument();
    expect(screen.getByText('Cashier')).toBeInTheDocument();
  });

  it('pages for other roles show a clear message instead of the page', () => {
    const routes = {
      '/admin/products': <RequireRole roles={ROLES_FOR.runShop} />,
      '/admin/staff': <RequireRole roles={ROLES_FOR.owner} />,
    };
    const { unmount } = renderAdmin(routes, { at: '/admin/products', auth: user('CASHIER') });
    expect(screen.getByRole('heading', { name: 'This page is not available for your role' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Go to my start page' })).toHaveAttribute('href', '/admin/billing');
    unmount();
    const { unmount: u2 } = renderAdmin(routes, { at: '/admin/staff', auth: user('MANAGER') });
    expect(screen.getByRole('heading', { name: 'This page is not available for your role' })).toBeInTheDocument();
    u2();
    renderAdmin(routes, { at: '/admin/products', auth: user('MANAGER') });
    expect(screen.queryByText('This page is not available for your role')).not.toBeInTheDocument();
  });
});

// keep the unused import check quiet for Route (used implicitly by renderAdmin's routes)
void Route;
