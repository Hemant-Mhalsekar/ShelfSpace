import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { tokens } from '../api/client'
import { apiError, authResponse, member, server } from '../test/server'
import { renderPage } from '../test/render'
import { LoginPage } from './LoginPage'

const home = { path: '/', element: <p>Home page</p> }

async function fillIn(user: ReturnType<typeof renderPage>['user'], email: string, password: string) {
  await user.type(screen.getByLabelText('Email'), email)
  await user.type(screen.getByLabelText('Password'), password)
  await user.click(screen.getByRole('button', { name: 'Sign In' }))
}

describe('LoginPage', () => {
  it('signs in, saves the tokens and goes home', async () => {
    server.use(http.post('*/api/v1/auth/login', () => HttpResponse.json(authResponse(member, 1))))
    const { user } = renderPage(<LoginPage />, { path: '/login', extra: [home] })

    await fillIn(user, 'member@shelfspace.dev', 'Member@12345')

    expect(await screen.findByText('Home page')).toBeInTheDocument()
    expect(tokens.access).toBe('access-1')
  })

  it('shows the server message for a wrong password', async () => {
    server.use(http.post('*/api/v1/auth/login', () => apiError(401, 'Invalid email or password')))
    const { user } = renderPage(<LoginPage />, { path: '/login', extra: [home] })

    await fillIn(user, 'member@shelfspace.dev', 'nope12345')

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password')
    expect(tokens.access).toBeNull()
  })

  it('explains the lockout after too many attempts', async () => {
    server.use(http.post('*/api/v1/auth/login', () => apiError(429, 'Too many failed sign-in attempts. Try again in 15 minutes.')))
    const { user } = renderPage(<LoginPage />, { path: '/login', extra: [home] })

    await fillIn(user, 'member@shelfspace.dev', 'nope12345')

    expect(await screen.findByRole('alert')).toHaveTextContent('Try again in 15 minutes')
  })

  it('the demo buttons sign in with the demo accounts', async () => {
    let sent: unknown
    server.use(http.post('*/api/v1/auth/login', async ({ request }) => {
      sent = await request.json()
      return HttpResponse.json(authResponse(member, 1))
    }))
    const { user } = renderPage(<LoginPage />, { path: '/login', extra: [home] })

    await user.click(screen.getByRole('button', { name: 'Librarian' }))

    expect(await screen.findByText('Home page')).toBeInTheDocument()
    expect(sent).toEqual({ email: 'admin@shelfspace.dev', password: 'Admin@12345' })
  })
})
