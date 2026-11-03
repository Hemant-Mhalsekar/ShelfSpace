import { Link } from 'react-router'
import type { Book, Recommendation } from '../api/types'
import { Availability } from '../pages/CatalogPage'
import { BookCover } from './BookCover'

/** Horizontal, scrollable shelf of covers (like an Apple Books row). */
function CoverShelf({ books, caption }: { books: Book[]; caption?: (b: Book) => string | undefined }) {
  return (
    <div className="-mx-4 flex snap-x scroll-px-4 gap-5 overflow-x-auto px-4 pb-2 [scrollbar-width:thin]">
      {books.map((book) => (
        <Link key={book.id} to={`/books/${book.id}`} className="group w-32 shrink-0 snap-start sm:w-36">
          <BookCover title={book.title} author={book.author} isbn={book.isbn} size="fill"
            className="transition-transform duration-200 group-hover:-translate-y-1" />
          <p className="mt-2.5 line-clamp-2 text-sm leading-snug font-semibold text-ink-900">{book.title}</p>
          <p className="truncate text-[13px] text-ink-500">{book.author}</p>
          {caption?.(book) && <p className="mt-0.5 line-clamp-2 text-xs text-ink-500">{caption(book)}</p>}
          <div className="mt-0.5"><Availability book={book} /></div>
        </Link>
      ))}
    </div>
  )
}

export function RecommendationGrid({ items }: { items: Recommendation[] }) {
  const reasons = new Map(items.map((r) => [r.book.id, r.reason]))
  return <CoverShelf books={items.map((r) => r.book)} caption={(b) => reasons.get(b.id)} />
}
