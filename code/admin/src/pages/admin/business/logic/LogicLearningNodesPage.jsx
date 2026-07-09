/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../../../../api/client'
import '../UsersPage.css'
import './LogicLearningPage.css'

const NODE_TYPE_LABELS = {
  INPUT: '输入节点',
  TIMER: '延时节点',
  OUTPUT: '输出节点',
}

export default function LogicLearningNodesPage() {
  const { logicDiagramId } = useParams()
  const logicDiagramIdNum = Number(logicDiagramId)
  const [logic, setLogic] = useState(null)
  const [device, setDevice] = useState(null)
  const [nodes, setNodes] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadData = useCallback(async () => {
    if (!logicDiagramIdNum) return
    setLoading(true)
    setError('')
    try {
      const [logicData, nodeData] = await Promise.all([
        api.getProtectionLogic(logicDiagramIdNum),
        api.listLogicLearningNodes(logicDiagramIdNum),
      ])
      setLogic(logicData)
      setNodes(nodeData)
      if (logicData.deviceId) {
        const deviceData = await api.getKnowledgeDevice(logicData.deviceId)
        setDevice(deviceData)
      } else {
        setDevice(null)
      }
    } catch (err) {
      setError(err.message || '加载逻辑节点失败')
      setLogic(null)
      setDevice(null)
      setNodes([])
    } finally {
      setLoading(false)
    }
  }, [logicDiagramIdNum])

  useEffect(() => {
    loadData()
  }, [loadData])

  if (!logicDiagramId || Number.isNaN(logicDiagramIdNum)) {
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
                <Link to={`/admin/logic-learning/devices/${device.id}/logics`}>
                  {device.name}
                </Link>
              </>
            )}
            {logic && (
              <>
                <span> / </span>
                <span>{logic.title} — 逻辑节点</span>
              </>
            )}
          </p>
          <h2 className="users-page__title">{logic ? `${logic.title} — 逻辑节点` : '逻辑节点'}</h2>
          <p className="users-page__desc">这里仅展示输入、延时、输出节点，门节点已自动过滤。</p>
        </div>
      </div>

      {error && <div className="users-page__error">{error}</div>}
      {loading ? (
        <p className="users-page__loading">加载中…</p>
      ) : !logic ? (
        <p className="users-page__empty">
          逻辑框图不存在，<Link to="/admin/logic-learning">返回列表</Link>。
        </p>
      ) : (
        <div className="users-page__table-wrap">
          <table className="users-page__table">
            <thead>
              <tr>
                <th>节点名称</th>
                <th>节点 ID</th>
                <th>类型</th>
                <th>认知条目数</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {nodes.length === 0 ? (
                <tr>
                  <td colSpan={5} className="users-page__empty-cell">暂无可配置节点</td>
                </tr>
              ) : (
                nodes.map((node) => (
                  <tr key={node.nodeId}>
                    <td>{node.nodeName}</td>
                    <td>{node.nodeId}</td>
                    <td>
                      <span className="logic-learning__tag">
                        {NODE_TYPE_LABELS[node.nodeType] ?? node.nodeType}
                      </span>
                    </td>
                    <td>{node.itemCount}</td>
                    <td>
                      <Link
                        className="users-page__link"
                        to={`/admin/logic-learning/logics/${logicDiagramIdNum}/nodes/${encodeURIComponent(node.nodeId)}/items`}
                      >
                        认知条目
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
