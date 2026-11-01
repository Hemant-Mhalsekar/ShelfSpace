import { useEffect, useState, type ReactNode } from 'react'
import { Button } from './ui'

/**
 * A button that needs a second click to confirm (for destructive actions).
 * First click shows "Confirm?"; it resets after a few seconds if not confirmed.
 */
export function ConfirmButton({ onConfirm, children, confirmLabel = 'Click again to confirm', loading, variant = 'danger' }: {
  onConfirm: () => void
  children: ReactNode
  confirmLabel?: string
  loading?: boolean
  variant?: 'danger' | 'secondary'
}) {
  const [armed, setArmed] = useState(false)

  useEffect(() => {
    if (!armed) return
    const t = setTimeout(() => setArmed(false), 4000)
    return () => clearTimeout(t)
  }, [armed])

  return (
    <Button
      variant={armed ? 'primary' : variant}
      className={armed ? 'bg-rose-600 hover:bg-rose-500' : ''}
      loading={loading}
      onClick={() => {
        if (armed) {
          setArmed(false)
          onConfirm()
        } else {
          setArmed(true)
        }
      }}
    >
      {armed ? confirmLabel : children}
    </Button>
  )
}
