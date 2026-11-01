import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { Alert, Button, Field } from '../components/ui'
import { errorMessage, inputClass } from '../lib/ui-helpers'
import { AuthShell } from './AuthShell'

const DEMO = {
  member: { email: 'member@shelfspace.dev', password: 'Member@12345' },
  admin: { email: 'admin@shelfspace.dev', password: 'Admin@12345' },
}

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/'

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={from} replace />

  async function signIn(e: string, p: string) {
    setBusy(true)
    setError(null)
    try {
      await login(e, p)
      navigate(from, { replace: true })
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    void signIn(email, password)
  }

  return (
    <AuthShell title="Sign in to ShelfSpace" subtitle={<>New here? <Link to="/register" className="font-semibold text-accent-600">Create an account</Link></>}>
      <form onSubmit={onSubmit} className="space-y-4">
        {error && <Alert>{error}</Alert>}
        <Field label="Email">
          <input className={inputClass} type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </Field>
        <Field label="Password">
          <input className={inputClass} type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
        </Field>
        <Button type="submit" loading={busy} className="w-full py-2.5 text-[15px]">Sign In</Button>
      </form>

      <div className="mt-6 border-t border-ink-200 pt-5">
        <p className="mb-3 text-center text-[13px] text-ink-500">Just looking around? Try a demo account</p>
        <div className="grid grid-cols-2 gap-2">
          <Button variant="secondary" disabled={busy} onClick={() => signIn(DEMO.member.email, DEMO.member.password)}>Member</Button>
          <Button variant="secondary" disabled={busy} onClick={() => signIn(DEMO.admin.email, DEMO.admin.password)}>Librarian</Button>
        </div>
      </div>
    </AuthShell>
  )
}
