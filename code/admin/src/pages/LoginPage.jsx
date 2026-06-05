import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import './LoginPage.css'

const HOME_BY_ROLE = {
  STUDENT: '/student',
  TEACHER: '/teacher',
  ADMIN: '/admin',
}

function validateForm(username, password) {
  if (!username.trim()) return '请输入用户名'
  if (!password) return '请输入密码'
  return null
}

export default function LoginPage() {
  const { session, initializing, login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  if (initializing) {
    return (
      <div className="login-page">
        <div className="login-card login-card--loading">
          <p>正在加载…</p>
        </div>
      </div>
    )
  }

  if (session) {
    return <Navigate to={HOME_BY_ROLE[session.role] || '/'} replace />
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const validationError = validateForm(username, password)
    if (validationError) {
      setError(validationError)
      return
    }

    setError(null)
    setLoading(true)
    try {
      const homePath = await login(username, password)
      navigate(homePath, { replace: true })
    } catch (err) {
      setError(err.message || '登录失败，请检查用户名和密码')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-page__backdrop" aria-hidden />

      <div className="login-card">
        <header className="login-card__header">
          <div className="login-card__logo">Zeta</div>
          <h1 className="login-card__title">继电保护逻辑教学系统</h1>
          <p className="login-card__subtitle">使用用户名和密码登录</p>
        </header>

        <form className="login-form" onSubmit={handleSubmit} noValidate>
          {error && (
            <div className="login-form__error" role="alert">
              {error}
            </div>
          )}

          <label className="login-field">
            <span>用户名</span>
            <input
              type="text"
              name="username"
              value={username}
              autoComplete="username"
              autoFocus
              disabled={loading}
              placeholder="请输入用户名"
              onChange={(e) => setUsername(e.target.value)}
            />
          </label>

          <label className="login-field">
            <span>密码</span>
            <input
              type="password"
              name="password"
              value={password}
              autoComplete="current-password"
              disabled={loading}
              placeholder="请输入密码"
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>

          <button type="submit" className="login-form__submit" disabled={loading}>
            {loading ? '登录中…' : '登录'}
          </button>
        </form>

        {import.meta.env.DEV && (
          <p className="login-card__hint">
            开发环境测试账号：学员 <code>student</code> · 教师 <code>teacher</code> · 管理员{' '}
            <code>admin</code>，密码 <code>123456</code>
          </p>
        )}
      </div>
    </div>
  )
}
