import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // DEV_API_PROXY_TARGET has no VITE_ prefix, so it is never bundled into the website.
  const env = loadEnv(mode, process.cwd(), '');
  const apiTarget = env.DEV_API_PROXY_TARGET || 'http://localhost:8081';

  return {
    plugins: [react(), tailwindcss()],
    server: {
      port: 5173,
      strictPort: true,
      // The browser talks only to localhost:5173; /api is forwarded to Spring Boot.
      // Same origin => cookies and CSRF work without CORS.
      proxy: {
        '/api': { target: apiTarget, changeOrigin: false },
      },
    },
  };
});
