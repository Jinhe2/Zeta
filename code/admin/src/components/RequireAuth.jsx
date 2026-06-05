import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const HOME_BY_ROLE = {
  STUDENT: '/student',
  TEACHER: '/teacher',
  ADMIN: '/admin',
}

export default function RequireAuth({ role, children }) {
  const { session } = useAuth()
  const location = useLocation()

  if (!session) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (role && session.role !== role) {
    return <Navigate to={HOME_BY_ROLE[session.role] || '/'} replace />
  }

  return children
}
