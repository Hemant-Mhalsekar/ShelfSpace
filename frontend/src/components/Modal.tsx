import { useEffect, type ReactNode } from 'react'

export function Modal({ title, open, onClose, children, wide = false }: {
  title: string
  open: boolean
  onClose: () => void
  children: ReactNode
  wide?: boolean
}) {
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open) return null
  return (
    <div className="fixed inset-0 z-40 flex animate-enter items-start justify-center overflow-y-auto bg-black/30 p-4 backdrop-blur-[2px] sm:items-center" onMouseDown={onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={`w-full rounded-3xl bg-canvas-50 shadow-[0_24px_64px_-12px_rgba(0,0,0,0.3)] ${wide ? 'max-w-2xl' : 'max-w-md'}`}
        onMouseDown={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-6 pt-5 pb-2">
          <h2 className="text-[17px] font-semibold">{title}</h2>
          <button onClick={onClose} className="flex size-7 items-center justify-center rounded-full bg-canvas-200 text-xs text-ink-600 hover:bg-canvas-300" aria-label="Close">✕</button>
        </div>
        <div className="px-6 pt-2 pb-6">{children}</div>
      </div>
    </div>
  )
}
