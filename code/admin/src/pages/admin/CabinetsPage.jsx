import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api/client'
import './UsersPage.css'

const EMPTY_CREATE = {
  code: '',
  name: '',
  description: '',
  sortOrder: 0,
  enabled: true,
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function CabinetsPage() {
  const [cabinets, setCabinets] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_CREATE)
  const [creating, setCreating] = useState(false)

  const [editingCabinet, setEditingCabinet] = useState(null)
  const [editForm, setEditForm] = useState({ name: '', description: '', sortOrder: 0, enabled: true })
  const [saving, setSaving] = useState(false)

  const loadCabinets = useCallback(async () => {
    setLoading(true)
    setError('')
    setCabinets([])
    try {
      const data = await api.listCabinets()
      setCabinets(data)
    } catch (err) {
      setError(err.message || '加载屏柜列表失败')
      setCabinets([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadCabinets()
  }, [loadCabinets])

  const flash = (text) => {
    setMessage(text)
    setTimeout(() => setMessage(''), 3000)
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    setCreating(true)
    setError('')
    try {
      await api.createCabinet({
        ...createForm,
        sortOrder: Number(createForm.sortOrder),
      })
      setShowCreate(false)
      setCreateForm(EMPTY_CREATE)
      flash('屏柜创建成功')
      await loadCabinets()
    } catch (err) {
      setError(err.message || '创建失败')
    } finally {
      setCreating(false)
    }
  }

  const openEdit = (cabinet) => {
    setEditingCabinet(cabinet)
    setEditForm({
      name: cabinet.name,
      description: cabinet.description || '',
      sortOrder: cabinet.sortOrder,
      enabled: cabinet.enabled,
    })
  }

  const handleUpdate = async (e) => {
    e.preventDefault()
    if (!editingCabinet) return
    setSaving(true)
    setError('')
    try {
      await api.updateCabinet(editingCabinet.id, {
        ...editForm,
        sortOrder: Number(editForm.sortOrder),
      })
      setEditingCabinet(null)
      flash('屏柜已更新')
      await loadCabinets()
    } catch (err) {
      setError(err.message || '更新失败')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (cabinet) => {
    if (cabinet.deviceCount > 0) {
      setError('该屏柜下存在设备，请先移除设备后再删除')
      return
    }
    if (!window.confirm(`确定删除屏柜「${cabinet.name}」（${cabinet.code}）？`)) {
      return
    }
    setError('')
    try {
      await api.deleteCabinet(cabinet.id)
      flash('屏柜已删除')
      await loadCabinets()
    } catch (err) {
      setError(err.message || '删除失败')
    }
  }

  return (
    <div className="users-page">
      <div className="users-page__header">
        <div>
          <h2 className="users-page__title">屏柜管理</h2>
          <p className="users-page__desc">管理知识结构顶层屏柜，其下可挂载多个设备。</p>
        </div>
        <button
          type="button"
          className="users-page__btn users-page__btn--primary"
          onClick={() => setShowCreate(true)}
        >
          新建屏柜
        </button>
      </div>

      {message && <div className="users-page__message">{message}</div>}
      {error && <div className="users-page__error">{error}</div>}

      {loading ? (
        <p className="users-page__loading">加载中…</p>
      ) : cabinets.length === 0 ? (
        <p className="users-page__empty">暂无屏柜，点击右上角新建。</p>
      ) : (
        <div className="users-page__table-wrap">
          <table className="users-page__table">
            <thead>
              <tr>
                <th>编码</th>
                <th>名称</th>
                <th>描述</th>
                <th>排序</th>
                <th>设备数</th>
                <th>状态</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {cabinets.map((cabinet) => (
                <tr key={cabinet.id}>
                  <td>{cabinet.code}</td>
                  <td>{cabinet.name}</td>
                  <td className="users-page__desc-cell">{cabinet.description || '—'}</td>
                  <td>{cabinet.sortOrder}</td>
                  <td>{cabinet.deviceCount}</td>
                  <td>
                    <span
                      className={`users-page__status${
                        cabinet.enabled ? ' users-page__status--on' : ' users-page__status--off'
                      }`}
                    >
                      {cabinet.enabled ? '启用' : '停用'}
                    </span>
                  </td>
                  <td>{formatDate(cabinet.createdAt)}</td>
                  <td>
                    <div className="users-page__actions">
                      <Link
                        to={`/admin/cabinets/${cabinet.id}/devices`}
                        className="users-page__link"
                      >
                        设备管理
                      </Link>
                      <button type="button" className="users-page__link" onClick={() => openEdit(cabinet)}>
                        编辑
                      </button>
                      <button
                        type="button"
                        className="users-page__link users-page__link--danger"
                        disabled={cabinet.deviceCount > 0}
                        title={cabinet.deviceCount > 0 ? '请先移除屏柜下的设备' : undefined}
                        onClick={() => handleDelete(cabinet)}
                      >
                        删除
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showCreate && (
        <div className="users-page__overlay" onClick={() => setShowCreate(false)}>
          <form className="users-page__dialog" onClick={(e) => e.stopPropagation()} onSubmit={handleCreate}>
            <h3>新建屏柜</h3>
            <label>
              编码
              <input
                value={createForm.code}
                onChange={(e) => setCreateForm({ ...createForm, code: e.target.value })}
                required
                autoFocus
                placeholder="如 cabinet-line-220"
              />
            </label>
            <label>
              名称
              <input
                value={createForm.name}
                onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
                required
              />
            </label>
            <label>
              描述
              <textarea
                value={createForm.description}
                onChange={(e) => setCreateForm({ ...createForm, description: e.target.value })}
                rows={3}
              />
            </label>
            <label>
              排序
              <input
                type="number"
                value={createForm.sortOrder}
                onChange={(e) => setCreateForm({ ...createForm, sortOrder: e.target.value })}
                required
              />
            </label>
            <label className="users-page__checkbox">
              <input
                type="checkbox"
                checked={createForm.enabled}
                onChange={(e) => setCreateForm({ ...createForm, enabled: e.target.checked })}
              />
              启用
            </label>
            <div className="users-page__dialog-actions">
              <button type="button" className="users-page__btn" onClick={() => setShowCreate(false)}>
                取消
              </button>
              <button type="submit" className="users-page__btn users-page__btn--primary" disabled={creating}>
                {creating ? '创建中…' : '创建'}
              </button>
            </div>
          </form>
        </div>
      )}

      {editingCabinet && (
        <div className="users-page__overlay" onClick={() => setEditingCabinet(null)}>
          <form className="users-page__dialog" onClick={(e) => e.stopPropagation()} onSubmit={handleUpdate}>
            <h3>编辑屏柜 — {editingCabinet.code}</h3>
            <label>
              名称
              <input
                value={editForm.name}
                onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                required
                autoFocus
              />
            </label>
            <label>
              描述
              <textarea
                value={editForm.description}
                onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
                rows={3}
              />
            </label>
            <label>
              排序
              <input
                type="number"
                value={editForm.sortOrder}
                onChange={(e) => setEditForm({ ...editForm, sortOrder: e.target.value })}
                required
              />
            </label>
            <label className="users-page__checkbox">
              <input
                type="checkbox"
                checked={editForm.enabled}
                onChange={(e) => setEditForm({ ...editForm, enabled: e.target.checked })}
              />
              启用
            </label>
            <div className="users-page__dialog-actions">
              <button type="button" className="users-page__btn" onClick={() => setEditingCabinet(null)}>
                取消
              </button>
              <button type="submit" className="users-page__btn users-page__btn--primary" disabled={saving}>
                {saving ? '保存中…' : '保存'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}
