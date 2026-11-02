import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { adminLoansApi, notificationsApi } from '../api/endpoints'
import { useAuth } from '../auth/useAuth'

const tierLabel = { STUDENT: 'Student', FACULTY: 'Faculty', GUEST: 'Guest' } as const

type NavItem = { to: string; label: string; icon: string; end?: boolean; badge?: boolean }

/** 24px outline icon paths */
const ICONS = {
  browse: 'M21 21l-5.2-5.2M10.5 18a7.5 7.5 0 1 0 0-15 7.5 7.5 0 0 0 0 15Z',
  books: 'M12 6.042A8.967 8.967 0 0 0 6 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 0 1 6 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 0 1 6-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0 0 18 18a8.967 8.967 0 0 0-6 2.292m0-14.25v14.25',
  heart: 'M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z',
  fines: 'M2.25 8.25h19.5M2.25 9h19.5m-16.5 5.25h6m-6 2.25h3m-3.75 3h15a2.25 2.25 0 0 0 2.25-2.25V6.75A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25v10.5A2.25 2.25 0 0 0 4.5 19.5Z',
  bell: 'M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0',
  menu: 'M4 7h16M4 12h16M4 17h16',
  person: 'M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z',
  close: 'M6 6l12 12M18 6 6 18',
}

function Icon({ d, className = 'size-5' }: { d: string; className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8} className={className} aria-hidden>
      <path strokeLinecap="round" strokeLinejoin="round" d={d} />
    </svg>
  )
}

const MEMBER_NAV: NavItem[] = [
  { to: '/books', label: 'Browse', icon: ICONS.browse },
  { to: '/dashboard', label: 'My Books', icon: ICONS.books },
  { to: '/wishlist', label: 'Want to Read', icon: ICONS.heart },
  { to: '/fines', label: 'Fines', icon: ICONS.fines },
]

const ADMIN_NAV: NavItem[] = [
  { to: '/admin', label: 'Overview', icon: '', end: true },
  { to: '/admin/loans', label: 'Loan Desk', icon: '', badge: true },
  { to: '/admin/books', label: 'Books', icon: '' },
  { to: '/admin/members', label: 'Members', icon: '' },
  { to: '/admin/fines', label: 'Fines', icon: '' },
  { to: '/admin/rules', label: 'Rules', icon: '' },
  { to: '/books', label: 'Catalog', icon: '' },
]

const topLink = ({ isActive }: { isActive: boolean }) =>
  `inline-flex items-center rounded-full px-3 py-1.5 text-sm transition-colors duration-150 ${
    isActive ? 'bg-ink-900 font-semibold text-white' : 'text-ink-600 hover:bg-canvas-200 hover:text-ink-900'
  }`

export function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const isAdmin = user?.role === 'ADMIN'
  const nav = isAdmin ? ADMIN_NAV : MEMBER_NAV

  return (
    <div className="min-h-screen pb-20 lg:pb-0">
      <header className="sticky top-0 z-20 border-b border-black/[0.06] bg-white/80 backdrop-blur-xl backdrop-saturate-150">
        <div className="mx-auto flex h-14 max-w-6xl items-center gap-3 px-4">
          <Link to="/" className="flex items-center gap-2">
            <img src="/favicon.svg" alt="" className="size-7 rounded-[7px]" />
            <span className="text-[17px] font-bold tracking-tight">ShelfSpace</span>
          </Link>

          <nav className="ml-6 hidden items-center gap-1 lg:flex">
            {nav.map((l) => (
              <NavLink key={l.to} to={l.to} end={l.end} className={topLink}>
                {l.label}{l.badge && <PendingBadge />}
              </NavLink>
            ))}
          </nav>

          <div className="ml-auto flex items-center gap-1">
            <NotificationBell />
            <Link
              to="/account"
              className="hidden rounded-xl px-2 py-1 text-right transition-colors hover:bg-canvas-200 sm:block"
              title="Account settings"
            >
              <p className="text-[13px] leading-tight font-semibold">{user?.name}</p>
              <p className="text-xs text-ink-500">{isAdmin ? 'Librarian' : `${tierLabel[user?.membershipTier ?? 'STUDENT']} member`}</p>
            </Link>
            <Link to="/account" aria-label="Account" className="rounded-full p-2 text-ink-700 transition-colors hover:bg-canvas-200 sm:hidden">
              <Icon d={ICONS.person} />
            </Link>
            <button
              onClick={() => { logout(); navigate('/login') }}
              className="rounded-full px-3 py-1.5 text-sm font-medium text-accent-600 transition-colors hover:bg-accent-50"
            >
              Sign Out
            </button>
            {isAdmin && (
              <button
                className="rounded-full p-2 text-ink-700 hover:bg-canvas-200 lg:hidden"
                aria-label="Menu"
                aria-expanded={menuOpen}
                onClick={() => setMenuOpen((o) => !o)}
              >
                <Icon d={menuOpen ? ICONS.close : ICONS.menu} />
              </button>
            )}
          </div>
        </div>
        {isAdmin && menuOpen && (
          <nav className="flex flex-wrap gap-1 border-t border-black/[0.06] px-4 py-3 lg:hidden">
            {nav.map((l) => (
              <NavLink key={l.to} to={l.to} end={l.end} className={topLink} onClick={() => setMenuOpen(false)}>
                {l.label}{l.badge && <PendingBadge />}
              </NavLink>
            ))}
          </nav>
        )}
      </header>

      <main className="mx-auto max-w-6xl px-4 py-8 sm:py-10">
        <Outlet />
      </main>

      {/* Members on phones get an app-style tab bar */}
      {!isAdmin && (
        <nav className="fixed inset-x-0 bottom-0 z-20 grid grid-cols-4 border-t border-black/[0.08] bg-white/85 pb-[env(safe-area-inset-bottom)] backdrop-blur-xl lg:hidden">
          {nav.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              className={({ isActive }) => `flex flex-col items-center gap-0.5 py-2 text-[10px] font-medium ${isActive ? 'text-accent-600' : 'text-ink-500'}`}
            >
              <Icon d={l.icon} className="size-6" />
              {l.label}
            </NavLink>
          ))}
        </nav>
      )}
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
      className="relative rounded-full p-2 text-ink-700 transition-colors hover:bg-canvas-200"
      aria-label={`Notifications${unread ? `, ${unread} unread` : ''}`}
    >
      <Icon d={ICONS.bell} />
      {unread > 0 && (
        <span className="absolute top-0.5 right-0.5 flex min-w-4 items-center justify-center rounded-full bg-danger-500 px-1 text-[10px] font-bold text-white ring-2 ring-white">
          {unread > 9 ? '9+' : unread}
        </span>
      )}
    </Link>
  )
}

/** Number of borrow requests waiting for a librarian. */
function PendingBadge() {
  const { data } = useQuery({
    queryKey: ['loans', 'admin', 'pending-count'],
    queryFn: () => adminLoansApi.search({ status: 'REQUESTED' }),
    refetchInterval: 60_000,
  })
  const n = data?.totalElements ?? 0
  if (!n) return null
  return <span className="ml-1.5 rounded-full bg-danger-500 px-1.5 text-[11px] leading-[18px] font-semibold text-white">{n}</span>
}
