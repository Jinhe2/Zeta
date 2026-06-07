import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../../api/client'
import './UsersPage.css'

const EMPTY_CREATE = {
  title: '',
  content: '',
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

function previewContent(text, max = 48) {
  if (!text) return '—'
  const oneLine = text.replace(/\s+/g, ' ').trim()
  return oneLine.length > max ? `${oneLine.slice(0, max)}…` : oneLine
}

export default function DeviceCognitionItemsPage() {
  const { cabinetId, deviceId } = useParams()
  const cabinetIdNum = Number(cabinetId)
  const deviceIdNum = Number(deviceId)

  const [device, setDevice] = useState(null)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_CREATE)
  const [creating, setCreating] = useState(false)

  const [editingItem, setEditingItem] = useState(null)
  const [editForm, setEditForm] = useState({ title: '', content: '', sortOrder: 0, enabled: true })
  const [saving, setSaving] = useState(false)

  const loadData = useCallback(async () => {
    if (!deviceIdNum) return
    setLoading(true)
    setError('')
    setItems([])
    try {
      const [deviceData, itemData] = await Promise.all([
        api.getDevice(deviceIdNum),
        api.listDeviceCognitionItems(deviceIdNum),
      ])
      setDevice(deviceData)
      setItems(itemData)
    } catch (err) {
      setError(err.message || '加载设备认知条目失败')
      setDevice(null)
      setItems([])
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
      await api.createDeviceCognitionItem(deviceIdNum, {
        ...createForm,
        sortOrder: Number(createForm.sortOrder),
      })
      setShowCreate(false)
      setCreateForm(EMPTY_CREATE)
      flash('认知条目创建成功')
      await loadData()
    } catch (err) {
      setError(err.message || '创建失败')
    } finally {
      setCreating(false)
    }
  }

  const openEdit = (item) => {
    setEditingItem(item)
    setEditForm({
      title: item.title,
      content: item.content,
      sortOrder: item.sortOrder,
      enabled: item.enabled,
    })
  }

  const handleUpdate = async (e) => {
    e.preventDefault()
    if (!editingItem) return
    setSaving(true)
    setError('')
    try {
      await api.updateDeviceCognitionItem(editingItem.id, {
        ...editForm,
        sortOrder: Number(editForm.sortOrder),
      })
      setEditingItem(null)
      flash('认知条目已更新')
      await loadData()
    } catch (err) {
      setError(err.message || '更新失败')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (item) => {
    if (!window.confirm(`确定删除认知条目「${item.title}」？`)) {
      return
    }
    setError('')
    try {
      await api.deleteDeviceCognitionItem(item.id)
      flash('认知条目已删除')
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
            <span>设备认知条目</span>
          </p>
          <h2 className="users-page__title">
            {device ? `${device.name} — 设备认知条目` : '设备认知条目'}
          </h2>
          <p className="users-page__desc">
            {device
              ? `设备编码：${device.code}，从多个角度分条介绍该设备。`
              : '管理设备的多角度认知介绍内容。'}
          </p>
        </div>
        <button
          type="button"
          className="users-page__btn users-page__btn--primary"
          onClick={() => setShowCreate(true)}
          disabled={!device}
        >
          新建认知条目
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
      ) : items.length === 0 ? (
        <p className="users-page__empty">该设备下暂无认知条目，点击右上角新建。</p>
      ) : (
        <div className="users-page__table-wrap">
          <table className="users-page__table">
            <thead>
              <tr>
                <th>标题</th>
                <th>内容摘要</th>
                <th>排序</th>
                <th>状态</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <td>{item.title}</td>
                  <td className="users-page__desc-cell">{previewContent(item.content)}</td>
                  <td>{item.sortOrder}</td>
                  <td>
                    <span
                      className={`users-page__status${
                        item.enabled ? ' users-page__status--on' : ' users-page__status--off'
                      }`}
                    >
                      {item.enabled ? '启用' : '停用'}
                    </span>
                  </td>
                  <td>{formatDate(item.createdAt)}</td>
                  <td>
                    <div className="users-page__actions">
                      <button type="button" className="users-page__link" onClick={() => openEdit(item)}>
                        编辑
                      </button>
                      <button
                        type="button"
                        className="users-page__link users-page__link--danger"
                        onClick={() => handleDelete(item)}
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
            <h3>新建认知条目</h3>
            <label>
              标题
              <input
                value={createForm.title}
                onChange={(e) => setCreateForm({ ...createForm, title: e.target.value })}
                required
                autoFocus
                placeholder="如 装置面板与指示灯"
              />
            </label>
            <label>
              介绍内容
              <textarea
                value={createForm.content}
                onChange={(e) => setCreateForm({ ...createForm, content: e.target.value })}
                rows={6}
                required
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

      {editingItem && (
        <div className="users-page__overlay" onClick={() => setEditingItem(null)}>
          <form className="users-page__dialog" onClick={(e) => e.stopPropagation()} onSubmit={handleUpdate}>
            <h3>编辑认知条目</h3>
            <label>
              标题
              <input
                value={editForm.title}
                onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
                required
                autoFocus
              />
            </label>
            <label>
              介绍内容
              <textarea
                value={editForm.content}
                onChange={(e) => setEditForm({ ...editForm, content: e.target.value })}
                rows={6}
                required
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
              <button type="button" className="users-page__btn" onClick={() => setEditingItem(null)}>
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
