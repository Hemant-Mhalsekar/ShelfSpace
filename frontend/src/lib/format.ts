const rupees = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' })
const day = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
const dayTime = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' })

export const formatMoney = (n: number | null | undefined) => rupees.format(n ?? 0)

/** "2026-11-15" (a date without time) is parsed as local time, not UTC, so it never shifts a day. */
export function formatDate(value: string | null | undefined) {
  if (!value) return '—'
  const d = /^\d{4}-\d{2}-\d{2}$/.test(value) ? new Date(`${value}T00:00:00`) : new Date(value)
  return day.format(d)
}

export function formatDateTime(value: string | null | undefined) {
  return value ? dayTime.format(new Date(value)) : '—'
}

/** Whole days from today until a yyyy-mm-dd date (negative if in the past). */
export function daysUntil(date: string | null | undefined) {
  if (!date) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const target = new Date(`${date}T00:00:00`)
  return Math.round((target.getTime() - today.getTime()) / 86_400_000)
}

export function timeAgo(value: string) {
  const seconds = Math.round((Date.now() - new Date(value).getTime()) / 1000)
  if (seconds < 60) return 'just now'
  const minutes = Math.round(seconds / 60)
  if (minutes < 60) return `${minutes} min ago`
  const hours = Math.round(minutes / 60)
  if (hours < 24) return `${hours} h ago`
  const days = Math.round(hours / 24)
  return days === 1 ? 'yesterday' : `${days} days ago`
}
