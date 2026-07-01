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

  // ADMIN 拥有学员全部权限，可进入学员界面
  if (role && session.role !== role && session.role !== 'ADMIN') {
    return <Navigate to={HOME_BY_ROLE[session.role] || '/'} replace />
  }

  return children
}
