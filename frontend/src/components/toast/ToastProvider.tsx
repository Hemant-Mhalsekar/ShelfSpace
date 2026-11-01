import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { ToastContext, type ToastKind } from './context'

interface Toast {
  id: number
  message: string
  kind: ToastKind
}

let nextId = 1

/** Small pop-up messages in the bottom-right corner that disappear after a few seconds. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const dismiss = useCallback((id: number) => setToasts((t) => t.filter((x) => x.id !== id)), [])

  const show = useCallback((message: string, kind: ToastKind = 'success') => {
    const id = nextId++
    setToasts((t) => [...t.slice(-3), { id, message, kind }])
    setTimeout(() => dismiss(id), kind === 'error' ? 6000 : 3500)
  }, [dismiss])

  const api = useMemo(() => ({ show }), [show])

  const colors: Record<ToastKind, string> = {
    success: 'bg-emerald-600',
    error: 'bg-rose-600',
    info: 'bg-slate-800',
  }

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="pointer-events-none fixed right-4 bottom-4 z-50 flex w-full max-w-sm flex-col gap-2" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`pointer-events-auto flex items-start gap-3 rounded-lg px-4 py-3 text-sm text-white shadow-lg ${colors[t.kind]}`}>
            <span className="flex-1">{t.message}</span>
            <button onClick={() => dismiss(t.id)} className="opacity-70 hover:opacity-100" aria-label="Dismiss">✕</button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
