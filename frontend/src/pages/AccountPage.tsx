import { useMutation, useQuery } from '@tanstack/react-query'
import { useState, type FormEvent, type ReactNode } from 'react'
import { ApiError } from '../api/client'
import { accountApi, policiesApi } from '../api/endpoints'
import type { Policy, User } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { useToast } from '../components/toast/useToast'
import { Alert, Button, Card, Field, PageHeader, SectionTitle, Spinner } from '../components/ui'
import { formatDate, formatMoney } from '../lib/format'
import { tierName } from '../lib/tiers'
import { errorMessage, inputClass } from '../lib/ui-helpers'

export function AccountPage() {
  const { user } = useAuth()
  if (!user) return null
  const isAdmin = user.role === 'ADMIN'

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader
        title="Account"
        subtitle={`${isAdmin ? 'Librarian' : `${tierName(user.membershipTier)} member`} since ${formatDate(user.createdAt)}`}
      />
      <div className="space-y-10">
        <section>
          <SectionTitle>Profile</SectionTitle>
          <Card className="p-5 sm:p-6">
            <ProfileForm key={`${user.name}-${user.phone}`} user={user} />
          </Card>
        </section>

        <section>
          <SectionTitle>Password</SectionTitle>
          <Card className="p-5 sm:p-6">
            <PasswordForm />
          </Card>
        </section>

        {!isAdmin && (
          <section>
            <SectionTitle>Your Membership</SectionTitle>
            <MembershipLimits user={user} />
          </section>
        )}
      </div>
    </div>
  )
}

function ProfileForm({ user }: { user: User }) {
  const { setUser } = useAuth()
  const toast = useToast()
  const [form, setForm] = useState({ name: user.name, phone: user.phone ?? '' })
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const dirty = form.name.trim() !== user.name || (form.phone.trim() || null) !== user.phone

  const save = useMutation({
    mutationFn: () => accountApi.updateProfile({ name: form.name, phone: form.phone.trim() || null }),
    onSuccess: (u) => { setUser(u); toast.show('Profile updated') },
    onError: (e) => {
      if (e instanceof ApiError) setFieldErrors(e.fieldErrors)
      toast.show(errorMessage(e), 'error')
    },
  })

  function submit(e: FormEvent) {
    e.preventDefault()
    setFieldErrors({})
    save.mutate()
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <Field label="Full name" error={fieldErrors.name}>
        <input className={inputClass} required maxLength={100} value={form.name} autoComplete="name"
          onChange={(e) => setForm({ ...form, name: e.target.value })} />
      </Field>
      <Field label="Email">
        <input className={`${inputClass} cursor-not-allowed text-ink-500 hover:bg-canvas-100`} value={user.email} readOnly aria-readonly />
        <span className="mt-1 block text-xs text-ink-500">Your email is your sign-in. Ask a librarian if it needs to change.</span>
      </Field>
      <Field label="Phone (optional)" error={fieldErrors.phone}>
        <input className={inputClass} type="tel" value={form.phone} placeholder="9876543210" autoComplete="tel"
          onChange={(e) => setForm({ ...form, phone: e.target.value })} />
      </Field>
      <div className="flex justify-end">
        <Button type="submit" disabled={!dirty} loading={save.isPending}>Save Changes</Button>
      </div>
    </form>
  )
}

const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d).{8,72}$/

function PasswordForm() {
  const toast = useToast()
  const empty = { current: '', next: '', confirm: '' }
  const [form, setForm] = useState(empty)
  const [error, setError] = useState<string | null>(null)

  const change = useMutation({
    mutationFn: () => accountApi.changePassword(form.current, form.next),
    onSuccess: () => { setForm(empty); toast.show('Password changed') },
    onError: (e) => setError(e instanceof ApiError && Object.keys(e.fieldErrors).length
      ? Object.values(e.fieldErrors)[0]
      : errorMessage(e)),
  })

  const mismatch = form.confirm.length > 0 && form.confirm !== form.next
  const weak = form.next.length > 0 && !PASSWORD_RULE.test(form.next)

  function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (weak || mismatch) return
    if (form.next === form.current) {
      setError('Choose a password different from your current one')
      return
    }
    change.mutate()
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      {error && <Alert>{error}</Alert>}
      <Field label="Current password">
        <input className={inputClass} type="password" required value={form.current} autoComplete="current-password"
          onChange={(e) => setForm({ ...form, current: e.target.value })} />
      </Field>
      <Field label="New password" error={weak ? 'At least 8 characters, with a letter and a number' : undefined}>
        <input className={inputClass} type="password" required value={form.next} autoComplete="new-password"
          onChange={(e) => setForm({ ...form, next: e.target.value })} />
        {!weak && <span className="mt-1 block text-xs text-ink-500">At least 8 characters, with a letter and a number.</span>}
      </Field>
      <Field label="Confirm new password" error={mismatch ? "Passwords don't match" : undefined}>
        <input className={inputClass} type="password" required value={form.confirm} autoComplete="new-password"
          onChange={(e) => setForm({ ...form, confirm: e.target.value })} />
      </Field>
      <div className="flex justify-end">
        <Button type="submit" loading={change.isPending}
          disabled={!form.current || !form.next || !form.confirm || weak || mismatch}>
          Change Password
        </Button>
      </div>
    </form>
  )
}

function MembershipLimits({ user }: { user: User }) {
  const policy = useQuery({
    queryKey: ['policies', user.membershipTier],
    queryFn: () => policiesApi.get(user.membershipTier),
  })

  if (policy.isPending) return <div className="flex justify-center py-10"><Spinner /></div>
  if (policy.isError) return <Alert>{errorMessage(policy.error)}</Alert>
  const p: Policy = policy.data
  const plural = (n: number, word: string) => `${n} ${word}${n === 1 ? '' : 's'}`

  return (
    <>
      <Card>
        <ul className="divide-y divide-black/[0.06]">
          <Row label="Books at a time">{plural(p.maxBooks, 'book')}</Row>
          <Row label="Loan period">{plural(p.loanDays, 'day')}</Row>
          <Row label="Renewals">{p.maxRenewals === 0 ? 'None' : `Up to ${plural(p.maxRenewals, 'time')} per loan`}</Row>
          <Row label="Reservations">{p.maxReservations === 0 ? 'None' : `Up to ${p.maxReservations} at once`}</Row>
          <Row label="Hold pickup window">{plural(p.holdHours, 'hour')}</Row>
          <Row label="Late fee">{`${formatMoney(p.finePerDay)} a day, capped at ${formatMoney(p.fineCap)} per book`}</Row>
          <Row label="Borrowing pauses above">{`${formatMoney(p.maxUnpaidFines)} unpaid`}</Row>
        </ul>
      </Card>
      <p className="mt-3 px-1 text-xs text-ink-500">Set by the library for {tierName(user.membershipTier)} members. Ask a librarian about changing your tier.</p>
    </>
  )
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <li className="flex items-center justify-between gap-4 px-5 py-3.5 text-[15px]">
      <span className="shrink-0 text-ink-600">{label}</span>
      <span className="text-right font-medium text-ink-900">{children}</span>
    </li>
  )
}
