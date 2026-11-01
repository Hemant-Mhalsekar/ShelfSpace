import { Navigate } from 'react-router'
import { useAuth } from '../auth/useAuth'

export function HomeRedirect() {
  const { user } = useAuth()
  return <Navigate to={user?.role === 'ADMIN' ? '/admin' : '/books'} replace />
}
