import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { SESSION_EXPIRED_EVENT, tokens } from '../api/client'
import { authApi } from '../api/endpoints'
import type { AuthResponse, User } from '../api/types'
import { AuthContext, type AuthState } from './context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(() => tokens.access !== null)

  // Restore the session from a saved token
  useEffect(() => {
    if (!tokens.access) return
    authApi.me()
      .then(setUser)
      .catch(() => tokens.clear())
      .finally(() => setLoading(false))
  }, [])

  const logout = useCallback(() => {
    tokens.clear()
    setUser(null)
    queryClient.clear()
  }, [queryClient])

  // The API client fires this when a refresh token is rejected
  useEffect(() => {
    window.addEventListener(SESSION_EXPIRED_EVENT, logout)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, logout)
  }, [logout])

  const start = useCallback((auth: AuthResponse) => {
    tokens.save(auth)
    queryClient.clear()
    setUser(auth.user)
    return auth.user
  }, [queryClient])

  const value = useMemo<AuthState>(() => ({
    user,
    loading,
    login: async (email, password) => start(await authApi.login(email, password)),
    register: async (data) => start(await authApi.register(data)),
    logout,
    setUser,
  }), [user, loading, start, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
