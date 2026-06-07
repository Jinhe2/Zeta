import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../../../api/client'
import '../UsersPage.css'

/** 屏柜/设备展示维护入口：列举屏柜系统数据，跳转至业务库展示条目维护。 */
export default function DisplayHubPage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [cabinets, setCabinets] = useState([])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')
    api
      .getKnowledgeTree()
      .then((tree) => {
        if (!cancelled) setCabinets(tree.cabinets ?? [])
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || '加载知识结构失败')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="users-page">
      <h2 className="users-page__title">屏柜展示维护</h2>
      <p className="users-page__desc">
        屏柜与设备基础数据来自屏柜系统（ct-screen，只读）；本页维护业务库（ct-screen-monitor）中的展示介绍内容。
      </p>

      {error && <div className="users-page__error">{error}</div>}
      {loading ? (
        <p className="users-page__loading">加载中…</p>
      ) : cabinets.length === 0 ? (
        <p className="users-page__empty">暂无屏柜数据，请确认屏柜系统可读。</p>
      ) : (
        cabinets.map((cabinet) => (
          <div key={cabinet.id} style={{ marginBottom: '2rem' }}>
            <div className="users-page__header">
              <div>
                <h3 className="users-page__title" style={{ fontSize: '1.1rem' }}>{cabinet.name}</h3>
                <p className="users-page__desc">{cabinet.description || cabinet.code}</p>
              </div>
              <div className="users-page__actions">
                <Link
                  className="users-page__btn"
                  to={`/admin/screen/cabinets/${cabinet.id}`}
                >
                  查看屏柜数据
                </Link>
                <Link
                  className="users-page__btn users-page__btn--primary"
                  to={`/admin/display/cabinets/${cabinet.id}/items`}
                >
                  屏柜展示
                </Link>
              </div>
            </div>

            <div className="users-page__table-wrap">
              <table className="users-page__table">
                <thead>
                  <tr>
                    <th>设备</th>
                    <th>编码</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {(cabinet.devices ?? []).length === 0 ? (
                    <tr>
                      <td colSpan={3} className="users-page__empty-cell">
                        暂无设备
                      </td>
                    </tr>
                  ) : (
                    cabinet.devices.map((device) => (
                      <tr key={device.id}>
                        <td>{device.name}</td>
                        <td>{device.code}</td>
                        <td>
                          <Link
                            className="users-page__link"
                            to={`/admin/display/devices/${device.id}/items`}
                          >
                            设备展示
                          </Link>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        ))
      )}
    </div>
  )
}
