import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { adminFinesApi } from '../../api/endpoints'
import type { Fine, FineStatus } from '../../api/types'
import { Modal } from '../../components/Modal'
import { Tabs } from '../../components/Tabs'
import { useToast } from '../../components/toast/useToast'
import { Alert, Button, Card, EmptyState, Field, PageHeader, PageLoader, Pagination, StatusBadge } from '../../components/ui'
import { formatDate, formatMoney } from '../../lib/format'
import { errorMessage, inputClass } from '../../lib/ui-helpers'

const reasonLabel = { OVERDUE: 'Late return', LOST: 'Lost book', DAMAGED: 'Damaged' } as const

export function AdminFinesPage() {
  const [params, setParams] = useSearchParams()
  const memberId = params.get('memberId') ? Number(params.get('memberId')) : undefined
  const [status, setStatus] = useState<FineStatus>('UNPAID')
  const [page, setPage] = useState(0)
  const [waiving, setWaiving] = useState<Fine | null>(null)

  const fines = useQuery({
    queryKey: ['fines', 'admin', status, memberId, page],
    queryFn: () => adminFinesApi.search({ status, memberId, page }),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <PageHeader
        title="Fines"
        subtitle={memberId
          ? <>One member's fines · <button className="text-accent-600 hover:underline" onClick={() => { params.delete('memberId'); setParams(params) }}>show everyone</button></>
          : 'Record payments at the desk, or waive a fine with a reason'}
      />
      <Tabs<FineStatus>
        value={status}
        onChange={(s) => { setStatus(s); setPage(0) }}
        tabs={[{ value: 'UNPAID', label: 'Unpaid' }, { value: 'PAID', label: 'Paid' }, { value: 'WAIVED', label: 'Waived' }]}
      />

      {fines.isPending ? <PageLoader />
        : fines.isError ? <Alert>{errorMessage(fines.error)}</Alert>
          : fines.data.content.length === 0 ? <EmptyState title={status === 'UNPAID' ? 'No unpaid fines' : 'Nothing here yet'} />
            : (
              <>
                <Card className={`overflow-x-auto ${fines.isPlaceholderData ? 'opacity-60' : ''}`}>
                  <table className="w-full text-left text-sm">
                    <thead className="border-b border-ink-200 text-[13px] text-ink-500">
                      <tr>
                        <th className="px-4 py-3 font-medium">Book</th>
                        <th className="px-4 py-3 font-medium">Reason</th>
                        <th className="px-4 py-3 font-medium">Charged</th>
                        <th className="px-4 py-3 text-right font-medium">Amount</th>
                        <th className="px-4 py-3 font-medium">Status</th>
                        <th className="px-4 py-3"><span className="sr-only">Actions</span></th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-ink-100">
                      {fines.data.content.map((f) => <FineRow key={f.id} fine={f} onWaive={() => setWaiving(f)} />)}
                    </tbody>
                  </table>
                </Card>
                <Pagination page={fines.data.page} totalPages={fines.data.totalPages} onChange={setPage} />
              </>
            )}

      {waiving && <WaiveDialog fine={waiving} onClose={() => setWaiving(null)} />}
    </>
  )
}

function useRefreshFines() {
  const queryClient = useQueryClient()
  return () => {
    void queryClient.invalidateQueries({ queryKey: ['fines'] })
    void queryClient.invalidateQueries({ queryKey: ['members'] })
    void queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }
}

function FineRow({ fine: f, onWaive }: { fine: Fine; onWaive: () => void }) {
  const toast = useToast()
  const refresh = useRefreshFines()
  const pay = useMutation({
    mutationFn: () => adminFinesApi.pay(f.id),
    onSuccess: () => { toast.show(`${formatMoney(f.amount)} recorded as paid`); refresh() },
    onError: (e) => toast.show(errorMessage(e), 'error'),
  })

  return (
    <tr>
      <td className="px-4 py-3">
        <p className="font-medium">{f.bookTitle}</p>
        <Link to={`/admin/members/${f.memberId}`} className="text-xs text-accent-600 hover:underline">View member</Link>
      </td>
      <td className="px-4 py-3 text-ink-600">{reasonLabel[f.reason]}</td>
      <td className="px-4 py-3 text-ink-600">{formatDate(f.createdAt)}</td>
      <td className="px-4 py-3 text-right font-mono">{formatMoney(f.amount)}</td>
      <td className="px-4 py-3">
        <StatusBadge status={f.status} />
        {f.note && <p className="mt-1 max-w-48 truncate text-xs text-ink-500" title={f.note}>{f.note}</p>}
      </td>
      <td className="px-4 py-3">
        {f.status === 'UNPAID' && (
          <div className="flex justify-end gap-2">
            <Button loading={pay.isPending} onClick={() => pay.mutate()}>Mark paid</Button>
            <Button variant="secondary" onClick={onWaive}>Waive</Button>
          </div>
        )}
      </td>
    </tr>
  )
}

function WaiveDialog({ fine, onClose }: { fine: Fine; onClose: () => void }) {
  const toast = useToast()
  const refresh = useRefreshFines()
  const [note, setNote] = useState('')
  const waive = useMutation({
    mutationFn: () => adminFinesApi.waive(fine.id, note.trim()),
    onSuccess: () => { toast.show('Fine waived'); refresh(); onClose() },
  })

  return (
    <Modal title={`Waive ${formatMoney(fine.amount)}?`} open onClose={onClose}>
      <p className="mb-4 text-sm text-ink-600">For “{fine.bookTitle}”. The reason is shown to the member and kept in the audit log.</p>
      {waive.error && <div className="mb-3"><Alert>{errorMessage(waive.error)}</Alert></div>}
      <Field label="Reason">
        <input className={inputClass} value={note} onChange={(e) => setNote(e.target.value)} placeholder="e.g. Library was closed for 2 days" maxLength={255} autoFocus />
      </Field>
      <div className="mt-5 flex justify-end gap-2">
        <Button variant="secondary" onClick={onClose}>Cancel</Button>
        <Button disabled={!note.trim()} loading={waive.isPending} onClick={() => waive.mutate()}>Waive fine</Button>
      </div>
    </Modal>
  )
}
