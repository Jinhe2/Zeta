import { useState } from 'react'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import './AccountProfilePage.css'

const ROLE_LABELS = {
  STUDENT: '学员',
  TEACHER: '教师',
  ADMIN: '管理员',
}

function validatePasswordForm(oldPassword, newPassword, confirmPassword) {
  if (!oldPassword) return '请输入原密码'
  if (!newPassword) return '请输入新密码'
  if (newPassword.length < 6) return '新密码至少 6 位'
  if (!confirmPassword) return '请确认新密码'
  if (newPassword !== confirmPassword) return '两次输入的新密码不一致'
  return null
}

export default function AccountProfilePage() {
  const { session } = useAuth()
  const [oldPassword, setOldPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)

  const displayName = session?.displayName || '用户'
  const infoItems = [
    { label: '姓名', value: displayName },
    ...(session?.role === 'STUDENT' ? [{ label: '学号', value: session?.studentNo || '未分配' }] : []),
    { label: '登录账号', value: session?.username || '未获取' },
    { label: '用户角色', value: ROLE_LABELS[session?.role] || session?.role || '未获取' },
  ]

  const handleSubmit = async (event) => {
    event.preventDefault()
    const validationError = validatePasswordForm(oldPassword, newPassword, confirmPassword)
    if (validationError) {
      setError(validationError)
      setSuccess('')
      return
    }

    setError('')
    setSuccess('')
    setLoading(true)
    try {
      await api.changePassword(oldPassword, newPassword)
      setOldPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setSuccess('密码修改成功')
    } catch (err) {
      setError(err.message || '密码修改失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="account-profile-page">
      <div className="account-profile-page__header">
        <div>
          <h2 className="account-profile-page__title">个人中心</h2>
          <p className="account-profile-page__desc">查看当前账号信息，并维护登录密码。</p>
        </div>
      </div>

      <section className="account-profile-card">
        <div className="account-profile-card__avatar" aria-hidden="true">
          {displayName.slice(0, 1)}
        </div>
        <div className="account-profile-card__summary">
          <span className="account-profile-card__eyebrow">当前登录账号</span>
          <h3>{displayName}</h3>
        </div>
      </section>

      <section className="account-profile-list" aria-label="个人信息">
        {infoItems.map((item) => (
          <div key={item.label} className="account-profile-list__item">
            <span className="account-profile-list__label">{item.label}</span>
            <span className="account-profile-list__value">{item.value}</span>
          </div>
        ))}
      </section>

      <section className="account-password-card">
        <h3>修改密码</h3>
        <form className="account-password-form" onSubmit={handleSubmit}>
          <label>
            <span>原密码</span>
            <input
              type="password"
              autoComplete="current-password"
              value={oldPassword}
              onChange={(event) => setOldPassword(event.target.value)}
            />
          </label>
          <label>
            <span>新密码</span>
            <input
              type="password"
              autoComplete="new-password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
            />
          </label>
          <label>
            <span>确认新密码</span>
            <input
              type="password"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
            />
          </label>
          {error && <p className="account-password-form__error">{error}</p>}
          {success && <p className="account-password-form__success">{success}</p>}
          <button type="submit" disabled={loading}>
            {loading ? '提交中…' : '确认修改'}
          </button>
        </form>
      </section>
    </div>
  )
}
