import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import './TabletShell.css'

export default function StudentPlaceholderPage({ title, description }) {
  const navigate = useNavigate()
  const location = useLocation()
  const { logout } = useAuth()
  const fromCoach = location.pathname.startsWith('/student/modes/coach')
  const backTo = fromCoach ? '/student/modes/coach' : '/student'

  return (
    <div className="tablet-shell">
      <header className="tablet-shell__header">
        <div className="tablet-shell__header-left">
          <button type="button" className="tablet-shell__back" onClick={() => navigate(backTo)}>
            ← 返回上级
          </button>
          {fromCoach && (
            <button type="button" className="tablet-shell__home" onClick={() => navigate('/student')}>
              返回首页
            </button>
          )}
        </div>
        <h1>{title}</h1>
        <div className="tablet-shell__header-actions">
          <button
            type="button"
            className="tablet-shell__logout"
            onClick={async () => {
              await logout()
              navigate('/login', { replace: true })
            }}
          >
            退出登录
          </button>
        </div>
      </header>
      <main className="tablet-shell__main">
        <div className="tablet-shell__placeholder">
          <h2>{title}</h2>
          <p>{description || '功能开发中，敬请期待。'}</p>
        </div>
      </main>
    </div>
  )
}
