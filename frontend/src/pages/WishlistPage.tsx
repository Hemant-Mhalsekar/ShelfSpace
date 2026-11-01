import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { wishlistApi } from '../api/endpoints'
import { WishlistButton } from '../components/WishlistButton'
import { Alert, Card, EmptyState, PageHeader, PageLoader } from '../components/ui'
import { formatDate } from '../lib/format'
import { errorMessage } from '../lib/ui-helpers'
import { Availability, BookCover } from './CatalogPage'

export function WishlistPage() {
  const wishlist = useQuery({ queryKey: ['wishlist'], queryFn: wishlistApi.list })

  return (
    <>
      <PageHeader title="Wishlist" subtitle="Books you've saved for later. We'll notify you when one that's all out comes back." />
      {wishlist.isPending ? <PageLoader />
        : wishlist.isError ? <Alert>{errorMessage(wishlist.error)}</Alert>
          : wishlist.data.length === 0 ? (
            <EmptyState title="Your wishlist is empty">
              Tap <span className="text-rose-500">♡ Save for later</span> on any book. <Link to="/books" className="text-indigo-600 hover:underline">Browse the catalog</Link>
            </EmptyState>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2">
              {wishlist.data.map(({ book, addedAt }) => (
                <Card key={book.id} className="flex gap-4 p-4">
                  <Link to={`/books/${book.id}`}><BookCover title={book.title} /></Link>
                  <div className="flex min-w-0 flex-1 flex-col">
                    <div className="flex items-start gap-2">
                      <Link to={`/books/${book.id}`} className="line-clamp-2 flex-1 font-semibold hover:text-indigo-700">{book.title}</Link>
                      <WishlistButton bookId={book.id} title={book.title} compact />
                    </div>
                    <p className="truncate text-sm text-slate-500">{book.author}</p>
                    <p className="mt-1 text-xs text-slate-400">Saved {formatDate(addedAt)}</p>
                    <div className="mt-auto pt-3"><Availability book={book} /></div>
                  </div>
                </Card>
              ))}
            </div>
          )}
    </>
  )
}
