import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { booksApi } from '../api/endpoints'
import type { Book } from '../api/types'
import { BookCover } from '../components/BookCover'
import { Alert, EmptyState, PageHeader, PageLoader, Pagination } from '../components/ui'
import { errorMessage, inputClass } from '../lib/ui-helpers'

/** Search state lives in the URL, so results can be bookmarked and the back button works. */
export function CatalogPage() {
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''
  const category = params.get('category') ?? ''
  const availableOnly = params.get('available') === '1'
  const page = Number(params.get('page') ?? 0)
  const sort = params.get('sort') ?? 'title'

  // The search box is local state; it syncs to the URL after a pause in typing.
  const [text, setText] = useState(q)
  const [syncedQ, setSyncedQ] = useState(q)
  if (q !== syncedQ) {
    // URL changed from outside (back button, link): show it in the box
    setSyncedQ(q)
    setText(q)
  }

  const update = useCallback((changes: Record<string, string>) => {
    const next = new URLSearchParams(params)
    for (const [k, v] of Object.entries(changes)) {
      if (v) next.set(k, v)
      else next.delete(k)
    }
    setParams(next, { replace: true })
  }, [params, setParams])

  useEffect(() => {
    if (text === q) return
    const t = setTimeout(() => update({ q: text, page: '' }), 350)
    return () => clearTimeout(t)
  }, [text, q, update])

  const categories = useQuery({ queryKey: ['categories'], queryFn: booksApi.categories, staleTime: 5 * 60_000 })
  const books = useQuery({
    queryKey: ['books', { q, category, availableOnly, page, sort }],
    queryFn: () => booksApi.search({ q, category, availableOnly, page, size: 24, sort }),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <PageHeader
        title="Browse"
        subtitle={books.data ? `${books.data.totalElements} book${books.data.totalElements === 1 ? '' : 's'} in the library` : undefined}
      />

      <div className="mb-6 grid gap-3 sm:grid-cols-[1fr_auto_auto_auto] sm:items-center">
        <div className="relative">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} aria-hidden
            className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-ink-500">
            <path strokeLinecap="round" d="M21 21l-5.2-5.2M10.5 18a7.5 7.5 0 1 0 0-15 7.5 7.5 0 0 0 0 15Z" />
          </svg>
          <input
            type="search"
            className={`${inputClass} pl-10`}
            placeholder="Titles, authors or ISBN"
            value={text}
            onChange={(e) => setText(e.target.value)}
            aria-label="Search books"
          />
        </div>
        <select
          className={inputClass}
          value={category}
          onChange={(e) => update({ category: e.target.value, page: '' })}
          aria-label="Category"
        >
          <option value="">All categories</option>
          {categories.data?.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
        <select className={inputClass} value={sort} onChange={(e) => update({ sort: e.target.value, page: '' })} aria-label="Sort by">
          <option value="title">Title A–Z</option>
          <option value="author">Author A–Z</option>
          <option value="publicationYear,desc">Newest first</option>
          <option value="createdAt,desc">Recently added</option>
        </select>
        <label className="flex items-center gap-2 text-sm text-ink-700">
          <input
            type="checkbox"
            className="size-4 rounded accent-accent-600"
            checked={availableOnly}
            onChange={(e) => update({ available: e.target.checked ? '1' : '', page: '' })}
          />
          Available now
        </label>
      </div>

      {books.isPending ? (
        <PageLoader />
      ) : books.isError ? (
        <Alert>{errorMessage(books.error)}</Alert>
      ) : books.data.content.length === 0 ? (
        <EmptyState title="No books found">Try a different search or clear the filters.</EmptyState>
      ) : (
        <>
          <div className={`grid grid-cols-2 gap-x-5 gap-y-8 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 ${books.isPlaceholderData ? 'opacity-60' : ''}`}>
            {books.data.content.map((b) => <BookCard key={b.id} book={b} />)}
          </div>
          <Pagination page={books.data.page} totalPages={books.data.totalPages} onChange={(p) => update({ page: p ? String(p) : '' })} />
        </>
      )}
    </>
  )
}

function BookCard({ book }: { book: Book }) {
  return (
    <Link to={`/books/${book.id}`} className="group block">
      <BookCover title={book.title} author={book.author} isbn={book.isbn} size="fill"
        className="transition-transform duration-200 group-hover:-translate-y-1" />
      <h2 className="mt-3 line-clamp-2 text-[15px] leading-snug font-semibold text-ink-900">{book.title}</h2>
      <p className="mt-0.5 truncate text-[13px] text-ink-500">{book.author}</p>
      <div className="mt-1"><Availability book={book} /></div>
    </Link>
  )
}

export function Availability({ book }: { book: Book }) {
  if (book.totalCopies === 0) return <span className="text-[13px] text-ink-500">No copies yet</span>
  if (book.availableCopies === 0) {
    return <span className="inline-flex items-center gap-1.5 text-[13px] font-medium text-accent-600"><Dot className="bg-accent-400" />Waitlist</span>
  }
  return (
    <span className="inline-flex items-center gap-1.5 text-[13px] font-medium text-success-700">
      <Dot className="bg-success-400" />{book.availableCopies} of {book.totalCopies} available
    </span>
  )
}

function Dot({ className }: { className: string }) {
  return <span aria-hidden className={`inline-block size-1.5 rounded-full ${className}`} />
}
