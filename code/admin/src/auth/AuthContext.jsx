import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react'
import { api, clearTokens, getAccessToken, getRefreshToken, setTokens } from '../api/client'

const AuthContext = createContext(null)

const SESSION_KEY = 'zeta_session'

function readSession() {
  try {
    const raw = localStorage.getItem(SESSION_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function writeSession(session) {
  if (session) {
    localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  } else {
    localStorage.removeItem(SESSION_KEY)
  }
}

function sessionFromProfile(profile) {
  return {
    username: profile.username,
    displayName: profile.displayName,
    role: profile.role,
    homePath: profile.homePath,
  }
}

function sessionFromLogin(result) {
  return {
    username: result.username,
    displayName: result.displayName,
    role: result.role,
    homePath: result.homePath,
  }
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(null)
  const [initializing, setInitializing] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function bootstrap() {
      const hasToken = getAccessToken() || getRefreshToken()
      if (!hasToken) {
        writeSession(null)
        if (!cancelled) {
          setSession(null)
          setInitializing(false)
        }
        return
      }

      try {
        let profile
        try {
          profile = await api.me()
        } catch {
          await api.refresh()
          profile = await api.me()
        }
        if (!cancelled) {
          const next = sessionFromProfile(profile)
          writeSession(next)
          setSession(next)
        }
      } catch {
        clearTokens()
        writeSession(null)
        if (!cancelled) setSession(null)
      } finally {
        if (!cancelled) setInitializing(false)
      }
    }

    bootstrap()
    return () => {
      cancelled = true
    }
  }, [])

  const login = useCallback(async (username, password) => {
    const result = await api.login(username.trim(), password)
    setTokens(result.accessToken, result.refreshToken)
    const next = sessionFromLogin(result)
    writeSession(next)
    setSession(next)
    return result.homePath
  }, [])

  const logout = useCallback(async () => {
    try {
      await api.logout()
    } catch {
      // ignore
    }
    clearTokens()
    writeSession(null)
    setSession(null)
  }, [])

  const hasRole = useCallback((role) => session?.role === role, [session])

  const value = useMemo(
    () => ({ session, initializing, login, logout, hasRole }),
    [session, initializing, login, logout, hasRole],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
