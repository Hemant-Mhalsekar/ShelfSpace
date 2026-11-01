import { useEffect, useState } from 'react'

/**
 * The demo API runs on a free host that sleeps when nobody uses it and takes about a minute to wake.
 * This pings the health endpoint as soon as the login page opens (which also starts the wake-up)
 * and explains the wait if it's slow.
 */
export function ServerWakeBanner() {
  const [state, setState] = useState<'checking' | 'slow' | 'up' | 'down'>('checking')

  useEffect(() => {
    let cancelled = false
    const slowTimer = setTimeout(() => !cancelled && setState((s) => (s === 'checking' ? 'slow' : s)), 2500)
    const started = Date.now()

    async function ping() {
      while (!cancelled && Date.now() - started < 120_000) {
        try {
          const res = await fetch('/actuator/health', { cache: 'no-store' })
          if (res.ok) {
            if (!cancelled) setState('up')
            return
          }
        } catch {
          /* still waking up */
        }
        await new Promise((r) => setTimeout(r, 4000))
      }
      if (!cancelled) setState('down')
    }
    void ping()
    return () => {
      cancelled = true
      clearTimeout(slowTimer)
    }
  }, [])

  if (state === 'slow') {
    return (
      <div role="status" className="mb-6 flex items-center gap-3 rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-800 ring-1 ring-amber-200 ring-inset">
        <span className="inline-block size-4 animate-spin rounded-full border-2 border-current border-r-transparent" />
        Waking up the server. The free demo host sleeps when idle, so this can take up to a minute.
      </div>
    )
  }
  if (state === 'down') {
    return (
      <div role="alert" className="mb-6 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200 ring-inset">
        The server isn't responding right now. Please try again in a minute.
      </div>
    )
  }
  return null
}
