import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { AuditEntry } from '../../api/types'
import { admin, page, server } from '../../test/server'
import { renderPage } from '../../test/render'
import { ActivityPage } from './ActivityPage'

// The API sends local times without a zone (LocalDateTime), so build one the same way
const d = new Date()
const now = new Date(d.getTime() - d.getTimezoneOffset() * 60_000).toISOString().slice(0, 19)
const entry = (e: Partial<AuditEntry>): AuditEntry => ({
  id: 1, createdAt: now, action: 'LOAN_APPROVED', actorId: 1, actorName: 'Library Admin', entityType: 'Loan',
  entityId: 40, entityLabel: 'Clean Code · Priya Sharma', bookId: 7, memberId: 3, details: 'dueDate=2026-11-16', ...e,
})

function open(entries: AuditEntry[], url = '/admin/activity') {
  const requests: URL[] = []
  server.use(
    http.get('*/api/v1/audit', ({ request }) => { requests.push(new URL(request.url)); return HttpResponse.json(page(entries)) }),
    http.get('*/api/v1/audit/actions', () => HttpResponse.json(['LOAN_APPROVED', 'PASSWORD_CHANGED'])),
  )
  return { ...renderPage(<ActivityPage />, { path: '/admin/activity', url, user: admin }), requests }
}

describe('ActivityPage', () => {
  it('reads each entry as a sentence with links to the book and member', async () => {
    open([entry({})])

    const book = await screen.findByRole('link', { name: 'Clean Code' })
    expect(book).toHaveAttribute('href', '/admin/books/7')
    expect(screen.getByRole('link', { name: 'Priya Sharma' })).toHaveAttribute('href', '/admin/members/3')
    expect(book.closest('p')).toHaveTextContent('Library Admin approved a loan of Clean Code for Priya Sharma')
    expect(screen.getByText('Due 16 Nov 2026')).toBeInTheDocument()
    expect(screen.getByText('Today')).toBeInTheDocument()
  })

  it('leaves out the subject when people act on their own account', async () => {
    open([entry({ action: 'PASSWORD_CHANGED', actorId: 2, actorName: 'Demo Member', entityType: 'User', entityId: 2, entityLabel: 'Demo Member', memberId: 2, details: null })])

    const name = await screen.findByRole('button', { name: 'Demo Member' })
    expect(name.closest('p')).toHaveTextContent(/^Demo Member changed their password$/)
  })

  it('formats money in details and shows free text as it is', async () => {
    open([
      entry({ id: 1, action: 'LOAN_RETURNED', details: 'daysLate=3, fine=30.00' }),
      entry({ id: 2, action: 'MEMBER_SUSPENDED', entityType: 'User', entityId: 4, entityLabel: 'Rahul Verma', memberId: 4, details: 'Repeated late returns' }),
    ])

    expect(await screen.findByText('Days late 3 · Fine ₹30.00')).toBeInTheDocument()
    expect(screen.getByText('Repeated late returns')).toBeInTheDocument()
  })

  it("clicking someone's name filters to what they did", async () => {
    const { user, requests, router } = open([entry({})])

    await user.click(await screen.findByRole('button', { name: 'Library Admin' }))

    expect(await screen.findByText('By Library Admin')).toBeInTheDocument()
    expect(router.state.location.search).toContain('actorId=1')
    expect(requests.at(-1)?.searchParams.get('actorId')).toBe('1')
  })

  it('sends the category tab as a filter', async () => {
    const { user, requests } = open([entry({})])
    await screen.findByRole('link', { name: 'Clean Code' })

    await user.click(screen.getByRole('tab', { name: 'Fines' }))

    await screen.findByRole('tab', { name: 'Fines', selected: true })
    expect(requests.at(-1)?.searchParams.get('category')).toBe('FINES')
  })

  it('says so when nothing matches', async () => {
    open([], '/admin/activity?category=RULES')
    expect(await screen.findByText('Nothing recorded')).toBeInTheDocument()
    expect(screen.getByText('Try widening the filters.')).toBeInTheDocument()
  })
})
