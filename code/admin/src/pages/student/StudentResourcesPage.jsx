import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api, learningResourceContentUrl } from '../../api/client'
import { getDeviceBindId } from '../../api/deviceBinding'
import { useAuth } from '../../auth/AuthContext'
import { useStudentCabinetId } from './studentCabinet'
import PdfDocumentReader from '../../components/PdfDocumentReader'
import MediaPreviewDialog from '../../components/MediaPreviewDialog'
import './StudentResourcesPage.css'

const TYPES = {
  outline: { api: 'DEBUG_OUTLINE', title: '调试大纲' },
  manual: { api: 'MANUAL', title: '说明书' },
  drawing: { api: 'DRAWING', title: '图纸' },
  video: { api: 'VIDEO_COURSE', title: '视频微课' },
}

function formatSize(size) {
  if (!Number.isFinite(size)) return ''
  return size < 1024 * 1024 ? `${Math.ceil(size / 1024)} KB` : `${(size / 1024 / 1024).toFixed(1)} MB`
}

export default function StudentResourcesPage() {
  const { type, id } = useParams()
  const navigate = useNavigate()
  const { logout, session } = useAuth()
  const role = session?.role
  const config = TYPES[type]
  const bindId = getDeviceBindId()
  const selectedCabinetId = useStudentCabinetId()
  const currentCabinetId = role === 'ADMIN' || role === 'STUDENT' ? selectedCabinetId : null
  const [items, setItems] = useState([])
  const [resource, setResource] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [previewOpen, setPreviewOpen] = useState(false)

  const load = useCallback(async () => {
    if (!config) return
    if (currentCabinetId === undefined) {
      setLoading(true)
      return
    }

    setLoading(true)
    setError('')
    try {
      if (role === 'ADMIN' && !currentCabinetId) throw new Error('请从管理后台选择要查看的屏柜')
      if (id) setResource(await api.getStudentLearningResource(id, bindId, currentCabinetId))
      else setItems(await api.listStudentLearningResources(config.api, bindId, currentCabinetId))
    } catch (err) {
      setError(err.message || '加载学习资料失败')
    } finally {
      setLoading(false)
    }
  }, [bindId, config, currentCabinetId, id, role])

  useEffect(() => {
    const timer = window.setTimeout(load, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  if (!config) return null
  const back = () => navigate(id ? `/student/resources/${type}` : '/student')

  return <div className="student-resources">
    <header className="student-resources__header"><button type="button" onClick={back}>← {id ? '返回列表' : '返回首页'}</button><h1>{resource?.name || config.title}</h1><button type="button" onClick={async () => { await logout(); navigate('/login', { replace: true }) }}>退出登录</button></header>
    <main className="student-resources__main">
      {loading ? <p>加载中…</p> : error ? <div className="student-resources__error"><p>{error}</p><button type="button" onClick={load}>重试</button></div> : id && resource ? <article className="student-resources__detail">
        <p className="student-resources__scope">{resource.cabinetName ? `适用屏柜：${resource.cabinetName}` : '适用范围：所有屏柜'}</p>
        <p className="student-resources__description">{resource.description}</p>
        {resource.resourceType === 'VIDEO_COURSE' ? <video className="student-resources__video" controls preload="metadata" src={learningResourceContentUrl(resource.id, bindId, currentCabinetId)}>当前浏览器不支持视频播放。</video> : <>
          <button type="button" className="student-resources__preview-btn" onClick={() => setPreviewOpen(true)}>单独窗口查看 PDF</button>
          <PdfDocumentReader key={resource.id} title={resource.name} fileUrl={learningResourceContentUrl(resource.id, bindId, currentCabinetId)} />
          <MediaPreviewDialog
            open={previewOpen}
            type="pdf"
            src={learningResourceContentUrl(resource.id, bindId, currentCabinetId)}
            title={resource.name}
            onClose={() => setPreviewOpen(false)}
          />
        </>}
      </article> : items.length === 0 ? <p className="student-resources__empty">当前屏柜暂无{config.title}。</p> : <section className="student-resources__list">{items.map((item) => <button key={item.id} type="button" className="student-resources__card" onClick={() => navigate(`/student/resources/${type}/${item.id}`)}><span className="student-resources__card-title">{item.name}</span><span className="student-resources__card-description">{item.description}</span><span className="student-resources__card-meta">{item.cabinetName ? item.cabinetName : '所有屏柜'} · {formatSize(item.fileSize)}</span></button>)}</section>}
    </main>
  </div>
}
