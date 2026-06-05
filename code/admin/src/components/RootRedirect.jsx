import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const HOME_BY_ROLE = {
  STUDENT: '/student',
  TEACHER: '/teacher',
  ADMIN: '/admin',
}

export default function RootRedirect() {
  const { session, initializing } = useAuth()

  if (initializing) {
    return (
      <div style={{ padding: 40, textAlign: 'center', color: '#8fa3bf' }}>正在加载…</div>
    )
  }

  if (!session) return <Navigate to="/login" replace />
  return <Navigate to={HOME_BY_ROLE[session.role] || '/login'} replace />
}
