import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import DashboardPage from '../pages/DashboardPage.jsx';
import { renderAdmin } from '../../test/renderAdmin.jsx';
import { adminService } from '../adminService.js';

vi.mock('../adminService.js', async (importOriginal) => {
  const real = await importOriginal();
  return { ...real, adminService: Object.fromEntries(Object.keys(real.adminService).map((k) => [k, vi.fn()])) };
});

const DATA = {
  totalProducts: 48, activeProducts: 46, inactiveProducts: 2, visibleProducts: 44, hiddenProducts: 2, featuredProducts: 8,
  lowStockProducts: 5, outOfStockProducts: 3, demoProducts: 48, totalCategories: 12, activeCategories: 11, totalBrands: 8,
  activeBrands: 8,
  productsByCategory: [
    { categoryId: 1, name: 'Sparklers', slug: 'sparklers', active: true, productCount: 6 },
    { categoryId: 2, name: 'Rockets', slug: 'rockets', active: false, productCount: 0 },
  ],
  recentProducts: [
    { id: 7, name: 'Golden Peacock', category: { name: 'Fancy' }, brand: { name: 'Kovai Star' }, price: 150, stockStatus: 'IN_STOCK',
      active: true, visibleOnWebsite: true, publiclyVisible: true, isDemo: false, imageUrl: null },
  ],
};

describe('Dashboard', () => {
  beforeEach(() => vi.clearAllMocks());

  it('shows the live figures, the category distribution and recent products', async () => {
    adminService.dashboard.mockResolvedValue(DATA);
    renderAdmin({ '/admin': <DashboardPage /> }, { at: '/admin' });
    const figures = await screen.findByRole('region', { name: 'Catalogue figures' });
    expect(within(figures).getByText('Total products').previousSibling).toHaveTextContent('48');
    expect(within(figures).getByText('Out of stock').previousSibling).toHaveTextContent('3');
    expect(within(figures).getByText('Categories (11 active)')).toBeInTheDocument();
    expect(screen.getByText('Sparklers')).toBeInTheDocument();
    expect(screen.getByText('(inactive)')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Golden Peacock' })).toHaveAttribute('href', '/admin/products/7');
    expect(screen.getByText(/48 products are/)).toBeInTheDocument(); // demo-data notice
    expect(screen.queryByText(/revenue|sales|orders/i)).not.toBeInTheDocument();
  });

  it('shows skeletons while loading', () => {
    adminService.dashboard.mockReturnValue(new Promise(() => {}));
    const { container } = renderAdmin({ '/admin': <DashboardPage /> }, { at: '/admin' });
    expect(container.querySelectorAll('.animate-pulse').length).toBeGreaterThan(0);
  });

  it('shows an error with a retry button', async () => {
    adminService.dashboard.mockRejectedValueOnce(new Error('Unable to reach the server. Please try again.')).mockResolvedValue(DATA);
    renderAdmin({ '/admin': <DashboardPage /> }, { at: '/admin' });
    expect(await screen.findByText('Unable to reach the server. Please try again.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(await screen.findByRole('region', { name: 'Catalogue figures' })).toBeInTheDocument();
  });

  it('shows a helpful empty state without products', async () => {
    adminService.dashboard.mockResolvedValue({ ...DATA, demoProducts: 0, recentProducts: [], productsByCategory: [] });
    renderAdmin({ '/admin': <DashboardPage /> }, { at: '/admin' });
    expect(await screen.findByRole('link', { name: 'Add your first product' })).toBeInTheDocument();
    expect(screen.getByText('No categories yet.')).toBeInTheDocument();
  });
});
