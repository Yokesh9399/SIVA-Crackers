import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import StaffPage from '../pages/StaffPage.jsx';
import InventoryPage from '../pages/InventoryPage.jsx';
import ExpensesPage from '../pages/ExpensesPage.jsx';
import CustomersPage from '../pages/CustomersPage.jsx';
import ReportsPage from '../pages/ReportsPage.jsx';
import { renderAdmin } from '../../test/renderAdmin.jsx';
import { adminService } from '../adminService.js';

vi.mock('../adminService.js', async (importOriginal) => {
  const real = await importOriginal();
  return { ...real, adminService: Object.fromEntries(Object.keys(real.adminService).map((k) => [k, vi.fn()])) };
});

const OWNER = { user: { id: 1, fullName: 'Shop Owner', role: 'OWNER' } };
const MANAGER = { user: { id: 2, fullName: 'Mani', role: 'MANAGER' } };
const CASHIER = { user: { id: 3, fullName: 'Kavi', role: 'CASHIER' } };

beforeEach(() => vi.clearAllMocks());

describe('Staff management', () => {
  const staff = [
    { id: 1, fullName: 'Shop Owner', email: 'o@x.test', phone: null, role: 'OWNER', active: true, createdAt: '2026-11-01T05:00:00Z', lastLoginAt: null },
    { id: 7, fullName: 'Kavi', email: 'k@x.test', phone: '9876500001', role: 'CASHIER', active: true, createdAt: '2026-11-02T05:00:00Z', lastLoginAt: null },
  ];

  it('lists staff with role, status and dates; the owner (you) cannot be deactivated or reset here', async () => {
    adminService.listStaff.mockResolvedValue(staff);
    renderAdmin({ '/admin/staff': <StaffPage /> }, { at: '/admin/staff', auth: OWNER });
    expect(await screen.findByText('k@x.test · 9876500001')).toBeInTheDocument();
    expect(screen.getByText('You')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Deactivate Shop Owner' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reset password for Shop Owner' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Deactivate Kavi' })).toBeInTheDocument();
  });

  it('creates a staff member: checks the form, offers only Manager and Cashier, sends the request', async () => {
    adminService.listStaff.mockResolvedValue(staff);
    adminService.createStaff.mockResolvedValue({ id: 9 });
    renderAdmin({ '/admin/staff': <StaffPage /> }, { at: '/admin/staff', auth: OWNER });
    await userEvent.click(await screen.findByRole('button', { name: 'Add staff member' }));
    const dialog = screen.getByRole('dialog', { name: 'Add staff member' });
    expect(within(dialog).getAllByRole('option').map((o) => o.textContent)).toEqual(['Manager', 'Cashier']);
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(within(dialog).getByText('Please enter your name.')).toBeInTheDocument();
    expect(adminService.createStaff).not.toHaveBeenCalled();
    await userEvent.type(within(dialog).getByLabelText('Full name'), 'Selvi');
    await userEvent.type(within(dialog).getByLabelText('Email (used to log in)'), 'selvi@x.test');
    await userEvent.selectOptions(within(dialog).getByLabelText('Role'), 'MANAGER');
    await userEvent.type(within(dialog).getByLabelText('Password'), 'Temp2026ab');
    await userEvent.type(within(dialog).getByLabelText('Confirm password'), 'Temp2026ab');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(adminService.createStaff).toHaveBeenCalledWith({ fullName: 'Selvi', email: 'selvi@x.test', phone: null, role: 'MANAGER', password: 'Temp2026ab' });
  });

  it('deactivation needs confirmation', async () => {
    adminService.listStaff.mockResolvedValue(staff);
    adminService.setStaffActive.mockResolvedValue({});
    renderAdmin({ '/admin/staff': <StaffPage /> }, { at: '/admin/staff', auth: OWNER });
    await userEvent.click(await screen.findByRole('button', { name: 'Deactivate Kavi' }));
    const dialog = screen.getByRole('dialog', { name: 'Deactivate Kavi?' });
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    expect(adminService.setStaffActive).not.toHaveBeenCalled();
    await userEvent.click(screen.getByRole('button', { name: 'Deactivate Kavi' }));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Deactivate' }));
    expect(adminService.setStaffActive).toHaveBeenCalledWith(7, false);
  });
});

describe('Stock', () => {
  const page = { content: [{ productId: 5, name: 'Flower Pot', sku: 'FP-1', category: { name: 'Flower pots' }, brand: { name: 'B' }, stockQuantity: 4, reorderLevel: 5, stockStatus: 'LOW_STOCK', active: true, imageUrl: null }], page: 0, size: 25, totalElements: 1, totalPages: 1 };
  const alerts = { lowStockCount: 1, outOfStockCount: 0, lowStock: page.content, outOfStock: [] };
  const reasons = [
    { code: 'PURCHASE', type: 'STOCK_IN', label: 'Received from supplier' },
    { code: 'SOLD_AT_SHOP', type: 'STOCK_OUT', label: 'Sold at the shop counter' },
    { code: 'STOCK_COUNT', type: 'ADJUSTMENT', label: 'Stock count correction' },
  ];
  beforeEach(() => {
    adminService.listStock.mockResolvedValue(page);
    adminService.stockAlerts.mockResolvedValue(alerts);
    adminService.listCategories.mockResolvedValue([]);
    adminService.movementReasons.mockResolvedValue(reasons);
    adminService.listSuppliers.mockResolvedValue([{ id: 3, name: 'Sivakasi Traders' }]);
  });

  it('cashiers see stock and alerts but no buttons that change stock', async () => {
    renderAdmin({ '/admin/inventory': <InventoryPage /> }, { at: '/admin/inventory', auth: CASHIER });
    expect(await screen.findByText('Flower Pot')).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Few left' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Record stock change for Flower Pot' })).not.toBeInTheDocument();
    expect(adminService.listCategories).not.toHaveBeenCalled();
  });

  it('stock out cannot exceed the stock; a valid stock-in is sent with supplier and cost', async () => {
    adminService.recordMovement.mockResolvedValue({ previousQuantity: 4, newQuantity: 14 });
    renderAdmin({ '/admin/inventory': <InventoryPage /> }, { at: '/admin/inventory', auth: MANAGER });
    await userEvent.click(await screen.findByRole('button', { name: 'Record stock change for Flower Pot' }));
    const dialog = await screen.findByRole('dialog');
    await waitFor(() => expect(within(dialog).getByLabelText('Reason')).toHaveValue('PURCHASE'));
    await userEvent.selectOptions(within(dialog).getByLabelText('What happened?'), 'STOCK_OUT');
    await userEvent.type(within(dialog).getByLabelText('Quantity'), '9');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(within(dialog).getByText('Only 4 in stock – stock cannot go below 0.')).toBeInTheDocument();
    expect(adminService.recordMovement).not.toHaveBeenCalled();

    await userEvent.selectOptions(within(dialog).getByLabelText('What happened?'), 'STOCK_IN');
    await userEvent.clear(within(dialog).getByLabelText('Quantity'));
    await userEvent.type(within(dialog).getByLabelText('Quantity'), '10');
    expect(within(dialog).getByText(/after saving/)).toHaveTextContent('14');
    await userEvent.selectOptions(within(dialog).getByLabelText('Supplier (optional)'), '3');
    await userEvent.type(within(dialog).getByLabelText('Purchase cost per unit (₹) (optional)'), '12.50');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(adminService.recordMovement).toHaveBeenCalledWith({ productId: 5, reason: 'PURCHASE', quantity: 10, supplierId: 3, unitCost: '12.50', note: null });
    expect(await screen.findByText('Flower Pot: stock 4 → 14.')).toBeInTheDocument();
  });
});

describe('Expenses', () => {
  const cats = [{ id: 1, name: 'Electricity', displayOrder: 1, active: true }];
  const expense = { id: 4, category: { id: 1, name: 'Electricity' }, expenseDate: '2026-11-20', amount: 1250.5, description: 'EB bill', paidTo: null, reference: null, notes: null, status: 'ACTIVE', recordedByName: 'Mani' };
  beforeEach(() => {
    adminService.expenseCategories.mockResolvedValue(cats);
    adminService.listExpenses.mockResolvedValue({ page: { content: [expense], page: 0, size: 25, totalElements: 1, totalPages: 1 }, activeCount: 1, activeTotal: 1250.5 });
    adminService.monthlyExpenses.mockResolvedValue({ year: 2026, months: Array.from({ length: 12 }, (_, i) => ({ month: i + 1, count: 0, total: 0 })), yearTotal: 0 });
  });

  it('shows the total of active expenses and requires a reason to cancel', async () => {
    adminService.cancelExpense.mockResolvedValue({});
    renderAdmin({ '/admin/expenses': <ExpensesPage /> }, { at: '/admin/expenses', auth: MANAGER });
    expect(await screen.findByText('₹1,250.50', { selector: 'span' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Cancel expense EB bill' }));
    const dialog = screen.getByRole('dialog', { name: 'Cancel expense?' });
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel expense' }));
    expect(within(dialog).getByText(/Please give a reason/)).toBeInTheDocument();
    expect(adminService.cancelExpense).not.toHaveBeenCalled();
    await userEvent.type(within(dialog).getByLabelText('Reason'), 'Entered twice');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel expense' }));
    expect(adminService.cancelExpense).toHaveBeenCalledWith(4, 'Entered twice');
  });

  it('checks a new expense before sending it', async () => {
    adminService.createExpense.mockResolvedValue({});
    renderAdmin({ '/admin/expenses': <ExpensesPage /> }, { at: '/admin/expenses', auth: MANAGER });
    await userEvent.click(await screen.findByRole('button', { name: 'Record expense' }));
    const dialog = screen.getByRole('dialog', { name: 'Record expense' });
    await userEvent.type(within(dialog).getByLabelText('Amount (₹)'), '0');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(within(dialog).getByText(/Enter an amount above 0/)).toBeInTheDocument();
    expect(within(dialog).getByText('Describe the expense.')).toBeInTheDocument();
    expect(adminService.createExpense).not.toHaveBeenCalled();
  });
});

describe('Customers', () => {
  it('cashiers can search but not add, edit or delete', async () => {
    adminService.listCustomers.mockResolvedValue([{ id: 1, fullName: 'Lakshmi', phone: '9876511111', email: null, createdAt: '2026-11-20T05:00:00Z' }]);
    renderAdmin({ '/admin/customers': <CustomersPage /> }, { at: '/admin/customers', auth: CASHIER });
    expect(await screen.findByText('Lakshmi')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Add customer' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Delete Lakshmi' })).not.toBeInTheDocument();
  });

  it('a new customer needs a mobile number or an email', async () => {
    adminService.listCustomers.mockResolvedValue([]);
    renderAdmin({ '/admin/customers': <CustomersPage /> }, { at: '/admin/customers', auth: MANAGER });
    await userEvent.click(await screen.findByRole('button', { name: 'Add customer' }));
    const dialog = screen.getByRole('dialog', { name: 'Add customer' });
    await userEvent.type(within(dialog).getByLabelText('Name'), 'Lakshmi');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(within(dialog).getByText('Enter a mobile number or an email address.')).toBeInTheDocument();
    expect(adminService.createCustomer).not.toHaveBeenCalled();
  });
});

describe('Reports', () => {
  it('shows recorded totals, no sales figures, and a CSV link for the chosen period', async () => {
    const summary = {
      from: '2026-10-28', to: '2026-11-26', groupBy: 'DAY',
      rows: [{ period: '2026-11-26', stockInUnits: 8, stockOutUnits: 3, adjustmentUnits: 0, movements: 2, purchaseValue: 0, expenseCount: 1, expenseTotal: 99 }],
      totals: { period: 'Total', stockInUnits: 8, stockOutUnits: 3, adjustmentUnits: 0, movements: 2, purchaseValue: 0, expenseCount: 1, expenseTotal: 99 },
    };
    const lowStock = { lowStockCount: 0, outOfStockCount: 0, lowStock: [], outOfStock: [] };
    adminService.report.mockImplementation((name) => Promise.resolve(name === 'low-stock' ? lowStock : summary));
    renderAdmin({ '/admin/reports': <ReportsPage /> }, { at: '/admin/reports', auth: MANAGER });
    expect(await screen.findByText('Units received')).toBeInTheDocument();
    expect(screen.getByText(/no sales, revenue or profit figures/)).toBeInTheDocument();
    const csv = screen.getByRole('link', { name: 'Download CSV' });
    expect(csv.getAttribute('href')).toMatch(/^\/api\/admin\/reports\/summary\?from=\d{4}-\d{2}-\d{2}&to=\d{4}-\d{2}-\d{2}&groupBy=DAY&format=csv$/);
    await userEvent.click(screen.getByRole('tab', { name: 'Low stock' }));
    expect(screen.getByRole('link', { name: 'Download CSV' }).getAttribute('href')).toBe('/api/admin/reports/low-stock?format=csv');
  });
});
