import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../../../../api/client'
import '../../business/UsersPage.css'

/** 只读展示设备详情及保护逻辑列表（来自屏柜系统） */
export default function CatalogDeviceDetailPage() {
  const { cabinetId, deviceId } = useParams()
  const deviceIdNum = Number(deviceId)

  const [device, setDevice] = useState(null)
  const [logics, setLogics] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadData = useCallback(async () => {
    if (!deviceIdNum) return
    setLoading(true)
    setError('')
    try {
      const [deviceData, logicData] = await Promise.all([
        api.getKnowledgeDevice(deviceIdNum),
        api.listKnowledgeDeviceProtectionLogics(deviceIdNum),
      ])
      setDevice(deviceData)
      setLogics(logicData)
    } catch (err) {
      setError(err.message || '加载设备详情失败')
      setDevice(null)
      setLogics([])
    } finally {
      setLoading(false)
    }
  }, [deviceIdNum])

  useEffect(() => {
    loadData()
  }, [loadData])

  if (!deviceId || Number.isNaN(deviceIdNum)) {
    return <Navigate to={`/admin/screen/cabinets/${cabinetId}`} replace />
  }

  return (
    <div className="users-page">
      <p className="users-page__breadcrumb">
        <Link to="/admin/screen/cabinets">屏柜数据</Link>
        <span> / </span>
        <Link to={`/admin/screen/cabinets/${cabinetId}`}>屏柜</Link>
        <span> / </span>
        <span>{device?.name ?? '设备详情'}</span>
      </p>

      <h2 className="users-page__title">{device ? device.name : '设备详情'}</h2>
      <p className="users-page__desc">屏柜系统只读数据。业务侧认知内容请通过「屏柜认知」入口编辑。</p>

      {error && <div className="users-page__error">{error}</div>}
      {loading ? (
        <p className="users-page__loading">加载中…</p>
      ) : !device ? (
        <p className="users-page__empty">
          设备不存在，<Link to={`/admin/screen/cabinets/${cabinetId}`}>返回屏柜</Link>。
        </p>
      ) : (
        <>
          <p className="users-page__desc">
            编码：{device.code}
            {device.description ? ` · ${device.description}` : ''}
          </p>

          <div className="users-page__header">
            <h3 className="users-page__title" style={{ fontSize: '1.1rem' }}>保护逻辑</h3>
            <Link
              className="users-page__btn users-page__btn--primary"
              to={`/admin/display/cabinets/${cabinetId}/devices/${deviceId}/items`}
            >
              设备认知
            </Link>
          </div>

          <div className="users-page__table-wrap">
            <table className="users-page__table">
              <thead>
                <tr>
                  <th>标题</th>
                  <th>编码</th>
                  <th>分类</th>
                  <th>描述</th>
                </tr>
              </thead>
              <tbody>
                {logics.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="users-page__empty-cell">
                      暂无保护逻辑
                    </td>
                  </tr>
                ) : (
                  logics.map((logic) => (
                    <tr key={logic.id}>
                      <td>{logic.title}</td>
                      <td>{logic.code}</td>
                      <td>{logic.category || '—'}</td>
                      <td>{logic.description || '—'}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  )
}
