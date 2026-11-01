import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { Link } from 'react-router'

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost'

const variants: Record<Variant, string> = {
  primary: 'bg-indigo-600 text-white hover:bg-indigo-500 focus-visible:outline-indigo-600',
  secondary: 'bg-white text-slate-700 ring-1 ring-inset ring-slate-300 hover:bg-slate-50',
  danger: 'bg-white text-rose-600 ring-1 ring-inset ring-rose-200 hover:bg-rose-50',
  ghost: 'text-slate-600 hover:bg-slate-100',
}

export function Button({
  variant = 'primary', loading = false, className = '', children, disabled, ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; loading?: boolean }) {
  return (
    <button
      {...rest}
      disabled={disabled || loading}
      className={`inline-flex items-center justify-center gap-2 rounded-lg px-3.5 py-2 text-sm font-semibold shadow-xs
        transition focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-50
        ${variants[variant]} ${className}`}
    >
      {loading && <Spinner small />}
      {children}
    </button>
  )
}

/** A link that looks like a primary button (avoids nesting a <button> inside an <a>). */
export function LinkButton({ to, children }: { to: string; children: ReactNode }) {
  return (
    <Link
      to={to}
      className={`inline-flex items-center justify-center rounded-lg px-3.5 py-2 text-sm font-semibold shadow-xs transition ${variants.primary}`}
    >
      {children}
    </Link>
  )
}

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`rounded-xl border border-slate-200 bg-white shadow-xs ${className}`}>{children}</div>
}

export function Spinner({ small = false }: { small?: boolean }) {
  return (
    <span
      role="status"
      aria-label="Loading"
      className={`inline-block animate-spin rounded-full border-2 border-current border-r-transparent
        ${small ? 'size-4' : 'size-6 text-indigo-600'}`}
    />
  )
}

export function PageLoader() {
  return <div className="flex justify-center py-20"><Spinner /></div>
}

export function Alert({ kind = 'error', children }: { kind?: 'error' | 'success' | 'info'; children: ReactNode }) {
  const styles = {
    error: 'bg-rose-50 text-rose-700 ring-rose-200',
    success: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
    info: 'bg-indigo-50 text-indigo-700 ring-indigo-200',
  }[kind]
  return <div role={kind === 'error' ? 'alert' : 'status'} className={`rounded-lg px-4 py-3 text-sm ring-1 ring-inset ${styles}`}>{children}</div>
}

export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="rounded-xl border border-dashed border-slate-300 px-6 py-12 text-center">
      <p className="font-medium text-slate-700">{title}</p>
      {children && <div className="mt-1 text-sm text-slate-500">{children}</div>}
    </div>
  )
}

const badgeColors = {
  green: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  amber: 'bg-amber-50 text-amber-800 ring-amber-600/20',
  red: 'bg-rose-50 text-rose-700 ring-rose-600/20',
  blue: 'bg-indigo-50 text-indigo-700 ring-indigo-600/20',
  gray: 'bg-slate-100 text-slate-600 ring-slate-500/20',
} as const

export type BadgeColor = keyof typeof badgeColors

export function Badge({ color = 'gray', children }: { color?: BadgeColor; children: ReactNode }) {
  return (
    <span className={`inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${badgeColors[color]}`}>
      {children}
    </span>
  )
}

const statusColor: Record<string, BadgeColor> = {
  REQUESTED: 'amber', APPROVED: 'blue', ISSUED: 'blue', OVERDUE: 'red', RETURNED: 'green',
  REJECTED: 'gray', CANCELLED: 'gray', LOST: 'red',
  WAITING: 'amber', READY: 'green', FULFILLED: 'gray', EXPIRED: 'gray',
  UNPAID: 'red', PAID: 'green', WAIVED: 'gray',
  AVAILABLE: 'green', BORROWED: 'blue', ON_HOLD: 'amber', DAMAGED: 'amber',
  ACTIVE: 'green', SUSPENDED: 'red',
}

export function StatusBadge({ status }: { status: string }) {
  const label = (status.charAt(0) + status.slice(1).toLowerCase()).replace('_', ' ')
  return <Badge color={statusColor[status] ?? 'gray'}>{label}</Badge>
}

export function Field({
  label, error, children,
}: { label: string; error?: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-medium text-slate-700">{label}</span>
      {children}
      {error && <span className="mt-1 block text-xs text-rose-600">{error}</span>}
    </label>
  )
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
      </div>
      {actions}
    </div>
  )
}

export function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (p: number) => void }) {
  if (totalPages <= 1) return null
  return (
    <nav className="mt-6 flex items-center justify-between text-sm" aria-label="Pagination">
      <Button variant="secondary" disabled={page === 0} onClick={() => onChange(page - 1)}>Previous</Button>
      <span className="text-slate-500">Page {page + 1} of {totalPages}</span>
      <Button variant="secondary" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Next</Button>
    </nav>
  )
}
