import { useEffect, useState } from 'react'

/**
 * The demo API runs on a free host that sleeps when nobody uses it and takes 2–3 minutes to wake.
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
      while (!cancelled && Date.now() - started < 240_000) {
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
      <div role="status" className="mb-6 flex items-center gap-3 rounded-r-md border-l-4 border-warn-500 bg-warn-50 px-4 py-3 text-sm text-warn-800">
        <span className="inline-block size-4 animate-spin rounded-full border-2 border-current border-r-transparent" />
        Waking up the server. The free demo host sleeps when idle, so this can take 2–3 minutes. You can log in once this message disappears.
      </div>
    )
  }
  if (state === 'down') {
    return (
      <div role="alert" className="mb-6 rounded-r-md border-l-4 border-danger-500 bg-danger-50 px-4 py-3 text-sm text-danger-700">
        The server isn't responding right now. Please try again in a minute.
      </div>
    )
  }
  return null
}
