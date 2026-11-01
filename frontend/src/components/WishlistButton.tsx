import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { wishlistApi } from '../api/endpoints'
import { errorMessage } from '../lib/ui-helpers'
import { useToast } from './toast/useToast'

/** ♡ / ♥ toggle. Reads the shared wishlist query, so every button on the page stays in sync. */
export function WishlistButton({ bookId, title, compact = false }: { bookId: number; title: string; compact?: boolean }) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const wishlist = useQuery({ queryKey: ['wishlist'], queryFn: wishlistApi.list })
  const saved = wishlist.data?.some((w) => w.book.id === bookId) ?? false

  const toggle = useMutation({
    mutationFn: async () => { if (saved) await wishlistApi.remove(bookId); else await wishlistApi.add(bookId) },
    onSuccess: () => {
      toast.show(saved ? `Removed “${title}” from your wishlist` : `Saved “${title}” to your wishlist. We'll tell you when a copy is free.`)
      void queryClient.invalidateQueries({ queryKey: ['wishlist'] })
      void queryClient.invalidateQueries({ queryKey: ['recommendations'] })
    },
    onError: (e) => toast.show(errorMessage(e), 'error'),
  })

  return (
    <button
      type="button"
      onClick={(e) => { e.preventDefault(); toggle.mutate() }}
      disabled={toggle.isPending || wishlist.isPending}
      aria-pressed={saved}
      aria-label={saved ? `Remove ${title} from wishlist` : `Save ${title} to wishlist`}
      className={`inline-flex items-center gap-1.5 rounded-lg text-sm font-semibold transition disabled:opacity-50 ${
        compact ? 'p-1.5' : 'px-3.5 py-2 ring-1 ring-inset'
      } ${saved ? 'text-rose-600 ring-rose-200 hover:bg-rose-50' : 'text-slate-600 ring-slate-300 hover:bg-slate-50'}`}
    >
      <svg viewBox="0 0 24 24" className="size-5" fill={saved ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth={1.8} aria-hidden>
        <path strokeLinecap="round" strokeLinejoin="round"
          d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z" />
      </svg>
      {!compact && (saved ? 'Saved' : 'Save for later')}
    </button>
  )
}
