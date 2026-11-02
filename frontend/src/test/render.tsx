import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import type { ReactNode } from 'react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import type { User } from '../api/types'
import { AuthProvider } from '../auth/AuthContext'
import { ToastProvider } from '../components/toast/ToastProvider'
import { server } from './server'

/**
 * Renders a page with the same providers as main.tsx, inside an in-memory router.
 *
 * @param path   route pattern, e.g. "/admin/members/:id"
 * @param url    where to start, e.g. "/admin/members/2"
 * @param user   signed-in user (served by GET /auth/me); omit for signed out
 * @param extra  other routes the page may navigate to
 */
export function renderPage(element: ReactNode, {
  path = '/', url = path, user, extra = [],
}: { path?: string; url?: string; user?: User; extra?: { path: string; element: ReactNode }[] } = {}) {
  if (user) {
    localStorage.setItem('shelfspace.accessToken', 'access-0')
    localStorage.setItem('shelfspace.refreshToken', 'refresh-0')
    server.use(http.get('*/api/v1/auth/me', () => HttpResponse.json(user)))
  }
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const router = createMemoryRouter([{ path, element }, ...extra], { initialEntries: [url] })

  const utils = render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <ToastProvider>
          <RouterProvider router={router} />
        </ToastProvider>
      </AuthProvider>
    </QueryClientProvider>,
  )
  return { ...utils, router, user: userEvent.setup() }
}
