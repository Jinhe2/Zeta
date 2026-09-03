/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../../../../api/client'
import '../UsersPage.css'
import './SettingListPage.css'

export default function LogicLearningGroupsPage() {
  const { deviceId } = useParams()
  const deviceIdNum = Number(deviceId)
  const [device, setDevice] = useState(null)
  const [groups, setGroups] = useState([])
  const [logics, setLogics] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [editing, setEditing] = useState(null) // null | { id?, name, members: Long[] }
  const [saving, setSaving] = useState(false)

  const loadData = useCallback(async () => {
    if (!deviceIdNum) return
    setLoading(true)
    setError('')
    try {
      const [deviceData, groupData, logicData] = await Promise.all([
        api.getKnowledgeDevice(deviceIdNum),
        api.listLogicGroups(deviceIdNum),
        api.listKnowledgeDeviceProtectionLogics(deviceIdNum),
      ])
      setDevice(deviceData)
      setGroups(groupData)
      setLogics(logicData)
    } catch (err) {
      setError(err.message || '加载组合逻辑失败')
      setDevice(null)
      setGroups([])
      setLogics([])
    } finally {
      setLoading(false)
    }
  }, [deviceIdNum])

  useEffect(() => {
    loadData()
  }, [loadData])

  const startCreate = () => {
    setEditing({ name: '', members: [] })
    setMessage('')
    setError('')
  }

  const startEdit = async (group) => {
    setError('')
    setMessage('')
    try {
      const detail = await api.getLogicGroup(group.id)
      setEditing({
        id: group.id,
        name: detail.name,
        members: (detail.members ?? []).map((m) => m.logicDiagramId),
      })
    } catch (err) {
      setError(err.message || '加载组合逻辑详情失败')
    }
  }

  const cancelEdit = () => {
    setEditing(null)
    setError('')
  }

  const toggleMember = (logicId) => {
    setEditing((current) => {
      if (!current) return current
      const members = current.members.includes(logicId)
        ? current.members.filter((id) => id !== logicId)
        : [...current.members, logicId]
      return { ...current, members }
    })
  }

  const moveMember = (index, delta) => {
    setEditing((current) => {
      if (!current) return current
      const target = index + delta
      if (target < 0 || target >= current.members.length) return current
      const members = [...current.members]
      const [item] = members.splice(index, 1)
      members.splice(target, 0, item)
      return { ...current, members }
    })
  }

  const save = async () => {
    const name = (editing?.name ?? '').trim()
    if (!name) {
      setError('组合逻辑名称不能为空')
      return
    }
    if (!editing.members.length) {
      setError('请至少选择一个基础逻辑')
      return
    }
    setSaving(true)
    setError('')
    setMessage('')
    const payload = {
      name,
      members: editing.members.map((logicId, index) => ({ logicDiagramId: logicId, sortOrder: index })),
    }
    try {
      if (editing.id) {
        await api.updateLogicGroup(editing.id, payload)
      } else {
        await api.createLogicGroup(deviceIdNum, payload)
      }
      setMessage(editing.id ? '组合逻辑已更新' : '组合逻辑已创建')
      setEditing(null)
      await loadData()
    } catch (err) {
      setError(err.message || '保存组合逻辑失败')
    } finally {
      setSaving(false)
    }
  }

  const remove = async (group) => {
    if (!window.confirm(`确定删除组合逻辑「${group.name}」吗？`)) return
    setError('')
    setMessage('')
    try {
      await api.deleteLogicGroup(group.id)
      setMessage(`「${group.name}」已删除`)
      await loadData()
    } catch (err) {
      setError(err.message || '删除组合逻辑失败')
    }
  }

  const logicTitle = (logicId) => {
    const logic = logics.find((item) => item.id === logicId)
    return logic ? `${logic.title}（${logic.code}）` : `#${logicId}`
  }

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
                <span>{device.name} — 组合逻辑</span>
              </>
            )}
          </p>
          <h2 className="users-page__title">{device ? `${device.name} — 组合逻辑` : '组合逻辑'}</h2>
          <p className="users-page__desc">将当前装置的若干基础逻辑按序拼接为组合逻辑，用于组合基准配置与组合实验。</p>
        </div>
        {!editing && (
          <button type="button" className="setting-list-page__save" onClick={startCreate}>
            + 新建组合
          </button>
        )}
      </div>

      {error && <div className="users-page__error">{error}</div>}
      {message && <div className="users-page__message">{message}</div>}

      {editing && (
        <div className="logic-group-editor">
          <h3 className="logic-group-editor__title">{editing.id ? '编辑组合逻辑' : '新建组合逻辑'}</h3>
          <label className="logic-group-editor__field">
            <span>名称</span>
            <input
              type="text"
              value={editing.name}
              onChange={(e) => setEditing((c) => ({ ...c, name: e.target.value }))}
              placeholder="例如：差动保护 + 重合闸"
            />
          </label>

          <div className="logic-group-editor__members">
            <span className="logic-group-editor__field-label">选择基础逻辑（按顺序组合，可上下移动调整顺序）</span>
            {logics.length === 0 ? (
              <p className="users-page__empty">当前装置暂无基础逻辑</p>
            ) : (
              <ul className="logic-group-editor__member-list">
                {editing.members.map((logicId, index) => (
                  <li key={logicId} className="logic-group-editor__member-item">
                    <span className="logic-group-editor__member-index">{index + 1}</span>
                    <span className="logic-group-editor__member-name">{logicTitle(logicId)}</span>
                    <button type="button" className="users-page__link" disabled={index === 0} onClick={() => moveMember(index, -1)}>上移</button>
                    <button type="button" className="users-page__link" disabled={index === editing.members.length - 1} onClick={() => moveMember(index, 1)}>下移</button>
                    <button type="button" className="users-page__link users-page__link--danger" onClick={() => toggleMember(logicId)}>移除</button>
                  </li>
                ))}
              </ul>
            )}
            {logics.length > 0 && (
              <div className="logic-group-editor__candidates">
                {logics.map((logic) => {
                  const checked = editing.members.includes(logic.id)
                  return (
                    <label key={logic.id} className="logic-group-editor__candidate">
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => toggleMember(logic.id)}
                      />
                      <span>{logic.title}（{logic.code}）</span>
                    </label>
                  )
                })}
              </div>
            )}
          </div>

          <div className="setting-list-page__actions">
            <button type="button" className="setting-list-page__save" onClick={save} disabled={saving}>
              {saving ? '保存中…' : '保存'}
            </button>
            <button type="button" className="users-page__link" onClick={cancelEdit}>取消</button>
          </div>
        </div>
      )}

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
                <th>成员数</th>
                <th>成员（按顺序）</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {groups.length === 0 ? (
                <tr>
                  <td colSpan={4} className="users-page__empty-cell">暂无组合逻辑，点击右上角「新建组合」创建。</td>
                </tr>
              ) : (
                groups.map((group) => (
                  <tr key={group.id}>
                    <td>{group.name}</td>
                    <td>{group.memberCount}</td>
                    <td>—</td>
                    <td className="users-page__actions">
                      <button type="button" className="users-page__link" onClick={() => startEdit(group)}>
                        编辑
                      </button>
                      <Link className="users-page__link" to={`/admin/logic-learning/groups/${group.id}/settings`}>
                        定值校验项目
                      </Link>
                      <Link className="users-page__link" to={`/admin/logic-learning/groups/${group.id}/soft-pressboards`}>
                        软压板校验项目
                      </Link>
                      <Link className="users-page__link" to={`/admin/logic-learning/groups/${group.id}/hard-pressboards`}>
                        硬压板校验项目
                      </Link>
                      <Link className="users-page__link" to={`/admin/logic-learning/groups/${group.id}/wiring`}>
                        试验仪接线
                      </Link>
                      <Link className="users-page__link" to={`/admin/logic-learning/groups/${group.id}/guide`}>
                        实验引导
                      </Link>
                      <button type="button" className="users-page__link users-page__link--danger" onClick={() => remove(group)}>
                        删除
                      </button>
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
