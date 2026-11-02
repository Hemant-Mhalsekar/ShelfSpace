import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { authResponse, member, server, apiError } from '../test/server'
import { api, ApiError, SESSION_EXPIRED_EVENT, tokens } from './client'

function signedIn() {
  tokens.save(authResponse(member, 0))
}

describe('api client', () => {
  it('sends the access token and returns the JSON body', async () => {
    signedIn()
    server.use(http.get('*/api/v1/books/1', ({ request }) =>
      HttpResponse.json({ id: 1, auth: request.headers.get('Authorization') })))

    await expect(api('/books/1')).resolves.toEqual({ id: 1, auth: 'Bearer access-0' })
  })

  it('turns an error response into an ApiError with the server message and field errors', async () => {
    server.use(http.post('*/api/v1/auth/register', () => apiError(400, 'Validation failed', { email: 'must be a well-formed email address' })))

    const err = await api('/auth/register', { method: 'POST', body: {} }).catch((e: unknown) => e)
    expect(err).toBeInstanceOf(ApiError)
    expect(err).toMatchObject({ status: 400, message: 'Validation failed', fieldErrors: { email: 'must be a well-formed email address' } })
  })

  it('on a 401, refreshes once, saves the new tokens and retries', async () => {
    signedIn()
    server.use(
      http.get('*/api/v1/loans/me', ({ request }) =>
        request.headers.get('Authorization') === 'Bearer access-1'
          ? HttpResponse.json({ ok: true })
          : apiError(401, 'Missing, invalid or expired token')),
      http.post('*/api/v1/auth/refresh', () => HttpResponse.json(authResponse(member, 1))),
    )

    await expect(api('/loans/me')).resolves.toEqual({ ok: true })
    expect(tokens.access).toBe('access-1')
    expect(tokens.refresh).toBe('refresh-1')
  })

  it('shares one refresh between requests that fail at the same time', async () => {
    signedIn()
    let refreshes = 0
    server.use(
      http.get('*/api/v1/:thing', ({ request }) =>
        request.headers.get('Authorization') === 'Bearer access-1'
          ? HttpResponse.json({ ok: true })
          : apiError(401, 'expired')),
      http.post('*/api/v1/auth/refresh', () => { refreshes++; return HttpResponse.json(authResponse(member, 1)) }),
    )

    await Promise.all([api('/a'), api('/b'), api('/c')])
    expect(refreshes).toBe(1)
  })

  it('signs out when the refresh token is rejected', async () => {
    signedIn()
    const expired = vi.fn()
    window.addEventListener(SESSION_EXPIRED_EVENT, expired)
    server.use(
      http.get('*/api/v1/loans/me', () => apiError(401, 'expired')),
      http.post('*/api/v1/auth/refresh', () => apiError(401, 'Your session has ended. Please sign in again.')),
    )

    await expect(api('/loans/me')).rejects.toMatchObject({ status: 401 })
    expect(tokens.access).toBeNull()
    expect(expired).toHaveBeenCalledOnce()
    window.removeEventListener(SESSION_EXPIRED_EVENT, expired)
  })

  it('keeps the session if another tab already refreshed', async () => {
    signedIn()
    server.use(
      http.get('*/api/v1/loans/me', ({ request }) =>
        request.headers.get('Authorization') === 'Bearer access-1' ? HttpResponse.json({ ok: true }) : apiError(401, 'expired')),
      // Our refresh token was already used by the other tab, which saved a newer pair meanwhile
      http.post('*/api/v1/auth/refresh', () => {
        tokens.save(authResponse(member, 1))
        return apiError(401, 'Your session has ended. Please sign in again.')
      }),
    )

    await expect(api('/loans/me')).resolves.toEqual({ ok: true })
    expect(tokens.access).toBe('access-1')
  })

  it("doesn't try to refresh for the auth endpoints themselves", async () => {
    signedIn()
    const refresh = vi.fn()
    server.use(
      http.post('*/api/v1/auth/login', () => apiError(401, 'Invalid email or password')),
      http.post('*/api/v1/auth/refresh', () => { refresh(); return HttpResponse.json(authResponse(member, 1)) }),
    )

    await expect(api('/auth/login', { method: 'POST', body: {} })).rejects.toMatchObject({ message: 'Invalid email or password' })
    expect(refresh).not.toHaveBeenCalled()
  })
})
