import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { adminBooksApi, booksApi } from '../../api/endpoints'
import type { Copy, CopyStatus } from '../../api/types'
import { ConfirmButton } from '../../components/ConfirmButton'
import { useToast } from '../../components/toast/useToast'
import { Alert, Button, Card, PageLoader, StatusBadge } from '../../components/ui'
import { formatDateTime } from '../../lib/format'
import { errorMessage, inputClass } from '../../lib/ui-helpers'
import { Availability } from '../CatalogPage'
import { BookForm } from './BookForm'

export function AdminBookPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()

  const book = useQuery({ queryKey: ['book', id], queryFn: () => booksApi.get(id) })
  const copies = useQuery({ queryKey: ['book', id, 'copies'], queryFn: () => adminBooksApi.copies(id) })

  const refresh = () => {
    void queryClient.invalidateQueries({ queryKey: ['book', id] })
    void queryClient.invalidateQueries({ queryKey: ['books'] })
    void queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const remove = useMutation({
    mutationFn: () => adminBooksApi.remove(id),
    onSuccess: () => {
      toast.show('Book deleted')
      void queryClient.invalidateQueries({ queryKey: ['books'] })
      navigate('/admin/books')
    },
    onError: (e) => toast.show(errorMessage(e), 'error'),
  })

  if (book.isPending) return <PageLoader />
  if (book.isError) return <Alert>{errorMessage(book.error)}</Alert>
  const b = book.data

  return (
    <>
      <Link to="/admin/books" className="mb-6 inline-block text-sm text-slate-500 hover:text-slate-800">← All books</Link>
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">{b.title}</h1>
          <p className="mt-1 flex items-center gap-3 text-slate-500">{b.author} <Availability book={b} /></p>
        </div>
        <ConfirmButton loading={remove.isPending} onConfirm={() => remove.mutate()} confirmLabel="Confirm delete">Delete book</ConfirmButton>
      </div>

      <div className="grid gap-6 lg:grid-cols-5">
        <Card className="p-5 lg:col-span-3">
          <h2 className="mb-4 font-semibold">Details</h2>
          <BookForm
            key={b.id + b.isbn + b.title}
            initial={b}
            submitLabel="Save changes"
            onSubmit={async (input) => {
              await adminBooksApi.update(id, input)
              toast.show('Saved')
              refresh()
            }}
          />
        </Card>

        <Card className="p-5 lg:col-span-2">
          <h2 className="mb-1 font-semibold">Copies</h2>
          <p className="mb-4 text-xs text-slate-500">Borrowed and on-hold copies are managed by the loan desk and reservations.</p>
          <AddCopies bookId={id} onDone={refresh} />
          {copies.isPending ? <PageLoader />
            : copies.isError ? <Alert>{errorMessage(copies.error)}</Alert>
              : copies.data.length === 0 ? <p className="text-sm text-slate-500">No copies yet.</p>
                : <ul className="divide-y divide-slate-100">{copies.data.map((c) => <CopyRow key={c.id} copy={c} onDone={refresh} />)}</ul>}
        </Card>
      </div>
    </>
  )
}

function AddCopies({ bookId, onDone }: { bookId: number; onDone: () => void }) {
  const toast = useToast()
  const [count, setCount] = useState(1)
  const add = useMutation({
    mutationFn: () => adminBooksApi.addCopies(bookId, count),
    onSuccess: (created) => {
      const held = created.filter((c) => c.status === 'ON_HOLD').length
      toast.show(`Added ${created.length} cop${created.length === 1 ? 'y' : 'ies'}${held ? `; ${held} went straight to members waiting` : ''}`)
      onDone()
    },
    onError: (e) => toast.show(errorMessage(e), 'error'),
  })
  return (
    <div className="mb-4 flex gap-2">
      <div className="w-20 shrink-0">
        <input type="number" min={1} max={50} value={count} onChange={(e) => setCount(Number(e.target.value))} className={inputClass} aria-label="Number of copies" />
      </div>
      <Button variant="secondary" className="whitespace-nowrap" loading={add.isPending} disabled={count < 1 || count > 50} onClick={() => add.mutate()}>Add copies</Button>
    </div>
  )
}

const settable: CopyStatus[] = ['AVAILABLE', 'DAMAGED', 'LOST']

function CopyRow({ copy, onDone }: { copy: Copy; onDone: () => void }) {
  const toast = useToast()
  const locked = copy.status === 'BORROWED' || copy.status === 'ON_HOLD'
  const update = useMutation({
    mutationFn: (status: CopyStatus) => adminBooksApi.setCopyStatus(copy.id, status),
    onSuccess: (c) => {
      toast.show(c.status === 'ON_HOLD' ? `${c.barcode} is available again and is now held for a waiting member` : `${c.barcode} marked ${c.status.toLowerCase()}`)
      onDone()
    },
    onError: (e) => toast.show(errorMessage(e), 'error'),
  })

  return (
    <li className="flex items-center gap-3 py-2.5">
      <div className="min-w-0 flex-1">
        <p className="font-mono text-sm">{copy.barcode}</p>
        <p className="text-xs text-slate-400">updated {formatDateTime(copy.updatedAt)}</p>
      </div>
      {locked ? <StatusBadge status={copy.status} /> : (
        <select
          className={`${inputClass} !w-36 shrink-0 py-1.5`}
          value={copy.status}
          disabled={update.isPending}
          onChange={(e) => update.mutate(e.target.value as CopyStatus)}
          aria-label={`Status of ${copy.barcode}`}
        >
          {settable.map((s) => <option key={s} value={s}>{s.charAt(0) + s.slice(1).toLowerCase()}</option>)}
        </select>
      )}
    </li>
  )
}
