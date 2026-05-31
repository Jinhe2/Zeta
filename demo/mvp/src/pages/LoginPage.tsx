import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { getHomePath } from '../auth/accounts'
import { useAuth } from '../auth/AuthContext'
import './LoginPage.css'

export default function LoginPage() {
  const { session, login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)

  if (session) {
    return <Navigate to={getHomePath(session.role)} replace />
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    const result = login(username, password)
    if (!result.ok) {
      setError(result.message)
      return
    }
    navigate(result.redirectTo, { replace: true })
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <header className="login-card__header">
          <h1 className="login-card__title">继电保护逻辑教学系统</h1>
          <p className="login-card__subtitle">请登录后继续</p>
        </header>

        <form className="login-form" onSubmit={handleSubmit}>
          {error && <div className="login-form__error">{error}</div>}
          <label className="login-field">
            <span>用户名</span>
            <input
              type="text"
              value={username}
              autoComplete="username"
              placeholder="请输入用户名"
              onChange={(e) => setUsername(e.target.value)}
            />
          </label>
          <label className="login-field">
            <span>密码</span>
            <input
              type="password"
              value={password}
              autoComplete="current-password"
              placeholder="请输入密码"
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>
          <button type="submit" className="login-form__submit">
            登录
          </button>
        </form>

        <p className="login-card__hint">
          测试账号：学员 <code>student</code> · 教师 <code>teacher</code> · 管理员{' '}
          <code>admin</code>，密码均为 <code>123456</code>
        </p>
      </div>
    </div>
  )
}
