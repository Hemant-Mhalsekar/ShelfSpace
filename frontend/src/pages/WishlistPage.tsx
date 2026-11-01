import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { wishlistApi } from '../api/endpoints'
import { BookCover } from '../components/BookCover'
import { WishlistButton } from '../components/WishlistButton'
import { Alert, EmptyState, PageHeader, PageLoader } from '../components/ui'
import { errorMessage } from '../lib/ui-helpers'
import { Availability } from './CatalogPage'

export function WishlistPage() {
  const wishlist = useQuery({ queryKey: ['wishlist'], queryFn: wishlistApi.list })

  return (
    <>
      <PageHeader
        title="Want to Read"
        subtitle={wishlist.data?.length ? `${wishlist.data.length} book${wishlist.data.length === 1 ? '' : 's'} · we'll let you know when one is back on the shelf` : undefined}
      />
      {wishlist.isPending ? <PageLoader />
        : wishlist.isError ? <Alert>{errorMessage(wishlist.error)}</Alert>
          : wishlist.data.length === 0 ? (
            <EmptyState title="Nothing here yet">
              Tap the heart on any book to save it. <Link to="/books" className="font-semibold text-accent-600">Browse the catalog ›</Link>
            </EmptyState>
          ) : (
            <div className="grid grid-cols-2 gap-x-5 gap-y-8 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
              {wishlist.data.map(({ book }) => (
                <div key={book.id} className="group relative">
                  <Link to={`/books/${book.id}`} className="block">
                    <BookCover title={book.title} author={book.author} isbn={book.isbn} size="fill"
                      className="transition-transform duration-200 group-hover:-translate-y-1" />
                    <p className="mt-3 line-clamp-2 text-[15px] leading-snug font-semibold">{book.title}</p>
                    <p className="truncate text-[13px] text-ink-500">{book.author}</p>
                  </Link>
                  <div className="mt-1"><Availability book={book} /></div>
                  <div className="absolute top-2 right-2 rounded-full bg-white/90 shadow-sm backdrop-blur">
                    <WishlistButton bookId={book.id} title={book.title} compact />
                  </div>
                </div>
              ))}
            </div>
          )}
    </>
  )
}
