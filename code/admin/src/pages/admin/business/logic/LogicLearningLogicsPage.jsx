/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../../../../api/client'
import '../UsersPage.css'

export default function LogicLearningLogicsPage() {
  const { deviceId } = useParams()
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
      setError(err.message || '加载逻辑框图失败')
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
    return <Navigate to="/admin/logic-learning" replace />
  }

  return (
    <div className="users-page">
      <div className="users-page__header">
        <div>
          <p className="users-page__breadcrumb">
            <Link to="/admin/logic-learning">逻辑学习</Link>
            {device && (
              <>
                <span> / </span>
                <Link to={`/admin/logic-learning/cabinets/${device.cabinetId}/devices`}>
                  {device.cabinetName}
                </Link>
                <span> / </span>
                <span>{device.name} — 逻辑框图</span>
              </>
            )}
          </p>
          <h2 className="users-page__title">{device ? `${device.name} — 逻辑框图` : '逻辑框图'}</h2>
          <p className="users-page__desc">选择逻辑框图后，系统会解析其中可配置的非门节点。</p>
        </div>
      </div>

      {error && <div className="users-page__error">{error}</div>}
      {loading ? (
        <p className="users-page__loading">加载中…</p>
      ) : !device ? (
        <p className="users-page__empty">
          装置不存在，<Link to="/admin/logic-learning">返回列表</Link>。
        </p>
      ) : (
        <div className="users-page__table-wrap">
          <table className="users-page__table">
            <thead>
              <tr>
                <th>名称</th>
                <th>编码</th>
                <th>类型</th>
                <th>描述</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {logics.length === 0 ? (
                <tr>
                  <td colSpan={5} className="users-page__empty-cell">暂无逻辑框图</td>
                </tr>
              ) : (
                logics.map((logic) => (
                  <tr key={logic.id}>
                    <td>{logic.title}</td>
                    <td>{logic.code}</td>
                    <td>{logic.category || '—'}</td>
                    <td>{logic.description || '—'}</td>
                    <td>
                      <Link className="users-page__link" to={`/admin/logic-learning/logics/${logic.id}/nodes`}>
                        逻辑节点
                      </Link>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
