import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../../api/client'
import './UsersPage.css'

const EMPTY_CREATE = {
  code: '',
  title: '',
  description: '',
  category: '',
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

export default function DeviceProtectionLogicsPage() {
  const { cabinetId, deviceId } = useParams()
  const cabinetIdNum = Number(cabinetId)
  const deviceIdNum = Number(deviceId)

  const [device, setDevice] = useState(null)
  const [logics, setLogics] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_CREATE)
  const [creating, setCreating] = useState(false)

  const [editingLogic, setEditingLogic] = useState(null)
  const [editForm, setEditForm] = useState({
    title: '',
    description: '',
    category: '',
    sortOrder: 0,
    enabled: true,
  })
  const [saving, setSaving] = useState(false)

  const loadData = useCallback(async () => {
    if (!deviceIdNum) return
    setLoading(true)
    setError('')
    setLogics([])
    try {
      const [deviceData, logicData] = await Promise.all([
        api.getDevice(deviceIdNum),
        api.listDeviceProtectionLogics(deviceIdNum),
      ])
      setDevice(deviceData)
      setLogics(logicData)
    } catch (err) {
      setError(err.message || '加载保护逻辑列表失败')
      setDevice(null)
      setLogics([])
    } finally {
      setLoading(false)
    }
  }, [deviceIdNum])

  useEffect(() => {
    loadData()
  }, [loadData])

  if (!cabinetId || !deviceId || Number.isNaN(cabinetIdNum) || Number.isNaN(deviceIdNum)) {
    return <Navigate to="/admin/cabinets" replace />
  }

  const flash = (text) => {
    setMessage(text)
    setTimeout(() => setMessage(''), 3000)
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    setCreating(true)
    setError('')
    try {
      await api.createDeviceProtectionLogic(deviceIdNum, {
        ...createForm,
        sortOrder: Number(createForm.sortOrder),
      })
      setShowCreate(false)
      setCreateForm(EMPTY_CREATE)
      flash('保护逻辑创建成功')
      await loadData()
    } catch (err) {
      setError(err.message || '创建失败')
    } finally {
      setCreating(false)
    }
  }

  const openEdit = (logic) => {
    setEditingLogic(logic)
    setEditForm({
      title: logic.title,
      description: logic.description || '',
      category: logic.category,
      sortOrder: logic.sortOrder,
      enabled: logic.enabled,
    })
  }

  const handleUpdate = async (e) => {
    e.preventDefault()
    if (!editingLogic) return
    setSaving(true)
    setError('')
    try {
      await api.updateProtectionLogicAdmin(editingLogic.id, {
        ...editForm,
        sortOrder: Number(editForm.sortOrder),
      })
      setEditingLogic(null)
      flash('保护逻辑已更新')
      await loadData()
    } catch (err) {
      setError(err.message || '更新失败')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (logic) => {
    if (!window.confirm(`确定删除保护逻辑「${logic.title}」（${logic.code}）？`)) {
      return
    }
    setError('')
    try {
      await api.deleteProtectionLogicAdmin(logic.id)
      flash('保护逻辑已删除')
      await loadData()
    } catch (err) {
      setError(err.message || '删除失败')
    }
  }

  return (
    <div className="users-page">
      <div className="users-page__header">
        <div>
          <p className="users-page__breadcrumb">
            <Link to="/admin/cabinets">屏柜管理</Link>
            <span> / </span>
            <Link to={`/admin/cabinets/${cabinetId}/devices`}>设备管理</Link>
            <span> / </span>
            <span>保护逻辑管理</span>
          </p>
          <h2 className="users-page__title">
            {device ? `${device.name} — 保护逻辑管理` : '保护逻辑管理'}
          </h2>
          <p className="users-page__desc">
            {device
              ? `设备编码：${device.code}，管理该设备下的保护逻辑（装置）。`
              : '管理设备下的保护逻辑。'}
          </p>
        </div>
        <button
          type="button"
          className="users-page__btn users-page__btn--primary"
          onClick={() => setShowCreate(true)}
          disabled={!device}
        >
          新建保护逻辑
        </button>
      </div>

      {message && <div className="users-page__message">{message}</div>}
      {error && <div className="users-page__error">{error}</div>}

      {loading ? (
        <p className="users-page__loading">加载中…</p>
      ) : !device ? (
        <p className="users-page__empty">
          设备不存在，<Link to={`/admin/cabinets/${cabinetId}/devices`}>返回设备列表</Link>。
        </p>
      ) : logics.length === 0 ? (
        <p className="users-page__empty">该设备下暂无保护逻辑，点击右上角新建。</p>
      ) : (
        <div className="users-page__table-wrap">
          <table className="users-page__table">
            <thead>
              <tr>
                <th>编码</th>
                <th>名称</th>
                <th>类别</th>
                <th>描述</th>
                <th>排序</th>
                <th>状态</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {logics.map((logic) => (
                <tr key={logic.id}>
                  <td>{logic.code}</td>
                  <td>{logic.title}</td>
                  <td>{logic.category}</td>
                  <td className="users-page__desc-cell">{logic.description || '—'}</td>
                  <td>{logic.sortOrder}</td>
                  <td>
                    <span
                      className={`users-page__status${
                        logic.enabled ? ' users-page__status--on' : ' users-page__status--off'
                      }`}
                    >
                      {logic.enabled ? '启用' : '停用'}
                    </span>
                  </td>
                  <td>{formatDate(logic.createdAt)}</td>
                  <td>
                    <div className="users-page__actions">
                      <Link
                        to={`/admin/cabinets/${cabinetId}/devices/${deviceId}/protection-logics/${logic.id}/config`}
                        className="users-page__link"
                      >
                        配置
                      </Link>
                      <button type="button" className="users-page__link" onClick={() => openEdit(logic)}>
                        编辑
                      </button>
                      <button
                        type="button"
                        className="users-page__link users-page__link--danger"
                        onClick={() => handleDelete(logic)}
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
            <h3>新建保护逻辑</h3>
            <label>
              编码
              <input
                value={createForm.code}
                onChange={(e) => setCreateForm({ ...createForm, code: e.target.value })}
                required
                autoFocus
                placeholder="如 overcurrent-1"
              />
            </label>
            <label>
              名称
              <input
                value={createForm.title}
                onChange={(e) => setCreateForm({ ...createForm, title: e.target.value })}
                required
              />
            </label>
            <label>
              保护类别
              <input
                value={createForm.category}
                onChange={(e) => setCreateForm({ ...createForm, category: e.target.value })}
                required
                placeholder="如 过流保护"
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

      {editingLogic && (
        <div className="users-page__overlay" onClick={() => setEditingLogic(null)}>
          <form className="users-page__dialog" onClick={(e) => e.stopPropagation()} onSubmit={handleUpdate}>
            <h3>编辑保护逻辑 — {editingLogic.code}</h3>
            <label>
              名称
              <input
                value={editForm.title}
                onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
                required
                autoFocus
              />
            </label>
            <label>
              保护类别
              <input
                value={editForm.category}
                onChange={(e) => setEditForm({ ...editForm, category: e.target.value })}
                required
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
              <button type="button" className="users-page__btn" onClick={() => setEditingLogic(null)}>
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
