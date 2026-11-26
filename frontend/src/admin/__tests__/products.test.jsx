import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ProductsPage from '../pages/ProductsPage.jsx';
import ProductFormPage from '../pages/ProductFormPage.jsx';
import { validateProduct } from '../productValidation.js';
import { renderAdmin } from '../../test/renderAdmin.jsx';
import { adminService } from '../adminService.js';

vi.mock('../adminService.js', async (importOriginal) => {
  const real = await importOriginal();
  return { ...real, adminService: Object.fromEntries(Object.keys(real.adminService).map((k) => [k, vi.fn()])) };
});

const CATS = [{ id: 1, name: 'Sparklers', active: true }, { id: 2, name: 'Rockets', active: false }];
const BRANDS = [{ id: 5, name: 'Kovai Star', active: true }];
const row = (id, name, extra = {}) => ({
  id, name, slug: name.toLowerCase().replace(/\s+/g, '-'), sku: `SKU-${id}`, category: { id: 1, name: 'Sparklers' },
  brand: { id: 5, name: 'Kovai Star' }, mrp: 200, price: 150, stockQuantity: 20, stockStatus: 'IN_STOCK', featured: false,
  visibleOnWebsite: true, active: true, isDemo: false, publiclyVisible: true, imageUrl: null, ...extra,
});
const page = (content) => ({ content, page: 0, size: 25, totalElements: content.length, totalPages: 1 });

beforeEach(() => {
  vi.clearAllMocks();
  adminService.listCategories.mockResolvedValue(CATS);
  adminService.listBrands.mockResolvedValue(BRANDS);
});

describe('Product list', () => {
  it('lists products with code, price, availability and visibility', async () => {
    adminService.listProducts.mockResolvedValue(page([row(1, 'Golden Peacock'), row(2, 'Night Rocket', { stockQuantity: 0, stockStatus: 'OUT_OF_STOCK', visibleOnWebsite: false, publiclyVisible: false })]));
    renderAdmin({ '/admin/products': <ProductsPage /> }, { at: '/admin/products' });
    const table = await screen.findByRole('table');
    expect(within(table).getByText('Golden Peacock')).toBeInTheDocument();
    expect(within(table).getByText('Code SKU-1')).toBeInTheDocument();
    expect(within(table).getAllByText('₹150').length).toBe(2);
    expect(within(table).getByText('Out of stock')).toBeInTheDocument();
    expect(within(table).getByText('Hidden')).toBeInTheDocument();
    expect(screen.getByText('2 products')).toBeInTheDocument();
  });

  it('sends filters from the URL and the controls to the API', async () => {
    adminService.listProducts.mockResolvedValue(page([row(1, 'A')]));
    renderAdmin({ '/admin/products': <ProductsPage /> }, { at: '/admin/products?availability=LOW_STOCK&sort=price,asc' });
    await screen.findByRole('table');
    expect(adminService.listProducts).toHaveBeenLastCalledWith(expect.objectContaining({ availability: 'LOW_STOCK', sort: 'price,asc', page: 0 }));
    await userEvent.selectOptions(screen.getByLabelText('Visibility'), 'INACTIVE');
    await waitFor(() => expect(adminService.listProducts).toHaveBeenLastCalledWith(expect.objectContaining({ visibility: 'INACTIVE' })));
    await userEvent.selectOptions(screen.getByLabelText('Category'), '1');
    await waitFor(() => expect(adminService.listProducts).toHaveBeenLastCalledWith(expect.objectContaining({ categoryId: '1' })));
  });

  it('asks for confirmation before deleting, and Cancel deletes nothing', async () => {
    adminService.listProducts.mockResolvedValue(page([row(1, 'Golden Peacock')]));
    renderAdmin({ '/admin/products': <ProductsPage /> }, { at: '/admin/products' });
    const table = await screen.findByRole('table');
    await userEvent.click(within(table).getByRole('button', { name: 'Delete Golden Peacock' }));
    const dialog = screen.getByRole('dialog', { name: 'Delete product?' });
    expect(within(dialog).getByText(/cannot be undone/)).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    expect(adminService.deleteProduct).not.toHaveBeenCalled();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('deletes after confirmation, shows a notice and reloads the list', async () => {
    adminService.listProducts.mockResolvedValueOnce(page([row(1, 'Golden Peacock')])).mockResolvedValue(page([]));
    adminService.deleteProduct.mockResolvedValue(undefined);
    renderAdmin({ '/admin/products': <ProductsPage /> }, { at: '/admin/products' });
    const table = await screen.findByRole('table');
    await userEvent.click(within(table).getByRole('button', { name: 'Delete Golden Peacock' }));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete' }));
    expect(adminService.deleteProduct).toHaveBeenCalledWith(1);
    expect(await screen.findByText('“Golden Peacock” was deleted.')).toBeInTheDocument();
    expect(await screen.findByText('No products yet')).toBeInTheDocument();
  });

  it('shows the API error with a retry button', async () => {
    adminService.listProducts.mockRejectedValue(new Error('Please log in to continue.'));
    renderAdmin({ '/admin/products': <ProductsPage /> }, { at: '/admin/products' });
    expect(await screen.findByText('Please log in to continue.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
  });
});

describe('Product form', () => {
  const routes = { '/admin/products/new': <ProductFormPage />, '/admin/products/:id/edit': <ProductFormPage />, '/admin/products/:id': <p>view page</p> };

  it('validation rules match the server', () => {
    const ok = { name: 'A', slug: '', sku: 'A-1', categoryId: '1', brandId: '5', packSize: 'Box', mrp: '200', price: '150', stockQuantity: '0', reorderLevel: '10' };
    expect(validateProduct(ok)).toEqual({});
    expect(validateProduct({ ...ok, price: '0' }).price).toMatch(/above 0/);
    expect(validateProduct({ ...ok, price: '250' }).price).toBe('The price cannot be more than the MRP.');
    expect(validateProduct({ ...ok, price: '1.234' }).price).toMatch(/2 decimal places/);
    expect(validateProduct({ ...ok, slug: 'Bad Slug' }).slug).toBeDefined();
    expect(validateProduct({ ...ok, sku: 'a b' }).sku).toBeDefined();
    expect(validateProduct({ ...ok, stockQuantity: '-1' }).stockQuantity).toBeDefined();
  });

  it('shows field errors and does not call the API when the form is incomplete', async () => {
    renderAdmin(routes, { at: '/admin/products/new' });
    await userEvent.click(await screen.findByRole('button', { name: 'Add product' }));
    expect(screen.getByText('Please correct the highlighted fields.')).toBeInTheDocument();
    expect(screen.getByText('Please enter the product name.')).toBeInTheDocument();
    expect(screen.getByText('Please choose a category.')).toBeInTheDocument();
    expect(adminService.createProduct).not.toHaveBeenCalled();
  });

  it('creates a product and opens it', async () => {
    adminService.createProduct.mockResolvedValue({ id: 42, name: 'Golden Peacock' });
    renderAdmin(routes, { at: '/admin/products/new' });
    await userEvent.type(await screen.findByLabelText('Product name'), 'Golden Peacock');
    await userEvent.type(screen.getByLabelText('Product code'), 'GP-1');
    await userEvent.selectOptions(screen.getByLabelText('Category'), '1');
    await userEvent.selectOptions(screen.getByLabelText('Brand'), '5');
    await userEvent.type(screen.getByLabelText('Pack size'), 'Box of 10');
    await userEvent.type(screen.getByLabelText('MRP (₹)'), '200');
    await userEvent.type(screen.getByLabelText('Price (₹)'), '150');
    await userEvent.click(screen.getByRole('button', { name: 'Add product' }));
    expect(adminService.createProduct).toHaveBeenCalledWith(expect.objectContaining({
      name: 'Golden Peacock', sku: 'GP-1', categoryId: 1, brandId: 5, mrp: '200', price: '150', visibleOnWebsite: true, active: true, images: [],
    }));
    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/admin/products/42'));
  });

  it('shows a duplicate product code reported by the server next to the field', async () => {
    const err = Object.assign(new Error('is already used by another product'), { status: 409, fieldErrors: [{ field: 'sku', message: 'is already used by another product' }] });
    adminService.updateProduct.mockRejectedValue(err);
    adminService.getProduct.mockResolvedValue({ ...row(9, 'Existing'), description: 'Old', packSize: 'Box', unit: 'BOX', reorderLevel: 10, images: [] });
    renderAdmin(routes, { at: '/admin/products/9/edit' });
    expect(await screen.findByLabelText('Product name')).toHaveValue('Existing');
    expect(screen.getByLabelText('Description (optional)')).toHaveValue('Old');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    expect(await screen.findByText('Product code is already used by another product.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeEnabled();
  });

  it('prevents double submission while saving', async () => {
    let finish;
    adminService.updateProduct.mockReturnValue(new Promise((r) => (finish = r)));
    adminService.getProduct.mockResolvedValue({ ...row(9, 'Existing'), description: '', packSize: 'Box', unit: 'BOX', reorderLevel: 10, images: [] });
    renderAdmin(routes, { at: '/admin/products/9/edit' });
    await screen.findByLabelText('Product name');
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));
    expect(screen.getByRole('button', { name: 'Saving…' })).toBeDisabled();
    await userEvent.click(screen.getByRole('button', { name: 'Saving…' }));
    expect(adminService.updateProduct).toHaveBeenCalledTimes(1);
    finish({ id: 9, name: 'Existing' });
  });
});
