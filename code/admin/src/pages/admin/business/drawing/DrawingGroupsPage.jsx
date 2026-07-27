import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../../../../api/client'
import '../UsersPage.css'

const EMPTY_FORM = { drawingType: 'BLUEPRINT', name: '', sortOrder: 0, enabled: true }
const TYPE_LABELS = { BLUEPRINT: '蓝图', WHITEPRINT: '白图' }

function formatDate(iso) {
  return iso ? new Date(iso).toLocaleString('zh-CN') : '—'
}

export default function DrawingGroupsPage() {
  const { cabinetId } = useParams()
  const cabinetIdNum = Number(cabinetId)
  const [cabinet, setCabinet] = useState(null)
  const [groups, setGroups] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_FORM)
  const [editingGroup, setEditingGroup] = useState(null)
  const [editForm, setEditForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)

  const loadData = useCallback(async () => {
    if (!cabinetIdNum) return
    setLoading(true)
    setError('')
    try {
      const [cabinetData, groupData] = await Promise.all([
        api.getKnowledgeCabinet(cabinetIdNum),
        api.listDrawingGroups(cabinetIdNum),
      ])
      setCabinet(cabinetData)
      setGroups(groupData)
    } catch (err) {
      setError(err.message || '加载图纸分组失败')
      setCabinet(null)
      setGroups([])
    } finally {
      setLoading(false)
    }
  }, [cabinetIdNum])

  useEffect(() => {
    const timer = window.setTimeout(loadData, 0)
    return () => window.clearTimeout(timer)
  }, [loadData])

  if (!cabinetId || Number.isNaN(cabinetIdNum)) return <Navigate to="/admin/drawing-learning" replace />

  const flash = (text) => {
    setMessage(text)
    setTimeout(() => setMessage(''), 3000)
  }

  const saveCreate = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      await api.createDrawingGroup(cabinetIdNum, { ...createForm, sortOrder: Number(createForm.sortOrder) })
      setShowCreate(false)
      setCreateForm(EMPTY_FORM)
      flash('图纸分组创建成功')
      await loadData()
    } catch (err) {
      setError(err.message || '创建失败')
    } finally {
      setSaving(false)
    }
  }

  const saveEdit = async (e) => {
    e.preventDefault()
    if (!editingGroup) return
    setSaving(true)
    setError('')
    try {
      await api.updateDrawingGroup(editingGroup.id, { ...editForm, sortOrder: Number(editForm.sortOrder) })
      setEditingGroup(null)
      flash('图纸分组已更新')
      await loadData()
    } catch (err) {
      setError(err.message || '更新失败')
    } finally {
      setSaving(false)
    }
  }

  const deleteGroup = async (group) => {
    if (!window.confirm(`确定删除图纸分组「${group.name}」及其图纸和认知条目？`)) return
    setError('')
    try {
      await api.deleteDrawingGroup(group.id)
      flash('图纸分组已删除')
      await loadData()
    } catch (err) {
      setError(err.message || '删除失败')
    }
  }

  const renderRows = (type) => {
    const rows = groups.filter((group) => group.drawingType === type)
    return rows.length === 0 ? (
      <tr><td colSpan={8} className="users-page__empty-cell">暂无{TYPE_LABELS[type]}分组</td></tr>
    ) : rows.map((group) => (
      <tr key={group.id}>
        <td>{group.name}</td>
        <td>{TYPE_LABELS[group.drawingType]}</td>
        <td>{group.sortOrder}</td>
        <td>{group.pageCount}</td>
        <td>{group.cognitionItemCount}</td>
        <td>{group.enabled ? '启用' : '停用'}</td>
        <td>{formatDate(group.createdAt)}</td>
        <td className="users-page__actions">
          <Link className="users-page__link" to={`/admin/drawing-learning/groups/${group.id}/pages`}>图纸</Link>
          <button type="button" className="users-page__link" onClick={() => {
            setEditingGroup(group)
            setEditForm({
              drawingType: group.drawingType,
              name: group.name,
              sortOrder: group.sortOrder,
              enabled: group.enabled,
            })
          }}>编辑</button>
          <button type="button" className="users-page__link users-page__link--danger" onClick={() => deleteGroup(group)}>删除</button>
        </td>
      </tr>
    ))
  }

  const renderDialog = (title, form, setForm, onSubmit, onCancel) => (
    <div className="users-page__overlay">
      <form className="users-page__dialog" onSubmit={onSubmit}>
        <h3>{title}</h3>
        <label>
          图纸类型
          <select value={form.drawingType} onChange={(e) => setForm({ ...form, drawingType: e.target.value })}>
            <option value="BLUEPRINT">蓝图</option>
            <option value="WHITEPRINT">白图</option>
          </select>
        </label>
        <label>
          分组名称
          <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
        </label>
        <label>
          排序
          <input type="number" value={form.sortOrder} onChange={(e) => setForm({ ...form, sortOrder: e.target.value })} />
        </label>
        <label className="users-page__checkbox">
          <input type="checkbox" checked={form.enabled} onChange={(e) => setForm({ ...form, enabled: e.target.checked })} />
          启用
        </label>
        <div className="users-page__dialog-actions">
          <button type="button" className="users-page__btn" onClick={onCancel}>取消</button>
          <button type="submit" className="users-page__btn users-page__btn--primary" disabled={saving}>
            {saving ? '保存中…' : '保存'}
          </button>
        </div>
      </form>
    </div>
  )

  return (
    <div className="users-page">
      <div className="users-page__header">
        <div>
          <p className="users-page__breadcrumb"><Link to="/admin/drawing-learning">图纸学习</Link><span> / </span><span>{cabinet?.name ?? '分组'}</span></p>
          <h2 className="users-page__title">{cabinet ? `${cabinet.name} — 图纸分组` : '图纸分组'}</h2>
          <p className="users-page__desc">蓝图、白图下的分组会作为学员端左侧分类展示。</p>
        </div>
        <button type="button" className="users-page__btn users-page__btn--primary" onClick={() => setShowCreate(true)}>新增分组</button>
      </div>
      {message && <div className="users-page__message">{message}</div>}
      {error && <div className="users-page__error">{error}</div>}
      {loading ? <p className="users-page__loading">加载中…</p> : (
        ['BLUEPRINT', 'WHITEPRINT'].map((type) => (
          <div className="users-page__table-wrap" key={type}>
            <h3>{TYPE_LABELS[type]}</h3>
            <table className="users-page__table">
              <thead><tr><th>分组</th><th>类型</th><th>排序</th><th>图纸数</th><th>认知条目数</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
              <tbody>{renderRows(type)}</tbody>
            </table>
          </div>
        ))
      )}
      {showCreate && renderDialog('新增图纸分组', createForm, setCreateForm, saveCreate, () => setShowCreate(false))}
      {editingGroup && renderDialog('编辑图纸分组', editForm, setEditForm, saveEdit, () => setEditingGroup(null))}
    </div>
  )
}
