import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../../../../api/client'
import '../UsersPage.css'

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

export default function DeviceDisplayItemsPage() {
  const { cabinetId, deviceId } = useParams()
  const deviceIdNum = Number(deviceId)
  const cabinetIdNum = cabinetId ? Number(cabinetId) : null

  const [device, setDevice] = useState(null)
  const [cabinet, setCabinet] = useState(null)
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
      const requests = [
        api.getKnowledgeDevice(deviceIdNum),
        api.listDeviceDisplayItems(deviceIdNum),
      ]
      if (cabinetIdNum) {
        requests.push(api.getKnowledgeCabinet(cabinetIdNum))
      }
      const results = await Promise.all(requests)
      setDevice(results[0])
      setItems(results[1])
      setCabinet(cabinetIdNum ? results[2] : null)
    } catch (err) {
      setError(err.message || '加载设备展示条目失败')
      setDevice(null)
      setItems([])
    } finally {
      setLoading(false)
    }
  }, [deviceIdNum, cabinetIdNum])

  useEffect(() => {
    loadData()
  }, [loadData])

  if (!deviceId || Number.isNaN(deviceIdNum)) {
    return <Navigate to="/admin/display" replace />
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
      await api.createDeviceDisplayItem(deviceIdNum, {
        ...createForm,
        sortOrder: Number(createForm.sortOrder),
      })
      setShowCreate(false)
      setCreateForm(EMPTY_CREATE)
      flash('展示条目创建成功')
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
      await api.updateDeviceDisplayItem(editingItem.id, {
        ...editForm,
        sortOrder: Number(editForm.sortOrder),
      })
      setEditingItem(null)
      flash('展示条目已更新')
      await loadData()
    } catch (err) {
      setError(err.message || '更新失败')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (item) => {
    if (!window.confirm(`确定删除展示条目「${item.title}」？`)) return
    setError('')
    try {
      await api.deleteDeviceDisplayItem(item.id)
      flash('展示条目已删除')
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
            <Link to="/admin/display">屏柜认知</Link>
            {cabinetIdNum && (
              <>
                <span> / </span>
                <Link to={`/admin/display/cabinets/${cabinetId}`}>
                  {cabinet?.name ?? '屏柜'}
                </Link>
              </>
            )}
            <span> / </span>
            <span>{device ? `${device.name} — 展示条目` : '设备展示条目'}</span>
          </p>
          <h2 className="users-page__title">
            {device ? `${device.name} — 展示条目` : '设备展示条目'}
          </h2>
          <p className="users-page__desc">
            业务库展示数据，screen_device_id = {deviceId}。学员端通过知识 API 读取已启用条目。
          </p>
        </div>
        <button type="button" className="users-page__btn users-page__btn--primary" onClick={() => setShowCreate(true)}>
          新增条目
        </button>
      </div>

      {message && <div className="users-page__message">{message}</div>}
      {error && <div className="users-page__error">{error}</div>}

      {loading ? (
        <p className="users-page__loading">加载中…</p>
      ) : !device ? (
        <p className="users-page__empty">
          设备不存在，<Link to="/admin/display">返回列表</Link>。
        </p>
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
              {items.length === 0 ? (
                <tr>
                  <td colSpan={6} className="users-page__empty-cell">
                    暂无展示条目
                  </td>
                </tr>
              ) : (
                items.map((item) => (
                  <tr key={item.id}>
                    <td>{item.title}</td>
                    <td>{previewContent(item.content)}</td>
                    <td>{item.sortOrder}</td>
                    <td>{item.enabled ? '启用' : '停用'}</td>
                    <td>{formatDate(item.createdAt)}</td>
                    <td className="users-page__actions">
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
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {showCreate && (
        <div className="users-page__overlay" onClick={() => setShowCreate(false)}>
          <form className="users-page__dialog" onClick={(e) => e.stopPropagation()} onSubmit={handleCreate}>
            <h3>新增展示条目</h3>
            <label>
              标题
              <input
                value={createForm.title}
                onChange={(e) => setCreateForm({ ...createForm, title: e.target.value })}
                required
              />
            </label>
            <label>
              内容
              <textarea
                rows={6}
                value={createForm.content}
                onChange={(e) => setCreateForm({ ...createForm, content: e.target.value })}
                required
              />
            </label>
            <label>
              排序
              <input
                type="number"
                value={createForm.sortOrder}
                onChange={(e) => setCreateForm({ ...createForm, sortOrder: e.target.value })}
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
            <h3>编辑展示条目</h3>
            <label>
              标题
              <input
                value={editForm.title}
                onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
                required
              />
            </label>
            <label>
              内容
              <textarea
                rows={6}
                value={editForm.content}
                onChange={(e) => setEditForm({ ...editForm, content: e.target.value })}
                required
              />
            </label>
            <label>
              排序
              <input
                type="number"
                value={editForm.sortOrder}
                onChange={(e) => setEditForm({ ...editForm, sortOrder: e.target.value })}
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
