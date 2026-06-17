import { useRef, useState } from 'react'
import { api } from '../api/client'
import CabinetImageEditor from './CabinetImageEditor'
import {
  isRasterImageFile,
  isSvgFile,
  loadImageSourceFromUrl,
} from '../utils/cropImage'

export default function CabinetImageUploadField({ imageUrl, onChange, disabled, uploadImage }) {
  const fileInputRef = useRef(null)
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState('')
  const [editorSrc, setEditorSrc] = useState(null)

  const doUpload = uploadImage ?? ((file) => api.uploadCabinetDisplayImage(file))

  const uploadFile = async (file) => {
    setUploading(true)
    setUploadError('')
    try {
      const result = await doUpload(file)
      onChange(result.imageUrl)
    } catch (err) {
      setUploadError(err.message || '图片上传失败')
      throw err
    } finally {
      setUploading(false)
    }
  }

  const uploadBlob = async (blob) => {
    const file = new File([blob], `cabinet-display-${Date.now()}.jpg`, { type: 'image/jpeg' })
    await uploadFile(file)
    setEditorSrc(null)
  }

  const openEditorWithFile = (file) => {
    const reader = new FileReader()
    reader.onload = () => setEditorSrc(reader.result)
    reader.onerror = () => setUploadError('读取图片失败')
    reader.readAsDataURL(file)
  }

  const handleFileChange = async (e) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return

    setUploadError('')
    if (isSvgFile(file)) {
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
    if (!imageUrl) return
    setUploadError('')
    try {
      const src = await loadImageSourceFromUrl(imageUrl)
      setEditorSrc(src)
    } catch (err) {
      setUploadError(err.message || '无法加载当前图片')
    }
  }

  return (
    <>
      <div className="cabinet-display-items__image-field">
        <span className="cabinet-display-items__image-label">认知图片</span>
        <div className="cabinet-display-items__image-actions">
          <button
            type="button"
            className="users-page__btn users-page__btn--ghost cabinet-display-items__pick-btn"
            onClick={() => fileInputRef.current?.click()}
            disabled={disabled || uploading || Boolean(editorSrc)}
          >
            {uploading ? '上传中…' : '选择并编辑图片'}
          </button>
          {imageUrl && (
            <button
              type="button"
              className="users-page__link"
              onClick={handleRecrop}
              disabled={disabled || uploading || Boolean(editorSrc)}
            >
              重新裁剪
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
        {imageUrl ? (
          <img className="cabinet-display-items__preview" src={imageUrl} alt="认知图片预览" />
        ) : (
          <span className="cabinet-display-items__upload-hint">
            支持 JPG、PNG、GIF、WebP（可裁切、旋转、翻转）；SVG 将直接上传
          </span>
        )}
      </div>

      {editorSrc && (
        <CabinetImageEditor
          key={editorSrc}
          imageSrc={editorSrc}
          onConfirm={uploadBlob}
          onCancel={() => setEditorSrc(null)}
        />
      )}
    </>
  )
}
