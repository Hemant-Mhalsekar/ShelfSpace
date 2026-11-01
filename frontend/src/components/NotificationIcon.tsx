/** One consistent outline icon per notification type (replaces emoji). Paths are 24x24 outline icons. */
const PATHS: Record<string, { d: string; tone: string }> = {
  LOAN_APPROVED: { d: 'm4.5 12.75 6 6 9-13.5', tone: 'bg-accent-50 text-accent-700' },
  LOAN_REJECTED: { d: 'M6 18 18 6M6 6l12 12', tone: 'bg-danger-50 text-danger-600' },
  DUE_SOON: { d: 'M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z', tone: 'bg-warn-50 text-warn-700' },
  OVERDUE: {
    d: 'M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z',
    tone: 'bg-danger-50 text-danger-600',
  },
  RESERVATION_READY: {
    d: 'M12 6.042A8.967 8.967 0 0 0 6 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 0 1 6 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 0 1 6-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0 0 18 18a8.967 8.967 0 0 0-6 2.292m0-14.25v14.25',
    tone: 'bg-accent-50 text-accent-700',
  },
  HOLD_EXPIRED: { d: 'M6.75 3h10.5M6.75 21h10.5M7.5 3c0 4.5 4.5 6 4.5 9s-4.5 4.5-4.5 9M16.5 3c0 4.5-4.5 6-4.5 9s4.5 4.5 4.5 9', tone: 'bg-canvas-200 text-ink-600' },
  WISHLIST_AVAILABLE: {
    d: 'M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z',
    tone: 'bg-danger-50 text-danger-500',
  },
}

const FALLBACK = { d: 'M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0', tone: 'bg-canvas-200 text-ink-600' }

export function NotificationIcon({ type }: { type: string }) {
  const { d, tone } = PATHS[type] ?? FALLBACK
  return (
    <span className={`flex size-9 shrink-0 items-center justify-center rounded-full ${tone}`} aria-hidden>
      <svg viewBox="0 0 24 24" className="size-[18px]" fill="none" stroke="currentColor" strokeWidth={1.7}>
        <path strokeLinecap="round" strokeLinejoin="round" d={d} />
      </svg>
    </span>
  )
}
