import { useState } from 'react'
import { serverMessage } from './validation'

/**
 * Shows a field's problem once the person has left the field (or tried to submit), then updates it live as they type.
 * Server errors for a field show until the field is edited.
 */
export function useFieldChecks<K extends string>(problems: Record<K, string | null>) {
  const [touched, setTouched] = useState<Partial<Record<K, boolean>>>({})
  const [submitted, setSubmitted] = useState(false)
  const [server, setServer] = useState<Record<string, string>>({})

  return {
    /** Message to show under the field, if any. */
    error: (k: K) => ((touched[k] || submitted) ? problems[k] : null) ?? serverMessage(server[k]),
    /** Spread onto the input: marks the field as visited and clears its server error on change. */
    field: (k: K) => ({
      onBlur: () => setTouched((t) => ({ ...t, [k]: true })),
      onInput: () => setServer((s) => (k in s ? Object.fromEntries(Object.entries(s).filter(([key]) => key !== k)) : s)),
      'aria-invalid': Boolean(((touched[k] || submitted) && problems[k]) || server[k]) || undefined,
    }),
    /** Call on submit. Shows every problem; returns true when the form can be sent. */
    ready: () => {
      setSubmitted(true)
      return Object.values<string | null>(problems).every((p) => !p)
    },
    setServerErrors: setServer,
    reset: () => { setTouched({}); setSubmitted(false); setServer({}) },
  }
}
