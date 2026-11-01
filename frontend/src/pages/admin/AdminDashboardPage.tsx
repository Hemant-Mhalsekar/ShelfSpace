import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router'
import { dashboardApi } from '../../api/endpoints'
import type { AdminDashboard } from '../../api/types'
import { Alert, Card, EmptyState, PageHeader, PageLoader } from '../../components/ui'
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

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <Tile label="Pending requests" value={d.circulation.pendingRequests} link="/admin/loans" tone={d.circulation.pendingRequests ? 'amber' : undefined} />
        <Tile label="Overdue now" value={d.circulation.overdueNow} link="/admin/loans?tab=overdue" tone={d.circulation.overdueNow ? 'red' : undefined} />
        <Tile label="Books out" value={d.circulation.borrowedNow} link="/admin/loans?tab=out" />
        <Tile label="Members waiting" value={d.circulation.membersWaiting} hint="on reservation queues" />
        <Tile label="Titles" value={d.inventory.titles} link="/admin/books" />
        <Tile label="Copies on shelf" value={`${d.inventory.availableCopies} / ${d.inventory.copies}`}
          hint={d.inventory.damagedCopies + d.inventory.lostCopies ? `${d.inventory.damagedCopies} damaged · ${d.inventory.lostCopies} lost` : undefined} />
        <Tile label="Active members" value={d.activeMembers} link="/admin/members" />
        <Tile label="Unpaid fines" value={formatMoney(d.fines.unpaidTotal)} link="/admin/fines"
          hint={`${formatMoney(d.fines.collectedThisMonth)} collected this month`} />
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-5">
        <Card className="p-5 lg:col-span-3">
          <h2 className="font-semibold">Books borrowed per month</h2>
          <p className="mb-4 text-sm text-slate-500">Last 6 months</p>
          <MonthlyChart data={d.borrowsPerMonth} />
        </Card>

        <Card className="p-5 lg:col-span-2">
          <h2 className="mb-4 font-semibold">Most borrowed</h2>
          {d.mostBorrowed.length === 0 ? <EmptyState title="No loans yet" /> : (
            <ol className="space-y-3">
              {d.mostBorrowed.map((b, i) => (
                <li key={b.bookId} className="flex items-center gap-3">
                  <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-slate-100 text-xs font-semibold text-slate-600">{i + 1}</span>
                  <div className="min-w-0 flex-1">
                    <Link to={`/admin/books/${b.bookId}`} className="block truncate text-sm font-medium hover:text-indigo-700">{b.title}</Link>
                    <p className="truncate text-xs text-slate-500">{b.author}</p>
                  </div>
                  <span className="text-sm text-slate-600 tabular-nums">{b.timesBorrowed}×</span>
                </li>
              ))}
            </ol>
          )}
        </Card>
      </div>
    </>
  )
}

function Tile({ label, value, hint, link, tone }: {
  label: string; value: string | number; hint?: string; link?: string; tone?: 'amber' | 'red'
}) {
  const color = tone === 'red' ? 'text-rose-600' : tone === 'amber' ? 'text-amber-600' : 'text-slate-900'
  const body = (
    <Card className="h-full p-5">
      <p className="text-sm text-slate-500">{label}</p>
      <p className={`mt-1 text-2xl font-bold tabular-nums ${color}`}>{value}</p>
      {hint && <p className="mt-1 text-xs text-slate-400">{hint}</p>}
    </Card>
  )
  return link ? <Link to={link} className="block transition hover:opacity-80">{body}</Link> : body
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
          <div key={t} className="pointer-events-none absolute right-0 left-8 border-t border-slate-100" style={{ bottom: `${(t / max) * 100}%` }}>
            <span className="absolute -top-2 -left-8 w-6 text-right text-[10px] text-slate-400 tabular-nums">{t}</span>
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
                <div className="absolute z-10 -translate-y-2 rounded-md bg-slate-900 px-2 py-1 text-xs whitespace-nowrap text-white shadow" style={{ bottom: `${pct}%` }}>
                  {monthLong.format(date)}: <strong>{m.borrowed}</strong>
                </div>
              )}
              <div
                className={`w-full max-w-12 rounded-t transition-colors ${hover === i ? 'bg-indigo-500' : 'bg-indigo-600'}`}
                style={{ height: m.borrowed ? `max(${pct}%, 4px)` : '0' }}
              />
            </div>
          )
        })}
      </div>
      <div className="mt-2 flex gap-3 pl-8">
        {data.map((m) => (
          <span key={m.month} className="flex-1 text-center text-xs text-slate-500">
            {monthName.format(new Date(`${m.month}-01T00:00:00`))}
          </span>
        ))}
      </div>
    </div>
  )
}
