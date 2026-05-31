import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { getHomePath } from '../auth/accounts'

export default function RootRedirect() {
  const { session } = useAuth()
  if (!session) {
    return <Navigate to="/login" replace />
  }
  return <Navigate to={getHomePath(session.role)} replace />
}
