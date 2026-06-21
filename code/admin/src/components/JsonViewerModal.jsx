import { useEffect, useRef } from 'react'
import './JsonViewerModal.css'

export default function JsonViewerModal({ open, title, jsonString, onClose }) {
  const overlayRef = useRef(null)

  useEffect(() => {
    if (!open) return
    const handleKey = (e) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKey)
    return () => document.removeEventListener('keydown', handleKey)
  }, [open, onClose])

  if (!open) return null

  let formatted = ''
  try {
    formatted = JSON.stringify(JSON.parse(jsonString), null, 2)
  } catch {
    formatted = jsonString || ''
  }

  const handleOverlayClick = (e) => {
    if (e.target === overlayRef.current) onClose()
  }

  const handleCopy = () => {
    navigator.clipboard.writeText(formatted).catch(() => {})
  }

  return (
    <div className="json-modal" ref={overlayRef} onClick={handleOverlayClick}>
      <div className="json-modal__panel">
        <header className="json-modal__header">
          <h3 className="json-modal__title">{title || 'Snapshot JSON'}</h3>
          <div className="json-modal__actions">
            <button type="button" className="json-modal__btn" onClick={handleCopy}>
              复制
            </button>
            <button type="button" className="json-modal__btn json-modal__btn--close" onClick={onClose}>
              ✕
            </button>
          </div>
        </header>
        <pre className="json-modal__code">
          <code>{formatted}</code>
        </pre>
      </div>
    </div>
  )
}
