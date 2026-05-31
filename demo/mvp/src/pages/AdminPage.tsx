import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import './RolePages.css'

export default function AdminPage() {
  const { session, logout } = useAuth()

  return (
    <div className="role-page role-page--admin">
      <header className="role-page__header">
        <div>
          <p className="role-page__eyebrow">管理员</p>
          <h1 className="role-page__title">管理员界面</h1>
        </div>
        <div className="role-page__actions">
          <span className="role-page__user">{session?.displayName}</span>
          <Link to="/login" className="role-page__btn" onClick={logout}>
            退出登录
          </Link>
        </div>
      </header>

      <main className="role-page__main">
        <div className="role-placeholder">
          <div className="role-placeholder__icon">⚙</div>
          <h2>管理员界面</h2>
          <p>该模块尚未实现，后续将提供用户管理、样本配置与系统设置等功能。</p>
        </div>
      </main>
    </div>
  )
}
