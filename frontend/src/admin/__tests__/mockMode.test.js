import { describe, expect, it, vi } from 'vitest';

// Mock mode = the website runs on sample data. The admin must then refuse to touch the API at all.
vi.mock('../../config/env.js', () => ({ USE_MOCKS: true, API_URL: '/api' }));

describe('Admin in mock mode', () => {
  it('never sends anything to the admin API', async () => {
    const apiClient = (await import('../../services/apiClient.js')).default;
    const spies = ['get', 'post', 'put', 'patch', 'delete'].map((m) => vi.spyOn(apiClient, m));
    const { adminService } = await import('../adminService.js');
    await expect(adminService.dashboard()).rejects.toThrow(/needs the backend/);
    await expect(adminService.deleteProduct(1)).rejects.toThrow(/needs the backend/);
    await expect(adminService.uploadImage('products', new File(['x'], 'a.png', { type: 'image/png' }))).rejects.toThrow();
    spies.forEach((s) => expect(s).not.toHaveBeenCalled());
  });
});
