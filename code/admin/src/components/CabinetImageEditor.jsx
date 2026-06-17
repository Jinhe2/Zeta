import { useCallback, useEffect, useRef, useState } from 'react'
import Cropper from 'react-easy-crop'
import CropResizeOverlay from './CropResizeOverlay'
import { getCroppedImageBlob } from '../utils/cropImage'
import './CabinetImageEditor.css'

const MIN_CROP_SIZE = 48

function defaultCropSize(containerWidth, containerHeight) {
  return {
    width: Math.round(containerWidth * 0.85),
    height: Math.round(containerHeight * 0.85),
  }
}

export default function CabinetImageEditor({ imageSrc, onConfirm, onCancel }) {
  const cropAreaRef = useRef(null)
  const [containerSize, setContainerSize] = useState({ width: 0, height: 0 })
  const [initialCropSize, setInitialCropSize] = useState(null)
  const [cropSize, setCropSize] = useState(null)

  const [crop, setCrop] = useState({ x: 0, y: 0 })
  const [zoom, setZoom] = useState(1)
  const [rotation, setRotation] = useState(0)
  const [flipH, setFlipH] = useState(false)
  const [flipV, setFlipV] = useState(false)
  const [croppedAreaPixels, setCroppedAreaPixels] = useState(null)
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const el = cropAreaRef.current
    if (!el) return undefined

    const updateSize = () => {
      setContainerSize({ width: el.clientWidth, height: el.clientHeight })
    }

    updateSize()
    const observer = new ResizeObserver(updateSize)
    observer.observe(el)
    return () => observer.disconnect()
  }, [])

  useEffect(() => {
    setCrop({ x: 0, y: 0 })
    setZoom(1)
    setRotation(0)
    setFlipH(false)
    setFlipV(false)
    setCroppedAreaPixels(null)
    setError('')
    setCropSize(null)
    setInitialCropSize(null)
  }, [imageSrc])

  useEffect(() => {
    if (containerSize.width <= 0) return
    const initial = defaultCropSize(containerSize.width, containerSize.height)
    setInitialCropSize(initial)
    setCropSize(initial)
  }, [imageSrc, containerSize.width, containerSize.height])

  const onCropComplete = useCallback((_croppedArea, croppedPixels) => {
    setCroppedAreaPixels(croppedPixels)
  }, [])

  const handleCropSizeChange = useCallback((nextSize) => {
    setCropSize(nextSize)
  }, [])

  const handleReset = () => {
    setCrop({ x: 0, y: 0 })
    setZoom(1)
    setRotation(0)
    setFlipH(false)
    setFlipV(false)
    setError('')
    if (initialCropSize) {
      setCropSize({ ...initialCropSize })
    }
  }

  const handleRotateLeft = () => setRotation((r) => r - 90)
  const handleRotateRight = () => setRotation((r) => r + 90)

  const handleConfirm = async () => {
    if (!croppedAreaPixels) {
      setError('请先调整裁切区域')
      return
    }
    setProcessing(true)
    setError('')
    try {
      const blob = await getCroppedImageBlob(imageSrc, croppedAreaPixels, rotation, {
        horizontal: flipH,
        vertical: flipV,
      })
      await onConfirm(blob)
    } catch (err) {
      setError(err.message || '处理图片失败')
    } finally {
      setProcessing(false)
    }
  }

  const maxWidth = Math.max(MIN_CROP_SIZE, containerSize.width)
  const maxHeight = Math.max(MIN_CROP_SIZE, containerSize.height)

  return (
    <div className="cabinet-image-editor__overlay" role="dialog" aria-modal="true" aria-label="图片编辑">
      <div className="cabinet-image-editor">
        <div className="cabinet-image-editor__header">
          <h3>裁切与调整图片</h3>
          <button type="button" className="cabinet-image-editor__close" onClick={onCancel} aria-label="关闭">
            ×
          </button>
        </div>

        <div className="cabinet-image-editor__crop-area" ref={cropAreaRef}>
          {cropSize ? (
            <>
              <Cropper
                image={imageSrc}
                crop={crop}
                zoom={zoom}
                rotation={rotation}
                cropSize={cropSize}
                onCropChange={setCrop}
                onZoomChange={setZoom}
                onRotationChange={setRotation}
                onCropComplete={onCropComplete}
                objectFit="contain"
                showGrid
              />
              {containerSize.width > 0 && (
                <CropResizeOverlay
                  cropSize={cropSize}
                  containerSize={containerSize}
                  onCropSizeChange={handleCropSizeChange}
                  minSize={MIN_CROP_SIZE}
                />
              )}
            </>
          ) : (
            <div className="cabinet-image-editor__crop-loading">加载图片…</div>
          )}
        </div>

        <div className="cabinet-image-editor__controls">
          <p className="cabinet-image-editor__hint">拖动裁切框边线或角点可调整宽高</p>

          <div className="cabinet-image-editor__toolbar">
            <button type="button" className="cabinet-image-editor__tool-btn" onClick={handleRotateLeft}>
              左旋 90°
            </button>
            <button type="button" className="cabinet-image-editor__tool-btn" onClick={handleRotateRight}>
              右旋 90°
            </button>
            <button
              type="button"
              className="cabinet-image-editor__tool-btn"
              onClick={() => setFlipH((v) => !v)}
              aria-pressed={flipH}
            >
              水平翻转
            </button>
            <button
              type="button"
              className="cabinet-image-editor__tool-btn"
              onClick={() => setFlipV((v) => !v)}
              aria-pressed={flipV}
            >
              垂直翻转
            </button>
            <button type="button" className="cabinet-image-editor__tool-btn" onClick={handleReset}>
              重置
            </button>
          </div>

          {cropSize && containerSize.width > 0 && (
            <>
              <div className="cabinet-image-editor__control-row">
                <label htmlFor="cabinet-crop-width">宽度</label>
                <input
                  id="cabinet-crop-width"
                  type="range"
                  min={MIN_CROP_SIZE}
                  max={maxWidth}
                  step={1}
                  value={cropSize.width}
                  onChange={(e) =>
                    handleCropSizeChange({ ...cropSize, width: Number(e.target.value) })
                  }
                />
                <span>{cropSize.width}px</span>
              </div>

              <div className="cabinet-image-editor__control-row">
                <label htmlFor="cabinet-crop-height">高度</label>
                <input
                  id="cabinet-crop-height"
                  type="range"
                  min={MIN_CROP_SIZE}
                  max={maxHeight}
                  step={1}
                  value={cropSize.height}
                  onChange={(e) =>
                    handleCropSizeChange({ ...cropSize, height: Number(e.target.value) })
                  }
                />
                <span>{cropSize.height}px</span>
              </div>
            </>
          )}

          <div className="cabinet-image-editor__control-row">
            <label htmlFor="cabinet-crop-zoom">缩放</label>
            <input
              id="cabinet-crop-zoom"
              type="range"
              min={1}
              max={3}
              step={0.05}
              value={zoom}
              onChange={(e) => setZoom(Number(e.target.value))}
            />
            <span>{zoom.toFixed(1)}×</span>
          </div>

          <div className="cabinet-image-editor__control-row">
            <label htmlFor="cabinet-crop-rotate">旋转</label>
            <input
              id="cabinet-crop-rotate"
              type="range"
              min={-180}
              max={180}
              step={1}
              value={rotation}
              onChange={(e) => setRotation(Number(e.target.value))}
            />
            <span>{rotation}°</span>
          </div>

          {error && <p className="users-page__error">{error}</p>}
        </div>

        <div className="cabinet-image-editor__footer">
          <button type="button" className="users-page__btn users-page__btn--ghost" onClick={onCancel} disabled={processing}>
            取消
          </button>
          <button type="button" className="users-page__btn users-page__btn--primary" onClick={handleConfirm} disabled={processing}>
            {processing ? '处理中…' : '确认并上传'}
          </button>
        </div>
      </div>
    </div>
  )
}
