import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { dashboardApi, loansApi, recommendationsApi, reservationsApi } from '../api/endpoints'
import type { Loan, Reservation } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { RecommendationGrid } from '../components/RecommendationGrid'
import { Alert, Button, Card, EmptyState, LinkButton, PageHeader, PageLoader, StatusBadge } from '../components/ui'
import { errorMessage } from '../lib/ui-helpers'
import { daysUntil, formatDate, formatDateTime, formatMoney } from '../lib/format'

export function DashboardPage() {
  const { user } = useAuth()
  const dashboard = useQuery({ queryKey: ['dashboard', 'me'], queryFn: dashboardApi.me })
  const recommended = useQuery({ queryKey: ['recommendations', 'me'], queryFn: () => recommendationsApi.mine(6) })

  if (dashboard.isPending) return <PageLoader />
  if (dashboard.isError) return <Alert>{errorMessage(dashboard.error)}</Alert>
  const { summary, currentLoans, reservations, recentlyReturned } = dashboard.data

  return (
    <>
      <PageHeader title={`Hi, ${user?.name.split(' ')[0] ?? 'there'}`} subtitle="Your loans, reservations and fines at a glance" />

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <Stat label="Books out" value={`${summary.booksOut} / ${summary.booksAllowed}`} />
        <Stat label="Due in 3 days" value={summary.dueWithin3Days} tone={summary.dueWithin3Days ? 'amber' : undefined} />
        <Stat label="Overdue" value={summary.overdue} tone={summary.overdue ? 'red' : undefined} />
        <Stat label="Fines owed" value={formatMoney(summary.unpaidFines)} tone={summary.unpaidFines > 0 ? 'red' : undefined} link="/fines" />
      </div>

      {summary.overdue > 0 && (
        <div className="mt-6"><Alert>You have overdue books. Fines grow daily, and you can't borrow more until they're returned.</Alert></div>
      )}

      <Section title="Current loans">
        {currentLoans.length === 0
          ? <EmptyState title="Nothing borrowed right now"><Link to="/books" className="text-indigo-600 hover:underline">Browse the catalog</Link></EmptyState>
          : <div className="space-y-3">{currentLoans.map((l) => <LoanRow key={l.id} loan={l} />)}</div>}
      </Section>

      <Section title="Reservations">
        {reservations.length === 0
          ? <EmptyState title="No reservations">When all copies of a book are out, you can join its waiting list.</EmptyState>
          : <div className="space-y-3">{reservations.map((r) => <ReservationRow key={r.id} reservation={r} />)}</div>}
      </Section>

      {recommended.data && recommended.data.length > 0 && (
        <Section title="Recommended for you">
          <RecommendationGrid items={recommended.data} />
        </Section>
      )}

      <Section title="Recently returned">
        {recentlyReturned.length === 0
          ? <EmptyState title="No history yet" />
          : (
            <Card className="divide-y divide-slate-100">
              {recentlyReturned.map((l) => (
                <div key={l.id} className="flex items-center justify-between px-4 py-3 text-sm">
                  <span className="font-medium">{l.bookTitle}</span>
                  <span className="flex items-center gap-3 text-slate-500">{formatDate(l.returnedAt)} <StatusBadge status={l.status} /></span>
                </div>
              ))}
            </Card>
          )}
      </Section>
    </>
  )
}

function Stat({ label, value, tone, link }: { label: string; value: string | number; tone?: 'amber' | 'red'; link?: string }) {
  const color = tone === 'red' ? 'text-rose-600' : tone === 'amber' ? 'text-amber-600' : 'text-slate-900'
  const body = (
    <Card className="p-5">
      <p className="text-sm text-slate-500">{label}</p>
      <p className={`mt-1 text-2xl font-bold ${color}`}>{value}</p>
    </Card>
  )
  return link ? <Link to={link} className="block transition hover:opacity-80">{body}</Link> : body
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="mt-10">
      <h2 className="mb-3 text-lg font-semibold">{title}</h2>
      {children}
    </section>
  )
}

function useRefreshDashboard() {
  const queryClient = useQueryClient()
  return () => {
    void queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    void queryClient.invalidateQueries({ queryKey: ['reservations'] })
    void queryClient.invalidateQueries({ queryKey: ['books'] })
  }
}

function LoanRow({ loan }: { loan: Loan }) {
  const refresh = useRefreshDashboard()
  const renew = useMutation({ mutationFn: () => loansApi.renew(loan.id), onSuccess: refresh })
  const cancel = useMutation({ mutationFn: () => loansApi.cancel(loan.id), onSuccess: refresh })
  const error = renew.error ?? cancel.error
  const days = daysUntil(loan.dueDate)

  let due: ReactNode = null
  if (loan.status === 'REQUESTED') due = <span className="text-slate-500">Waiting for librarian approval</span>
  else if (days !== null && days < 0) due = <span className="font-medium text-rose-600">{-days} day{days === -1 ? '' : 's'} overdue · fine so far {formatMoney(loan.accruedFine)}</span>
  else if (days === 0) due = <span className="font-medium text-amber-600">Due today</span>
  else if (days !== null && days <= 3) due = <span className="font-medium text-amber-600">Due in {days} day{days === 1 ? '' : 's'} ({formatDate(loan.dueDate)})</span>
  else if (loan.dueDate) due = <span className="text-slate-500">Due {formatDate(loan.dueDate)}</span>

  return (
    <Card className="p-4">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            {loan.bookId
              ? <Link to={`/books/${loan.bookId}`} className="truncate font-medium hover:text-indigo-700">{loan.bookTitle}</Link>
              : <span className="font-medium">{loan.bookTitle}</span>}
            <StatusBadge status={loan.status} />
          </div>
          <p className="mt-1 text-sm">{due}</p>
        </div>
        {loan.status === 'ISSUED' && (
          <Button variant="secondary" loading={renew.isPending} onClick={() => renew.mutate()}>
            Renew{loan.renewCount ? ` (${loan.renewCount} used)` : ''}
          </Button>
        )}
        {loan.status === 'REQUESTED' && (
          <Button variant="danger" loading={cancel.isPending} onClick={() => cancel.mutate()}>Cancel request</Button>
        )}
      </div>
      {error && <div className="mt-3"><Alert>{errorMessage(error)}</Alert></div>}
    </Card>
  )
}

function ReservationRow({ reservation: r }: { reservation: Reservation }) {
  const refresh = useRefreshDashboard()
  const cancel = useMutation({ mutationFn: () => reservationsApi.cancel(r.id), onSuccess: refresh })

  return (
    <Card className="p-4">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <Link to={`/books/${r.bookId}`} className="truncate font-medium hover:text-indigo-700">{r.bookTitle}</Link>
            <StatusBadge status={r.status} />
          </div>
          <p className="mt-1 text-sm text-slate-500">
            {r.status === 'READY'
              ? <span className="font-medium text-emerald-700">Ready! Held for you until {formatDateTime(r.expiresAt)}</span>
              : <>#{r.position} in queue{r.estimatedAvailableDate && <> · expected around {formatDate(r.estimatedAvailableDate)}</>}</>}
          </p>
        </div>
        {r.status === 'READY' && <LinkButton to={`/books/${r.bookId}`}>Borrow now</LinkButton>}
        <Button variant="danger" loading={cancel.isPending} onClick={() => cancel.mutate()}>Cancel</Button>
      </div>
      {cancel.error && <div className="mt-3"><Alert>{errorMessage(cancel.error)}</Alert></div>}
    </Card>
  )
}
