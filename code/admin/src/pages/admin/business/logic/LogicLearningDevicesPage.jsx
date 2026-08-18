/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../../../../api/client'
import ConfigCopyDialog from '../../../../components/ConfigCopyDialog'
import '../UsersPage.css'

export default function LogicLearningDevicesPage() {
  const { cabinetId } = useParams()
  const cabinetIdNum = Number(cabinetId)
  const [cabinet, setCabinet] = useState(null)
  const [devices, setDevices] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [copySource, setCopySource] = useState(null)

  const loadData = useCallback(async () => {
    if (!cabinetIdNum) return
    setLoading(true)
    setError('')
    try {
      const [cabinetData, deviceData] = await Promise.all([
        api.getKnowledgeCabinet(cabinetIdNum),
        api.listKnowledgeCabinetDevices(cabinetIdNum),
      ])
      setCabinet(cabinetData)
      setDevices(deviceData)
    } catch (err) {
      setError(err.message || '加载装置列表失败')
      setCabinet(null)
      setDevices([])
    } finally {
      setLoading(false)
    }
  }, [cabinetIdNum])

  useEffect(() => {
    loadData()
  }, [loadData])

  if (!cabinetId || Number.isNaN(cabinetIdNum)) {
    return <Navigate to="/admin/logic-learning" replace />
  }

  return (
    <div className="users-page">
      <div className="users-page__header">
        <div>
          <p className="users-page__breadcrumb">
            <Link to="/admin/logic-learning">逻辑学习</Link>
            <span> / </span>
            <span>{cabinet?.name ?? '装置列表'}</span>
          </p>
          <h2 className="users-page__title">{cabinet ? `${cabinet.name} — 上装置` : '上装置'}</h2>
          <p className="users-page__desc">选择屏柜下的上装置后，继续维护该装置下的逻辑框图节点认知。</p>
        </div>
      </div>

      {error && <div className="users-page__error">{error}</div>}
      {message && <div className="users-page__message">{message}</div>}
      {loading ? (
        <p className="users-page__loading">加载中…</p>
      ) : !cabinet ? (
        <p className="users-page__empty">
          屏柜不存在，<Link to="/admin/logic-learning">返回列表</Link>。
        </p>
      ) : (
        <div className="users-page__table-wrap">
          <table className="users-page__table">
            <thead>
              <tr>
                <th>名称</th>
                <th>编码</th>
                <th>描述</th>
                <th>逻辑框图数量</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {devices.length === 0 ? (
                <tr>
                  <td colSpan={5} className="users-page__empty-cell">暂无上装置</td>
                </tr>
              ) : (
                devices.map((device) => (
                  <tr key={device.id}>
                    <td>{device.name}</td>
                    <td>{device.code}</td>
                    <td>{device.description || '—'}</td>
                    <td>{device.logicCount}</td>
                    <td className="users-page__actions">
                      <Link className="users-page__link" to={`/admin/logic-learning/devices/${device.id}/logics`}>
                        逻辑框图
                      </Link>
                      <Link className="users-page__link" to={`/admin/logic-learning/devices/${device.id}/settings`}>
                        装置定值清单
                      </Link>
                      <Link className="users-page__link" to={`/admin/logic-learning/devices/${device.id}/soft-pressboards`}>
                        装置软压板基准清单
                      </Link>
                      <Link className="users-page__link" to={`/admin/logic-learning/devices/${device.id}/hard-pressboards`}>
                        装置硬压板基准清单
                      </Link>
                      <button type="button" className="users-page__link" onClick={() => setCopySource(device)}>
                        复制逻辑配置
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
      {copySource && <ConfigCopyDialog
        scope="DEVICE"
        sourceId={copySource.id}
        sourceName={`${cabinet?.name || ''} / ${copySource.name}（${copySource.code}）`}
        onClose={() => setCopySource(null)}
        onSuccess={() => setMessage(`已完成“${copySource.name}”的逻辑配置复制`)}
      />}
    </div>
  )
}
