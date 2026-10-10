import { useEffect, useRef, useState } from 'react'
import { api, imageUrl as resolveImageUrl } from '../api/client'
import CabinetImageEditor from './CabinetImageEditor'
import {
  isRasterImageFile,
  isGifFile,
  isSvgFile,
  extensionForImageType,
  loadImageSourceFromUrl,
  resolveImageOutputType,
} from '../utils/cropImage'

const MAX_IMAGE_UPLOAD_BYTES = 20 * 1024 * 1024

function formatFileSize(bytes) {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0MB'
  return `${(bytes / 1024 / 1024).toFixed(bytes >= 10 * 1024 * 1024 ? 0 : 1)}MB`
}

export default function CabinetImageUploadField({
  imageUrl,
  previewUrl,
  onChange,
  disabled,
  uploadImage,
  renderPreviewExtra,
  hidePreviewImage = false,
  allowClear = false,
}) {
  const fileInputRef = useRef(null)
  const localPreviewRef = useRef('')
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState('')
  const [editorSource, setEditorSource] = useState(null)
  const [localPreviewUrl, setLocalPreviewUrl] = useState('')

  const doUpload = uploadImage ?? ((file) => api.uploadCabinetDisplayImage(file))

  useEffect(() => () => {
    if (localPreviewRef.current) {
      URL.revokeObjectURL(localPreviewRef.current)
    }
  }, [])

  const setLocalPreview = (file) => {
    if (localPreviewRef.current) {
      URL.revokeObjectURL(localPreviewRef.current)
    }
    const nextPreviewUrl = URL.createObjectURL(file)
    localPreviewRef.current = nextPreviewUrl
    setLocalPreviewUrl(nextPreviewUrl)
  }

  const clearLocalPreview = () => {
    if (localPreviewRef.current) {
      URL.revokeObjectURL(localPreviewRef.current)
      localPreviewRef.current = ''
    }
    setLocalPreviewUrl('')
  }

  const uploadFile = async (file) => {
    setUploading(true)
    setUploadError('')
    try {
      const result = await doUpload(file)
      const nextImageUrl = result?.imageUrl ?? ''
      onChange(nextImageUrl, result)
      if (nextImageUrl) {
        clearLocalPreview()
      } else {
        setLocalPreview(file)
      }
    } catch (err) {
      setUploadError(err.message || '图片上传失败')
      throw err
    } finally {
      setUploading(false)
    }
  }

  const uploadBlob = async (blob) => {
    const extension = extensionForImageType(blob.type)
    const file = new File([blob], `image-${Date.now()}.${extension}`, { type: blob.type })
    await uploadFile(file)
    setEditorSource(null)
  }

  const openEditorWithFile = (file) => {
    const reader = new FileReader()
    reader.onload = () => setEditorSource({
      src: reader.result,
      outputType: resolveImageOutputType(file.type),
    })
    reader.onerror = () => setUploadError('读取图片失败')
    reader.readAsDataURL(file)
  }

  const handleFileChange = async (e) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return

    setUploadError('')
    if (file.size > MAX_IMAGE_UPLOAD_BYTES) {
      setUploadError(`图片不能超过 ${formatFileSize(MAX_IMAGE_UPLOAD_BYTES)}，当前约 ${formatFileSize(file.size)}`)
      return
    }
    if (isSvgFile(file) || isGifFile(file)) {
      await uploadFile(file)
      return
    }
    if (!isRasterImageFile(file)) {
      setUploadError('请选择 JPG、PNG、GIF、WebP 或 SVG 图片')
      return
    }
    openEditorWithFile(file)
  }

  const handleRecrop = async () => {
    const recropSource = imageUrl ? resolveImageUrl(imageUrl) : previewUrl || localPreviewUrl
    if (!recropSource) return
    setUploadError('')
    try {
      const src = await loadImageSourceFromUrl(recropSource)
      const sourceType = /^data:([^;,]+)/i.exec(src)?.[1] ?? ''
      setEditorSource({ src, outputType: resolveImageOutputType(sourceType) })
    } catch (err) {
      setUploadError(err.message || '无法加载当前图片')
    }
  }

  const handleClear = () => {
    clearLocalPreview()
    onChange('', { imageId: null, removeImage: true })
  }

  const previewSrc = imageUrl ? resolveImageUrl(imageUrl) : previewUrl || localPreviewUrl

  return (
    <>
      <div className="cabinet-display-items__image-field">
        <span className="cabinet-display-items__image-label">认知图片</span>
        <div className="cabinet-display-items__image-actions">
          <button
            type="button"
            className="users-page__btn users-page__btn--ghost cabinet-display-items__pick-btn"
            onClick={() => fileInputRef.current?.click()}
            disabled={disabled || uploading || Boolean(editorSource)}
          >
            {uploading ? '上传中…' : '选择并编辑图片'}
          </button>
          {previewSrc && (
            <button
              type="button"
              className="users-page__link"
              onClick={handleRecrop}
              disabled={disabled || uploading || Boolean(editorSource)}
            >
              重新裁剪
            </button>
          )}
          {allowClear && previewSrc && (
            <button
              type="button"
              className="users-page__link users-page__link--danger"
              onClick={handleClear}
              disabled={disabled || uploading || Boolean(editorSource)}
            >
              移除图片
            </button>
          )}
        </div>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          hidden
          onChange={handleFileChange}
          disabled={disabled || uploading}
        />
        {uploadError && <span className="cabinet-display-items__upload-error">{uploadError}</span>}
        {previewSrc ? (
          <>
            {!hidePreviewImage && (
              <img className="cabinet-display-items__preview" src={previewSrc} alt="认知图片预览" />
            )}
            {renderPreviewExtra?.(previewSrc)}
          </>
        ) : (
          <span className="cabinet-display-items__upload-hint">
            支持 JPG、PNG、WebP（保持高清裁切）；GIF、SVG 将保留原文件直接上传，最大 20MB
          </span>
        )}
      </div>

      {editorSource && (
        <CabinetImageEditor
          key={editorSource.src}
          imageSrc={editorSource.src}
          outputType={editorSource.outputType}
          onConfirm={uploadBlob}
          onCancel={() => setEditorSource(null)}
        />
      )}
    </>
  )
}
