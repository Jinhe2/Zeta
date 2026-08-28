import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import RoleIdentityBadge from './RoleIdentityBadge'
import './AdminLayout.css'

const TEACHER_NAV = [
  { to: '/teacher/students', label: '学员管理' },
  { to: '/teacher/baselines', label: '基准管理' },
  { to: '/teacher/profile', label: '个人中心' },
]

export default function TeacherLayout() {
  const { session, logout } = useAuth()
  const navigate = useNavigate()
  const displayName = session?.displayName || '教师'

  return (
    <div className="admin-layout teacher-layout">
      <aside className="admin-layout__sidebar">
        <div className="admin-layout__brand">
          <p className="admin-layout__eyebrow">Zeta</p>
          <h1 className="admin-layout__title">教师工作台</h1>
        </div>
        <nav className="admin-layout__nav">
          <p className="admin-layout__nav-section">教学</p>
          {TEACHER_NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `admin-layout__nav-link admin-layout__nav-link--root${
                  isActive ? ' admin-layout__nav-link--active' : ''
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="admin-layout__body">
        <header className="admin-layout__header">
          <RoleIdentityBadge displayName={displayName} roleLabel="教师" />
          <button
            type="button"
            className="admin-layout__logout"
            onClick={() => {
              logout().then(() => {
                navigate('/login', { replace: true })
              })
            }}
          >
            退出登录
          </button>
        </header>
        <main className="admin-layout__main">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
