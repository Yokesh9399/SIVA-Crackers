import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BillingPage from '../pages/BillingPage.jsx';
import BillsPage from '../pages/BillsPage.jsx';
import ReportsPage from '../pages/ReportsPage.jsx';
import { renderAdmin } from '../../test/renderAdmin.jsx';
import { adminService, invoiceUrl } from '../adminService.js';
import { discountPaise, subtotalPaise } from '../billing.js';

vi.mock('../adminService.js', async (importOriginal) => {
  const real = await importOriginal();
  return { ...real, adminService: Object.fromEntries(Object.keys(real.adminService).map((k) => [k, vi.fn()])) };
});

const OWNER = { user: { id: 1, fullName: 'Shop Owner', role: 'OWNER' } };
const CASHIER = { user: { id: 3, fullName: 'Kavi', role: 'CASHIER' } };

const METHODS = [
  { id: 1, code: 'CASH', label: 'Cash', displayOrder: 10, active: true },
  { id: 2, code: 'UPI', label: 'UPI (paid at the shop)', displayOrder: 20, active: true },
  { id: 3, code: 'OLD', label: 'Old method', displayOrder: 30, active: false },
];
const STOCK = {
  content: [
    { productId: 5, name: 'Flower Pot', sku: 'FP-1', stockQuantity: 3, stockStatus: 'LOW_STOCK', active: true, price: 120.5, packSize: '10 pcs' },
    { productId: 6, name: 'Sparkler', sku: 'SP-1', stockQuantity: 0, stockStatus: 'OUT_OF_STOCK', active: true, price: 40 },
    { productId: 7, name: 'Old Rocket', sku: 'RK-1', stockQuantity: 9, stockStatus: 'IN_STOCK', active: false, price: 10 },
  ],
  page: 0, size: 25, totalElements: 3, totalPages: 1,
};
const SAVED = {
  id: 11, billNumber: 'SC-2026-000011', status: 'FINALIZED', total: 216.9, paymentMethod: METHODS[0], paymentStatus: 'PAID',
  amountReceived: 300, changeDue: 83.1, replayed: false, items: [],
};

beforeEach(() => {
  vi.clearAllMocks();
  adminService.paymentMethods.mockResolvedValue(METHODS);
  adminService.listStock.mockResolvedValue(STOCK);
  adminService.listCustomers.mockResolvedValue([]);
});

describe('Bill arithmetic (paise)', () => {
  it('adds lines exactly and caps the discount at 20% of the subtotal', () => {
    const sub = subtotalPaise([{ price: 0.1, quantity: 3 }, { price: 120.5, quantity: 2 }]);
    expect(sub).toBe(24130);
    expect(discountPaise('PERCENT', '10', sub)).toEqual({ paise: 2413 });
    expect(discountPaise('AMOUNT', '48.26', sub)).toEqual({ paise: 4826 });
    expect(discountPaise('AMOUNT', '48.27', sub).error).toMatch(/at most 20%/);
    expect(discountPaise('PERCENT', '21', sub).error).toMatch(/at most 20%/);
    expect(discountPaise('AMOUNT', '1.234', sub).error).toMatch(/2 decimal places/);
    expect(discountPaise('', '', sub)).toEqual({ paise: 0 });
  });
});

describe('Billing / POS', () => {
  it('lists only active products, blocks out-of-stock ones and caps quantity at stock', async () => {
    renderAdmin({ '/admin/billing': <BillingPage /> }, { at: '/admin/billing', auth: CASHIER });
    expect(await screen.findByText('Flower Pot')).toBeInTheDocument();
    expect(screen.queryByText('Old Rocket')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Add Sparkler to the bill' })).toBeDisabled();

    const add = screen.getByRole('button', { name: 'Add Flower Pot to the bill' });
    await userEvent.click(add);
    await userEvent.click(add);
    await userEvent.click(add);
    expect(add).toBeDisabled(); // all 3 in stock are on the bill
    const bill = screen.getByRole('list', { name: 'Products on this bill' });
    expect(within(bill).getByLabelText('Quantity of Flower Pot')).toHaveValue(3);
    expect(within(bill).getByRole('button', { name: 'One more Flower Pot' })).toBeDisabled();
    expect(screen.getByTestId('pos-total')).toHaveTextContent('₹361.50');

    await userEvent.click(within(bill).getByRole('button', { name: 'Remove Flower Pot' }));
    expect(screen.queryByRole('list', { name: 'Products on this bill' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Save bill/ })).toBeDisabled();
  });

  it('only offers active payment methods and saves the bill once with a request key', async () => {
    adminService.createBill.mockResolvedValue(SAVED);
    renderAdmin({ '/admin/billing': <BillingPage /> }, { at: '/admin/billing', auth: CASHIER });
    await userEvent.click(await screen.findByRole('button', { name: 'Add Flower Pot to the bill' }));
    await userEvent.click(screen.getByRole('button', { name: 'Add Flower Pot to the bill' }));
    const method = screen.getByLabelText('Payment method');
    expect(within(method).getAllByRole('option').map((o) => o.textContent)).toEqual(['Cash', 'UPI (paid at the shop)']);

    await userEvent.selectOptions(screen.getByLabelText('Discount'), 'PERCENT');
    await userEvent.type(screen.getByLabelText(/^Discount %/), '25');
    expect(screen.getByText(/at most 20% of the subtotal/)).toBeInTheDocument();
    await userEvent.clear(screen.getByLabelText(/^Discount %/));
    await userEvent.type(screen.getByLabelText(/^Discount %/), '10');
    expect(screen.getByTestId('pos-total')).toHaveTextContent('₹216.90');

    await userEvent.type(screen.getByLabelText(/Amount received/), '200');
    expect(screen.getByText(/Less than the total/)).toBeInTheDocument();
    await userEvent.clear(screen.getByLabelText(/Amount received/));
    await userEvent.type(screen.getByLabelText(/Amount received/), '300');
    expect(screen.getByText('Change to return: ₹83.10')).toBeInTheDocument();

    const save = screen.getByRole('button', { name: /Save bill/ });
    await userEvent.dblClick(save);
    await screen.findByRole('heading', { name: 'Bill SC-2026-000011 saved' });
    expect(adminService.createBill).toHaveBeenCalledTimes(1);
    const body = adminService.createBill.mock.calls[0][0];
    expect(body).toMatchObject({
      customerId: null, items: [{ productId: 5, quantity: 2 }], discountType: 'PERCENT', discountValue: '10',
      paymentMethod: 'CASH', paymentStatus: 'PAID', amountReceived: '300', notes: null,
    });
    expect(body.requestKey).toMatch(/^[A-Za-z0-9-]{8,64}$/);
    expect(body).not.toHaveProperty('unitPrice');
    expect(screen.getByRole('link', { name: /Print invoice/ })).toHaveAttribute('href', invoiceUrl(11));
    expect(screen.getByRole('link', { name: /Download PDF/ })).toHaveAttribute('href', invoiceUrl(11, true));
  });

  it('a retry after a failure reuses the same request key, so the shop is never billed twice', async () => {
    const err = Object.assign(new Error('Unable to reach the server. Please try again.'), { fieldErrors: [] });
    adminService.createBill.mockRejectedValueOnce(err).mockResolvedValue(SAVED);
    renderAdmin({ '/admin/billing': <BillingPage /> }, { at: '/admin/billing', auth: CASHIER });
    await userEvent.click(await screen.findByRole('button', { name: 'Add Flower Pot to the bill' }));
    await userEvent.click(screen.getByRole('button', { name: /Save bill/ }));
    expect(await screen.findByText('Unable to reach the server. Please try again.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /Save bill/ }));
    await screen.findByRole('heading', { name: /saved/ });
    const [first, second] = adminService.createBill.mock.calls.map((c) => c[0].requestKey);
    expect(first).toBe(second);
  });

  it('shows the stock message when stock ran out meanwhile', async () => {
    const err = Object.assign(new Error('Conflict'), { status: 409, fieldErrors: [{ field: 'items', message: 'only 1 of Flower Pot in stock' }] });
    adminService.createBill.mockRejectedValue(err);
    renderAdmin({ '/admin/billing': <BillingPage /> }, { at: '/admin/billing', auth: CASHIER });
    await userEvent.click(await screen.findByRole('button', { name: 'Add Flower Pot to the bill' }));
    await userEvent.click(screen.getByRole('button', { name: 'Add Flower Pot to the bill' }));
    await userEvent.click(screen.getByRole('button', { name: /Save bill/ }));
    expect(await screen.findByText('only 1 of Flower Pot in stock.')).toBeInTheDocument();
  });

  it('a cashier can add a new customer from the bill', async () => {
    adminService.createCustomer.mockResolvedValue({ id: 42, fullName: 'Meena', phone: '9876500042' });
    renderAdmin({ '/admin/billing': <BillingPage /> }, { at: '/admin/billing', auth: CASHIER });
    await userEvent.click(await screen.findByRole('button', { name: 'New' }));
    const dialog = screen.getByRole('dialog', { name: 'Add customer' });
    await userEvent.type(within(dialog).getByLabelText('Name'), 'Meena');
    await userEvent.type(within(dialog).getByLabelText(/Mobile number/), '9876500042');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Meena')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Remove customer from the bill' })).toBeInTheDocument();
  });
});

describe('Bill history', () => {
  const PAGE = {
    page: { content: [{ id: 11, billNumber: 'SC-2026-000011', status: 'FINALIZED', customerName: null, customerPhone: null, itemCount: 2, total: 216.9, paymentMethod: 'Cash', paymentStatus: 'PAID', createdByName: 'Kavi', createdAt: '2026-11-26T05:00:00Z' }], page: 0, size: 25, totalElements: 1, totalPages: 1 },
    finalizedCount: 1, finalizedTotal: 216.9, voidedCount: 0, pendingTotal: 0,
  };
  const BILL = {
    ...SAVED, customerName: null, subtotal: 241, discountAmount: 24.1, createdByName: 'Kavi', createdAt: '2026-11-26T05:00:00Z',
    items: [{ lineNo: 1, productId: 5, productName: 'Flower Pot', productSku: 'FP-1', unitPrice: 120.5, quantity: 2, lineTotal: 241 }],
  };

  it('shows totals and bills; a cashier cannot void', async () => {
    adminService.listBills.mockResolvedValue(PAGE);
    adminService.getBill.mockResolvedValue(BILL);
    renderAdmin({ '/admin/bills': <BillsPage /> }, { at: '/admin/bills', auth: CASHIER });
    expect((await screen.findByText('Sales (finalized bills)')).nextSibling).toHaveTextContent('₹216.90');
    await userEvent.click(screen.getAllByRole('button', { name: 'SC-2026-000011' })[0]);
    const dialog = await screen.findByRole('dialog', { name: 'Bill SC-2026-000011' });
    expect(await within(dialog).findByText('Flower Pot')).toBeInTheDocument();
    expect(within(dialog).getByText('Walk-in customer')).toBeInTheDocument();
    expect(within(dialog).queryByRole('button', { name: 'Void bill' })).not.toBeInTheDocument();
  });

  it('the owner voids a bill with a reason', async () => {
    adminService.listBills.mockResolvedValue(PAGE);
    adminService.getBill.mockResolvedValue(BILL);
    adminService.voidBill.mockResolvedValue({ ...BILL, status: 'VOIDED' });
    renderAdmin({ '/admin/bills': <BillsPage /> }, { at: '/admin/bills', auth: OWNER });
    await userEvent.click((await screen.findAllByRole('button', { name: 'SC-2026-000011' }))[0]);
    const dialog = await screen.findByRole('dialog', { name: 'Bill SC-2026-000011' });
    await userEvent.click(await within(dialog).findByRole('button', { name: 'Void bill' }));
    await userEvent.click(within(dialog).getAllByRole('button', { name: 'Void bill' }).at(-1));
    expect(within(dialog).getByText('Please give a reason for voiding this bill.')).toBeInTheDocument();
    expect(adminService.voidBill).not.toHaveBeenCalled();
    await userEvent.type(within(dialog).getByLabelText('Reason'), 'Customer returned the items');
    await userEvent.click(within(dialog).getAllByRole('button', { name: 'Void bill' }).at(-1));
    await waitFor(() => expect(adminService.voidBill).toHaveBeenCalledWith(11, 'Customer returned the items'));
  });

  it('filters are sent to the server', async () => {
    adminService.listBills.mockResolvedValue(PAGE);
    renderAdmin({ '/admin/bills': <BillsPage /> }, { at: '/admin/bills?status=VOIDED&paymentMethod=UPI', auth: OWNER });
    await screen.findByText('Sales (finalized bills)');
    expect(adminService.listBills).toHaveBeenCalledWith(expect.objectContaining({ status: 'VOIDED', paymentMethod: 'UPI', page: 0 }));
  });
});

describe('Reports with billing', () => {
  it('billing report shows revenue from finalized bills, never profit, with CSV and PDF downloads', async () => {
    adminService.report.mockImplementation(async (name) => (name === 'billing' ? {
      finalizedBills: 4, revenue: 1000, discounts: 50, voidedBills: 1, voidedTotal: 99, pendingBills: 1, pendingTotal: 200,
      byPaymentMethod: [{ code: 'CASH', label: 'Cash', bills: 4, total: 1000 }],
    } : { rows: [], totals: { bills: 0, revenue: 0, discounts: 0, stockInUnits: 0, stockOutUnits: 0, adjustmentUnits: 0, purchaseValue: 0, expenseCount: 0, expenseTotal: 0 } }));
    renderAdmin({ '/admin/reports': <ReportsPage /> }, { at: '/admin/reports', auth: OWNER });
    await userEvent.click(screen.getByRole('tab', { name: 'Billing' }));
    expect((await screen.findByText('Revenue (4 finalized bills)')).previousSibling).toHaveTextContent('₹1,000.00');
    expect(screen.getByText('Voided (1 bill, not counted)')).toBeInTheDocument();
    expect(screen.queryByText(/^profit/i)).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Download PDF/ }).getAttribute('href')).toMatch(/\/admin\/reports\/billing\?.*format=pdf/);
    expect(screen.getByRole('link', { name: /Download CSV/ }).getAttribute('href')).toMatch(/\/admin\/reports\/billing\?.*format=csv/);
  });
});
