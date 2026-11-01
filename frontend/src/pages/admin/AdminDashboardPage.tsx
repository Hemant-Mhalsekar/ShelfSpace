import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router'
import { dashboardApi } from '../../api/endpoints'
import type { AdminDashboard } from '../../api/types'
import { Alert, Card, EmptyState, PageHeader, PageLoader, StatItem, StatStrip } from '../../components/ui'
import { formatMoney } from '../../lib/format'
import { errorMessage } from '../../lib/ui-helpers'

export function AdminDashboardPage() {
  const dash = useQuery({ queryKey: ['dashboard', 'admin'], queryFn: dashboardApi.admin, refetchInterval: 60_000 })

  if (dash.isPending) return <PageLoader />
  if (dash.isError) return <Alert>{errorMessage(dash.error)}</Alert>
  const d = dash.data

  return (
    <>
      <PageHeader title="Library dashboard" subtitle="Live numbers; refreshes every minute" />

      <StatStrip>
        <StatItem label="Pending requests" value={d.circulation.pendingRequests} to="/admin/loans" tone={d.circulation.pendingRequests ? 'warn' : undefined} />
        <StatItem label="Overdue now" value={d.circulation.overdueNow} to="/admin/loans?tab=overdue" tone={d.circulation.overdueNow ? 'danger' : undefined} />
        <StatItem label="Books out" value={d.circulation.borrowedNow} to="/admin/loans?tab=out" />
        <StatItem label="Members waiting" value={d.circulation.membersWaiting} hint="on reservation queues" />
      </StatStrip>
      <div className="mt-4">
        <StatStrip>
          <StatItem label="Titles" value={d.inventory.titles} to="/admin/books" />
          <StatItem label="Copies on shelf" value={<>{d.inventory.availableCopies}<span className="text-xl text-ink-500"> / {d.inventory.copies}</span></>}
            hint={d.inventory.damagedCopies + d.inventory.lostCopies ? `${d.inventory.damagedCopies} damaged · ${d.inventory.lostCopies} lost` : undefined} />
          <StatItem label="Active members" value={d.activeMembers} to="/admin/members" />
          <StatItem label="Unpaid fines" value={formatMoney(d.fines.unpaidTotal)} to="/admin/fines"
            hint={`${formatMoney(d.fines.collectedThisMonth)} collected this month`} />
        </StatStrip>
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-5">
        <Card className="p-5 lg:col-span-3">
          <h2 className="text-[17px] font-semibold">Books borrowed per month</h2>
          <p className="mb-4 text-sm text-ink-500">Last 6 months</p>
          <MonthlyChart data={d.borrowsPerMonth} />
        </Card>

        <Card className="p-5 lg:col-span-2">
          <h2 className="mb-4 text-[17px] font-semibold">Most borrowed</h2>
          {d.mostBorrowed.length === 0 ? <EmptyState title="No loans yet" /> : (
            <ol className="space-y-3">
              {d.mostBorrowed.map((b, i) => (
                <li key={b.bookId} className="flex items-center gap-3">
                  <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-ink-100 text-xs font-semibold text-ink-600">{i + 1}</span>
                  <div className="min-w-0 flex-1">
                    <Link to={`/admin/books/${b.bookId}`} className="block truncate text-sm font-medium hover:text-accent-700">{b.title}</Link>
                    <p className="truncate text-xs text-ink-500">{b.author}</p>
                  </div>
                  <span className="text-sm text-ink-600 tabular-nums">{b.timesBorrowed}×</span>
                </li>
              ))}
            </ol>
          )}
        </Card>
      </div>
    </>
  )
}

const monthName = new Intl.DateTimeFormat('en-IN', { month: 'short' })
const monthLong = new Intl.DateTimeFormat('en-IN', { month: 'long', year: 'numeric' })

/** Single-series bar chart: one hue, recessive grid, hover shows the exact value. */
function MonthlyChart({ data }: { data: AdminDashboard['borrowsPerMonth'] }) {
  const [hover, setHover] = useState<number | null>(null)
  const max = Math.max(1, ...data.map((m) => m.borrowed))
  const ticks = [0, Math.ceil(max / 2), max]

  return (
    <div>
      <div className="relative flex h-48 gap-3 pl-8">
        {/* gridlines */}
        {ticks.map((t) => (
          <div key={t} className="pointer-events-none absolute right-0 left-8 border-t border-ink-100" style={{ bottom: `${(t / max) * 100}%` }}>
            <span className="absolute -top-2 -left-8 w-6 text-right text-[10px] text-ink-500 tabular-nums">{t}</span>
          </div>
        ))}
        {data.map((m, i) => {
          const date = new Date(`${m.month}-01T00:00:00`)
          const pct = (m.borrowed / max) * 100
          return (
            <div
              key={m.month}
              className="relative flex flex-1 cursor-default items-end justify-center"
              onMouseEnter={() => setHover(i)}
              onMouseLeave={() => setHover(null)}
              aria-label={`${monthLong.format(date)}: ${m.borrowed} borrowed`}
              role="img"
            >
              {hover === i && (
                <div className="absolute z-10 -translate-y-2 rounded-md bg-ink-900 px-2 py-1 text-xs whitespace-nowrap text-white shadow" style={{ bottom: `${pct}%` }}>
                  {monthLong.format(date)}: <strong>{m.borrowed}</strong>
                </div>
              )}
              <div
                className={`w-full max-w-12 rounded-t transition-colors ${hover === i ? 'bg-accent-400' : 'bg-accent-500'}`}
                style={{ height: m.borrowed ? `max(${pct}%, 4px)` : '0' }}
              />
            </div>
          )
        })}
      </div>
      <div className="mt-2 flex gap-3 pl-8">
        {data.map((m) => (
          <span key={m.month} className="flex-1 text-center text-xs text-ink-500">
            {monthName.format(new Date(`${m.month}-01T00:00:00`))}
          </span>
        ))}
      </div>
    </div>
  )
}
