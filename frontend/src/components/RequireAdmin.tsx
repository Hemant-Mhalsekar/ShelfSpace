import { Navigate, Outlet } from 'react-router'
import { useAuth } from '../auth/useAuth'

/** Librarian-only pages. Members are sent to their own dashboard. */
export function RequireAdmin() {
  const { user } = useAuth()
  if (user?.role !== 'ADMIN') return <Navigate to="/dashboard" replace />
  return <Outlet />
}
