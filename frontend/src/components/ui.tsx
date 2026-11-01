import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { Link } from 'react-router'

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost'

/** Apple-style pill buttons: filled accent, tinted grey, or plain text. */
const variants: Record<Variant, string> = {
  primary: 'bg-accent-600 text-white hover:bg-accent-700 active:bg-accent-800',
  secondary: 'bg-canvas-200 text-ink-900 hover:bg-canvas-300',
  danger: 'bg-danger-50 text-danger-600 hover:bg-danger-100',
  ghost: 'text-accent-600 hover:bg-accent-50',
}

export function Button({
  variant = 'primary', loading = false, className = '', children, disabled, ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; loading?: boolean }) {
  return (
    <button
      {...rest}
      disabled={disabled || loading}
      className={`inline-flex items-center justify-center gap-2 rounded-full px-4 py-2 text-sm font-semibold
        transition-[background-color,transform] duration-150 active:scale-[0.97] disabled:cursor-not-allowed disabled:opacity-40
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
      className={`inline-flex items-center justify-center rounded-full px-4 py-2 text-sm font-semibold transition-colors duration-150 ${variants.primary}`}
    >
      {children}
    </Link>
  )
}

/** Grouped-list surface: white, rounded, separated from the grey page by a faint hairline. */
export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`rounded-2xl bg-canvas-50 ring-1 ring-black/[0.06] ${className}`}>{children}</div>
}

export function Spinner({ small = false }: { small?: boolean }) {
  return (
    <span
      role="status"
      aria-label="Loading"
      className={`inline-block animate-spin rounded-full border-2 border-current border-r-transparent
        ${small ? 'size-4' : 'size-6 text-ink-400'}`}
    />
  )
}

export function PageLoader() {
  return <div className="flex justify-center py-24"><Spinner /></div>
}

export function Alert({ kind = 'error', children }: { kind?: 'error' | 'success' | 'info'; children: ReactNode }) {
  const styles = {
    error: 'bg-danger-50 text-danger-700',
    success: 'bg-success-50 text-success-700',
    info: 'bg-accent-50 text-accent-700',
  }[kind]
  return <div role={kind === 'error' ? 'alert' : 'status'} className={`rounded-xl px-4 py-3 text-sm ${styles}`}>{children}</div>
}

export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="rounded-2xl bg-canvas-100 px-6 py-14 text-center">
      <p className="text-lg font-semibold text-ink-900">{title}</p>
      {children && <div className="mt-1.5 text-sm text-ink-500">{children}</div>}
    </div>
  )
}

const badgeColors = {
  green: 'bg-success-50 text-success-700',
  amber: 'bg-warn-50 text-warn-700',
  red: 'bg-danger-50 text-danger-600',
  blue: 'bg-accent-50 text-accent-700',
  gray: 'bg-canvas-200 text-ink-600',
} as const

export type BadgeColor = keyof typeof badgeColors

export function Badge({ color = 'gray', children }: { color?: BadgeColor; children: ReactNode }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${badgeColors[color]}`}>
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

const statusLabel: Record<string, string> = {
  ISSUED: 'Checked out', REQUESTED: 'Requested', ON_HOLD: 'On hold', WAITING: 'In queue', READY: 'Ready',
}

export function StatusBadge({ status }: { status: string }) {
  const label = statusLabel[status] ?? (status.charAt(0) + status.slice(1).toLowerCase()).replace('_', ' ')
  return <Badge color={statusColor[status] ?? 'gray'}>{label}</Badge>
}

export function Field({
  label, error, children,
}: { label: string; error?: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-[13px] font-medium text-ink-600">{label}</span>
      {children}
      {error && <span className="mt-1 block text-xs text-danger-600">{error}</span>}
    </label>
  )
}

/** Large title, like iOS / macOS. */
export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="font-display text-[34px] leading-tight font-bold tracking-tight text-ink-900">{title}</h1>
        {subtitle && <p className="mt-1 text-[15px] text-ink-500">{subtitle}</p>}
      </div>
      {actions}
    </div>
  )
}

export function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (p: number) => void }) {
  if (totalPages <= 1) return null
  return (
    <nav className="mt-8 flex items-center justify-center gap-4 text-sm" aria-label="Pagination">
      <Button variant="secondary" disabled={page === 0} onClick={() => onChange(page - 1)}>Previous</Button>
      <span className="text-ink-500 tabular-nums">{page + 1} of {totalPages}</span>
      <Button variant="secondary" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Next</Button>
    </nav>
  )
}

/** Widget-style figures. */
export function StatStrip({ children, columns = 4 }: { children: ReactNode; columns?: 3 | 4 }) {
  return (
    <div className={`grid grid-cols-2 gap-3 ${columns === 4 ? 'lg:grid-cols-4' : 'lg:grid-cols-3'}`}>
      {children}
    </div>
  )
}

export function StatItem({ label, value, hint, tone, to }: {
  label: string; value: ReactNode; hint?: ReactNode; tone?: 'warn' | 'danger'; to?: string
}) {
  const color = tone === 'danger' ? 'text-danger-600' : tone === 'warn' ? 'text-accent-600' : 'text-ink-900'
  const body = (
    <>
      <p className="text-[13px] font-medium text-ink-500">{label}</p>
      <p className={`mt-1 font-display text-3xl font-bold tracking-tight tabular-nums ${color}`}>{value}</p>
      {hint && <p className="mt-1 text-xs text-ink-500">{hint}</p>}
    </>
  )
  const box = 'block rounded-2xl bg-canvas-100 p-4'
  return to
    ? <Link to={to} className={`${box} transition-colors duration-150 hover:bg-canvas-200`}>{body}</Link>
    : <div className={box}>{body}</div>
}

export function SectionTitle({ children, aside }: { children: ReactNode; aside?: ReactNode }) {
  return (
    <div className="mb-3 flex items-baseline justify-between gap-4">
      <h2 className="font-display text-[22px] font-bold tracking-tight text-ink-900">{children}</h2>
      {aside}
    </div>
  )
}
