import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CategoriesPage from '../pages/CategoriesPage.jsx';
import BrandsPage from '../pages/BrandsPage.jsx';
import ImageUploader from '../components/ImageUploader.jsx';
import { renderAdmin } from '../../test/renderAdmin.jsx';
import { adminService, checkImageFile } from '../adminService.js';

vi.mock('../adminService.js', async (importOriginal) => {
  const real = await importOriginal();
  return { ...real, adminService: Object.fromEntries(Object.keys(real.adminService).map((k) => [k, vi.fn()])) };
});

const category = (id, name, productCount, extra = {}) => ({
  id, name, slug: name.toLowerCase(), description: '', imageUrl: null, displayOrder: id, active: true, isDemo: false,
  safetyNotes: [], productCount, visibleProductCount: productCount, ...extra,
});
const brand = (id, name, productCount) => ({
  id, name, slug: name.toLowerCase().replace(/\s+/g, '-'), description: '', logoUrl: null, displayOrder: id, active: true, isDemo: false,
  productCount, visibleProductCount: productCount,
});

beforeEach(() => vi.clearAllMocks());

describe('Categories', () => {
  it('lists categories with product counts and filters by search', async () => {
    adminService.listCategories.mockResolvedValue([category(1, 'Sparklers', 6), category(2, 'Rockets', 0, { active: false })]);
    renderAdmin({ '/admin/categories': <CategoriesPage /> }, { at: '/admin/categories' });
    expect(await screen.findByText('6 products')).toBeInTheDocument();
    expect(screen.getByText('Inactive')).toBeInTheDocument();
    await userEvent.type(screen.getByPlaceholderText('Search categories…'), 'rock');
    expect(screen.queryByText('Sparklers')).not.toBeInTheDocument();
    expect(screen.getByText('Rockets')).toBeInTheDocument();
  });

  it('adds a category from the dialog and reloads the list', async () => {
    adminService.listCategories.mockResolvedValueOnce([]).mockResolvedValue([category(3, 'Night Wonders', 0)]);
    adminService.createCategory.mockResolvedValue(category(3, 'Night Wonders', 0));
    renderAdmin({ '/admin/categories': <CategoriesPage /> }, { at: '/admin/categories?new=1' });
    const dialog = await screen.findByRole('dialog', { name: 'Add category' });
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(within(dialog).getByText('Please enter a category name.')).toBeInTheDocument();
    await userEvent.type(within(dialog).getByLabelText('Name'), 'Night Wonders');
    await userEvent.type(within(dialog).getByLabelText('Safety notes (optional)'), 'Keep water nearby{enter}{enter}Adults only');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(adminService.createCategory).toHaveBeenCalledWith(expect.objectContaining({
      name: 'Night Wonders', active: true, safetyNotes: ['Keep water nearby', 'Adults only'],
    }));
    expect(await screen.findByText('Category “Night Wonders” was added.')).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(await screen.findByText('Night Wonders')).toBeInTheDocument();
  });

  it('shows a duplicate name from the server next to the field', async () => {
    adminService.listCategories.mockResolvedValue([]);
    adminService.createCategory.mockRejectedValue(Object.assign(new Error('is already used by another category'), {
      status: 409, fieldErrors: [{ field: 'name', message: 'is already used by another category' }],
    }));
    renderAdmin({ '/admin/categories': <CategoriesPage /> }, { at: '/admin/categories?new=1' });
    const dialog = await screen.findByRole('dialog');
    await userEvent.type(within(dialog).getByLabelText('Name'), 'Sparklers');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(await within(dialog).findByText('Name is already used by another category.')).toBeInTheDocument();
  });

  it('explains why a category with products cannot be deleted (no delete button)', async () => {
    adminService.listCategories.mockResolvedValue([category(1, 'Sparklers', 6)]);
    renderAdmin({ '/admin/categories': <CategoriesPage /> }, { at: '/admin/categories' });
    await userEvent.click(await screen.findByRole('button', { name: 'Delete Sparklers' }));
    const dialog = screen.getByRole('dialog', { name: 'This category cannot be deleted' });
    expect(within(dialog).getByText(/still has 6 products/)).toBeInTheDocument();
    expect(within(dialog).queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Close' }));
    expect(adminService.deleteCategory).not.toHaveBeenCalled();
  });

  it('deletes an empty category after confirmation', async () => {
    adminService.listCategories.mockResolvedValueOnce([category(2, 'Empty', 0)]).mockResolvedValue([]);
    adminService.deleteCategory.mockResolvedValue(undefined);
    renderAdmin({ '/admin/categories': <CategoriesPage /> }, { at: '/admin/categories' });
    await userEvent.click(await screen.findByRole('button', { name: 'Delete Empty' }));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete' }));
    expect(adminService.deleteCategory).toHaveBeenCalledWith(2);
    expect(await screen.findByText('No categories yet')).toBeInTheDocument();
  });
});

describe('Brands', () => {
  it('lists brands and edits one', async () => {
    adminService.listBrands.mockResolvedValue([brand(1, 'Kovai Star', 7)]);
    adminService.updateBrand.mockResolvedValue(brand(1, 'Kovai Star Pyro', 7));
    renderAdmin({ '/admin/brands': <BrandsPage /> }, { at: '/admin/brands' });
    expect(await screen.findByText('7 products')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Edit Kovai Star' }));
    const dialog = screen.getByRole('dialog', { name: 'Edit brand' });
    const name = within(dialog).getByLabelText('Name');
    expect(name).toHaveValue('Kovai Star');
    await userEvent.type(name, ' Pyro');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(adminService.updateBrand).toHaveBeenCalledWith(1, expect.objectContaining({ name: 'Kovai Star Pyro', logoUrl: '' }));
  });

  it('shows an empty state when there are no brands', async () => {
    adminService.listBrands.mockResolvedValue([]);
    renderAdmin({ '/admin/brands': <BrandsPage /> }, { at: '/admin/brands' });
    expect(await screen.findByText('No brands yet')).toBeInTheDocument();
  });
});

describe('Image upload', () => {
  it('client check: JPEG / PNG / WebP up to 2 MB only', () => {
    expect(checkImageFile(new File(['x'], 'a.png', { type: 'image/png' }))).toBeNull();
    expect(checkImageFile(new File(['<svg/>'], 'a.svg', { type: 'image/svg+xml' }))).toMatch(/JPEG, PNG or WebP/);
    expect(checkImageFile(new File([new Uint8Array(2 * 1024 * 1024 + 1)], 'big.jpg', { type: 'image/jpeg' }))).toMatch(/larger than 2 MB/);
  });

  it('rejects a wrong file type without uploading, uploads a valid image and shows a preview', async () => {
    const onChange = vi.fn();
    adminService.uploadImage.mockResolvedValue('/uploads/brands/0123456789abcdef0123456789abcdef.png');
    const { rerender } = renderAdmin({ '/': <ImageUploader label="Logo" folder="brands" value={null} onChange={onChange} /> });
    const input = document.querySelector('input[type=file]');
    await userEvent.upload(input, new File(['hello'], 'notes.txt', { type: 'text/plain' }), { applyAccept: false });
    expect(screen.getByRole('alert')).toHaveTextContent('Please choose a JPEG, PNG or WebP image.');
    expect(adminService.uploadImage).not.toHaveBeenCalled();

    await userEvent.upload(input, new File(['png'], 'logo.png', { type: 'image/png' }));
    await waitFor(() => expect(onChange).toHaveBeenCalledWith('/uploads/brands/0123456789abcdef0123456789abcdef.png'));
    expect(adminService.uploadImage).toHaveBeenCalledWith('brands', expect.any(File));
    rerender(null);
  });

  it('shows the stored image with Replace and Remove', async () => {
    const onChange = vi.fn();
    renderAdmin({ '/': <ImageUploader label="Logo" folder="brands" value="/uploads/brands/x.png" onChange={onChange} /> });
    expect(screen.getByRole('img', { name: 'Logo' })).toHaveAttribute('src', '/uploads/brands/x.png');
    expect(screen.getByRole('button', { name: 'Replace' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Remove' }));
    expect(onChange).toHaveBeenCalledWith(null);
  });
});
