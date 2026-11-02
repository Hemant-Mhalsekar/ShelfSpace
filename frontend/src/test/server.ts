import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import type { AuthResponse, User } from '../api/types'

/** Fake API. Each test adds the handlers it needs with server.use(...). */
export const server = setupServer(
  // The sign-in page pings this to wake the free-tier server
  http.get('*/actuator/health', () => HttpResponse.json({ status: 'UP' })),
  http.get('*/api/v1/notifications/me/unread-count', () => HttpResponse.json({ unread: 0 })),
)

export const member: User = {
  id: 2, name: 'Demo Member', email: 'member@shelfspace.dev', phone: null,
  role: 'MEMBER', membershipTier: 'STUDENT', status: 'ACTIVE', createdAt: '2026-09-01T10:00:00',
}

export const admin: User = {
  ...member, id: 1, name: 'Library Admin', email: 'admin@shelfspace.dev', role: 'ADMIN', membershipTier: 'FACULTY',
}

export function authResponse(user: User, n = 1): AuthResponse {
  return { accessToken: `access-${n}`, refreshToken: `refresh-${n}`, tokenType: 'Bearer', expiresIn: 900, user }
}

/** The API's error shape. */
export function apiError(status: number, message: string, fieldErrors?: Record<string, string>) {
  return HttpResponse.json(
    { timestamp: new Date().toISOString(), status, error: 'Error', message, path: '/', fieldErrors: fieldErrors ?? null },
    { status },
  )
}

export function page<T>(content: T[], totalPages = 1) {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages, last: totalPages <= 1 }
}
