import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { findDemoAccount, getHomePath, type UserRole } from './accounts'
import { clearSession, readSession, writeSession, type AuthSession } from './authStorage'

interface AuthContextValue {
  session: AuthSession | null
  login: (username: string, password: string) => { ok: true; redirectTo: string } | { ok: false; message: string }
  logout: () => void
  hasRole: (role: UserRole) => boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => readSession())

  const login = useCallback((username: string, password: string) => {
    const account = findDemoAccount(username, password)
    if (!account) {
      return { ok: false as const, message: '用户名或密码错误' }
    }
    const next: AuthSession = {
      username: account.username,
      role: account.role,
      displayName: account.displayName,
    }
    writeSession(next)
    setSession(next)
    return { ok: true as const, redirectTo: getHomePath(account.role) }
  }, [])

  const logout = useCallback(() => {
    clearSession()
    setSession(null)
  }, [])

  const hasRole = useCallback(
    (role: UserRole) => session?.role === role,
    [session],
  )

  const value = useMemo(
    () => ({ session, login, logout, hasRole }),
    [session, login, logout, hasRole],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
