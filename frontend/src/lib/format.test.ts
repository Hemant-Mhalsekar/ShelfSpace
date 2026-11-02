import { afterEach, describe, expect, it, vi } from 'vitest'
import { daysUntil, formatDate, formatMoney, timeAgo } from './format'

describe('format', () => {
  afterEach(() => { vi.useRealTimers() })

  it('formats rupees', () => {
    expect(formatMoney(30)).toBe('₹30.00')
    expect(formatMoney(null)).toBe('₹0.00')
  })

  it('shows a plain date without shifting it a day', () => {
    expect(formatDate('2026-11-15')).toBe('15 Nov 2026')
    expect(formatDate(null)).toBe('—')
  })

  it('counts whole days to a due date', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-11-03T18:30:00'))
    expect(daysUntil('2026-11-03')).toBe(0)
    expect(daysUntil('2026-11-05')).toBe(2)
    expect(daysUntil('2026-11-01')).toBe(-2)
  })

  it('says how long ago something happened', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-11-03T12:00:00'))
    expect(timeAgo('2026-11-03T11:59:30')).toBe('just now')
    expect(timeAgo('2026-11-03T11:15:00')).toBe('45 min ago')
    expect(timeAgo('2026-11-02T11:00:00')).toBe('yesterday')
  })
})
