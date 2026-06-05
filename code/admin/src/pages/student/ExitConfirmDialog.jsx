import './ExitConfirmDialog.css'

export default function ExitConfirmDialog({ open, message, onConfirm, onCancel }) {
  if (!open) return null

  return (
    <div className="exit-dialog" role="dialog" aria-modal="true" aria-labelledby="exit-dialog-message">
      <button
        type="button"
        className="exit-dialog__mask"
        aria-label="关闭对话框"
        onClick={onCancel}
      />
      <div className="exit-dialog__panel">
        <p id="exit-dialog-message" className="exit-dialog__message">
          {message}
        </p>
        <div className="exit-dialog__actions">
          <button type="button" className="exit-dialog__btn exit-dialog__btn--confirm" onClick={onConfirm}>
            确定
          </button>
          <button type="button" className="exit-dialog__btn exit-dialog__btn--cancel" onClick={onCancel}>
            取消
          </button>
        </div>
      </div>
    </div>
  )
}
