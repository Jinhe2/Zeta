import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../auth/AuthContext'
import { getHomePath, type UserRole } from '../auth/accounts'

export default function RequireAuth({
  role,
  children,
}: {
  role: UserRole
  children: ReactNode
}) {
  const { session } = useAuth()
  const location = useLocation()

  if (!session) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (session.role !== role) {
    return <Navigate to={getHomePath(session.role)} replace />
  }

  return children
}
