import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import './AdminLayout.css'

/** 业务模块（持续迭代） */
const BUSINESS_NAV = [
  {
    id: 'users',
    label: '用户管理',
    children: [
      { to: '/admin/users/students', label: '学员' },
      { to: '/admin/users/teachers', label: '教师' },
      { to: '/admin/users/admins', label: '管理员' },
    ],
  },
  { to: '/admin/display', label: '屏柜认知' },
  { to: '/admin/binding', label: '屏柜绑定' },
  { to: '/admin/settings', label: '系统设置' },
]

/** 屏柜系统数据（只读） */
const SCREEN_NAV = { to: '/admin/screen/cabinets', label: '屏柜数据' }

function NavGroup({ item }) {
  const location = useLocation()
  const childPaths = item.children.map((child) => child.to)
  const isChildActive = childPaths.some((path) => location.pathname.startsWith(path))
  const [expanded, setExpanded] = useState(isChildActive)

  useEffect(() => {
    if (isChildActive) {
      setExpanded(true)
    }
  }, [isChildActive])

  return (
    <div
      className={`admin-layout__nav-group${expanded ? ' admin-layout__nav-group--expanded' : ''}`}
    >
      <button
        type="button"
        className={`admin-layout__nav-toggle${isChildActive ? ' admin-layout__nav-toggle--active' : ''}`}
        onClick={() => setExpanded((prev) => !prev)}
        aria-expanded={expanded}
      >
        <span className="admin-layout__nav-toggle-label">{item.label}</span>
        <span className="admin-layout__nav-chevron" aria-hidden="true" />
      </button>
      <div className="admin-layout__nav-children">
        <div className="admin-layout__nav-children-inner">
          {item.children.map((child) => (
            <NavLink
              key={child.to}
              to={child.to}
              className={({ isActive }) =>
                `admin-layout__nav-link admin-layout__nav-link--sub${
                  isActive ? ' admin-layout__nav-link--active' : ''
                }`
              }
            >
              {child.label}
            </NavLink>
          ))}
        </div>
      </div>
    </div>
  )
}

export default function AdminLayout() {
  const { session, logout } = useAuth()

  return (
    <div className="admin-layout">
      <aside className="admin-layout__sidebar">
        <div className="admin-layout__brand">
          <p className="admin-layout__eyebrow">Zeta</p>
          <h1 className="admin-layout__title">管理后台</h1>
        </div>
        <nav className="admin-layout__nav">
          <p className="admin-layout__nav-section">业务</p>
          {BUSINESS_NAV.map((item) =>
            item.children ? (
              <NavGroup key={item.id} item={item} />
            ) : (
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
            ),
          )}

          <p className="admin-layout__nav-section">屏柜</p>
          <NavLink
            to={SCREEN_NAV.to}
            className={({ isActive }) =>
              `admin-layout__nav-link admin-layout__nav-link--root${
                isActive ? ' admin-layout__nav-link--active' : ''
              }`
            }
          >
            {SCREEN_NAV.label}
          </NavLink>
        </nav>
      </aside>

      <div className="admin-layout__body">
        <header className="admin-layout__header">
          <div className="admin-layout__header-meta">
            <span className="admin-layout__user">{session?.displayName}</span>
            <span className="admin-layout__role">管理员</span>
          </div>
          <button
            type="button"
            className="admin-layout__logout"
            onClick={() => {
              logout().then(() => {
                window.location.href = '/login'
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
