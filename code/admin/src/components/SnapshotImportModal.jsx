import { useState } from 'react'
import './SnapshotImportModal.css'

export default function SnapshotImportModal({ open, onClose, onImport }) {
  const [jsonText, setJsonText] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  if (!open) return null

  const validate = (text) => {
    if (!text.trim()) return '请输入 JSON 内容'
    let parsed
    try {
      parsed = JSON.parse(text)
    } catch (e) {
      return `JSON 格式错误: ${e.message}`
    }
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      return 'JSON 必须为对象'
    }
    const missing = ['nodes', 'channels', 'timestamps'].filter((f) => !Array.isArray(parsed[f]))
    if (missing.length > 0) return `缺少 v2.3 必要字段: ${missing.join(', ')}`
    if (parsed.nodes.length !== parsed.channels.length) {
      return `nodes(${parsed.nodes.length}) 与 channels(${parsed.channels.length}) 数量不一致`
    }
    return null
  }

  const handleSubmit = async () => {
    const validationError = validate(jsonText)
    if (validationError) {
      setError(validationError)
      return
    }
    setError(null)
    setSubmitting(true)
    try {
      await onImport(jsonText)
      setJsonText('')
      onClose()
    } catch (e) {
      setError(e.message || '导入失败')
    } finally {
      setSubmitting(false)
    }
  }

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget && !submitting) {
      setJsonText('')
      setError(null)
      onClose()
    }
  }

  return (
    <div className="import-modal" onClick={handleOverlayClick}>
      <div className="import-modal__panel">
        <header className="import-modal__header">
          <h3 className="import-modal__title">导入断面 JSON</h3>
          <button
            type="button"
            className="import-modal__close"
            disabled={submitting}
            onClick={() => { setJsonText(''); setError(null); onClose() }}
          >
            ✕
          </button>
        </header>

        {error && <div className="import-modal__error">{error}</div>}

        <textarea
          className="import-modal__textarea"
          placeholder="粘贴 v2.3 格式的断面 JSON…"
          value={jsonText}
          onChange={(e) => { setJsonText(e.target.value); setError(null) }}
          spellCheck={false}
        />

        <div className="import-modal__footer">
          <span className="import-modal__hint">
            需要包含 nodes、channels、timestamps 数组
          </span>
          <div className="import-modal__actions">
            <button
              type="button"
              className="import-modal__btn import-modal__btn--cancel"
              disabled={submitting}
              onClick={() => { setJsonText(''); setError(null); onClose() }}
            >
              取消
            </button>
            <button
              type="button"
              className="import-modal__btn import-modal__btn--submit"
              disabled={submitting || !jsonText.trim()}
              onClick={handleSubmit}
            >
              {submitting ? '导入中…' : '导入'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
