import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { Loan } from '../../api/types'
import { admin, member, server } from '../../test/server'
import { renderPage } from '../../test/render'
import { MemberPage } from './MemberPage'

const loan: Loan = {
  id: 40, status: 'ISSUED', bookId: 1, bookTitle: 'Clean Code', bookAuthor: 'Robert C. Martin', bookIsbn: '9780132350884',
  copyBarcode: 'SS000001-001', memberId: 2, memberName: 'Demo Member', requestedAt: '2026-11-01T10:00:00',
  borrowedAt: '2026-11-01T11:00:00', dueDate: '2026-11-15', returnedAt: null, renewCount: 0, overdueDays: null, accruedFine: null,
}

function open(fineCharged: number) {
  let returned = false
  server.use(
    http.get('*/api/v1/members/2', () => HttpResponse.json({
      member: { ...member, activeLoans: returned ? 0 : 1, overdueLoans: 0, unpaidFines: fineCharged },
      currentLoans: returned ? [] : [loan],
      reservations: [],
      recentHistory: returned ? [{ ...loan, status: 'RETURNED', returnedAt: '2026-11-03T10:00:00' }] : [],
    })),
    http.post('*/api/v1/loans/40/return', () => {
      returned = true
      return HttpResponse.json({ loan: { ...loan, status: 'RETURNED' }, overdueDays: fineCharged ? 3 : 0, fineCharged })
    }),
  )
  return renderPage(<MemberPage />, { path: '/admin/members/:id', url: '/admin/members/2', user: admin })
}

describe('MemberPage check in', () => {
  it('checks a book in from the profile', async () => {
    const { user } = open(0)

    await user.click(await screen.findByRole('button', { name: 'Check in' }))

    expect(await screen.findByText('Returned on time. No fine.')).toBeInTheDocument()
    expect(await screen.findByText('Nothing borrowed')).toBeInTheDocument()
  })

  it('tells the librarian about a late fine', async () => {
    const { user } = open(30)

    await user.click(await screen.findByRole('button', { name: 'Check in' }))

    expect(await screen.findByText('Returned 3 day(s) late. Fine of ₹30.00 charged.')).toBeInTheDocument()
  })
})
