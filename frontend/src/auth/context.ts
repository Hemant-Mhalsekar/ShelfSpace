import { createContext } from 'react'
import type { User } from '../api/types'

export interface AuthState {
  user: User | null
  /** True while we check a saved token on first load. */
  loading: boolean
  login: (email: string, password: string) => Promise<User>
  register: (data: { name: string; email: string; phone?: string; password: string }) => Promise<User>
  logout: () => void
}

export const AuthContext = createContext<AuthState | null>(null)
