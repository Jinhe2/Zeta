import type { UserRole } from './accounts'

const STORAGE_KEY = 'zeta_demo_session'

export interface AuthSession {
  username: string
  role: UserRole
  displayName: string
}

export function readSession(): AuthSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as AuthSession
    if (!parsed.username || !parsed.role) return null
    return parsed
  } catch {
    return null
  }
}

export function writeSession(session: AuthSession): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function clearSession(): void {
  localStorage.removeItem(STORAGE_KEY)
}
