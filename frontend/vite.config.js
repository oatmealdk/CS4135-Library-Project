import { defineConfig } from 'vite';

/**
 * Dev server proxies to local Docker services (see README).
 * Browser calls same-origin /api/... → correct microservice port.
 */
export default defineConfig({
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api/search': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
      '/api/borrows': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
    },
  },
});
