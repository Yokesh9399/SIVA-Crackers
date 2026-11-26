import { describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AdminRoute from '../AdminRoute.jsx';
import AdminLoginPage from '../pages/AdminLoginPage.jsx';
import { renderAdmin } from '../../test/renderAdmin.jsx';

const protectedRoutes = (extra = {}) => ({
  '/admin/login': <AdminLoginPage />,
  '/admin': <AdminRoute />,
  ...extra,
});

describe('Protected admin routes', () => {
  it('sends visitors to the admin login page', async () => {
    renderAdmin(protectedRoutes(), { at: '/admin', auth: { user: null, status: 'anonymous' } });
    expect(await screen.findByRole('heading', { name: 'Admin login' })).toBeInTheDocument();
    expect(screen.getByTestId('location')).toHaveTextContent('/admin/login');
  });

  it('shows a loader while the session is being checked', () => {
    renderAdmin(protectedRoutes(), { at: '/admin', auth: { user: null, status: 'loading' } });
    expect(screen.getByRole('status', { name: 'Loading' })).toBeInTheDocument();
  });

  it('refuses customers and other staff (only the OWNER may use the dashboard)', () => {
    for (const role of ['CUSTOMER', 'MANAGER', 'CASHIER']) {
      const { unmount } = renderAdmin(protectedRoutes(), { at: '/admin', auth: { user: { fullName: 'Asha', role } } });
      expect(screen.getByRole('heading', { name: 'No access to the admin dashboard' })).toBeInTheDocument();
      unmount();
    }
  });

  it('lets the OWNER in', () => {
    renderAdmin({ '/admin': <AdminRoute />, '/admin/inside': <p>inside</p> }, { at: '/admin' });
    expect(screen.queryByText('No access to the admin dashboard')).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Admin login' })).not.toBeInTheDocument();
  });
});

describe('Admin login page', () => {
  const anonymous = { user: null, status: 'anonymous' };

  it('checks the fields before calling the server', async () => {
    const { auth } = renderAdmin({ '/admin/login': <AdminLoginPage /> }, { at: '/admin/login', auth: anonymous });
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));
    expect(screen.getByText('Please enter your email address or mobile number.')).toBeInTheDocument();
    expect(screen.getByText('Please enter your password.')).toBeInTheDocument();
    expect(auth.staffLogin).not.toHaveBeenCalled();
  });

  it('logs in through the staff login and opens the dashboard', async () => {
    const staffLogin = vi.fn().mockResolvedValue({});
    renderAdmin({ '/admin/login': <AdminLoginPage />, '/admin': <p>dashboard</p> }, { at: '/admin/login', auth: { ...anonymous, staffLogin } });
    await userEvent.type(screen.getByLabelText('Email or mobile number'), '  owner@example.test ');
    await userEvent.type(screen.getByLabelText('Password'), 'Secret123');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));
    expect(staffLogin).toHaveBeenCalledWith({ identifier: 'owner@example.test', password: 'Secret123' });
    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent(/^\/admin$/));
  });

  it('shows the server message and clears the password on failure', async () => {
    const staffLogin = vi.fn().mockRejectedValue(new Error('Incorrect email/phone or password.'));
    renderAdmin({ '/admin/login': <AdminLoginPage /> }, { at: '/admin/login', auth: { ...anonymous, staffLogin } });
    await userEvent.type(screen.getByLabelText('Email or mobile number'), 'owner@example.test');
    await userEvent.type(screen.getByLabelText('Password'), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));
    expect(await screen.findByText('Incorrect email/phone or password.')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toHaveValue('');
  });

  it('explains an ended session', () => {
    renderAdmin({ '/admin/login': <AdminLoginPage /> }, { at: '/admin/login', auth: { ...anonymous, sessionExpired: true } });
    expect(screen.getByText('Your session has ended. Please log in again.')).toBeInTheDocument();
  });

  it('password field never stores anything in browser storage', async () => {
    renderAdmin({ '/admin/login': <AdminLoginPage /> }, { at: '/admin/login', auth: anonymous });
    await userEvent.type(screen.getByLabelText('Password'), 'Secret123');
    expect(JSON.stringify({ ...localStorage })).not.toContain('Secret123');
    expect(JSON.stringify({ ...sessionStorage })).not.toContain('Secret123');
  });
});
