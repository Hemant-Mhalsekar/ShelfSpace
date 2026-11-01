import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { adminLoansApi } from '../../api/endpoints'
import type { Loan, LoanStatus } from '../../api/types'
import { ConfirmButton } from '../../components/ConfirmButton'
import { Modal } from '../../components/Modal'
import { Tabs } from '../../components/Tabs'
import { useToast } from '../../components/toast/useToast'
import { Alert, Button, Card, EmptyState, Field, PageHeader, PageLoader, Pagination, StatusBadge } from '../../components/ui'
import { daysUntil, formatDate, formatDateTime, formatMoney } from '../../lib/format'
import { errorMessage, inputClass } from '../../lib/ui-helpers'

type Tab = 'requests' | 'out' | 'overdue' | 'returned'

const TAB_STATUS: Record<Tab, LoanStatus> = {
  requests: 'REQUESTED',
  out: 'ISSUED',
  overdue: 'OVERDUE',
  returned: 'RETURNED',
}

export function LoanDeskPage() {
  const [params, setParams] = useSearchParams()
  const tab = (params.get('tab') as Tab | null) ?? 'requests'
  const memberId = params.get('memberId') ? Number(params.get('memberId')) : undefined
  const [page, setPage] = useState(0)

  const sort = tab === 'requests' ? 'requestedAt,asc' : tab === 'returned' ? 'returnedAt,desc' : 'dueDate,asc'
  const loans = useQuery({
    queryKey: ['loans', 'admin', tab, memberId, page],
    queryFn: () => adminLoansApi.search({ status: TAB_STATUS[tab], memberId, page, sort }),
    placeholderData: keepPreviousData,
  })
  const pending = useQuery({ queryKey: ['loans', 'admin', 'pending-count'], queryFn: () => adminLoansApi.search({ status: 'REQUESTED' }) })

  function setTab(t: Tab) {
    const next = new URLSearchParams(params)
    next.set('tab', t)
    setParams(next, { replace: true })
    setPage(0)
  }

  return (
    <>
      <PageHeader
        title="Loan desk"
        subtitle={memberId
          ? <>Showing one member's loans · <button className="text-indigo-600 hover:underline" onClick={() => { params.delete('memberId'); setParams(params) }}>show everyone</button></>
          : 'Approve requests, check books in and keep an eye on overdue loans'}
      />
      <Tabs<Tab>
        value={tab}
        onChange={setTab}
        tabs={[
          { value: 'requests', label: 'Requests', count: pending.data?.totalElements },
          { value: 'out', label: 'Out' },
          { value: 'overdue', label: 'Overdue' },
          { value: 'returned', label: 'Returned' },
        ]}
      />
      {tab === 'overdue' && (
        <p className="-mt-2 mb-4 text-xs text-slate-500">Loans are flagged overdue by the nightly job. Loans that went past due today still show under “Out”, in red.</p>
      )}

      {loans.isPending ? <PageLoader />
        : loans.isError ? <Alert>{errorMessage(loans.error)}</Alert>
          : loans.data.content.length === 0 ? <EmptyState title={emptyText[tab]} />
            : (
              <>
                <div className={`space-y-3 ${loans.isPlaceholderData ? 'opacity-60' : ''}`}>
                  {loans.data.content.map((l) => <LoanCard key={l.id} loan={l} />)}
                </div>
                <Pagination page={loans.data.page} totalPages={loans.data.totalPages} onChange={setPage} />
              </>
            )}
    </>
  )
}

const emptyText: Record<Tab, string> = {
  requests: 'No requests waiting. All caught up!',
  out: 'No books are out right now',
  overdue: 'Nothing overdue',
  returned: 'No returns yet',
}

function LoanCard({ loan }: { loan: Loan }) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [rejecting, setRejecting] = useState(false)

  const done = () => {
    void queryClient.invalidateQueries({ queryKey: ['loans'] })
    void queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    void queryClient.invalidateQueries({ queryKey: ['books'] })
  }
  const onError = (e: unknown) => toast.show(errorMessage(e), 'error')

  const approve = useMutation({
    mutationFn: () => adminLoansApi.approve(loan.id),
    onSuccess: (l) => { toast.show(`Issued “${l.bookTitle}” to ${l.memberName}, due ${formatDate(l.dueDate)}`); done() },
    onError,
  })
  const returnBook = useMutation({
    mutationFn: () => adminLoansApi.returnBook(loan.id),
    onSuccess: (r) => {
      toast.show(r.fineCharged > 0
        ? `Returned ${r.overdueDays} day(s) late. Fine of ${formatMoney(r.fineCharged)} charged.`
        : 'Returned on time. No fine.', r.fineCharged > 0 ? 'info' : 'success')
      done()
    },
    onError,
  })
  const lost = useMutation({
    mutationFn: () => adminLoansApi.markLost(loan.id),
    onSuccess: (r) => { toast.show(`Marked lost. ${formatMoney(r.fineCharged)} charged.`, 'info'); done() },
    onError,
  })

  const days = daysUntil(loan.dueDate)
  const late = days !== null && days < 0 && (loan.status === 'ISSUED' || loan.status === 'OVERDUE')

  return (
    <Card className={`p-4 ${late ? 'border-rose-200' : ''}`}>
      <div className="flex flex-wrap items-center gap-x-6 gap-y-3">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            {loan.bookId
              ? <Link to={`/admin/books/${loan.bookId}`} className="font-medium hover:text-indigo-700">{loan.bookTitle}</Link>
              : <span className="font-medium">{loan.bookTitle}</span>}
            <StatusBadge status={loan.status} />
            <span className="font-mono text-xs text-slate-400">{loan.copyBarcode}</span>
          </div>
          <p className="mt-1 text-sm text-slate-600">
            <Link to={`/admin/members/${loan.memberId}`} className="hover:text-indigo-700 hover:underline">{loan.memberName}</Link>
            <span className="text-slate-400"> · </span>
            {loan.status === 'REQUESTED' && <>requested {formatDateTime(loan.requestedAt)}</>}
            {(loan.status === 'ISSUED' || loan.status === 'OVERDUE') && (late
              ? <span className="font-medium text-rose-600">{-days!} day(s) overdue · {formatMoney(loan.accruedFine)} so far</span>
              : <>due {formatDate(loan.dueDate)}{loan.renewCount ? ` · renewed ${loan.renewCount}×` : ''}</>)}
            {(loan.status === 'RETURNED' || loan.status === 'LOST') && <>closed {formatDate(loan.returnedAt)}</>}
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          {loan.status === 'REQUESTED' && (
            <>
              <Button loading={approve.isPending} onClick={() => approve.mutate()}>Approve & issue</Button>
              <Button variant="secondary" onClick={() => setRejecting(true)}>Reject</Button>
            </>
          )}
          {(loan.status === 'ISSUED' || loan.status === 'OVERDUE') && (
            <>
              <Button loading={returnBook.isPending} onClick={() => returnBook.mutate()}>Check in</Button>
              <ConfirmButton loading={lost.isPending} onConfirm={() => lost.mutate()} confirmLabel="Confirm lost">Mark lost</ConfirmButton>
            </>
          )}
        </div>
      </div>

      <RejectDialog loan={loan} open={rejecting} onClose={() => setRejecting(false)} onDone={done} />
    </Card>
  )
}

function RejectDialog({ loan, open, onClose, onDone }: { loan: Loan; open: boolean; onClose: () => void; onDone: () => void }) {
  const toast = useToast()
  const [reason, setReason] = useState('')
  const reject = useMutation({
    mutationFn: () => adminLoansApi.reject(loan.id, reason.trim() || undefined),
    onSuccess: () => { toast.show('Request rejected; the member has been notified'); onClose(); onDone() },
  })

  return (
    <Modal title="Reject request" open={open} onClose={onClose}>
      <p className="mb-4 text-sm text-slate-600">
        <strong>{loan.memberName}</strong> asked for <strong>{loan.bookTitle}</strong>. The copy goes back to the shelf (or to the next person waiting).
      </p>
      {reject.error && <div className="mb-3"><Alert>{errorMessage(reject.error)}</Alert></div>}
      <Field label="Reason (shown to the member)">
        <input className={inputClass} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="e.g. Reference copy, library use only" maxLength={255} />
      </Field>
      <div className="mt-5 flex justify-end gap-2">
        <Button variant="secondary" onClick={onClose}>Cancel</Button>
        <Button variant="danger" loading={reject.isPending} onClick={() => reject.mutate()}>Reject request</Button>
      </div>
    </Modal>
  )
}
