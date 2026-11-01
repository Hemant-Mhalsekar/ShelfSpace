import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { ApiError } from '../../api/client'
import { policiesApi } from '../../api/endpoints'
import type { Policy, PolicyInput } from '../../api/types'
import { useToast } from '../../components/toast/useToast'
import { Alert, Button, Card, Field, PageHeader, PageLoader } from '../../components/ui'
import { formatDateTime } from '../../lib/format'
import { tierName } from '../../lib/tiers'
import { errorMessage, inputClass } from '../../lib/ui-helpers'

const FIELDS: { key: keyof PolicyInput; label: string; hint: string; step?: string; money?: boolean }[] = [
  { key: 'maxBooks', label: 'Books at once', hint: '1–50' },
  { key: 'loanDays', label: 'Loan length (days)', hint: '1–365' },
  { key: 'maxRenewals', label: 'Renewals allowed', hint: '0–10' },
  { key: 'finePerDay', label: 'Fine per day (₹)', hint: 'up to ₹1000', step: '0.01', money: true },
  { key: 'fineCap', label: 'Fine cap per loan (₹)', hint: 'at least the daily fine', step: '0.01', money: true },
  { key: 'maxUnpaidFines', label: 'Blocked above (₹ unpaid)', hint: 'borrowing stops past this', step: '0.01', money: true },
  { key: 'maxReservations', label: 'Reservations at once', hint: '0–20' },
  { key: 'holdHours', label: 'Hold time (hours)', hint: '1–336' },
]

export function RulesPage() {
  const policies = useQuery({ queryKey: ['policies'], queryFn: policiesApi.list })

  return (
    <>
      <PageHeader title="Borrowing rules" subtitle="Changes apply to new loans straight away. Existing due dates don't move." />
      {policies.isPending ? <PageLoader />
        : policies.isError ? <Alert>{errorMessage(policies.error)}</Alert>
          : <div className="grid gap-6 xl:grid-cols-3">{policies.data.map((p) => <PolicyCard key={`${p.membershipTier}-${p.updatedAt}`} policy={p} />)}</div>}
    </>
  )
}

function PolicyCard({ policy }: { policy: Policy }) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const initial = Object.fromEntries(FIELDS.map((f) => [f.key, String(policy[f.key])])) as Record<keyof PolicyInput, string>
  const [form, setForm] = useState(initial)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const dirty = FIELDS.some((f) => Number(form[f.key]) !== Number(initial[f.key]))

  const save = useMutation({
    mutationFn: () => policiesApi.update(policy.membershipTier, Object.fromEntries(
      FIELDS.map((f) => [f.key, Number(form[f.key])])) as unknown as PolicyInput),
    onSuccess: () => {
      toast.show(`${tierName(policy.membershipTier)} rules saved`)
      void queryClient.invalidateQueries({ queryKey: ['policies'] })
    },
    onError: (e) => {
      if (e instanceof ApiError) setFieldErrors(e.fieldErrors)
    },
  })

  function submit(e: FormEvent) {
    e.preventDefault()
    setFieldErrors({})
    save.mutate()
  }

  return (
    <Card className="p-5">
      <div className="mb-4 flex items-baseline justify-between">
        <h2 className="text-lg font-semibold">{tierName(policy.membershipTier)}</h2>
        <span className="text-xs text-slate-400">updated {formatDateTime(policy.updatedAt)}</span>
      </div>
      <form onSubmit={submit} className="space-y-3">
        {save.error && <Alert>{errorMessage(save.error)}</Alert>}
        {FIELDS.map((f) => (
          <Field key={f.key} label={f.label} error={fieldErrors[f.key]}>
            <input
              className={inputClass}
              type="number"
              step={f.step ?? '1'}
              min={0}
              required
              value={form[f.key]}
              onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
            />
            <span className="mt-0.5 block text-xs text-slate-400">{f.hint}</span>
          </Field>
        ))}
        <div className="flex gap-2 pt-2">
          <Button type="submit" disabled={!dirty} loading={save.isPending}>Save</Button>
          {dirty && <Button type="button" variant="ghost" onClick={() => { setForm(initial); setFieldErrors({}) }}>Undo changes</Button>}
        </div>
      </form>
    </Card>
  )
}
