import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { booksApi } from '../api/endpoints'
import type { Book } from '../api/types'
import { Alert, Badge, Card, EmptyState, PageHeader, PageLoader, Pagination } from '../components/ui'
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
    queryFn: () => booksApi.search({ q, category, availableOnly, page, size: 12, sort }),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <PageHeader
        title="Catalog"
        subtitle={books.data ? `${books.data.totalElements} book${books.data.totalElements === 1 ? '' : 's'}` : 'Search by title, author or ISBN'}
      />

      <div className="mb-6 grid gap-3 sm:grid-cols-[1fr_auto_auto_auto] sm:items-center">
        <input
          type="search"
          className={inputClass}
          placeholder="Search by title, author or ISBN…"
          value={text}
          onChange={(e) => setText(e.target.value)}
          aria-label="Search books"
        />
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
        <label className="flex items-center gap-2 text-sm text-slate-700">
          <input
            type="checkbox"
            className="size-4 rounded border-slate-300 text-indigo-600"
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
          <div className={`grid gap-4 sm:grid-cols-2 lg:grid-cols-3 ${books.isPlaceholderData ? 'opacity-60' : ''}`}>
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
    <Link to={`/books/${book.id}`} className="group">
      <Card className="flex h-full gap-4 p-4 transition group-hover:border-indigo-300 group-hover:shadow-md">
        <BookCover title={book.title} />
        <div className="flex min-w-0 flex-1 flex-col">
          <h2 className="line-clamp-2 font-semibold text-slate-900 group-hover:text-indigo-700">{book.title}</h2>
          <p className="mt-0.5 truncate text-sm text-slate-500">{book.author}</p>
          <p className="mt-1 text-xs text-slate-400">{book.category}{book.publicationYear ? ` · ${book.publicationYear}` : ''}</p>
          <div className="mt-auto pt-3">
            <Availability book={book} />
          </div>
        </div>
      </Card>
    </Link>
  )
}

export function Availability({ book }: { book: Book }) {
  if (book.totalCopies === 0) return <Badge>No copies yet</Badge>
  if (book.availableCopies === 0) return <Badge color="amber">All {book.totalCopies} out · reservable</Badge>
  return <Badge color="green">{book.availableCopies} of {book.totalCopies} available</Badge>
}

/** A simple generated "cover": the colour comes from the title, so each book looks different. */
export function BookCover({ title, large = false }: { title: string; large?: boolean }) {
  const hue = [...title].reduce((h, c) => (h * 31 + c.charCodeAt(0)) % 360, 7)
  return (
    <div
      aria-hidden
      className={`flex shrink-0 items-end rounded-md p-2 font-semibold leading-tight text-white shadow-inner
        ${large ? 'h-56 w-40 text-base' : 'h-24 w-16 text-[9px]'}`}
      style={{ background: `linear-gradient(160deg, hsl(${hue} 55% 45%), hsl(${(hue + 40) % 360} 60% 30%))` }}
    >
      <span className="line-clamp-4">{title}</span>
    </div>
  )
}
