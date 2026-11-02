import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { dashboardApi, loansApi, recommendationsApi, reservationsApi } from '../api/endpoints'
import type { Loan, Reservation } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { BookCover } from '../components/BookCover'
import { RecommendationGrid } from '../components/RecommendationGrid'
import { useToast } from '../components/toast/useToast'
import { Alert, Card, EmptyState, PageLoader, SectionTitle, StatItem, StatStrip, StatusBadge } from '../components/ui'
import { daysUntil, formatDate, formatDateTime, formatMoney } from '../lib/format'
import { errorMessage } from '../lib/ui-helpers'

export function DashboardPage() {
  const { user } = useAuth()
  const dashboard = useQuery({ queryKey: ['dashboard', 'me'], queryFn: dashboardApi.me })
  const recommended = useQuery({ queryKey: ['recommendations', 'me'], queryFn: () => recommendationsApi.mine(8) })

  if (dashboard.isPending) return <PageLoader />
  if (dashboard.isError) return <Alert>{errorMessage(dashboard.error)}</Alert>
  const { summary, currentLoans, reservations, recentlyReturned } = dashboard.data

  return (
    <>
      <p className="text-[13px] font-semibold tracking-wide text-ink-500 uppercase">{todayLabel()}</p>
      <h1 className="mb-6 font-display text-[34px] leading-tight font-bold tracking-tight">
        {greeting()}, {user?.name.split(' ')[0] ?? 'reader'}
      </h1>

      <StatStrip>
        <StatItem label="Checked out" value={<>{summary.booksOut}<span className="text-lg text-ink-400"> of {summary.booksAllowed}</span></>} />
        <StatItem label="Due in 3 days" value={summary.dueWithin3Days} tone={summary.dueWithin3Days ? 'warn' : undefined} />
        <StatItem label="Overdue" value={summary.overdue} tone={summary.overdue ? 'danger' : undefined} />
        <StatItem label="Fines" value={formatMoney(summary.unpaidFines)} tone={summary.unpaidFines > 0 ? 'danger' : undefined} to="/fines" />
      </StatStrip>

      {summary.overdue > 0 && (
        <div className="mt-4"><Alert>You have overdue books. Fines add up daily, and you can't borrow more until they're back.</Alert></div>
      )}

      <Shelf title="Checked Out" empty={<>Nothing checked out. <Link to="/books" className="font-semibold text-accent-600">Find something to read ›</Link></>}>
        {currentLoans.map((l) => <LoanTile key={l.id} loan={l} />)}
      </Shelf>
      {currentLoans.some((l) => l.status === 'ISSUED' || l.status === 'OVERDUE') && (
        <p className="mt-4 text-[13px] text-ink-500">
          To return a book, bring it to the library desk. It's checked in there, and the loan closes on your account.
        </p>
      )}

      {reservations.length > 0 && (
        <Shelf title="Waitlist">
          {reservations.map((r) => <ReservationTile key={r.id} reservation={r} />)}
        </Shelf>
      )}

      {recommended.data && recommended.data.length > 0 && (
        <section className="mt-12">
          <SectionTitle aside={<Link to="/books" className="text-[15px] font-medium text-accent-600">See All</Link>}>You Might Like</SectionTitle>
          <RecommendationGrid items={recommended.data} />
        </section>
      )}

      {recentlyReturned.length > 0 && (
        <section className="mt-12">
          <SectionTitle>Recently Returned</SectionTitle>
          <Card className="divide-y divide-ink-200/70">
            {recentlyReturned.map((l) => (
              <div key={l.id} className="flex items-center gap-3 px-4 py-3">
                <BookCover title={l.bookTitle} isbn={l.bookIsbn ?? undefined} size="xs" />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-[15px] font-semibold">{l.bookTitle}</p>
                  <p className="truncate text-[13px] text-ink-500">{l.bookAuthor}</p>
                </div>
                <span className="text-[13px] text-ink-500">{formatDate(l.returnedAt)}</span>
                <StatusBadge status={l.status} />
              </div>
            ))}
          </Card>
        </section>
      )}
    </>
  )
}

function Shelf({ title, empty, children }: { title: string; empty?: ReactNode; children: ReactNode[] }) {
  return (
    <section className="mt-12">
      <SectionTitle>{title}</SectionTitle>
      {children.length === 0 && empty
        ? <EmptyState title="Your shelf is empty">{empty}</EmptyState>
        : <div className="-mx-4 flex snap-x scroll-px-4 gap-6 overflow-x-auto px-4 pb-2">{children}</div>}
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

function LoanTile({ loan }: { loan: Loan }) {
  const refresh = useRefreshDashboard()
  const toast = useToast()
  const onError = (e: unknown) => toast.show(errorMessage(e), 'error')
  const renew = useMutation({
    mutationFn: () => loansApi.renew(loan.id),
    onSuccess: (l) => { toast.show(`Renewed. Now due ${formatDate(l.dueDate)}`); refresh() },
    onError,
  })
  const cancel = useMutation({
    mutationFn: () => loansApi.cancel(loan.id),
    onSuccess: () => { toast.show('Request cancelled'); refresh() },
    onError,
  })
  const days = daysUntil(loan.dueDate)

  let status: ReactNode
  if (loan.status === 'REQUESTED') status = <span className="text-ink-500">Awaiting approval</span>
  else if (days !== null && days < 0) status = <span className="font-semibold text-danger-600">{-days} day{days === -1 ? '' : 's'} overdue · {formatMoney(loan.accruedFine)}</span>
  else if (days === 0) status = <span className="font-semibold text-accent-600">Due today</span>
  else if (days !== null && days <= 3) status = <span className="font-semibold text-accent-600">Due in {days} day{days === 1 ? '' : 's'}</span>
  else status = <span className="text-ink-500">Due {formatDate(loan.dueDate)}</span>

  return (
    <div className="w-36 shrink-0 snap-start sm:w-40">
      <Link to={loan.bookId ? `/books/${loan.bookId}` : '#'} className="group block">
        <div className="relative">
          <BookCover title={loan.bookTitle} author={loan.bookAuthor ?? undefined} isbn={loan.bookIsbn ?? undefined} size="fill"
            className={`transition-transform duration-200 group-hover:-translate-y-1 ${loan.status === 'REQUESTED' ? 'opacity-60' : ''}`} />
        </div>
        <p className="mt-2.5 line-clamp-2 text-sm leading-snug font-semibold">{loan.bookTitle}</p>
      </Link>
      <p className="mt-0.5 text-[13px]">{status}</p>
      {loan.status === 'ISSUED' && (
        <button onClick={() => renew.mutate()} disabled={renew.isPending}
          className="mt-1 text-[13px] font-semibold text-accent-600 hover:text-accent-700 disabled:opacity-40">
          {renew.isPending ? 'Renewing…' : 'Renew'}
        </button>
      )}
      {loan.status === 'REQUESTED' && (
        <button onClick={() => cancel.mutate()} disabled={cancel.isPending}
          className="mt-1 text-[13px] font-semibold text-danger-600 hover:text-danger-700 disabled:opacity-40">
          Cancel Request
        </button>
      )}
    </div>
  )
}

function ReservationTile({ reservation: r }: { reservation: Reservation }) {
  const refresh = useRefreshDashboard()
  const toast = useToast()
  const cancel = useMutation({
    mutationFn: () => reservationsApi.cancel(r.id),
    onSuccess: () => { toast.show('Left the waitlist'); refresh() },
    onError: (e) => toast.show(errorMessage(e), 'error'),
  })
  return (
    <div className="w-36 shrink-0 snap-start sm:w-40">
      <Link to={`/books/${r.bookId}`} className="group block">
        <div className="relative">
          <BookCover title={r.bookTitle} author={r.bookAuthor} isbn={r.bookIsbn} size="fill" className="transition-transform duration-200 group-hover:-translate-y-1" />
        </div>
        <p className="mt-2.5 line-clamp-2 text-sm leading-snug font-semibold">{r.bookTitle}</p>
      </Link>
      <p className="mt-0.5 text-[13px] text-ink-500">
        {r.status === 'READY'
          ? <span className="font-semibold text-success-700">Ready · held until {formatDateTime(r.expiresAt)}</span>
          : <><span className="font-semibold text-ink-900">#{r.position} in line</span>{r.estimatedAvailableDate ? ` · around ${formatDate(r.estimatedAvailableDate)}` : ''}</>}
      </p>
      <button onClick={() => cancel.mutate()} disabled={cancel.isPending}
        className="mt-1 text-[13px] font-semibold text-danger-600 hover:text-danger-700 disabled:opacity-40">
        Leave Waitlist
      </button>
    </div>
  )
}

function greeting() {
  const h = new Date().getHours()
  return h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening'
}

function todayLabel() {
  return new Intl.DateTimeFormat('en-IN', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date())
}
