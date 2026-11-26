import { describe, expect, it } from 'vitest';
import { screen, within } from '@testing-library/react';
import { Route } from 'react-router-dom';
import RequireRole from '../RequireRole.jsx';
import AdminLayout from '../AdminLayout.jsx';
import { ROLES_FOR, homeFor, sectionsFor } from '../roles.js';
import { renderAdmin } from '../../test/renderAdmin.jsx';

const user = (role) => ({ user: { id: 9, fullName: 'Ravi Kumar', email: 'r@example.test', role } });

describe('Role table in the admin', () => {
  it('menu sections follow the role table', () => {
    const labels = (role) => sectionsFor(role).map((s) => s.label);
    expect(labels('OWNER')).toEqual(expect.arrayContaining(['Dashboard', 'Products', 'Stock', 'Staff', 'Activity log', 'Business Settings', 'Website Content']));
    expect(labels('MANAGER')).toEqual(expect.arrayContaining(['Dashboard', 'Products', 'Stock', 'Suppliers', 'Expenses', 'Reports', 'Customers']));
    expect(labels('MANAGER')).not.toEqual(expect.arrayContaining(['Staff']));
    expect(labels('MANAGER')).not.toContain('Business Settings');
    expect(labels('MANAGER')).not.toContain('Website Content');
    expect(labels('CASHIER')).toEqual(['Stock', 'Stock history', 'Customers']);
    expect(homeFor('CASHIER')).toBe('/admin/inventory');
    expect(homeFor('MANAGER')).toBe('/admin');
  });

  it('the sidebar shows only the allowed sections and the role name', () => {
    renderAdmin({ '/admin': <AdminLayout /> }, { at: '/admin', auth: user('CASHIER') });
    const nav = screen.getByRole('complementary', { name: 'Admin navigation' });
    expect(within(nav).getByRole('link', { name: 'Stock' })).toBeInTheDocument();
    expect(within(nav).queryByRole('link', { name: 'Products' })).not.toBeInTheDocument();
    expect(within(nav).queryByRole('link', { name: 'Staff' })).not.toBeInTheDocument();
    expect(screen.getByText('Cashier')).toBeInTheDocument();
  });

  it('pages for other roles show a clear message instead of the page', () => {
    const routes = {
      '/admin/products': <RequireRole roles={ROLES_FOR.runShop} />,
      '/admin/staff': <RequireRole roles={ROLES_FOR.owner} />,
    };
    const { unmount } = renderAdmin(routes, { at: '/admin/products', auth: user('CASHIER') });
    expect(screen.getByRole('heading', { name: 'This page is not available for your role' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Go to my start page' })).toHaveAttribute('href', '/admin/inventory');
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
