import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from './server'

beforeAll(() => {
  // Fail loudly if a component calls an endpoint the test didn't mock
  server.listen({ onUnhandledFrame: 'error' })

  // The app calls fetch('/api/...'). Node's fetch needs an absolute URL, so resolve it like a browser would.
  const mswFetch = globalThis.fetch
  globalThis.fetch = (input, init) =>
    mswFetch(typeof input === 'string' ? new URL(input, window.location.href) : input, init)
})

afterEach(() => {
  cleanup()
  server.resetHandlers()
  localStorage.clear()
})

afterAll(() => server.close())
