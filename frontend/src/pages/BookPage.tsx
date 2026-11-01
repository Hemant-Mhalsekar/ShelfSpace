import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { Link, useParams } from 'react-router'
import { booksApi, loansApi, recommendationsApi, reservationsApi } from '../api/endpoints'
import type { Book, Reservation } from '../api/types'
import { RecommendationGrid } from '../components/RecommendationGrid'
import { Alert, Button, PageLoader, SectionTitle } from '../components/ui'
import { WishlistButton } from '../components/WishlistButton'
import { errorMessage } from '../lib/ui-helpers'
import { formatDate, formatDateTime, formatMoney } from '../lib/format'
import { BookCover } from '../components/BookCover'
import { Availability } from './CatalogPage'

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
      <Link to="/books" className="mb-6 inline-flex items-center gap-1 text-[15px] font-medium text-accent-600 hover:text-accent-700">
        <span aria-hidden>‹</span> Catalog
      </Link>

      <div className="flex flex-col gap-8 sm:flex-row sm:gap-12">
        <BookCover title={b.title} author={b.author} isbn={b.isbn} size="lg" className="self-center sm:self-start" />

        <div className="min-w-0 flex-1">
          <h1 className="font-display text-[34px] leading-tight font-bold tracking-tight">{b.title}</h1>
          <p className="mt-1 text-xl font-medium text-accent-600">{b.author}</p>

          <div className="mt-6">
            <BorrowPanel book={b} reservation={myReservation} side={<WishlistButton bookId={b.id} title={b.title} />} />
          </div>

          <dl className="mt-8 grid grid-cols-2 divide-x divide-ink-200 border-y border-ink-200 py-4 text-center sm:grid-cols-4">
            <Fact label="Genre" value={b.category} />
            <Fact label="Published" value={b.publicationYear ?? '—'} />
            <Fact label="Copies" value={<Availability book={b} />} />
            <Fact label="ISBN" value={<span className="font-mono text-[13px]">{b.isbn}</span>} />
          </dl>
          {b.replacementCost != null && (
            <p className="mt-3 text-[13px] text-ink-500">Replacement cost if lost: {formatMoney(b.replacementCost)}</p>
          )}
        </div>
      </div>

      {recommendations.data && recommendations.data.length > 0 && (
        <section className="mt-14">
          <SectionTitle>Readers Also Borrowed</SectionTitle>
          <RecommendationGrid items={recommendations.data} />
        </section>
      )}
    </>
  )
}

function Fact({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="px-3 py-1">
      <dt className="text-[11px] font-semibold tracking-wide text-ink-500 uppercase">{label}</dt>
      <dd className="mt-1 truncate text-[15px] font-semibold text-ink-900">{value}</dd>
    </div>
  )
}

function BorrowPanel({ book, reservation, side }: { book: Book; reservation?: Reservation; side: ReactNode }) {
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

  const big = 'px-8 py-3 text-base'

  if (message) {
    return (
      <div className="w-full space-y-2">
        <Alert kind="success">{message}</Alert>
        <Link to="/dashboard" className="text-sm font-semibold text-accent-600 hover:text-accent-700">Go to My Books ›</Link>
      </div>
    )
  }

  return (
    <div className="flex flex-col items-start gap-2">
      {error && <Alert>{errorMessage(error)}</Alert>}

      {reservation?.status === 'READY' ? (
        <>
          <Row side={side}><Button className={big} loading={borrow.isPending} onClick={() => borrow.mutate()}>Borrow Held Copy</Button></Row>
          <p className="text-[13px] text-success-700">Held for you until {formatDateTime(reservation.expiresAt)}</p>
        </>
      ) : reservation?.status === 'WAITING' ? (
        <>
          <Row side={side}><Button className={big} variant="secondary" disabled>#{reservation.position} in Line</Button></Row>
          <p className="text-[13px] text-ink-500">
            {reservation.estimatedAvailableDate ? `Expected around ${formatDate(reservation.estimatedAvailableDate)}. ` : ''}We'll notify you.
          </p>
        </>
      ) : book.availableCopies > 0 ? (
        <>
          <Row side={side}><Button className={big} loading={borrow.isPending} onClick={() => borrow.mutate()}>Borrow</Button></Row>
          <p className="text-[13px] text-ink-500">A librarian approves requests, usually the same day.</p>
        </>
      ) : book.totalCopies > 0 ? (
        <>
          <Row side={side}><Button className={big} loading={reserve.isPending} onClick={() => reserve.mutate()}>Join Waitlist</Button></Row>
          <p className="text-[13px] text-ink-500">All copies are out. We'll hold the next one for you.</p>
        </>
      ) : (
        <Row side={side}><p className="text-[15px] text-ink-500">No copies yet.</p></Row>
      )}
    </div>
  )
}

function Row({ side, children }: { side: ReactNode; children: ReactNode }) {
  return <div className="flex items-center gap-3">{children}{side}</div>
}
