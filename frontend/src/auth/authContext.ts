import { createContext } from 'react'
import type { RegisterRequest, Role, User } from '../api/types'

export interface AuthContextValue {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (body: RegisterRequest) => Promise<void>
  demoLogin: (role: Role) => Promise<void>
  becomeHost: () => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
