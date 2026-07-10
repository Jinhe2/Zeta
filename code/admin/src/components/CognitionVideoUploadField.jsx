import { useEffect, useRef, useState } from 'react'
import { api } from '../api/client'
import './CognitionVideoUploadField.css'

const MAX_VIDEO_BYTES = 50 * 1024 * 1024

export default function CognitionVideoUploadField({ value, previewUrl = '', onChange, disabled = false }) {
  const inputRef = useRef(null)
  const [localPreviewUrl, setLocalPreviewUrl] = useState('')
  const [uploadedPath, setUploadedPath] = useState('')
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => () => {
    if (localPreviewUrl) URL.revokeObjectURL(localPreviewUrl)
  }, [localPreviewUrl])

  const cleanupUploaded = async (path) => {
    if (!path) return
    try {
      await api.deleteUnreferencedCognitionVideo(path)
    } catch {
      // A saved/referenced video must not be removed; cleanup is best-effort.
    }
  }

  const chooseFile = async (event) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setError('')
    if (!file.name.toLowerCase().endsWith('.mp4') || file.type !== 'video/mp4') {
      setError('仅支持 MP4 视频')
      return
    }
    if (file.size > MAX_VIDEO_BYTES) {
      setError('视频文件不能超过 50MB')
      return
    }

    const objectUrl = URL.createObjectURL(file)
    setLocalPreviewUrl(objectUrl)
    setUploading(true)
    try {
      const result = await api.uploadCognitionVideo(file)
      await cleanupUploaded(uploadedPath)
      setUploadedPath(result.videoPath)
      onChange(result.videoPath)
    } catch (err) {
      setError(err.message || '视频上传失败')
      setLocalPreviewUrl('')
    } finally {
      setUploading(false)
    }
  }

  const clear = async () => {
    await cleanupUploaded(uploadedPath)
    setUploadedPath('')
    setLocalPreviewUrl('')
    setError('')
    onChange('')
  }

  const source = localPreviewUrl || previewUrl

  return (
    <div className="cognition-video-upload">
      <span className="cognition-video-upload__label">认知视频</span>
      <div className="cognition-video-upload__actions">
        <input
          ref={inputRef}
          type="file"
          accept="video/mp4,.mp4"
          hidden
          onChange={chooseFile}
          disabled={disabled || uploading}
        />
        <button
          type="button"
          className="users-page__btn users-page__btn--ghost"
          onClick={() => inputRef.current?.click()}
          disabled={disabled || uploading}
        >
          {uploading ? '上传中…' : value ? '更换视频' : '选择视频'}
        </button>
        {value && (
          <button type="button" className="users-page__btn" onClick={clear} disabled={disabled || uploading}>
            清除视频
          </button>
        )}
      </div>
      <span className="cognition-video-upload__hint">仅支持 MP4，单个文件不超过 50MB</span>
      {error && <span className="cognition-video-upload__error">{error}</span>}
      {source && (
        <video className="cognition-video-upload__preview" src={source} controls preload="auto" playsInline>
          当前环境不支持视频播放。
        </video>
      )}
    </div>
  )
}
