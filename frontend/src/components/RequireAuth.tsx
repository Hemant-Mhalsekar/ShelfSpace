import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { PageLoader } from './ui'

/** Sends logged-out users to /login and remembers where they were going. */
export function RequireAuth() {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) return <PageLoader />
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}
