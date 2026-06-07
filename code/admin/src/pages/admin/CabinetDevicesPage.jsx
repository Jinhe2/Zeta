import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
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

export default function CabinetDevicesPage() {
  const { cabinetId } = useParams()
  const id = Number(cabinetId)

  const [cabinet, setCabinet] = useState(null)
  const [devices, setDevices] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_CREATE)
  const [creating, setCreating] = useState(false)

  const [editingDevice, setEditingDevice] = useState(null)
  const [editForm, setEditForm] = useState({ name: '', description: '', sortOrder: 0, enabled: true })
  const [saving, setSaving] = useState(false)

  const loadData = useCallback(async () => {
    if (!id) return
    setLoading(true)
    setError('')
    setDevices([])
    try {
      const [cabinetData, deviceData] = await Promise.all([
        api.getCabinet(id),
        api.listCabinetDevices(id),
      ])
      setCabinet(cabinetData)
      setDevices(deviceData)
    } catch (err) {
      setError(err.message || '加载设备列表失败')
      setCabinet(null)
      setDevices([])
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    loadData()
  }, [loadData])

  if (!cabinetId || Number.isNaN(id)) {
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
      await api.createCabinetDevice(id, {
        ...createForm,
        sortOrder: Number(createForm.sortOrder),
      })
      setShowCreate(false)
      setCreateForm(EMPTY_CREATE)
      flash('设备创建成功')
      await loadData()
    } catch (err) {
      setError(err.message || '创建失败')
    } finally {
      setCreating(false)
    }
  }

  const openEdit = (device) => {
    setEditingDevice(device)
    setEditForm({
      name: device.name,
      description: device.description || '',
      sortOrder: device.sortOrder,
      enabled: device.enabled,
    })
  }

  const handleUpdate = async (e) => {
    e.preventDefault()
    if (!editingDevice) return
    setSaving(true)
    setError('')
    try {
      await api.updateDevice(editingDevice.id, {
        ...editForm,
        sortOrder: Number(editForm.sortOrder),
      })
      setEditingDevice(null)
      flash('设备已更新')
      await loadData()
    } catch (err) {
      setError(err.message || '更新失败')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (device) => {
    if (device.protectionLogicCount > 0) {
      setError('该设备下存在保护逻辑，请先移除后再删除')
      return
    }
    if (!window.confirm(`确定删除设备「${device.name}」（${device.code}）？`)) {
      return
    }
    setError('')
    try {
      await api.deleteDevice(device.id)
      flash('设备已删除')
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
            <span>设备管理</span>
          </p>
          <h2 className="users-page__title">
            {cabinet ? `${cabinet.name} — 设备管理` : '设备管理'}
          </h2>
          <p className="users-page__desc">
            {cabinet
              ? `屏柜编码：${cabinet.code}，管理该屏柜下的设备，设备下可挂载保护逻辑。`
              : '管理屏柜下的设备。'}
          </p>
        </div>
        <button
          type="button"
          className="users-page__btn users-page__btn--primary"
          onClick={() => setShowCreate(true)}
          disabled={!cabinet}
        >
          新建设备
        </button>
      </div>

      {message && <div className="users-page__message">{message}</div>}
      {error && <div className="users-page__error">{error}</div>}

      {loading ? (
        <p className="users-page__loading">加载中…</p>
      ) : !cabinet ? (
        <p className="users-page__empty">屏柜不存在，<Link to="/admin/cabinets">返回列表</Link>。</p>
      ) : devices.length === 0 ? (
        <p className="users-page__empty">该屏柜下暂无设备，点击右上角新建。</p>
      ) : (
        <div className="users-page__table-wrap">
          <table className="users-page__table">
            <thead>
              <tr>
                <th>编码</th>
                <th>名称</th>
                <th>描述</th>
                <th>排序</th>
                <th>保护逻辑数</th>
                <th>认知条目数</th>
                <th>状态</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {devices.map((device) => (
                <tr key={device.id}>
                  <td>{device.code}</td>
                  <td>{device.name}</td>
                  <td className="users-page__desc-cell">{device.description || '—'}</td>
                  <td>{device.sortOrder}</td>
                  <td>{device.protectionLogicCount}</td>
                  <td>{device.cognitionItemCount ?? 0}</td>
                  <td>
                    <span
                      className={`users-page__status${
                        device.enabled ? ' users-page__status--on' : ' users-page__status--off'
                      }`}
                    >
                      {device.enabled ? '启用' : '停用'}
                    </span>
                  </td>
                  <td>{formatDate(device.createdAt)}</td>
                  <td>
                    <div className="users-page__actions">
                      <Link
                        to={`/admin/cabinets/${cabinetId}/devices/${device.id}/cognition-items`}
                        className="users-page__link"
                      >
                        认知条目
                      </Link>
                      <Link
                        to={`/admin/cabinets/${cabinetId}/devices/${device.id}/protection-logics`}
                        className="users-page__link"
                      >
                        保护逻辑
                      </Link>
                      <button type="button" className="users-page__link" onClick={() => openEdit(device)}>
                        编辑
                      </button>
                      <button
                        type="button"
                        className="users-page__link users-page__link--danger"
                        disabled={device.protectionLogicCount > 0}
                        title={device.protectionLogicCount > 0 ? '请先移除设备下的保护逻辑' : undefined}
                        onClick={() => handleDelete(device)}
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
            <h3>新建设备</h3>
            <label>
              编码
              <input
                value={createForm.code}
                onChange={(e) => setCreateForm({ ...createForm, code: e.target.value })}
                required
                autoFocus
                placeholder="如 device-line-a"
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

      {editingDevice && (
        <div className="users-page__overlay" onClick={() => setEditingDevice(null)}>
          <form className="users-page__dialog" onClick={(e) => e.stopPropagation()} onSubmit={handleUpdate}>
            <h3>编辑设备 — {editingDevice.code}</h3>
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
              <button type="button" className="users-page__btn" onClick={() => setEditingDevice(null)}>
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
