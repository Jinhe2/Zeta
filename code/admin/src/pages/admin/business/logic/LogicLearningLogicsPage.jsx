/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../../../../api/client'
import '../UsersPage.css'
import './LogicLearningLogicsPage.css'

export default function LogicLearningLogicsPage() {
  const { deviceId } = useParams()
  const deviceIdNum = Number(deviceId)
  const [device, setDevice] = useState(null)
  const [logics, setLogics] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [sortOrders, setSortOrders] = useState({})
  const [sequences, setSequences] = useState({})
  const [saving, setSaving] = useState(false)

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
      setSequences(Object.fromEntries(logicData.map((logic) => [logic.id, logic.wholeExperimentSequence ?? 1])))
      setSortOrders(Object.fromEntries(logicData.map((logic) => [logic.id, logic.sortOrder ?? 0])))
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

  const changedLogics = logics.filter((logic) => {
    const rawSort = String(sortOrders[logic.id] ?? '')
    return rawSort.trim() === '' || Number(rawSort) !== (logic.sortOrder ?? 0)
      || Number(sequences[logic.id]) !== (logic.wholeExperimentSequence ?? 1)
  })
  const dirty = changedLogics.length > 0

  useEffect(() => {
    const beforeUnload = (event) => {
      if (!dirty) return
      event.preventDefault()
      event.returnValue = ''
    }
    const captureLinks = (event) => {
      if (dirty && event.target.closest?.('a[href]')
        && !window.confirm('排序和序列存在未保存修改，确定离开吗？')) {
        event.preventDefault()
        event.stopPropagation()
      }
    }
    window.addEventListener('beforeunload', beforeUnload)
    document.addEventListener('click', captureLinks, true)
    return () => {
      window.removeEventListener('beforeunload', beforeUnload)
      document.removeEventListener('click', captureLinks, true)
    }
  }, [dirty])

  const saveAll = async () => {
    const items = []
    for (const logic of changedLogics) {
      const rawSort = String(sortOrders[logic.id] ?? '').trim()
      const sortOrder = Number(rawSort)
      const wholeExperimentSequence = Number(sequences[logic.id])
      if (!rawSort || !Number.isInteger(sortOrder) || sortOrder < -2147483648 || sortOrder > 2147483647) {
        setError(`「${logic.title}」的排序序号必须是有效整数`)
        return
      }
      if (![1, 2, 3].includes(wholeExperimentSequence)) {
        setError(`「${logic.title}」请选择序列 1、2、3`)
        return
      }
      items.push({ logicDiagramId: logic.id, sortOrder, wholeExperimentSequence })
    }
    if (items.length === 0) return
    setSaving(true)
    setError('')
    setMessage('')
    try {
      const saved = await api.saveLogicLearningConfigs(deviceIdNum, items)
      const updates = new Map(saved.map((item) => [item.logicDiagramId, item]))
      const next = logics.map((logic) => ({ ...logic, ...updates.get(logic.id) }))
        .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.id - b.id)
      setLogics(next)
      setSortOrders(Object.fromEntries(next.map((logic) => [logic.id, logic.sortOrder ?? 0])))
      setSequences(Object.fromEntries(next.map((logic) => [logic.id, logic.wholeExperimentSequence ?? 1])))
      setMessage('排序和整组试验序列已全部保存')
    } catch (err) {
      setError(err.message || '保存失败，修改已保留，请重试')
    } finally {
      setSaving(false)
    }
  }

  if (!deviceId || Number.isNaN(deviceIdNum)) {
    return <Navigate to="/admin/logic-learning" replace />
  }

  return (
    <div className="users-page logic-list-page">
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
          <p className="users-page__desc">排序序号控制学员端显示顺序；整组试验序列可选 1、2、3。修改后统一保存。</p>
        </div>
        <div className="logic-list-page__save-bar">
          <span className={dirty ? 'logic-list-page__pending' : ''}>
            {dirty ? `已修改 ${changedLogics.length} 条` : '暂无未保存修改'}
          </span>
          <button type="button" className="users-page__btn users-page__btn--primary"
            onClick={saveAll} disabled={saving || loading || !dirty}>
            {saving ? '保存中…' : '保存修改'}
          </button>
        </div>
      </div>

      {error && <div className="users-page__error">{error}</div>}
      {message && <div className="users-page__message">{message}</div>}
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
                <th>类型</th>
                <th>描述</th>
                <th>排序序号</th>
                <th>整组试验序列</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {logics.length === 0 ? (
                <tr>
                  <td colSpan={6} className="users-page__empty-cell">暂无逻辑框图</td>
                </tr>
              ) : (
                logics.map((logic) => (
                  <tr key={logic.id}>
                    <td><strong className="logic-list-page__name">{logic.title}</strong><span className="logic-list-page__code">{logic.code}</span></td>
                    <td>{logic.category || '—'}</td>
                    <td className="logic-list-page__description">{logic.description || '—'}</td>
                    <td>
                      <input
                        className="logic-list-page__sort-input"
                        disabled={saving}
                        type="number"
                        step="1"
                        value={sortOrders[logic.id] ?? 0}
                        onChange={(e) => { setSortOrders((current) => ({ ...current, [logic.id]: e.target.value })); setMessage('') }}
                        aria-label={`${logic.title}排序序号`}
                      />
                    </td>
                    <td>
                      <select className="logic-list-page__sequence" value={sequences[logic.id] ?? 1}
                        aria-label={`${logic.title}整组试验序列`}
                        disabled={saving}
                        onChange={(e) => { setSequences((current) => ({ ...current, [logic.id]: Number(e.target.value) })); setMessage('') }}>
                        {[1, 2, 3].map((sequence) => <option key={sequence} value={sequence}>序列{sequence}</option>)}
                      </select>
                    </td>
                    <td><div className="logic-list-page__links">
                      <Link className="users-page__link" to={`/admin/logic-learning/logics/${logic.id}/nodes`}>
                        逻辑节点
                      </Link>
                      <Link className="users-page__link" to={`/admin/logic-learning/logics/${logic.id}/settings`}>
                        定值校验项目
                      </Link>
                      <Link className="users-page__link" to={`/admin/logic-learning/logics/${logic.id}/soft-pressboards`}>
                        软压板校验项目
                      </Link>
                      <Link className="users-page__link" to={`/admin/logic-learning/logics/${logic.id}/hard-pressboards`}>
                        硬压板校验项目
                      </Link>
                      <Link className="users-page__link" to={`/admin/logic-learning/logics/${logic.id}/wiring`}>
                        试验仪接线要求
                      </Link>
                      <Link className="users-page__link" to={`/admin/logic-learning/logics/${logic.id}/guide`}>
                        实验引导
                      </Link>
                    </div></td>
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
