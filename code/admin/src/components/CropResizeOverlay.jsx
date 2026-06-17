import { useRef } from 'react'
import './CropResizeOverlay.css'

const HANDLES = ['n', 's', 'e', 'w', 'ne', 'nw', 'se', 'sw']

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}

export default function CropResizeOverlay({ cropSize, containerSize, onCropSizeChange, minSize = 48 }) {
  const propsRef = useRef({ cropSize, containerSize, onCropSizeChange, minSize })
  propsRef.current = { cropSize, containerSize, onCropSizeChange, minSize }

  const boxLeft = (containerSize.width - cropSize.width) / 2
  const boxTop = (containerSize.height - cropSize.height) / 2

  const startDrag = (handle, event) => {
    event.preventDefault()
    event.stopPropagation()

    const startX = event.clientX
    const startY = event.clientY
    const startWidth = cropSize.width
    const startHeight = cropSize.height

    const onMove = (moveEvent) => {
      const { containerSize: size, onCropSizeChange: onChange, minSize: min } = propsRef.current
      const dx = moveEvent.clientX - startX
      const dy = moveEvent.clientY - startY
      let width = startWidth
      let height = startHeight

      if (handle.includes('e')) width = startWidth + dx
      if (handle.includes('w')) width = startWidth - dx
      if (handle.includes('s')) height = startHeight + dy
      if (handle.includes('n')) height = startHeight - dy

      onChange({
        width: Math.round(clamp(width, min, size.width)),
        height: Math.round(clamp(height, min, size.height)),
      })
    }

    const onUp = () => {
      document.body.classList.remove('cabinet-crop-dragging')
      document.removeEventListener('mousemove', onMove)
      document.removeEventListener('mouseup', onUp)
    }

    document.body.classList.add('cabinet-crop-dragging')
    document.addEventListener('mousemove', onMove)
    document.addEventListener('mouseup', onUp)
  }

  return (
    <div className="crop-resize-overlay" aria-hidden="true">
      <div
        className="crop-resize-box"
        style={{
          left: boxLeft,
          top: boxTop,
          width: cropSize.width,
          height: cropSize.height,
        }}
      >
        {HANDLES.map((handle) => (
          <div
            key={handle}
            className={`crop-resize-handle crop-resize-handle--${handle}`}
            onMouseDown={(event) => startDrag(handle, event)}
          />
        ))}
      </div>
    </div>
  )
}
