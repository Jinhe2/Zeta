import { useState, useEffect } from 'react'
import { getApiBaseUrl, setApiBaseUrl, clearApiBaseUrl } from '../api/client'
import './ServerSettingsModal.css'

function validateAddress(address) {
  if (!address.trim()) {
    return '请输入服务器地址'
  }

  // 支持格式: IP:port 或 http://IP:port 或 https://IP:port
  const pattern = /^(https?:\/\/)?([\w.-]+)(:\d+)?$/
  if (!pattern.test(address.trim())) {
    return '地址格式不正确，请使用 IP:端口 格式（如 192.168.1.100:8080）'
  }

  return null
}

function normalizeAddress(address) {
  let normalized = address.trim()
  // 如果没有协议前缀，添加 http://
  if (!normalized.startsWith('http://') && !normalized.startsWith('https://')) {
    normalized = 'http://' + normalized
  }
  return normalized
}

export default function ServerSettingsModal({ open, onClose, onSaved }) {
  const [address, setAddress] = useState('')
  const [error, setError] = useState(null)
  const [testing, setTesting] = useState(false)
  const [testResult, setTestResult] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (open) {
      setAddress(getApiBaseUrl())
      setError(null)
      setTestResult(null)
    }
  }, [open])

  if (!open) return null

  const handleTest = async () => {
    const validationError = validateAddress(address)
    if (validationError) {
      setError(validationError)
      return
    }

    setError(null)
    setTesting(true)
    setTestResult(null)

    try {
      const normalized = normalizeAddress(address)
      const res = await fetch(normalized + '/api/auth/me', {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
      })

      // 401 表示服务器可达，只是未登录
      if (res.status === 401 || res.ok) {
        setTestResult({ success: true, message: '连接成功' })
      } else {
        setTestResult({ success: false, message: `服务器返回 ${res.status}` })
      }
    } catch (err) {
      setTestResult({ success: false, message: '无法连接到服务器，请检查地址和网络' })
    } finally {
      setTesting(false)
    }
  }

  const handleSave = () => {
    const validationError = validateAddress(address)
    if (validationError) {
      setError(validationError)
      return
    }

    setSaving(true)
    try {
      const normalized = normalizeAddress(address)
      setApiBaseUrl(normalized)
      onSaved?.(normalized)
      onClose()
    } catch (err) {
      setError('保存失败: ' + err.message)
    } finally {
      setSaving(false)
    }
  }

  const handleClear = () => {
    clearApiBaseUrl()
    setAddress('')
    setError(null)
    setTestResult(null)
    onSaved?.('')
  }

  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose()
    }
  }

  return (
    <div className="server-settings-modal" onClick={handleBackdropClick}>
      <div className="server-settings-modal__card">
        <header className="server-settings-modal__header">
          <h2>服务器设置</h2>
          <button type="button" className="server-settings-modal__close" onClick={onClose}>
            ✕
          </button>
        </header>

        <div className="server-settings-modal__body">
          <p className="server-settings-modal__desc">
            配置后端服务器地址，用于本地局域网部署场景。格式：<code>IP:端口</code>
          </p>

          <label className="server-settings-field">
            <span>服务器地址</span>
            <input
              type="text"
              value={address}
              onChange={(e) => {
                setAddress(e.target.value)
                setError(null)
                setTestResult(null)
              }}
              placeholder="例如：192.168.1.100:8080"
              disabled={testing || saving}
            />
          </label>

          {error && <div className="server-settings-modal__error">{error}</div>}

          {testResult && (
            <div className={`server-settings-modal__test-result ${testResult.success ? 'success' : 'error'}`}>
              {testResult.message}
            </div>
          )}

          <div className="server-settings-modal__actions">
            <button
              type="button"
              className="server-settings-modal__btn server-settings-modal__btn--secondary"
              onClick={handleTest}
              disabled={testing || saving || !address.trim()}
            >
              {testing ? '测试中…' : '测试连接'}
            </button>
            <button
              type="button"
              className="server-settings-modal__btn server-settings-modal__btn--danger"
              onClick={handleClear}
              disabled={testing || saving || !address}
            >
              清除配置
            </button>
            <button
              type="button"
              className="server-settings-modal__btn server-settings-modal__btn--primary"
              onClick={handleSave}
              disabled={testing || saving || !address.trim()}
            >
              {saving ? '保存中…' : '保存'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
