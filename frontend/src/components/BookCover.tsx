import { useState } from 'react'

type Size = 'xs' | 'sm' | 'md' | 'lg' | 'fill'

const SIZES: Record<Size, string> = {
  xs: 'w-10',
  sm: 'w-16',
  md: 'w-28',
  lg: 'w-48 sm:w-56',
  fill: 'w-full',
}

/** Cover colours for books without a real cover (chosen from the title, so a book always looks the same). */
const PLAIN = ['#1c3d5a', '#7a2e2e', '#2f5d50', '#5b3f8c', '#8a5a1f', '#2b2b2e', '#a33d5c', '#36577a']

/**
 * Real cover from Open Library (looked up by ISBN). If there isn't one, draws a plain coloured cover
 * with the title, the way Apple Books shows books without artwork.
 */
export function BookCover({ title, author, isbn, size = 'md', className = '' }: {
  title: string
  author?: string
  isbn?: string
  size?: Size
  className?: string
}) {
  // The plain cover is always drawn; a real cover fades in on top once it has loaded.
  const [state, setState] = useState<'loading' | 'loaded' | 'failed'>(isbn ? 'loading' : 'failed')
  const color = PLAIN[[...title].reduce((h, c) => (h * 31 + c.charCodeAt(0)) % 9973, 7) % PLAIN.length]
  const tiny = size === 'xs' || size === 'sm'

  return (
    <div
      className={`relative flex aspect-[2/3] shrink-0 flex-col justify-between overflow-hidden rounded-[4px] text-white
        shadow-[0_1px_2px_rgba(0,0,0,0.12),0_6px_16px_-6px_rgba(0,0,0,0.25)] ${SIZES[size]} ${className}`}
      style={{ background: `linear-gradient(180deg, ${color}, color-mix(in srgb, ${color} 78%, black))` }}
    >
      <span aria-hidden className={`font-display leading-[1.1] font-bold tracking-tight ${tiny ? 'line-clamp-4 p-1.5 text-[8px]' : size === 'md' ? 'line-clamp-5 p-2.5 text-[13px]' : 'line-clamp-5 p-4 text-xl'}`}>
        {title}
      </span>
      {author && !tiny && (
        <span aria-hidden className={`text-white/75 ${size === 'md' ? 'p-2.5 pt-0 text-[9px]' : 'p-4 pt-0 text-xs'}`}>{author}</span>
      )}

      {isbn && state !== 'failed' && (
        <img
          src={`https://covers.openlibrary.org/b/isbn/${isbn}-${size === 'lg' || size === 'fill' ? 'L' : 'M'}.jpg?default=false`}
          alt=""
          loading="lazy"
          onLoad={() => setState('loaded')}
          onError={() => setState('failed')}
          className={`absolute inset-0 size-full object-cover transition-opacity duration-300 ${state === 'loaded' ? 'opacity-100' : 'opacity-0'}`}
        />
      )}
      <span aria-hidden className="pointer-events-none absolute inset-y-0 left-0 w-[3px] bg-gradient-to-r from-black/25 to-transparent" />
    </div>
  )
}
