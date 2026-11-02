import type { ReactNode } from 'react'
import { ServerWakeBanner } from '../components/ServerWakeBanner'

/** Fanned stack of plain, untitled covers above the form (decoration only). */
const FAN = [
  { color: '#1c3d5a', rotate: '-12deg', x: '-88px' },
  { color: '#7a2e2e', rotate: '-4deg', x: '-30px' },
  { color: '#2f5d50', rotate: '4deg', x: '30px' },
  { color: '#5b3f8c', rotate: '12deg', x: '88px' },
]

export function AuthShell({ title, subtitle, children }: { title: string; subtitle: ReactNode; children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-canvas-100 px-5 py-12">
      <div aria-hidden className="relative mb-8 h-36 w-72">
        {FAN.map((b) => (
          <div
            key={b.color}
            className="absolute bottom-0 left-1/2 h-32 w-[86px] overflow-hidden rounded-[4px] shadow-[0_8px_24px_-8px_rgba(0,0,0,0.35)]"
            style={{
              transform: `translateX(calc(-50% + ${b.x})) rotate(${b.rotate})`,
              background: `linear-gradient(180deg, ${b.color}, color-mix(in srgb, ${b.color} 78%, black))`,
            }}
          >
            <span className="absolute inset-y-0 left-0 w-[3px] bg-gradient-to-r from-black/25 to-transparent" />
          </div>
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
