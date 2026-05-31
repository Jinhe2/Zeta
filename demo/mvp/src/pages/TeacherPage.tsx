import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import './RolePages.css'

export default function TeacherPage() {
  const { session, logout } = useAuth()

  return (
    <div className="role-page role-page--teacher">
      <header className="role-page__header">
        <div>
          <p className="role-page__eyebrow">教师</p>
          <h1 className="role-page__title">教师界面</h1>
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
          <div className="role-placeholder__icon">📋</div>
          <h2>教师界面</h2>
          <p>该模块尚未实现，后续将提供课程编排、学员进度与逻辑框图批改等功能。</p>
        </div>
      </main>
    </div>
  )
}
