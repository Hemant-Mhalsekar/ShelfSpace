import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { tokens } from '../api/client'
import { admin, apiError, authResponse, member, server } from '../test/server'
import { renderPage } from '../test/render'
import { AccountPage } from './AccountPage'

const studentRules = {
  membershipTier: 'STUDENT', maxBooks: 5, loanDays: 14, maxRenewals: 2, finePerDay: 10, fineCap: 500,
  maxUnpaidFines: 100, maxReservations: 3, holdHours: 48, updatedAt: '2026-10-30T19:00:00',
}

function open(user = member) {
  server.use(http.get('*/api/v1/policies/STUDENT', () => HttpResponse.json(studentRules)))
  return renderPage(<AccountPage />, { path: '/account', user })
}

const passwordSection = () => within(screen.getByRole('heading', { name: 'Password' }).closest('section')!)

describe('AccountPage', () => {
  it("shows the member's limits in plain words", async () => {
    open()
    expect(await screen.findByText('5 books')).toBeInTheDocument()
    expect(screen.getByText('Up to 2 times per loan')).toBeInTheDocument()
    expect(screen.getByText('₹10.00 a day, capped at ₹500.00 per book')).toBeInTheDocument()
  })

  it("librarians don't get a membership section", async () => {
    open(admin)
    expect(await screen.findByRole('heading', { name: 'Profile' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Your Membership' })).not.toBeInTheDocument()
  })

  it('only enables Save once something changed, then saves the profile', async () => {
    let sent: unknown
    server.use(http.put('*/api/v1/account/profile', async ({ request }) => {
      sent = await request.json()
      return HttpResponse.json({ ...member, name: 'Asha Rao' })
    }))
    const { user } = open()
    const save = await screen.findByRole('button', { name: 'Save Changes' })
    expect(save).toBeDisabled()

    const name = screen.getByLabelText('Full name')
    await user.clear(name)
    await user.type(name, 'Asha Rao')
    expect(save).toBeEnabled()
    await user.click(save)

    expect(await screen.findByText('Profile updated')).toBeInTheDocument()
    expect(sent).toEqual({ name: 'Asha Rao', phone: null })
  })

  it('checks the new password before sending anything', async () => {
    const { user } = open()
    const pw = await screen.findByRole('heading', { name: 'Password' }).then(() => passwordSection())

    await user.type(pw.getByLabelText('Current password'), 'Secret123')
    await user.type(pw.getByLabelText(/^New password/), 'short')
    expect(pw.getByText('At least 8 characters, with a letter and a number')).toBeInTheDocument()

    await user.clear(pw.getByLabelText(/^New password/))
    await user.type(pw.getByLabelText(/^New password/), 'Brand1New')
    await user.type(pw.getByLabelText(/^Confirm new password/), 'Brand1Nex')
    expect(pw.getByText("Passwords don't match")).toBeInTheDocument()
    expect(pw.getByRole('button', { name: 'Change Password' })).toBeDisabled()
  })

  it('shows the error when the current password is wrong', async () => {
    server.use(http.post('*/api/v1/account/password', () => apiError(422, 'Your current password is incorrect')))
    const { user } = open()
    await screen.findByRole('heading', { name: 'Password' })
    const pw = passwordSection()

    await user.type(pw.getByLabelText('Current password'), 'Wrong1234')
    await user.type(pw.getByLabelText(/^New password/), 'Brand1New')
    await user.type(pw.getByLabelText(/^Confirm new password/), 'Brand1New')
    await user.click(pw.getByRole('button', { name: 'Change Password' }))

    expect(await pw.findByRole('alert')).toHaveTextContent('Your current password is incorrect')
  })

  it('keeps this device signed in with the new tokens after a change', async () => {
    server.use(http.post('*/api/v1/account/password', () => HttpResponse.json(authResponse(member, 7))))
    const { user } = open()
    await screen.findByRole('heading', { name: 'Password' })
    const pw = passwordSection()

    await user.type(pw.getByLabelText('Current password'), 'Secret123')
    await user.type(pw.getByLabelText(/^New password/), 'Brand1New')
    await user.type(pw.getByLabelText(/^Confirm new password/), 'Brand1New')
    await user.click(pw.getByRole('button', { name: 'Change Password' }))

    expect(await screen.findByText(/Password changed/)).toBeInTheDocument()
    expect(tokens.access).toBe('access-7')
    expect(pw.getByLabelText('Current password')).toHaveValue('')
  })

  it('asks before signing out other devices', async () => {
    server.use(http.post('*/api/v1/account/sign-out-everywhere', () => HttpResponse.json(authResponse(member, 9))))
    const { user } = open()

    await user.click(await screen.findByRole('button', { name: 'Sign Out Others…' }))
    expect(screen.getByText(/You stay signed in here/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Sign Out Others' }))

    expect(await screen.findByText('Signed out of all other devices')).toBeInTheDocument()
    expect(tokens.refresh).toBe('refresh-9')
  })
})
