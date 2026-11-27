// Regression tests for the problems found during the browser verification on 27 Nov 2026.
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BillingPage from '../pages/BillingPage.jsx';
import BillsPage from '../pages/BillsPage.jsx';
import AdminLoginPage from '../pages/AdminLoginPage.jsx';
import Modal from '../components/Modal.jsx';
import { renderAdmin } from '../../test/renderAdmin.jsx';
import { adminService } from '../adminService.js';
import { startPageFor } from '../roles.js';
import { plural } from '../format.js';

vi.mock('../adminService.js', async (importOriginal) => {
  const real = await importOriginal();
  return { ...real, adminService: Object.fromEntries(Object.keys(real.adminService).map((k) => [k, vi.fn()])) };
});

const CASHIER = { user: { id: 3, fullName: 'Kavi', role: 'CASHIER' } };
const OWNER = { user: { id: 1, fullName: 'Shop Owner', role: 'OWNER' } };
const METHODS = [
  { id: 1, code: 'CASH', label: 'Cash', displayOrder: 10, active: true },
  { id: 2, code: 'UPI', label: 'UPI (paid at the shop)', displayOrder: 20, active: true },
];
const STOCK = {
  content: [{ productId: 5, name: 'Flower Pot', sku: 'FP-1', stockQuantity: 3, stockStatus: 'LOW_STOCK', active: true, price: 120.5 }],
  page: 0, size: 25, totalElements: 1, totalPages: 1,
};

beforeEach(() => {
  vi.clearAllMocks();
  adminService.paymentMethods.mockResolvedValue(METHODS);
  adminService.listStock.mockResolvedValue(STOCK);
  adminService.listCustomers.mockResolvedValue([]);
});

describe('POS product search', () => {
  it('asks the stock API only with values it accepts (sort name,asc; size 25)', async () => {
    renderAdmin({ '/admin/billing': <BillingPage /> }, { at: '/admin/billing', auth: CASHIER });
    await screen.findByText('Flower Pot');
    const [filters] = adminService.listStock.mock.calls[0];
    // Must match AdminProductQuery: sort ∈ default, name,asc, name,desc, price,asc, price,desc, newest; size ∈ 12|24|25|48|50|100
    expect(filters.sort).toBe('name,asc');
    expect(filters.size).toBe(25);
  });

  it('on phones, a summary bar shows the item count and total', async () => {
    renderAdmin({ '/admin/billing': <BillingPage /> }, { at: '/admin/billing', auth: CASHIER });
    await userEvent.click(await screen.findByRole('button', { name: 'Add Flower Pot to the bill' }));
    expect(screen.getByRole('button', { name: /1 item · ₹120\.50\s*View bill/ })).toBeInTheDocument();
  });
});

describe('Dialogs inside a page form', () => {
  it('submitting the new-customer dialog (e.g. Enter) saves the customer only – never the bill', async () => {
    adminService.createCustomer.mockResolvedValue({ id: 42, fullName: 'Meena', phone: '9876500042' });
    renderAdmin({ '/admin/billing': <BillingPage /> }, { at: '/admin/billing', auth: CASHIER });
    await userEvent.click(await screen.findByRole('button', { name: 'Add Flower Pot to the bill' }));
    await userEvent.click(screen.getByRole('button', { name: 'New' }));
    const dialog = screen.getByRole('dialog', { name: 'Add customer' });
    await userEvent.type(within(dialog).getByLabelText('Name'), 'Meena');
    await userEvent.type(within(dialog).getByLabelText(/Mobile number/), '9876500042');
    // The submit event of the dialog's form must not reach the bill form behind it (React events cross portals).
    fireEvent.submit(dialog.querySelector('form'));
    await waitFor(() => expect(adminService.createCustomer).toHaveBeenCalledTimes(1));
    expect(await screen.findByRole('button', { name: 'Remove customer from the bill' })).toBeInTheDocument();
    expect(adminService.createBill).not.toHaveBeenCalled();
  });

  it('dialogs are rendered on <body>, outside any page form', () => {
    const { container } = renderAdmin({ '/': <form data-testid="page-form"><Modal title="Test" onClose={() => {}}><p>inside</p></Modal></form> }, { at: '/' });
    expect(screen.getByRole('dialog', { name: 'Test' }).closest('form')).toBeNull();
    expect(within(container).getByTestId('page-form')).not.toContainElement(screen.getByRole('dialog'));
  });
});

describe('Login goes to a page the role may open', () => {
  it('startPageFor keeps allowed pages and replaces others with the start page', () => {
    expect(startPageFor('CASHIER', '/admin/staff')).toBe('/admin/billing');
    expect(startPageFor('CASHIER', '/admin/bills?search=SC-1')).toBe('/admin/bills?search=SC-1');
    expect(startPageFor('MANAGER', '/admin/settings')).toBe('/admin');
    expect(startPageFor('MANAGER', '/admin/products/5/edit')).toBe('/admin/products/5/edit');
    expect(startPageFor('OWNER', '/admin/staff')).toBe('/admin/staff');
    expect(startPageFor('CASHIER', '/admin')).toBe('/admin/billing');
    expect(startPageFor('CASHIER', '/admin/unknown')).toBe('/admin/billing');
  });

  it('a cashier who logs in after the owner was on Staff lands on Billing / POS', async () => {
    const staffLogin = vi.fn().mockResolvedValue({ id: 3, role: 'CASHIER' });
    renderAdmin({ '/admin/login': <AdminLoginPage />, '/admin/billing': <p>pos</p>, '/admin/staff': <p>staff</p> }, {
      at: '/admin/login',
      auth: { user: null, status: 'anonymous', staffLogin },
    });
    // simulate the redirect state left by the previous session
    window.history.replaceState({ usr: { from: '/admin/staff' } }, '');
    await userEvent.type(screen.getByLabelText('Email or mobile number'), 'kavi@example.test');
    await userEvent.type(screen.getByLabelText('Password'), 'Secret123');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));
    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/admin/billing'));
  });
});

describe('Mark as paid', () => {
  it('starts with the payment method recorded on the bill', async () => {
    adminService.listBills.mockResolvedValue({
      page: { content: [{ id: 4, billNumber: 'SC-2026-000004', status: 'FINALIZED', itemCount: 1, total: 70, paymentMethod: 'UPI (paid at the shop)', paymentStatus: 'PENDING', createdAt: '2026-11-27T04:00:00Z' }], page: 0, size: 25, totalElements: 1, totalPages: 1 },
      finalizedCount: 1, finalizedTotal: 70, voidedCount: 0, pendingTotal: 70,
    });
    adminService.getBill.mockResolvedValue({
      id: 4, billNumber: 'SC-2026-000004', status: 'FINALIZED', paymentStatus: 'PENDING', paymentMethod: METHODS[1], subtotal: 75.5, discountAmount: 5.5, total: 70,
      createdAt: '2026-11-27T04:00:00Z', items: [{ lineNo: 1, productName: 'Flower Pot', productSku: 'FP-1', unitPrice: 75.5, quantity: 1, lineTotal: 75.5 }],
    });
    adminService.markBillPaid.mockResolvedValue({});
    renderAdmin({ '/admin/bills': <BillsPage /> }, { at: '/admin/bills', auth: OWNER });
    await userEvent.click((await screen.findAllByRole('button', { name: 'SC-2026-000004' }))[0]);
    const dialog = await screen.findByRole('dialog', { name: 'Bill SC-2026-000004' });
    await userEvent.click(await within(dialog).findByRole('button', { name: 'Mark as paid' }));
    expect(within(dialog).getByLabelText('Paid with')).toHaveValue('UPI');
    await userEvent.click(within(dialog).getAllByRole('button', { name: 'Mark as paid' }).at(-1));
    await waitFor(() => expect(adminService.markBillPaid).toHaveBeenCalledWith(4, { paymentMethod: 'UPI', amountReceived: null }));
  });
});

describe('Wording', () => {
  it('uses the singular for one', () => {
    expect(plural(1, 'item')).toBe('1 item');
    expect(plural(0, 'bill')).toBe('0 bills');
    expect(plural(2, 'active expense')).toBe('2 active expenses');
  });
});
