import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import './TabletShell.css'

export default function StudentPlaceholderPage({ title, description }) {
  const navigate = useNavigate()
  const { logout } = useAuth()

  return (
    <div className="tablet-shell">
      <header className="tablet-shell__header">
        <button type="button" className="tablet-shell__back" onClick={() => navigate('/student')}>
          ← 返回首页
        </button>
        <h1>{title}</h1>
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
