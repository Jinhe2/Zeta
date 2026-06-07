import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../../../../api/client'
import '../../business/UsersPage.css'

/** 只读展示屏柜详情及下属设备（来自屏柜系统） */
export default function CabinetCatalogDetailPage() {
  const { cabinetId } = useParams()
  const cabinetIdNum = Number(cabinetId)

  const [cabinet, setCabinet] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadCabinet = useCallback(async () => {
    if (!cabinetIdNum) return
    setLoading(true)
    setError('')
    try {
      const data = await api.getKnowledgeCabinet(cabinetIdNum)
      setCabinet(data)
    } catch (err) {
      setError(err.message || '加载屏柜详情失败')
      setCabinet(null)
    } finally {
      setLoading(false)
    }
  }, [cabinetIdNum])

  useEffect(() => {
    loadCabinet()
  }, [loadCabinet])

  if (!cabinetId || Number.isNaN(cabinetIdNum)) {
    return <Navigate to="/admin/screen/cabinets" replace />
  }

  return (
    <div className="users-page">
      <p className="users-page__breadcrumb">
        <Link to="/admin/screen/cabinets">屏柜数据</Link>
        <span> / </span>
        <span>{cabinet?.name ?? '屏柜详情'}</span>
      </p>

      <h2 className="users-page__title">{cabinet ? cabinet.name : '屏柜详情'}</h2>
      <p className="users-page__desc">屏柜系统只读数据。业务侧展示介绍请通过「维护展示」入口编辑。</p>

      {error && <div className="users-page__error">{error}</div>}
      {loading ? (
        <p className="users-page__loading">加载中…</p>
      ) : !cabinet ? (
        <p className="users-page__empty">
          屏柜不存在，<Link to="/admin/screen/cabinets">返回列表</Link>。
        </p>
      ) : (
        <>
          <p className="users-page__desc">
            编码：{cabinet.code} · 排序：{cabinet.sortOrder}
            {cabinet.description ? ` · ${cabinet.description}` : ''}
          </p>

          <div className="users-page__header">
            <h3 className="users-page__title" style={{ fontSize: '1.1rem' }}>下属设备</h3>
            <Link
              className="users-page__btn users-page__btn--primary"
              to={`/admin/display/cabinets/${cabinetId}/items`}
            >
              维护屏柜展示
            </Link>
          </div>

          <div className="users-page__table-wrap">
            <table className="users-page__table">
              <thead>
                <tr>
                  <th>设备名称</th>
                  <th>编码</th>
                  <th>描述</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {(cabinet.devices ?? []).length === 0 ? (
                  <tr>
                    <td colSpan={4} className="users-page__empty-cell">
                      暂无设备
                    </td>
                  </tr>
                ) : (
                  cabinet.devices.map((device) => (
                    <tr key={device.id}>
                      <td>{device.name}</td>
                      <td>{device.code}</td>
                      <td>{device.description || '—'}</td>
                      <td className="users-page__actions">
                        <Link
                          className="users-page__link"
                          to={`/admin/screen/cabinets/${cabinetId}/devices/${device.id}`}
                        >
                          查看详情
                        </Link>
                        <Link
                          className="users-page__link"
                          to={`/admin/display/devices/${device.id}/items`}
                        >
                          维护展示
                        </Link>
                      </td>
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
