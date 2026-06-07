import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../../api/client'
import ConfigPanel from '../../components/ConfigPanel'
import LogicVisualEditor from '../../diagram/LogicVisualEditor'
import './UsersPage.css'
import './ProtectionLogicConfigPage.css'

function tryParseJson(text) {
  try {
    return { ok: true, value: JSON.parse(text) }
  } catch (err) {
    return { ok: false, error: err.message }
  }
}

function formatJson(text) {
  const parsed = tryParseJson(text)
  if (!parsed.ok) {
    throw new Error(parsed.error)
  }
  return `${JSON.stringify(parsed.value, null, 2)}\n`
}

function configToText(config) {
  return `${JSON.stringify(config, null, 2)}\n`
}

export default function ProtectionLogicConfigPage() {
  const { cabinetId, deviceId, logicId } = useParams()
  const logicIdNum = Number(logicId)

  const [meta, setMeta] = useState(null)
  const [editorText, setEditorText] = useState('')
  const [savedText, setSavedText] = useState('')
  const [mode, setMode] = useState('visual')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const loadConfig = useCallback(async () => {
    if (!logicIdNum) return
    setLoading(true)
    setError('')
    try {
      const data = await api.getProtectionLogicConfig(logicIdNum)
      setMeta(data)
      setEditorText(data.configJson || '')
      setSavedText(data.configJson || '')
    } catch (err) {
      setError(err.message || '加载配置失败')
      setMeta(null)
      setEditorText('')
      setSavedText('')
    } finally {
      setLoading(false)
    }
  }, [logicIdNum])

  useEffect(() => {
    loadConfig()
  }, [loadConfig])

  const parsed = useMemo(() => tryParseJson(editorText), [editorText])
  const dirty = editorText !== savedText

  if (!cabinetId || !deviceId || !logicId || Number.isNaN(logicIdNum)) {
    return <Navigate to="/admin/cabinets" replace />
  }

  const flash = (text) => {
    setMessage(text)
    setTimeout(() => setMessage(''), 3000)
  }

  const handleFormat = () => {
    setError('')
    try {
      setEditorText(formatJson(editorText))
    } catch (err) {
      setError(`JSON 格式错误：${err.message}`)
    }
  }

  const handleReset = () => {
    if (dirty && !window.confirm('放弃未保存的修改？')) {
      return
    }
    setEditorText(savedText)
    setError('')
  }

  const handleSave = async () => {
    setError('')
    const check = tryParseJson(editorText)
    if (!check.ok) {
      setError(`JSON 格式错误：${check.error}`)
      return
    }
    setSaving(true)
    try {
      const data = await api.updateProtectionLogicConfig(logicIdNum, editorText)
      setMeta(data)
      setEditorText(data.configJson || '')
      setSavedText(data.configJson || '')
      flash('配置已保存')
    } catch (err) {
      setError(err.message || '保存失败')
    } finally {
      setSaving(false)
    }
  }

  const handleVisualConfigChange = (nextConfig) => {
    setEditorText(configToText(nextConfig))
  }

  const devicesPath = `/admin/cabinets/${cabinetId}/devices`
  const logicsPath = `${devicesPath}/${deviceId}/protection-logics`

  return (
    <div className="logic-config-page">
      <div className="logic-config-page__header">
        <div>
          <p className="users-page__breadcrumb">
            <Link to="/admin/cabinets">屏柜管理</Link>
            <span> / </span>
            <Link to={devicesPath}>设备管理</Link>
            <span> / </span>
            <Link to={logicsPath}>保护逻辑</Link>
            <span> / </span>
            <span>逻辑配置</span>
          </p>
          <h2 className="users-page__title">
            {meta ? `${meta.title} — 逻辑配置` : '逻辑配置'}
          </h2>
          <p className="users-page__desc">
            {meta
              ? `编码：${meta.code}。支持可视化编辑与 JSON 编辑，两种方式修改同一份配置。`
              : '编辑保护逻辑配置。'}
          </p>
        </div>
        <div className="logic-config-page__actions">
          <button type="button" className="users-page__btn" onClick={handleReset} disabled={!dirty || saving}>
            还原
          </button>
          {mode === 'json' && (
            <button type="button" className="users-page__btn" onClick={handleFormat} disabled={loading || saving}>
              格式化
            </button>
          )}
          <button
            type="button"
            className="users-page__btn users-page__btn--primary"
            onClick={handleSave}
            disabled={loading || saving || !dirty}
          >
            {saving ? '保存中…' : '保存配置'}
          </button>
        </div>
      </div>

      <div className="logic-config-page__tabs">
        <button
          type="button"
          className={`logic-config-page__tab${mode === 'visual' ? ' logic-config-page__tab--active' : ''}`}
          onClick={() => setMode('visual')}
        >
          可视化编辑
        </button>
        <button
          type="button"
          className={`logic-config-page__tab${mode === 'json' ? ' logic-config-page__tab--active' : ''}`}
          onClick={() => setMode('json')}
        >
          JSON 编辑
        </button>
        {dirty && <span className="logic-config-page__dirty">未保存</span>}
      </div>

      {message && <div className="users-page__message">{message}</div>}
      {error && <div className="users-page__error">{error}</div>}

      <div className="logic-config-page__content">
        {loading ? (
          <p className="users-page__loading">加载中…</p>
        ) : !meta ? (
          <p className="users-page__empty">
            保护逻辑不存在，<Link to={logicsPath}>返回列表</Link>。
          </p>
        ) : mode === 'visual' ? (
          parsed.ok ? (
            <LogicVisualEditor config={parsed.value} onConfigChange={handleVisualConfigChange} />
          ) : (
            <div className="users-page__error">JSON 无效，请切换到 JSON 模式修复后再使用可视化编辑。</div>
          )
        ) : (
          <div className="logic-config-page__workspace">
            <div className="logic-config-page__editor-wrap">
              <div className="logic-config-page__editor-bar">
                <span>config.json</span>
                {!parsed.ok && <span className="logic-config-page__invalid">JSON 无效</span>}
              </div>
              <textarea
                className="logic-config-page__editor"
                value={editorText}
                onChange={(e) => setEditorText(e.target.value)}
                spellCheck={false}
              />
            </div>
            <div className="logic-config-page__preview">
              <ConfigPanel config={parsed.ok ? parsed.value : null} title={meta.title} loading={false} />
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
