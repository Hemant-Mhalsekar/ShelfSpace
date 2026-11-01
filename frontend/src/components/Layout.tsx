import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { notificationsApi } from '../api/endpoints'
import { useAuth } from '../auth/useAuth'

const tierLabel = { STUDENT: 'Student', FACULTY: 'Faculty', GUEST: 'Guest' } as const

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-2 text-sm font-medium transition ${
    isActive ? 'bg-indigo-50 text-indigo-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
  }`

export function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  const links = [
    { to: '/books', label: 'Catalog' },
    { to: '/dashboard', label: 'My dashboard' },
    { to: '/fines', label: 'My fines' },
  ]

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-10 border-b border-slate-200 bg-white/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-4 px-4">
          <Link to="/books" className="flex items-center gap-2 font-bold text-slate-900">
            <img src="/favicon.svg" alt="" className="size-7" />
            ShelfSpace
          </Link>

          <nav className="ml-4 hidden gap-1 md:flex">
            {links.map((l) => <NavLink key={l.to} to={l.to} className={navLinkClass}>{l.label}</NavLink>)}
          </nav>

          <div className="ml-auto flex items-center gap-2">
            <NotificationBell />
            <div className="hidden text-right sm:block">
              <p className="text-sm font-medium leading-tight">{user?.name}</p>
              <p className="text-xs text-slate-500">{user?.role === 'ADMIN' ? 'Librarian' : `${tierLabel[user?.membershipTier ?? 'STUDENT']} member`}</p>
            </div>
            <button
              onClick={() => { logout(); navigate('/login') }}
              className="rounded-md px-3 py-2 text-sm text-slate-600 hover:bg-slate-100"
            >
              Log out
            </button>
            <button
              className="rounded-md p-2 text-slate-600 hover:bg-slate-100 md:hidden"
              aria-label="Menu"
              aria-expanded={menuOpen}
              onClick={() => setMenuOpen((o) => !o)}
            >
              ☰
            </button>
          </div>
        </div>
        {menuOpen && (
          <nav className="flex flex-col gap-1 border-t border-slate-200 px-4 py-2 md:hidden">
            {links.map((l) => (
              <NavLink key={l.to} to={l.to} className={navLinkClass} onClick={() => setMenuOpen(false)}>{l.label}</NavLink>
            ))}
          </nav>
        )}
      </header>

      <main className="mx-auto max-w-6xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}

function NotificationBell() {
  const { data } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: notificationsApi.unreadCount,
    refetchInterval: 60_000,
  })
  const unread = data?.unread ?? 0

  return (
    <Link
      to="/notifications"
      className="relative rounded-md p-2 text-slate-600 hover:bg-slate-100"
      aria-label={`Notifications${unread ? `, ${unread} unread` : ''}`}
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8} className="size-5">
        <path strokeLinecap="round" strokeLinejoin="round"
          d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0" />
      </svg>
      {unread > 0 && (
        <span className="absolute -top-0.5 -right-0.5 flex min-w-4.5 items-center justify-center rounded-full bg-rose-500 px-1 text-[10px] font-bold text-white">
          {unread > 9 ? '9+' : unread}
        </span>
      )}
    </Link>
  )
}
