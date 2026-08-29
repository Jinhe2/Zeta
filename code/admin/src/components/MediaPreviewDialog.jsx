import { useEffect } from 'react'
import { TransformComponent, TransformWrapper } from 'react-zoom-pan-pinch'
import PdfDocumentReader from './PdfDocumentReader'
import './MediaPreviewDialog.css'

export default function MediaPreviewDialog({ open, type = 'image', src, title = '预览', alt = '', onClose }) {
  useEffect(() => {
    if (!open) return undefined
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onClose?.()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose, open])

  if (!open || !src) return null

  return (
    <div
      className="media-preview-dialog"
      role="dialog"
      aria-modal="true"
      aria-label={`${title} 预览`}
      onClick={(event) => event.stopPropagation()}
      onKeyDown={(event) => event.stopPropagation()}
    >
      <button type="button" className="media-preview-dialog__backdrop" aria-label="关闭预览" onClick={onClose} />
      <section className="media-preview-dialog__panel">
        <header className="media-preview-dialog__header">
          <h2>{title}</h2>
          <button type="button" className="media-preview-dialog__close" onClick={onClose}>关闭</button>
        </header>
        <div className="media-preview-dialog__body">
          {type === 'pdf' ? (
            <PdfDocumentReader fileUrl={src} title={title} />
          ) : (
            <TransformWrapper
              initialScale={1}
              minScale={0.5}
              maxScale={8}
              centerOnInit
              wheel={{ step: 0.18 }}
              pinch={{ step: 6 }}
              doubleClick={{ mode: 'reset' }}
            >
              {({ zoomIn, zoomOut, resetTransform }) => (
                <>
                  <div className="media-preview-dialog__tools">
                    <button type="button" onClick={() => zoomOut()}>−</button>
                    <button type="button" onClick={() => zoomIn()}>＋</button>
                    <button type="button" onClick={() => resetTransform()}>还原</button>
                  </div>
                  <TransformComponent
                    wrapperClass="media-preview-dialog__image-wrapper"
                    contentClass="media-preview-dialog__image-content"
                  >
                    <img className="media-preview-dialog__image" src={src} alt={alt || title} />
                  </TransformComponent>
                </>
              )}
            </TransformWrapper>
          )}
        </div>
      </section>
    </div>
  )
}
