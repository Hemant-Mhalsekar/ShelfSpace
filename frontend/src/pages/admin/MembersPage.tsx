import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { ApiError } from '../../api/client'
import { membersApi, type MemberSearch } from '../../api/endpoints'
import type { MembershipTier, Role } from '../../api/types'
import { Modal } from '../../components/Modal'
import { PasswordChecklist } from '../../components/PasswordChecklist'
import { useToast } from '../../components/toast/useToast'
import { Alert, Badge, Button, Card, EmptyState, Field, PageHeader, PageLoader, Pagination, StatusBadge } from '../../components/ui'
import { formatMoney } from '../../lib/format'
import { TIERS, tierName } from '../../lib/tiers'
import { errorMessage, inputClass } from '../../lib/ui-helpers'
import { useFieldChecks } from '../../lib/useFieldChecks'
import { emailProblem, nameProblem, passwordProblem, phoneProblem } from '../../lib/validation'

export function MembersPage() {
  const [filters, setFilters] = useState<MemberSearch>({ page: 0 })
  const [adding, setAdding] = useState(false)

  const members = useQuery({
    queryKey: ['members', filters],
    queryFn: () => membersApi.search(filters),
    placeholderData: keepPreviousData,
  })

  const set = (patch: Partial<MemberSearch>) => setFilters((f) => ({ ...f, ...patch, page: 0 }))

  return (
    <>
      <PageHeader title="Members" subtitle="Search accounts, check who owes what, add people at the desk"
        actions={<Button onClick={() => setAdding(true)}>+ Add member</Button>} />

      <div className="mb-5 grid gap-3 sm:grid-cols-[1fr_auto_auto]">
        <input type="search" className={inputClass} placeholder="Search name, email or phone…"
          value={filters.q ?? ''} onChange={(e) => set({ q: e.target.value })} />
        <select className={inputClass} value={filters.tier ?? ''} onChange={(e) => set({ tier: (e.target.value || undefined) as MembershipTier | undefined })} aria-label="Tier">
          <option value="">All tiers</option>
          {TIERS.map((t) => <option key={t} value={t}>{tierName(t)}</option>)}
        </select>
        <select className={inputClass} value={filters.status ?? ''} onChange={(e) => set({ status: (e.target.value || undefined) as MemberSearch['status'] })} aria-label="Status">
          <option value="">Any status</option>
          <option value="ACTIVE">Active</option>
          <option value="SUSPENDED">Suspended</option>
        </select>
      </div>

      {members.isPending ? <PageLoader />
        : members.isError ? <Alert>{errorMessage(members.error)}</Alert>
          : members.data.content.length === 0 ? <EmptyState title="No members match" />
            : (
              <>
                <Card className={`overflow-x-auto ${members.isPlaceholderData ? 'opacity-60' : ''}`}>
                  <table className="w-full text-left text-sm">
                    <thead className="border-b border-ink-200 text-[13px] text-ink-500">
                      <tr>
                        <th className="px-4 py-3 font-medium">Member</th>
                        <th className="px-4 py-3 font-medium">Tier</th>
                        <th className="px-4 py-3 font-medium">Status</th>
                        <th className="px-4 py-3 text-right font-medium">Loans</th>
                        <th className="px-4 py-3 text-right font-medium">Unpaid</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-ink-100">
                      {members.data.content.map((m) => (
                        <tr key={m.id} className="hover:bg-ink-50">
                          <td className="px-4 py-3">
                            <Link to={`/admin/members/${m.id}`} className="font-medium hover:text-accent-700">{m.name}</Link>
                            {m.role === 'ADMIN' && <span className="ml-2"><Badge color="blue">Librarian</Badge></span>}
                            <p className="text-xs text-ink-500">{m.email}</p>
                          </td>
                          <td className="px-4 py-3 text-ink-600">{tierName(m.membershipTier)}</td>
                          <td className="px-4 py-3"><StatusBadge status={m.status} /></td>
                          <td className="px-4 py-3 text-right tabular-nums">
                            {m.activeLoans}
                            {m.overdueLoans > 0 && <span className="ml-1 text-xs font-medium text-danger-600">({m.overdueLoans} late)</span>}
                          </td>
                          <td className={`px-4 py-3 text-right tabular-nums ${m.unpaidFines > 0 ? 'font-medium text-danger-600' : 'text-ink-500'}`}>
                            {formatMoney(m.unpaidFines)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </Card>
                <Pagination page={members.data.page} totalPages={members.data.totalPages}
                  onChange={(p) => setFilters((f) => ({ ...f, page: p }))} />
              </>
            )}

      <AddMemberDialog open={adding} onClose={() => setAdding(false)} />
    </>
  )
}

function AddMemberDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const toast = useToast()
  const empty = { name: '', email: '', phone: '', password: '', membershipTier: 'STUDENT' as MembershipTier, role: 'MEMBER' as Role }
  const [form, setForm] = useState(empty)
  const [error, setError] = useState<string | null>(null)
  const checks = useFieldChecks({
    name: nameProblem(form.name),
    email: emailProblem(form.email),
    phone: phoneProblem(form.phone),
    password: passwordProblem(form.password, form),
  })
  const [busy, setBusy] = useState(false)

  const set = (k: keyof typeof form) => (e: { target: { value: string } }) => setForm((f) => ({ ...f, [k]: e.target.value }))

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (!checks.ready()) return
    setBusy(true)
    try {
      const m = await membersApi.create({ ...form, name: form.name.trim(), email: form.email.trim(), phone: form.phone.trim() || undefined })
      void queryClient.invalidateQueries({ queryKey: ['members'] })
      toast.show(`Created ${m.role === 'ADMIN' ? 'librarian' : 'member'} account for ${m.name}`)
      setForm(empty)
      checks.reset()
      onClose()
      navigate(`/admin/members/${m.id}`)
    } catch (err) {
      if (err instanceof ApiError) checks.setServerErrors(err.fieldErrors)
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal title="Add member" open={open} onClose={onClose} wide>
      <form onSubmit={submit} noValidate className="space-y-4">
        {error && <Alert>{error}</Alert>}
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Full name" error={checks.error('name')}>
            <input className={inputClass} required maxLength={100} value={form.name} onChange={set('name')} {...checks.field('name')} />
          </Field>
          <Field label="Email" error={checks.error('email')}>
            <input className={inputClass} type="email" required maxLength={150} value={form.email} onChange={set('email')} {...checks.field('email')} />
          </Field>
          <Field label="Phone (optional)" error={checks.error('phone')}>
            <input className={inputClass} type="tel" value={form.phone} onChange={set('phone')} {...checks.field('phone')} />
          </Field>
          <Field label="Starting password" error={checks.error('password')}>
            <input className={inputClass} required maxLength={72} value={form.password} onChange={set('password')} autoComplete="new-password" {...checks.field('password')} />
            <PasswordChecklist password={form.password} person={form} />
          </Field>
          <Field label="Membership tier">
            <select className={inputClass} value={form.membershipTier} onChange={set('membershipTier')}>
              {TIERS.map((t) => <option key={t} value={t}>{tierName(t)}</option>)}
            </select>
          </Field>
          <Field label="Account type">
            <select className={inputClass} value={form.role} onChange={set('role')}>
              <option value="MEMBER">Member</option>
              <option value="ADMIN">Librarian</option>
            </select>
          </Field>
        </div>
        <p className="text-xs text-ink-500">Give the member their starting password in person; they can change it from their Account page.</p>
        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" loading={busy}>Create account</Button>
        </div>
      </form>
    </Modal>
  )
}
