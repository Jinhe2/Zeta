import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import StudentSubpageBar from './StudentSubpageBar'
import './StudentSubpageLayout.css'
import './ChangePasswordPage.css'

function validateForm(oldPassword, newPassword, confirmPassword) {
  if (!oldPassword) return '请输入原密码'
  if (!newPassword) return '请输入新密码'
  if (newPassword.length < 6) return '新密码至少 6 位'
  if (!confirmPassword) return '请确认新密码'
  if (newPassword !== confirmPassword) return '两次输入的新密码不一致'
  return null
}

export default function ChangePasswordPage() {
  const navigate = useNavigate()
  const [oldPassword, setOldPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    const validationError = validateForm(oldPassword, newPassword, confirmPassword)
    if (validationError) {
      setError(validationError)
      setSuccess(null)
      return
    }

    setError(null)
    setSuccess(null)
    setLoading(true)
    try {
      await api.changePassword(oldPassword, newPassword)
      setSuccess('密码修改成功')
      setOldPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setTimeout(() => navigate('/student'), 1200)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="student-subpage change-password-page">
      <StudentSubpageBar title="修改密码" />

      <main className="change-password-page__main">
        <form className="change-password-form" onSubmit={handleSubmit}>
          <div className="change-password-form__row">
            <label className="change-password-form__label" htmlFor="old-password">
              原密码：
            </label>
            <input
              id="old-password"
              className="change-password-form__input"
              type="password"
              autoComplete="current-password"
              value={oldPassword}
              onChange={(e) => setOldPassword(e.target.value)}
            />
          </div>

          <div className="change-password-form__row">
            <label className="change-password-form__label" htmlFor="new-password">
              新密码：
            </label>
            <input
              id="new-password"
              className="change-password-form__input"
              type="password"
              autoComplete="new-password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
            />
          </div>

          <div className="change-password-form__row">
            <label className="change-password-form__label" htmlFor="confirm-password">
              确认新密码：
            </label>
            <input
              id="confirm-password"
              className="change-password-form__input"
              type="password"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />
          </div>

          {error && <p className="change-password-form__error">{error}</p>}
          {success && <p className="change-password-form__success">{success}</p>}

          <button
            type="submit"
            className="change-password-form__submit"
            disabled={loading}
          >
            {loading ? '提交中…' : '确认修改'}
          </button>
        </form>
      </main>
    </div>
  )
}
