import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { booksApi, loansApi, recommendationsApi, reservationsApi } from '../api/endpoints'
import type { Book, Reservation } from '../api/types'
import { RecommendationGrid } from '../components/RecommendationGrid'
import { Alert, Button, Card, PageLoader } from '../components/ui'
import { WishlistButton } from '../components/WishlistButton'
import { errorMessage } from '../lib/ui-helpers'
import { formatDate, formatDateTime, formatMoney } from '../lib/format'
import { Availability, BookCover } from './CatalogPage'

export function BookPage() {
  const id = Number(useParams().id)
  const book = useQuery({ queryKey: ['book', id], queryFn: () => booksApi.get(id), enabled: Number.isFinite(id) })
  const reservations = useQuery({ queryKey: ['reservations', 'mine'], queryFn: reservationsApi.mine })
  const recommendations = useQuery({
    queryKey: ['recommendations', 'book', id],
    queryFn: () => recommendationsApi.forBook(id, 6),
    enabled: Number.isFinite(id),
  })

  if (book.isPending) return <PageLoader />
  if (book.isError) return <Alert>{errorMessage(book.error)}</Alert>

  const b = book.data
  const myReservation = reservations.data?.find(
    (r) => r.bookId === b.id && (r.status === 'WAITING' || r.status === 'READY'),
  )

  return (
    <>
      <Link to="/books" className="mb-6 inline-block text-sm text-slate-500 hover:text-slate-800">← Back to catalog</Link>
      <Card className="flex flex-col gap-8 p-6 sm:flex-row">
        <BookCover title={b.title} large />
        <div className="flex-1">
          <p className="text-sm font-medium text-indigo-600">{b.category}</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight">{b.title}</h1>
          <p className="mt-1 text-lg text-slate-600">{b.author}</p>

          <dl className="mt-6 grid grid-cols-2 gap-x-6 gap-y-3 text-sm sm:max-w-md">
            <dt className="text-slate-500">ISBN</dt><dd className="font-mono">{b.isbn}</dd>
            <dt className="text-slate-500">Published</dt><dd>{b.publicationYear ?? '—'}</dd>
            <dt className="text-slate-500">Copies</dt><dd><Availability book={b} /></dd>
            {b.replacementCost != null && (<><dt className="text-slate-500">Replacement cost</dt><dd>{formatMoney(b.replacementCost)}</dd></>)}
          </dl>

          <div className="mt-8 flex flex-wrap items-start gap-3">
            <div className="min-w-0 flex-1"><BorrowPanel book={b} reservation={myReservation} /></div>
            <WishlistButton bookId={b.id} title={b.title} />
          </div>
        </div>
      </Card>

      {recommendations.data && recommendations.data.length > 0 && (
        <section className="mt-10">
          <h2 className="mb-1 text-lg font-semibold">Readers also borrowed</h2>
          <p className="mb-4 text-sm text-slate-500">Based on what other members borrowed alongside this book</p>
          <RecommendationGrid items={recommendations.data} />
        </section>
      )}
    </>
  )
}

function BorrowPanel({ book, reservation }: { book: Book; reservation?: Reservation }) {
  const queryClient = useQueryClient()
  const [message, setMessage] = useState<string | null>(null)

  const refresh = () => {
    void queryClient.invalidateQueries({ queryKey: ['book', book.id] })
    void queryClient.invalidateQueries({ queryKey: ['reservations'] })
    void queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    void queryClient.invalidateQueries({ queryKey: ['books'] })
  }

  const borrow = useMutation({
    mutationFn: () => loansApi.request(book.id),
    onSuccess: () => {
      setMessage('Request sent! A librarian will approve it, and it will appear on your dashboard.')
      refresh()
    },
  })
  const reserve = useMutation({
    mutationFn: () => reservationsApi.reserve(book.id),
    onSuccess: (r) => {
      setMessage(`You're #${r.position} in the queue${r.estimatedAvailableDate ? `, expected around ${formatDate(r.estimatedAvailableDate)}` : ''}.`)
      refresh()
    },
  })
  const error = borrow.error ?? reserve.error

  if (message) {
    return (
      <div className="space-y-3">
        <Alert kind="success">{message}</Alert>
        <Link to="/dashboard" className="text-sm font-medium text-indigo-600 hover:underline">Go to my dashboard →</Link>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {error && <Alert>{errorMessage(error)}</Alert>}

      {reservation?.status === 'READY' ? (
        <>
          <Alert kind="info">
            A copy is being held for you until <strong>{formatDateTime(reservation.expiresAt)}</strong>.
          </Alert>
          <Button loading={borrow.isPending} onClick={() => borrow.mutate()}>Borrow my held copy</Button>
        </>
      ) : reservation?.status === 'WAITING' ? (
        <Alert kind="info">
          You're <strong>#{reservation.position}</strong> in the queue
          {reservation.estimatedAvailableDate && <>, expected around <strong>{formatDate(reservation.estimatedAvailableDate)}</strong></>}.
          We'll notify you when it's ready.
        </Alert>
      ) : book.availableCopies > 0 ? (
        <Button loading={borrow.isPending} onClick={() => borrow.mutate()}>Borrow this book</Button>
      ) : book.totalCopies > 0 ? (
        <>
          <p className="text-sm text-slate-600">All copies are out right now. Join the waiting list and we'll hold the next copy for you.</p>
          <Button loading={reserve.isPending} onClick={() => reserve.mutate()}>Reserve</Button>
        </>
      ) : (
        <p className="text-sm text-slate-500">This book has no copies yet.</p>
      )}
    </div>
  )
}
