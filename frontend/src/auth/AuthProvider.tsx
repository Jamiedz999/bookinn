import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import * as authApi from '../api/auth'
import { setAuthFailureHandler } from '../api/client'
import type { RegisterRequest, Role, User } from '../api/types'
import { AuthContext } from './authContext'
import type { AuthContextValue } from './authContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setAuthFailureHandler(() => setUser(null))
    // On load there is no in-memory access token; getMe triggers a 401 that the client silently
    // refreshes from the httpOnly cookie, restoring the session across reloads.
    authApi
      .getMe()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
    return () => setAuthFailureHandler(null)
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const result = await authApi.login({ email, password })
    setUser(result.user)
  }, [])

  const register = useCallback(async (body: RegisterRequest) => {
    await authApi.register(body)
  }, [])

  const demoLogin = useCallback(async (role: Role) => {
    const result = await authApi.demoLogin(role)
    setUser(result.user)
  }, [])

  const becomeHost = useCallback(async () => {
    setUser(await authApi.becomeHost())
  }, [])

  const logout = useCallback(async () => {
    await authApi.logout()
    setUser(null)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ user, loading, login, register, demoLogin, becomeHost, logout }),
    [user, loading, login, register, demoLogin, becomeHost, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
