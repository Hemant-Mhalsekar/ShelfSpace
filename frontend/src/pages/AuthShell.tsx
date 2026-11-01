import type { ReactNode } from 'react'
import { ServerWakeBanner } from '../components/ServerWakeBanner'

/** Fanned stack of covers above the form; real covers from Open Library for a few well-known books. */
const FAN = [
  { isbn: '9780132350884', rotate: '-12deg', x: '-88px', color: '#1c3d5a' },
  { isbn: '9780735211292', rotate: '-4deg', x: '-30px', color: '#7a2e2e' },
  { isbn: '9781449373320', rotate: '4deg', x: '30px', color: '#2f5d50' },
  { isbn: '9780134685991', rotate: '12deg', x: '88px', color: '#5b3f8c' },
]

export function AuthShell({ title, subtitle, children }: { title: string; subtitle: ReactNode; children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-canvas-100 px-5 py-12">
      <div aria-hidden className="relative mb-8 h-36 w-72">
        {FAN.map((b) => (
          <img
            key={b.isbn}
            src={`https://covers.openlibrary.org/b/isbn/${b.isbn}-M.jpg?default=false`}
            alt=""
            onError={(e) => { e.currentTarget.removeAttribute('src') }}
            className="absolute bottom-0 left-1/2 h-32 w-[86px] rounded-[4px] object-cover shadow-[0_8px_24px_-8px_rgba(0,0,0,0.35)]"
            style={{ transform: `translateX(calc(-50% + ${b.x})) rotate(${b.rotate})`, background: b.color }}
          />
        ))}
      </div>

      <div className="w-full max-w-[380px]">
        <div className="text-center">
          <h1 className="font-display text-[28px] leading-tight font-bold tracking-tight">{title}</h1>
          <p className="mt-1.5 text-[15px] text-ink-500">{subtitle}</p>
        </div>
        <div className="mt-7 rounded-3xl bg-canvas-50 p-6 shadow-[0_1px_2px_rgba(0,0,0,0.04),0_12px_40px_-12px_rgba(0,0,0,0.12)]">
          <ServerWakeBanner />
          {children}
        </div>
        <p className="mt-8 text-center text-xs text-ink-500">ShelfSpace · Spring Boot, React and MySQL</p>
      </div>
    </div>
  )
}
