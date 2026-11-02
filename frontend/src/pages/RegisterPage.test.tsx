import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { apiError, authResponse, member, server } from '../test/server'
import { renderPage } from '../test/render'
import { RegisterPage } from './RegisterPage'

const books = { path: '/books', element: <p>Catalog</p> }

describe('RegisterPage', () => {
  it('shows problems after leaving a field, and fixes them live', async () => {
    const { user } = renderPage(<RegisterPage />, { path: '/register', extra: [books] })

    await user.type(await screen.findByLabelText('Email'), 'ananya@gmail')
    expect(screen.queryByText(/valid email address/)).not.toBeInTheDocument()   // not while typing the first time
    await user.tab()
    expect(screen.getByText('Enter a valid email address, like name@example.com')).toBeInTheDocument()

    await user.type(screen.getByLabelText('Email'), '.com')
    expect(screen.queryByText(/valid email address/)).not.toBeInTheDocument()
  })

  it("doesn't send the form until every field is valid", async () => {
    let sent = false
    server.use(http.post('*/api/v1/auth/register', () => { sent = true; return HttpResponse.json(authResponse(member)) }))
    const { user } = renderPage(<RegisterPage />, { path: '/register', extra: [books] })

    await user.type(await screen.findByLabelText('Full name'), '1234')
    await user.type(screen.getByLabelText('Email'), 'ananya@example.com')
    await user.type(screen.getByLabelText(/^Password/), 'Password1')
    await user.click(screen.getByRole('button', { name: 'Create Account' }))

    expect(screen.getByText('Use letters, spaces, apostrophes, hyphens and dots only')).toBeInTheDocument()
    expect(screen.getByText('Too common. Choose something harder to guess')).toBeInTheDocument()
    expect(sent).toBe(false)
  })

  it('signs up once everything is fine', async () => {
    let body: unknown
    server.use(http.post('*/api/v1/auth/register', async ({ request }) => {
      body = await request.json()
      return HttpResponse.json(authResponse(member), { status: 201 })
    }))
    const { user } = renderPage(<RegisterPage />, { path: '/register', extra: [books] })

    await user.type(await screen.findByLabelText('Full name'), '  Ananya Iyer ')
    await user.type(screen.getByLabelText('Email'), 'ananya@example.com')
    await user.type(screen.getByLabelText(/^Password/), 'Shelf2Read')
    expect(screen.getByText('Good')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Create Account' }))

    expect(await screen.findByText('Catalog')).toBeInTheDocument()
    expect(body).toEqual({ name: 'Ananya Iyer', email: 'ananya@example.com', phone: undefined, password: 'Shelf2Read' })
  })

  it("shows the server's field errors under the right field", async () => {
    server.use(http.post('*/api/v1/auth/register', () => apiError(409, 'An account with this email already exists')))
    const { user } = renderPage(<RegisterPage />, { path: '/register', extra: [books] })

    await user.type(await screen.findByLabelText('Full name'), 'Ananya Iyer')
    await user.type(screen.getByLabelText('Email'), 'ananya@example.com')
    await user.type(screen.getByLabelText(/^Password/), 'Shelf2Read')
    await user.click(screen.getByRole('button', { name: 'Create Account' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('An account with this email already exists')
  })
})
