import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Same-origin dev: the SPA calls /api/... and Vite proxies to the backend, so the httpOnly
    // refresh cookie stays first-party. Mirrors the nginx /api reverse proxy used in production.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
    coverage: {
      provider: 'v8',
      include: ['src/api/**'],
      // types.ts is type-only (erased at build), so there is nothing to execute.
      exclude: ['src/api/types.ts'],
      thresholds: {
        lines: 100,
        functions: 100,
        statements: 100,
        branches: 90,
      },
    },
  },
})
