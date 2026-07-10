import { useEffect, useRef, useState } from 'react'
import { imageUrl, videoUrl } from '../api/client'
import { ImageRegionViewer } from './ImageRegionEditor'
import './CognitionMediaViewer.css'

export default function CognitionMediaViewer({ item, imageType, region, alt }) {
  const videoRef = useRef(null)
  const [videoError, setVideoError] = useState(false)

  useEffect(() => {
    const video = videoRef.current
    return () => {
      if (video) {
        video.pause()
        video.currentTime = 0
      }
    }
  }, [item?.id])

  if (!item) return null
  if (item.mediaType === 'VIDEO') {
    if (videoError) {
      return <p className="cognition-media-viewer__error">视频加载失败，请联系管理员检查视频文件。</p>
    }
    return (
      <video
        key={item.id}
        ref={videoRef}
        className="cognition-media-viewer__video"
        src={videoUrl(imageType, item.id)}
        controls
        preload="auto"
        playsInline
        onError={() => setVideoError(true)}
      >
        当前环境不支持视频播放。
      </video>
    )
  }
  if (item.mediaType === 'TEXT') return null
  return (
    <ImageRegionViewer
      key={item.id}
      imageUrl={imageUrl(imageType, item.id)}
      region={region}
      alt={alt || item.title}
    />
  )
}
