import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client'
import { getDeviceBindId, clearDeviceBindId } from '../api/deviceBinding'
import { useAuth } from '../auth/AuthContext'
import UnboundDeviceModal from './UnboundDeviceModal'
import './BindingGuard.css'

/**
 * 绑定检查守卫：学生/教师路由使用。
 *
 * 判定逻辑：
 *  - 服务端明确返回 BOUND → 放行
 *  - 服务端明确返回 UNBOUND → 清除本地存储，阻断并显示未绑定弹窗
 *  - 网络错误/其他错误 → 阻断并显示错误提示，用户可重试
 */
export default function BindingGuard({ children }) {
  const { logout, session } = useAuth()
  const [status, setStatus] = useState('checking') // checking | bound | unbound | error
  const [errorMsg, setErrorMsg] = useState('')

  // 管理员无需设备绑定，直接放行
  if (session?.role === 'ADMIN') {
    return children
  }

  const check = useCallback(() => {
    setStatus('checking')
    setErrorMsg('')
    const bindId = getDeviceBindId()

    api.checkBinding(bindId)
      .then((res) => {
        if (res.status === 'UNBOUND') {
          clearDeviceBindId()
          setStatus('unbound')
        } else {
          setStatus('bound')
        }
      })
      .catch((err) => {
        setErrorMsg(err.message || '网络请求失败，请检查网络连接')
        setStatus('error')
      })
  }, [])

  useEffect(() => { check() }, [check])

  if (status === 'unbound') {
    return <UnboundDeviceModal onLogout={() => logout()} />
  }

  if (status === 'error') {
    return (
      <div className="binding-guard-error">
        <div className="binding-guard-error__panel">
          <div className="binding-guard-error__icon">⚠</div>
          <h3 className="binding-guard-error__title">无法验证设备绑定状态</h3>
          <p className="binding-guard-error__msg">{errorMsg}</p>
          <div className="binding-guard-error__actions">
            <button type="button" className="binding-guard-error__retry" onClick={check}>
              重试
            </button>
            <button type="button" className="binding-guard-error__logout" onClick={() => logout()}>
              退出登录
            </button>
          </div>
        </div>
      </div>
    )
  }

  if (status === 'checking') {
    return (
      <div className="binding-guard-checking">
        <span className="binding-guard-checking__spinner" />
        <span>正在验证设备绑定…</span>
      </div>
    )
  }

  // status === 'bound' → 放行
  return children
}
