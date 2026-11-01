import { Link } from 'react-router'
import type { Recommendation } from '../api/types'
import { Availability, BookCover } from '../pages/CatalogPage'
import { Card } from './ui'

/** Small book cards with the reason each one was suggested. */
export function RecommendationGrid({ items }: { items: Recommendation[] }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {items.map(({ book, reason }) => (
        <Link key={book.id} to={`/books/${book.id}`} className="group">
          <Card className="flex h-full gap-3 p-3 transition group-hover:border-indigo-300 group-hover:shadow-md">
            <BookCover title={book.title} />
            <div className="flex min-w-0 flex-1 flex-col">
              <p className="line-clamp-2 text-sm font-semibold group-hover:text-indigo-700">{book.title}</p>
              <p className="truncate text-xs text-slate-500">{book.author}</p>
              <p className="mt-1 line-clamp-2 text-xs text-slate-400 italic">{reason}</p>
              <div className="mt-auto pt-2"><Availability book={book} /></div>
            </div>
          </Card>
        </Link>
      ))}
    </div>
  )
}
