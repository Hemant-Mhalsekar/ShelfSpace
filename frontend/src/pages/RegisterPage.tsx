import { useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { Alert, Button, Field } from '../components/ui'
import { errorMessage, inputClass } from '../lib/ui-helpers'
import { AuthShell } from './AuthShell'

export function RegisterPage() {
  const { user, register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', phone: '', password: '' })
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/books" replace />

  const set = (key: keyof typeof form) => (e: ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }))

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setFieldErrors({})
    try {
      await register({ ...form, phone: form.phone || undefined })
      navigate('/books', { replace: true })
    } catch (err) {
      if (err instanceof ApiError) setFieldErrors(err.fieldErrors)
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthShell title="Create your account" subtitle={<>Already a member? <Link to="/login" className="font-semibold text-accent-600">Sign in</Link></>}>
      <form onSubmit={onSubmit} className="space-y-4">
        {error && <Alert>{error}</Alert>}
        <Field label="Full name" error={fieldErrors.name}>
          <input className={inputClass} required value={form.name} onChange={set('name')} autoComplete="name" />
        </Field>
        <Field label="Email" error={fieldErrors.email}>
          <input className={inputClass} type="email" required value={form.email} onChange={set('email')} autoComplete="email" />
        </Field>
        <Field label="Phone (optional)" error={fieldErrors.phone}>
          <input className={inputClass} type="tel" value={form.phone} onChange={set('phone')} placeholder="9876543210" autoComplete="tel" />
        </Field>
        <Field label="Password" error={fieldErrors.password}>
          <input className={inputClass} type="password" required minLength={8} value={form.password} onChange={set('password')} autoComplete="new-password" />
          <span className="mt-1 block text-xs text-ink-500">At least 8 characters, with a letter and a number.</span>
        </Field>
        <Button type="submit" loading={busy} className="w-full py-2.5 text-[15px]">Create Account</Button>
      </form>
    </AuthShell>
  )
}
