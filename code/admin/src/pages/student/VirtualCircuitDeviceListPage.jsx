import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import { resolveStudentCabinetId, useStudentCabinetId } from './studentCabinet'
import './TabletShell.css'
import './PanoramaListPage.css'

export default function VirtualCircuitDeviceListPage() {
  const navigate = useNavigate()
  const { logout } = useAuth()
  const selectedCabinetId = useStudentCabinetId()
  const [cabinet, setCabinet] = useState(null)
  const [devices, setDevices] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      if (selectedCabinetId === undefined) return
      setLoading(true)
      setError(null)
      try {
        const tree = await api.getKnowledgeTree()
        const cabinetId = resolveStudentCabinetId(tree, selectedCabinetId)
        if (!cabinetId) throw new Error('未找到当前绑定屏柜')
        const [currentCabinet, eligibleDevices] = await Promise.all([
          api.getKnowledgeCabinet(cabinetId),
          api.listVirtualCircuitDevices(cabinetId),
        ])
        if (!cancelled) {
          setCabinet(currentCabinet)
          setDevices(Array.isArray(eligibleDevices) ? eligibleDevices : [])
        }
      } catch (err) {
        if (!cancelled) setError(err.message)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [selectedCabinetId])

  return (
    <div className="tablet-shell panorama-list-shell">
      <header className="tablet-shell__header">
        <div className="tablet-shell__header-left">
          <button type="button" className="tablet-shell__back" onClick={() => navigate('/student/modes/coach')}>← 返回上级</button>
          <button type="button" className="tablet-shell__home" onClick={() => navigate('/student')}>返回首页</button>
        </div>
        <h1>虚回路学习 · 选择装置</h1>
        <div className="tablet-shell__header-actions">
          <button type="button" className="tablet-shell__logout" onClick={async () => { await logout(); navigate('/login', { replace: true }) }}>退出登录</button>
        </div>
      </header>

      <main className="tablet-shell__main panorama-list">
        <p className="panorama-list__intro">
          当前屏柜{cabinet?.name ? `「${cabinet.name}」` : ''}中以下装置包含虚回路信息，请选择需要查看的中心装置。
        </p>
        {loading && <p className="panorama-list__status">加载中…</p>}
        {error && <p className="panorama-list__status panorama-list__status--error">{error}</p>}
        {!loading && !error && devices.length === 0 && <p className="panorama-list__empty">当前屏柜暂无虚回路信息</p>}
        {!loading && !error && devices.length > 0 && (
          <div className="panorama-list__grid panorama-list__grid--devices">
            {devices.map((device) => (
              <button
                key={device.id}
                type="button"
                className="panorama-list__card panorama-list__device-card"
                onClick={() => navigate(`/student/modes/coach/virtual-circuit/${device.id}`)}
              >
                <div className="panorama-list__device-icon" aria-hidden="true">⇄</div>
                <div className="panorama-list__device-info">
                  <h3 title={device.displayName}>{device.displayName}</h3>
                  <p>{device.description || device.iedName || '保护装置'}</p>
                  <span className="panorama-list__device-count">IED：{device.iedName}</span>
                </div>
              </button>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
