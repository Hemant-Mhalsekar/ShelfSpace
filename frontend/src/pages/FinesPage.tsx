import { useQuery } from '@tanstack/react-query'
import { finesApi } from '../api/endpoints'
import { Alert, Card, EmptyState, PageHeader, PageLoader, StatusBadge } from '../components/ui'
import { errorMessage } from '../lib/ui-helpers'
import { formatDate, formatMoney } from '../lib/format'

const reasonLabel = { OVERDUE: 'Late return', LOST: 'Lost book', DAMAGED: 'Damaged book' } as const

export function FinesPage() {
  const fines = useQuery({ queryKey: ['fines', 'mine'], queryFn: finesApi.mine })

  if (fines.isPending) return <PageLoader />
  if (fines.isError) return <Alert>{errorMessage(fines.error)}</Alert>
  const { totalUnpaid, fines: list } = fines.data

  return (
    <>
      <PageHeader title="My fines" subtitle="Pay at the library desk; the librarian will mark it paid." />

      <Card className="mb-6 flex items-center justify-between p-5">
        <span className="text-sm text-ink-500">Total unpaid</span>
        <span className={`text-2xl font-bold ${totalUnpaid > 0 ? 'text-danger-600' : 'text-accent-600'}`}>{formatMoney(totalUnpaid)}</span>
      </Card>

      {list.length === 0 ? (
        <EmptyState title="No fines">Return books on time and this stays empty.</EmptyState>
      ) : (
        <Card className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-ink-200 text-[13px] text-ink-500">
              <tr>
                <th className="px-4 py-3 font-medium">Book</th>
                <th className="px-4 py-3 font-medium">Reason</th>
                <th className="px-4 py-3 font-medium">Date</th>
                <th className="px-4 py-3 text-right font-medium">Amount</th>
                <th className="px-4 py-3 font-medium">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ink-100">
              {list.map((f) => (
                <tr key={f.id}>
                  <td className="px-4 py-3 font-medium">{f.bookTitle}</td>
                  <td className="px-4 py-3 text-ink-600">{reasonLabel[f.reason]}</td>
                  <td className="px-4 py-3 text-ink-600">{formatDate(f.createdAt)}</td>
                  <td className="px-4 py-3 text-right font-mono">{formatMoney(f.amount)}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={f.status} />
                    {f.note && <p className="mt-1 text-xs text-ink-500">{f.note}</p>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </>
  )
}
