import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router'
import { auditApi, type AuditSearch } from '../../api/endpoints'
import type { AuditCategory, AuditEntry } from '../../api/types'
import { Tabs } from '../../components/Tabs'
import { useToast } from '../../components/toast/useToast'
import { Alert, Button, Card, EmptyState, PageHeader, PageLoader, Pagination } from '../../components/ui'
import { formatDate, formatMoney } from '../../lib/format'
import { errorMessage, inputClass } from '../../lib/ui-helpers'

type Tab = 'ALL' | AuditCategory

const TABS: { value: Tab; label: string }[] = [
  { value: 'ALL', label: 'All' },
  { value: 'LOANS', label: 'Loans' },
  { value: 'BOOKS', label: 'Books' },
  { value: 'FINES', label: 'Fines' },
  { value: 'MEMBERS', label: 'Members' },
  { value: 'RULES', label: 'Rules' },
]

/** What each action reads as after the person's name. */
const VERBS: Record<string, string> = {
  BOOK_CREATED: 'added',
  BOOK_UPDATED: 'edited',
  BOOK_DELETED: 'removed',
  COPIES_ADDED: 'added copies of',
  COPY_STATUS_CHANGED: 'changed the status of',
  LOAN_APPROVED: 'approved a loan of',
  LOAN_REJECTED: 'turned down a request for',
  LOAN_RETURNED: 'checked in',
  LOAN_LOST: 'marked as lost',
  LOAN_RENEWED: 'renewed',
  FINE_PAID: 'recorded a payment for',
  FINE_WAIVED: 'waived a fine for',
  MEMBER_CREATED: 'added',
  MEMBER_UPDATED: 'updated',
  MEMBER_SUSPENDED: 'suspended',
  MEMBER_ACTIVATED: 'reactivated',
  PASSWORD_RESET: 'reset the password for',
  PASSWORD_CHANGED: 'changed their password',
  SIGNED_OUT_EVERYWHERE: 'signed out of their other devices',
  POLICY_UPDATED: 'changed the rules for',
}

const actionName = (a: string) => VERBS[a] ? capitalize(VERBS[a]) : capitalize(a.toLowerCase().replaceAll('_', ' '))

export function ActivityPage() {
  const toast = useToast()
  const [params, setParams] = useSearchParams()
  const num = (k: string) => (params.get(k) ? Number(params.get(k)) : undefined)

  const tab = (params.get('category') as AuditCategory | null) ?? 'ALL'
  const page = num('page') ?? 0
  const filter: AuditSearch = {
    category: tab === 'ALL' ? undefined : tab,
    action: params.get('action') ?? undefined,
    actorId: num('actorId'),
    entityType: params.get('entityType') ?? undefined,
    entityId: num('entityId'),
    from: params.get('from') ?? undefined,
    to: params.get('to') ?? undefined,
  }

  /** Change some filters and go back to page 1. */
  const update = (changes: Record<string, string | number | undefined>) => {
    const next = new URLSearchParams(params)
    for (const [k, v] of Object.entries(changes)) {
      if (v === undefined || v === '') next.delete(k)
      else next.set(k, String(v))
    }
    if (!('page' in changes)) next.delete('page')
    setParams(next)
  }

  const log = useQuery({
    queryKey: ['audit', filter, page],
    queryFn: () => auditApi.search({ ...filter, page }),
    placeholderData: keepPreviousData,
  })
  const actions = useQuery({ queryKey: ['audit', 'actions'], queryFn: auditApi.actions, staleTime: 60_000 })
  const exportCsv = useMutation({
    mutationFn: () => auditApi.exportCsv(filter),
    onError: (e) => toast.show(errorMessage(e), 'error'),
  })

  const chips: { key: string[]; label: ReactNode }[] = []
  if (filter.actorId) chips.push({ key: ['actorId', 'actor'], label: <>By {params.get('actor') ?? `user #${filter.actorId}`}</> })
  if (filter.entityId) chips.push({ key: ['entityType', 'entityId', 'about'], label: <>About {params.get('about') ?? `${filter.entityType} #${filter.entityId}`}</> })
  const filtered = chips.length > 0 || filter.action || filter.from || filter.to

  return (
    <>
      <PageHeader
        title="Activity"
        subtitle="Every change made by librarians and members, newest first"
        actions={<Button variant="secondary" loading={exportCsv.isPending} onClick={() => exportCsv.mutate()}>Export CSV</Button>}
      />

      <Tabs<Tab> value={tab} onChange={(t) => update({ category: t === 'ALL' ? undefined : t })} tabs={TABS} />

      <div className="mb-6 flex flex-wrap items-end gap-3">
        <label className="block w-full sm:w-60">
          <span className="mb-1.5 block text-[13px] font-medium text-ink-600">Action</span>
          <select className={inputClass} value={filter.action ?? ''} onChange={(e) => update({ action: e.target.value })}>
            <option value="">Any action</option>
            {(actions.data ?? []).map((a) => <option key={a} value={a}>{actionName(a)}</option>)}
          </select>
        </label>
        <label className="block w-[calc(50%-6px)] sm:w-44">
          <span className="mb-1.5 block text-[13px] font-medium text-ink-600">From</span>
          <input type="date" className={inputClass} value={filter.from ?? ''} max={filter.to} onChange={(e) => update({ from: e.target.value })} />
        </label>
        <label className="block w-[calc(50%-6px)] sm:w-44">
          <span className="mb-1.5 block text-[13px] font-medium text-ink-600">To</span>
          <input type="date" className={inputClass} value={filter.to ?? ''} min={filter.from} onChange={(e) => update({ to: e.target.value })} />
        </label>
        {chips.map((c) => (
          <span key={c.key.join()} className="inline-flex h-[42px] items-center gap-1.5 rounded-full bg-accent-50 pr-1.5 pl-3.5 text-sm font-medium text-accent-700">
            {c.label}
            <button
              className="flex size-6 items-center justify-center rounded-full text-xs hover:bg-accent-100"
              aria-label="Remove filter"
              onClick={() => update(Object.fromEntries(c.key.map((k) => [k, undefined])))}
            >✕</button>
          </span>
        ))}
        {filtered && (
          <button className="h-[42px] px-2 text-sm font-medium text-accent-600 hover:underline" onClick={() => setParams(tab === 'ALL' ? {} : { category: tab })}>
            Clear filters
          </button>
        )}
      </div>

      {log.isPending ? <PageLoader />
        : log.isError ? <Alert>{errorMessage(log.error)}</Alert>
          : log.data.content.length === 0 ? <EmptyState title="Nothing recorded">{filtered || tab !== 'ALL' ? 'Try widening the filters.' : undefined}</EmptyState>
            : (
              <>
                <div className={`space-y-8 transition-opacity ${log.isPlaceholderData ? 'opacity-60' : ''}`}>
                  {groupByDay(log.data.content).map(([day, entries]) => (
                    <section key={day}>
                      <h2 className="mb-2 px-1 text-[13px] font-semibold tracking-wide text-ink-500 uppercase">{day}</h2>
                      <Card>
                        <ul className="divide-y divide-black/[0.06]">
                          {entries.map((e) => <Entry key={e.id} entry={e} onActor={() => update({ actorId: e.actorId ?? undefined, actor: e.actorName ?? undefined })} />)}
                        </ul>
                      </Card>
                    </section>
                  ))}
                </div>
                <Pagination page={page} totalPages={log.data.totalPages} onChange={(p) => update({ page: p || undefined })} />
              </>
            )}
    </>
  )
}

function Entry({ entry: e, onActor }: { entry: AuditEntry; onActor: () => void }) {
  const self = e.entityType === 'User' && e.actorId !== null && e.actorId === e.entityId
  const verb = VERBS[e.action] ?? e.action.toLowerCase().replaceAll('_', ' ')
  const who = e.actorName ?? (e.actorId ? `User #${e.actorId}` : 'System')
  const time = new Date(e.createdAt).toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit' })

  return (
    <li className="flex gap-3 px-4 py-3.5 sm:px-5">
      <span aria-hidden className={`mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-full text-[13px] font-semibold ${tone(e.action)}`}>
        {initials(who)}
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-[15px] leading-snug text-ink-800">
          {e.actorId
            ? <button className="font-semibold text-ink-900 hover:underline" onClick={onActor} title={`Only show ${who}`}>{who}</button>
            : <span className="font-semibold text-ink-900">{who}</span>}
          {' '}{verb}
          {!self && <>{' '}<Subject entry={e} /></>}
        </p>
        {e.details && <p className="mt-0.5 text-[13px] break-words text-ink-500">{formatDetails(e.details)}</p>}
      </div>
      <time dateTime={e.createdAt} className="shrink-0 pt-0.5 text-[13px] text-ink-500 tabular-nums">{time}</time>
    </li>
  )
}

/** The thing acted on, linked to its page where there is one. */
function Subject({ entry: e }: { entry: AuditEntry }) {
  const label = e.entityLabel ?? `${e.entityType} #${e.entityId ?? '?'}`
  const parts = label.split(' · ')
  const link = (text: string, to: string | null) =>
    to ? <Link to={to} className="font-medium text-accent-600 hover:underline">{text}</Link> : <span className="font-medium text-ink-900">{text}</span>

  if (e.entityType === 'BorrowingPolicy') return link(label, '/admin/rules')
  if (e.entityType === 'User') return link(label, e.memberId ? `/admin/members/${e.memberId}` : null)
  if (e.entityType === 'Book') return link(label, e.bookId && e.action !== 'BOOK_DELETED' ? `/admin/books/${e.bookId}` : null)
  if (parts.length === 2) {
    // "Clean Code · Priya Sharma" (loans, fines) or "SS000001-002 · Clean Code" (copies)
    if (e.entityType === 'BookCopy') return <>{parts[0]} of {link(parts[1], e.bookId ? `/admin/books/${e.bookId}` : null)}</>
    const book = link(parts[0], e.bookId ? `/admin/books/${e.bookId}` : null)
    if (e.memberId !== null && e.memberId === e.actorId) return book   // a member acting on their own loan
    return (
      <>
        {book}
        {' for '}
        {link(parts[1], e.memberId ? `/admin/members/${e.memberId}` : null)}
      </>
    )
  }
  return link(label, null)
}

// ---------- formatting ----------

const DETAIL_KEYS: Record<string, string> = {
  dueDate: 'Due', newDueDate: 'New due date', daysLate: 'Days late', fine: 'Fine', amount: 'Amount', count: 'Copies', isbn: 'ISBN',
}
const MONEY_KEYS = new Set(['fine', 'amount'])

/** "daysLate=2, fine=20.00" → "Days late 2 · Fine ₹20.00". Free text (like a reason) is shown as is. */
function formatDetails(details: string) {
  const pairs = details.split(', ').map((p) => /^(\w+)=(.*)$/.exec(p))
  if (pairs.some((m) => !m || !DETAIL_KEYS[m[1]])) return details
  return pairs.map((m) => {
    const [, key, value] = m!
    const shown = MONEY_KEYS.has(key) && !Number.isNaN(Number(value)) ? formatMoney(Number(value))
      : /^\d{4}-\d{2}-\d{2}$/.test(value) ? formatDate(value) : value
    return `${DETAIL_KEYS[key]} ${shown}`
  }).join(' · ')
}

function groupByDay(entries: AuditEntry[]) {
  const groups: [string, AuditEntry[]][] = []
  for (const e of entries) {
    const label = dayLabel(new Date(e.createdAt))
    const last = groups.at(-1)
    if (last && last[0] === label) last[1].push(e)
    else groups.push([label, [e]])
  }
  return groups
}

function dayLabel(d: Date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const that = new Date(d)
  that.setHours(0, 0, 0, 0)
  const diff = Math.round((today.getTime() - that.getTime()) / 86_400_000)
  if (diff === 0) return 'Today'
  if (diff === 1) return 'Yesterday'
  return d.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short', year: d.getFullYear() === today.getFullYear() ? undefined : 'numeric' })
}

function initials(name: string) {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0]!.toUpperCase()).join('') || '?'
}

/** Colour by kind of action: removals and suspensions stand out, money is green, the rest stay quiet. */
function tone(action: string) {
  if (/DELETED|SUSPENDED|LOST|REJECTED|RESET/.test(action)) return 'bg-danger-50 text-danger-600'
  if (/FINE_/.test(action)) return 'bg-success-50 text-success-700'
  if (/POLICY/.test(action)) return 'bg-accent-50 text-accent-700'
  return 'bg-canvas-200 text-ink-700'
}

function capitalize(s: string) {
  return s.charAt(0).toUpperCase() + s.slice(1)
}

