import { createContext } from 'react'
import type { User } from '../api/types'

export interface AuthState {
  user: User | null
  /** True while we check a saved token on first load. */
  loading: boolean
  login: (email: string, password: string) => Promise<User>
  register: (data: { name: string; email: string; phone?: string; password: string }) => Promise<User>
  logout: () => void
  /** Replace the signed-in user after a profile edit. */
  setUser: (user: User) => void
}

export const AuthContext = createContext<AuthState | null>(null)
