import { useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { PasswordChecklist } from '../components/PasswordChecklist'
import { Alert, Button, Field } from '../components/ui'
import { errorMessage, inputClass } from '../lib/ui-helpers'
import { useFieldChecks } from '../lib/useFieldChecks'
import { emailProblem, nameProblem, passwordProblem, phoneProblem } from '../lib/validation'
import { AuthShell } from './AuthShell'

export function RegisterPage() {
  const { user, register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', phone: '', password: '' })
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const checks = useFieldChecks({
    name: nameProblem(form.name),
    email: emailProblem(form.email),
    phone: phoneProblem(form.phone),
    password: passwordProblem(form.password, form),
  })

  if (user) return <Navigate to="/books" replace />

  const set = (key: keyof typeof form) => (e: ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }))

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (!checks.ready()) return
    setBusy(true)
    try {
      await register({ ...form, name: form.name.trim(), email: form.email.trim(), phone: form.phone.trim() || undefined })
      navigate('/books', { replace: true })
    } catch (err) {
      if (err instanceof ApiError) checks.setServerErrors(err.fieldErrors)
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthShell title="Create your account" subtitle={<>Already a member? <Link to="/login" className="font-semibold text-accent-600">Sign in</Link></>}>
      <form onSubmit={onSubmit} noValidate className="space-y-4">
        {error && <Alert>{error}</Alert>}
        <Field label="Full name" error={checks.error('name')}>
          <input className={inputClass} required maxLength={100} value={form.name} onChange={set('name')} autoComplete="name" {...checks.field('name')} />
        </Field>
        <Field label="Email" error={checks.error('email')}>
          <input className={inputClass} type="email" required maxLength={150} value={form.email} onChange={set('email')} autoComplete="email" {...checks.field('email')} />
        </Field>
        <Field label="Phone (optional)" error={checks.error('phone')}>
          <input className={inputClass} type="tel" value={form.phone} onChange={set('phone')} placeholder="9876543210" autoComplete="tel" {...checks.field('phone')} />
        </Field>
        <Field label="Password" error={checks.error('password')}>
          <input className={inputClass} type="password" required maxLength={72} value={form.password} onChange={set('password')} autoComplete="new-password" {...checks.field('password')} />
          <PasswordChecklist password={form.password} person={form} />
        </Field>
        <Button type="submit" loading={busy} className="w-full py-2.5 text-[15px]">Create Account</Button>
      </form>
    </AuthShell>
  )
}
