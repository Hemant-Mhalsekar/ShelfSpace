import type { ReactNode } from 'react'
import { ServerWakeBanner } from '../components/ServerWakeBanner'

export function AuthShell({ title, subtitle, children }: { title: string; subtitle: ReactNode; children: ReactNode }) {
  return (
    <div className="flex min-h-screen">
      <div className="hidden w-1/2 flex-col justify-between bg-indigo-600 p-12 text-white lg:flex">
        <div className="flex items-center gap-2 text-lg font-bold">
          <img src="/favicon.svg" alt="" className="size-8 rounded-md ring-2 ring-white/30" /> ShelfSpace
        </div>
        <div>
          <p className="text-3xl font-semibold leading-snug">Find it, borrow it, read it.</p>
          <p className="mt-3 max-w-md text-indigo-100">
            Search the catalog, borrow books, join waiting lists and keep track of due dates, all in one place.
          </p>
        </div>
        <p className="text-sm text-indigo-200">A Spring Boot + React portfolio project</p>
      </div>
      <div className="flex flex-1 items-center justify-center px-6 py-12">
        <div className="w-full max-w-sm">
          <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
          <p className="mt-1 mb-8 text-sm text-slate-500">{subtitle}</p>
          <ServerWakeBanner />
          {children}
        </div>
      </div>
    </div>
  )
}
