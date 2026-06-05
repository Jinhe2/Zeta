import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import './RoleLayout.css'

export default function RoleLayout({ eyebrow, title, children }) {
  const { session, logout } = useAuth()

  return (
    <div className="role-layout">
      <header className="role-layout__header">
        <div>
          <p className="role-layout__eyebrow">{eyebrow}</p>
          <h1 className="role-layout__title">{title}</h1>
        </div>
        <div className="role-layout__actions">
          <span className="role-layout__user">{session?.displayName}</span>
          <Link
            to="/login"
            className="role-layout__btn"
            onClick={(e) => {
              e.preventDefault()
              logout().then(() => {
                window.location.href = '/login'
              })
            }}
          >
            退出登录
          </Link>
        </div>
      </header>
      <main className="role-layout__main">{children}</main>
    </div>
  )
}
