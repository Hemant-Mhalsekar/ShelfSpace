import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { ApiError } from '../../api/client'
import { membersApi } from '../../api/endpoints'
import type { Loan, MemberSummary, MembershipTier } from '../../api/types'
import { useAuth } from '../../auth/useAuth'
import { Modal } from '../../components/Modal'
import { useToast } from '../../components/toast/useToast'
import { Alert, Badge, Button, Card, EmptyState, Field, PageLoader, StatusBadge } from '../../components/ui'
import { formatDate, formatMoney } from '../../lib/format'
import { errorMessage, inputClass } from '../../lib/ui-helpers'
import { TIERS, tierName } from '../../lib/tiers'

export function MemberPage() {
  const id = Number(useParams().id)
  const { user } = useAuth()
  const detail = useQuery({ queryKey: ['members', id], queryFn: () => membersApi.get(id) })
  const [suspending, setSuspending] = useState(false)
  const [resetting, setResetting] = useState(false)

  if (detail.isPending) return <PageLoader />
  if (detail.isError) return <Alert>{errorMessage(detail.error)}</Alert>
  const { member: m, currentLoans, reservations, recentHistory } = detail.data
  const isSelf = user?.id === m.id

  return (
    <>
      <Link to="/admin/members" className="mb-6 inline-block text-sm text-ink-500 hover:text-ink-800">← All members</Link>

      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight">{m.name}</h1>
            <StatusBadge status={m.status} />
            {m.role === 'ADMIN' && <Badge color="blue">Librarian</Badge>}
          </div>
          <p className="mt-1 text-sm text-ink-500">{m.email}{m.phone ? ` · ${m.phone}` : ''} · joined {formatDate(m.createdAt)}</p>
        </div>
        {!isSelf && (
          <div className="flex gap-2">
            <Button variant="secondary" onClick={() => setResetting(true)}>Reset Password</Button>
            {m.status === 'ACTIVE'
              ? <Button variant="danger" onClick={() => setSuspending(true)}>Suspend</Button>
              : <ActivateButton memberId={m.id} />}
          </div>
        )}
      </div>

      {m.status === 'SUSPENDED' && (
        <div className="mb-6"><Alert>This account is suspended. They can't log in, borrow or reserve, but can still return books.</Alert></div>
      )}

      <div className="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
        <Card className="p-4"><p className="text-sm text-ink-500">Active loans</p><p className="text-xl font-bold">{m.activeLoans}</p></Card>
        <Card className="p-4"><p className="text-sm text-ink-500">Overdue</p><p className={`text-xl font-bold ${m.overdueLoans ? 'text-danger-600' : ''}`}>{m.overdueLoans}</p></Card>
        <Link to={`/admin/fines?memberId=${m.id}`} className="block hover:opacity-80">
          <Card className="p-4"><p className="text-sm text-ink-500">Unpaid fines</p><p className={`text-xl font-bold ${m.unpaidFines > 0 ? 'text-danger-600' : ''}`}>{formatMoney(m.unpaidFines)}</p></Card>
        </Link>
        <Card className="p-4"><p className="text-sm text-ink-500">Reservations</p><p className="text-xl font-bold">{reservations.length}</p></Card>
      </div>

      <div className="grid gap-6 lg:grid-cols-5">
        <div className="space-y-6 lg:col-span-3">
          <LoanList title="Current loans" loans={currentLoans} empty="Nothing borrowed" memberId={m.id} />
          <LoanList title="Recent history" loans={recentHistory} empty="No returned books yet" />
          {reservations.length > 0 && (
            <Card className="p-5">
              <h2 className="mb-3 font-semibold">Reservations</h2>
              <ul className="divide-y divide-ink-100 text-sm">
                {reservations.map((r) => (
                  <li key={r.id} className="flex items-center justify-between py-2">
                    <span>{r.bookTitle}</span>
                    <span className="flex items-center gap-2 text-ink-500">{r.position ? `#${r.position}` : ''} <StatusBadge status={r.status} /></span>
                  </li>
                ))}
              </ul>
            </Card>
          )}
        </div>
        <Card className="h-fit p-5 lg:col-span-2">
          <h2 className="mb-4 font-semibold">Edit details</h2>
          <EditMember key={`${m.id}-${m.name}-${m.membershipTier}-${m.phone}`} member={m} />
        </Card>
      </div>

      <SuspendDialog member={m} open={suspending} onClose={() => setSuspending(false)} />
      <ResetPasswordDialog member={m} open={resetting} onClose={() => setResetting(false)} />
    </>
  )
}

function LoanList({ title, loans, empty, memberId }: { title: string; loans: Loan[]; empty: string; memberId?: number }) {
  return (
    <Card className="p-5">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="font-semibold">{title}</h2>
        {memberId && loans.length > 0 && <Link to={`/admin/loans?tab=out&memberId=${memberId}`} className="text-sm text-accent-600 hover:underline">Open in loan desk</Link>}
      </div>
      {loans.length === 0 ? <EmptyState title={empty} /> : (
        <ul className="divide-y divide-ink-100 text-sm">
          {loans.map((l) => (
            <li key={l.id} className="flex items-center justify-between gap-3 py-2.5">
              <span className="min-w-0 truncate font-medium">{l.bookTitle}</span>
              <span className="flex shrink-0 items-center gap-2 text-ink-500">
                {l.returnedAt ? formatDate(l.returnedAt) : l.dueDate ? `due ${formatDate(l.dueDate)}` : ''}
                {l.overdueDays ? <span className="text-danger-600">{l.overdueDays}d late</span> : null}
                <StatusBadge status={l.status} />
              </span>
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}

function useRefreshMember(id: number) {
  const queryClient = useQueryClient()
  return () => {
    void queryClient.invalidateQueries({ queryKey: ['members'] })
    void queryClient.invalidateQueries({ queryKey: ['members', id] })
  }
}

function EditMember({ member }: { member: MemberSummary }) {
  const toast = useToast()
  const refresh = useRefreshMember(member.id)
  const [form, setForm] = useState({ name: member.name, phone: member.phone ?? '', membershipTier: member.membershipTier })
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const save = useMutation({
    mutationFn: () => membersApi.update(member.id, { name: form.name, phone: form.phone || null, membershipTier: form.membershipTier }),
    onSuccess: (m) => {
      toast.show(m.membershipTier !== member.membershipTier ? `Saved. ${m.name} is now ${tierName(m.membershipTier)} tier.` : 'Saved')
      refresh()
    },
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
      <Field label="Name" error={fieldErrors.name}>
        <input className={inputClass} required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
      </Field>
      <Field label="Phone" error={fieldErrors.phone}>
        <input className={inputClass} value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
      </Field>
      <Field label="Membership tier">
        <select className={inputClass} value={form.membershipTier} onChange={(e) => setForm({ ...form, membershipTier: e.target.value as MembershipTier })}>
          {TIERS.map((t) => <option key={t} value={t}>{tierName(t)}</option>)}
        </select>
        <span className="mt-1 block text-xs text-ink-500">New limits apply to their next loan. Current due dates don't change.</span>
      </Field>
      <Button type="submit" loading={save.isPending}>Save</Button>
    </form>
  )
}

function SuspendDialog({ member, open, onClose }: { member: MemberSummary; open: boolean; onClose: () => void }) {
  const toast = useToast()
  const refresh = useRefreshMember(member.id)
  const [reason, setReason] = useState('')
  const suspend = useMutation({
    mutationFn: () => membersApi.suspend(member.id, reason.trim()),
    onSuccess: () => { toast.show(`${member.name} has been suspended`, 'info'); setReason(''); onClose(); refresh() },
  })

  return (
    <Modal title={`Suspend ${member.name}?`} open={open} onClose={onClose}>
      <p className="mb-4 text-sm text-ink-600">They won't be able to log in, borrow or reserve until reactivated. Books they have can still be returned.</p>
      {suspend.error && <div className="mb-3"><Alert>{errorMessage(suspend.error)}</Alert></div>}
      <Field label="Reason (kept in the audit log)">
        <input className={inputClass} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="e.g. Repeated late returns" maxLength={255} />
      </Field>
      <div className="mt-5 flex justify-end gap-2">
        <Button variant="secondary" onClick={onClose}>Cancel</Button>
        <Button variant="danger" disabled={!reason.trim()} loading={suspend.isPending} onClick={() => suspend.mutate()}>Suspend</Button>
      </div>
    </Modal>
  )
}

function ActivateButton({ memberId }: { memberId: number }) {
  const toast = useToast()
  const refresh = useRefreshMember(memberId)
  const activate = useMutation({
    mutationFn: () => membersApi.activate(memberId),
    onSuccess: (m) => { toast.show(`${m.name} is active again`); refresh() },
    onError: (e) => toast.show(errorMessage(e), 'error'),
  })
  return <Button loading={activate.isPending} onClick={() => activate.mutate()}>Reactivate</Button>
}

const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d).{8,72}$/

function ResetPasswordDialog({ member, open, onClose }: { member: MemberSummary; open: boolean; onClose: () => void }) {
  const toast = useToast()
  const [password, setPassword] = useState('')
  const valid = PASSWORD_RULE.test(password)
  const close = () => { setPassword(''); reset.reset(); onClose() }
  const reset = useMutation({
    mutationFn: () => membersApi.resetPassword(member.id, password),
    onSuccess: () => { toast.show(`New password set for ${member.name}`); close() },
  })

  return (
    <Modal title={`Reset password for ${member.name}?`} open={open} onClose={close}>
      <p className="mb-4 text-sm text-ink-600">
        Use this when a member has forgotten their password. Give them the new one in person; they can change it from their Account page. They'll be signed out on every device.
      </p>
      {reset.error && <div className="mb-3"><Alert>{errorMessage(reset.error)}</Alert></div>}
      <form onSubmit={(e) => { e.preventDefault(); if (valid) reset.mutate() }}>
        <Field label="New password" error={password && !valid ? 'At least 8 characters, with a letter and a number' : undefined}>
          <input className={inputClass} type="text" value={password} autoComplete="off" spellCheck={false}
            onChange={(e) => setPassword(e.target.value)} />
        </Field>
        <p className="mt-2 text-xs text-ink-500">The change is recorded in the audit log.</p>
        <div className="mt-5 flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={close}>Cancel</Button>
          <Button type="submit" disabled={!valid} loading={reset.isPending}>Set Password</Button>
        </div>
      </form>
    </Modal>
  )
}
