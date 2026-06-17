import { useCallback, useEffect, useRef, useState } from 'react'
import {
  clampRegion,
  DEFAULT_REGION,
  getContainedImageRect,
  percentToPixelRect,
  pixelRectToPercent,
} from '../utils/imageRegionUtils'
import './ImageRegionEditor.css'

const HANDLES = ['n', 's', 'e', 'w', 'ne', 'nw', 'se', 'sw']
const MIN_SIZE = 16

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}

export default function ImageRegionEditor({ imageUrl, region, onChange, readOnly = false }) {
  const containerRef = useRef(null)
  const [containerSize, setContainerSize] = useState({ width: 0, height: 0 })
  const [naturalSize, setNaturalSize] = useState({ width: 0, height: 0 })
  const dragRef = useRef(null)
  const propsRef = useRef({ region, onChange, containerSize, naturalSize })
  propsRef.current = { region, onChange, containerSize, naturalSize }

  useEffect(() => {
    const el = containerRef.current
    if (!el) return undefined
    const update = () => setContainerSize({ width: el.clientWidth, height: el.clientHeight })
    update()
    const observer = new ResizeObserver(update)
    observer.observe(el)
    return () => observer.disconnect()
  }, [])

  const imageRect = getContainedImageRect(
    containerSize.width,
    containerSize.height,
    naturalSize.width,
    naturalSize.height,
  )

  const pixelRect = region
    ? percentToPixelRect(region, imageRect)
    : percentToPixelRect(DEFAULT_REGION, imageRect)

  const emitRegion = useCallback((nextPixelRect) => {
    const { containerSize: size, naturalSize: nat, onChange: emit } = propsRef.current
    const rect = getContainedImageRect(size.width, size.height, nat.width, nat.height)
    const next = clampRegion(pixelRectToPercent(nextPixelRect, rect))
    emit?.(next)
  }, [])

  const startDrag = (mode, handle, event) => {
    if (readOnly) return
    event.preventDefault()
    event.stopPropagation()

    const startX = event.clientX
    const startY = event.clientY
    const startRect = { ...pixelRect }

    const onMove = (moveEvent) => {
      const { containerSize: size, naturalSize: nat } = propsRef.current
      const imgRect = getContainedImageRect(size.width, size.height, nat.width, nat.height)
      const dx = moveEvent.clientX - startX
      const dy = moveEvent.clientY - startY
      let { left, top, width, height } = startRect

      if (mode === 'move') {
        left = clamp(startRect.left + dx, imgRect.left, imgRect.left + imgRect.width - width)
        top = clamp(startRect.top + dy, imgRect.top, imgRect.top + imgRect.height - height)
      } else {
        if (handle.includes('e')) width = startRect.width + dx
        if (handle.includes('w')) {
          width = startRect.width - dx
          left = startRect.left + dx
        }
        if (handle.includes('s')) height = startRect.height + dy
        if (handle.includes('n')) {
          height = startRect.height - dy
          top = startRect.top + dy
        }

        width = clamp(width, MIN_SIZE, imgRect.width)
        height = clamp(height, MIN_SIZE, imgRect.height)
        left = clamp(left, imgRect.left, imgRect.left + imgRect.width - width)
        top = clamp(top, imgRect.top, imgRect.top + imgRect.height - height)
      }

      emitRegion({ left, top, width, height })
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
    <div className="image-region-editor" ref={containerRef}>
      <img
        className="image-region-editor__image"
        src={imageUrl}
        alt=""
        onLoad={(e) =>
          setNaturalSize({ width: e.currentTarget.naturalWidth, height: e.currentTarget.naturalHeight })
        }
      />
      {naturalSize.width > 0 && region && (
        <div
          className={`image-region-editor__box${readOnly ? ' image-region-editor__box--readonly' : ''}`}
          style={{
            left: pixelRect.left,
            top: pixelRect.top,
            width: pixelRect.width,
            height: pixelRect.height,
          }}
          onMouseDown={(e) => startDrag('move', null, e)}
        >
          {!readOnly &&
            HANDLES.map((handle) => (
              <div
                key={handle}
                className={`image-region-editor__handle image-region-editor__handle--${handle}`}
                onMouseDown={(e) => startDrag('resize', handle, e)}
              />
            ))}
        </div>
      )}
    </div>
  )
}

/** 学员端只读高亮 */
export function ImageRegionViewer({ imageUrl, region, alt = '' }) {
  const containerRef = useRef(null)
  const [containerSize, setContainerSize] = useState({ width: 0, height: 0 })
  const [naturalSize, setNaturalSize] = useState({ width: 0, height: 0 })

  useEffect(() => {
    const el = containerRef.current
    if (!el) return undefined
    const update = () => setContainerSize({ width: el.clientWidth, height: el.clientHeight })
    update()
    const observer = new ResizeObserver(update)
    observer.observe(el)
    return () => observer.disconnect()
  }, [])

  const imageRect = getContainedImageRect(
    containerSize.width,
    containerSize.height,
    naturalSize.width,
    naturalSize.height,
  )

  const highlight = region ? percentToPixelRect(region, imageRect) : null

  return (
    <div className="image-region-viewer" ref={containerRef}>
      <img
        className="image-region-viewer__image"
        src={imageUrl}
        alt={alt}
        onLoad={(e) =>
          setNaturalSize({ width: e.currentTarget.naturalWidth, height: e.currentTarget.naturalHeight })
        }
      />
      {highlight && (
        <div
          className="image-region-highlight"
          style={{
            left: highlight.left,
            top: highlight.top,
            width: highlight.width,
            height: highlight.height,
          }}
        />
      )}
    </div>
  )
}
