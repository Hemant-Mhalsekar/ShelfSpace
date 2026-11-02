import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// Component tests run in jsdom (a fake browser in Node) against a fake API (MSW), so no backend is needed.
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    environmentOptions: { jsdom: { url: 'http://localhost:5173/' } },
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    restoreMocks: true,
  },
})
