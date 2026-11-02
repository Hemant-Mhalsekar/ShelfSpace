import { passwordChecks, passwordStrength, type Person } from '../lib/validation'

const STRENGTH = {
  weak: { label: 'Too weak', width: 'w-1/3', color: 'bg-danger-500' },
  good: { label: 'Good', width: 'w-2/3', color: 'bg-accent-500' },
  strong: { label: 'Strong', width: 'w-full', color: 'bg-success-500' },
}

/** Live checklist and strength bar under a new-password field. */
export function PasswordChecklist({ password, person }: { password: string; person?: Person }) {
  if (!password) {
    return <p className="mt-1.5 text-xs text-ink-500">At least 8 characters, with upper and lowercase letters and a number.</p>
  }
  const strength = STRENGTH[passwordStrength(password, person)]
  return (
    <div className="mt-2" aria-live="polite">
      <div className="flex items-center gap-3">
        <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-canvas-200">
          <div className={`h-full rounded-full transition-all duration-300 ${strength.width} ${strength.color}`} />
        </div>
        <span className="w-16 text-right text-xs font-medium text-ink-600">{strength.label}</span>
      </div>
      <ul className="mt-2 grid gap-1 text-xs sm:grid-cols-2">
        {passwordChecks(password, person).map((c) => (
          <li key={c.label} className={`flex items-center gap-1.5 ${c.ok ? 'text-success-700' : 'text-ink-500'}`}>
            <span aria-hidden className={`flex size-3.5 items-center justify-center rounded-full text-[9px] ${c.ok ? 'bg-success-500 text-white' : 'border border-ink-300'}`}>
              {c.ok ? '✓' : ''}
            </span>
            {c.label}
            <span className="sr-only">{c.ok ? '(done)' : '(not yet)'}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
