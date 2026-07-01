import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api, imageUrl } from '../../../../api/client'
import CabinetImageUploadField from '../../../../components/CabinetImageUploadField'
import '../UsersPage.css'
import './DeviceDisplayItemsPage.css'

const EMPTY_CREATE = {
  title: '',
  imageUrl: '',
  content: '',
  sortOrder: 0,
  enabled: true,
}

const DEVICE_TYPE_LABELS = {
  IED: 'IED 设备',
  TERMINAL_GROUP: '端子组',
  PLATE_GROUP: '压板组',
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
  const { cognitionDeviceId } = useParams()
  const cognitionDeviceIdNum = Number(cognitionDeviceId)

  const [cognitionDevice, setCognitionDevice] = useState(null)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_CREATE)
  const [creating, setCreating] = useState(false)

  const [editingItem, setEditingItem] = useState(null)
  const [editForm, setEditForm] = useState({ title: '', imageUrl: '', content: '', sortOrder: 0, enabled: true })
  const [saving, setSaving] = useState(false)

  const loadData = useCallback(async () => {
    if (!cognitionDeviceIdNum) return
    setLoading(true)
    setError('')
    setItems([])
    try {
      const [deviceData, itemData] = await Promise.all([
        api.getCognitionDevice(cognitionDeviceIdNum),
        api.listCognitionDeviceDisplayItems(cognitionDeviceIdNum),
      ])
      setCognitionDevice(deviceData)
      setItems(itemData)
    } catch (err) {
      setError(err.message || '加载认知条目失败')
      setCognitionDevice(null)
      setItems([])
    } finally {
      setLoading(false)
    }
  }, [cognitionDeviceIdNum])

  useEffect(() => {
    loadData()
  }, [loadData])

  if (!cognitionDeviceId || Number.isNaN(cognitionDeviceIdNum)) {
    return <Navigate to="/admin/display" replace />
  }

  const flash = (text) => {
    setMessage(text)
    setTimeout(() => setMessage(''), 3000)
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    if (!createForm.imageUrl) {
      setError('请上传认知图片')
      return
    }
    setCreating(true)
    setError('')
    try {
      await api.createCognitionDeviceDisplayItem(cognitionDeviceIdNum, {
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
      imageUrl: item.imageUrl,
      content: item.content,
      sortOrder: item.sortOrder,
      enabled: item.enabled,
    })
  }

  const handleUpdate = async (e) => {
    e.preventDefault()
    if (!editingItem) return
    if (!editForm.imageUrl) {
      setError('请上传认知图片')
      return
    }
    setSaving(true)
    setError('')
    try {
      await api.updateDeviceDisplayItem(editingItem.id, {
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
    if (!window.confirm(`确定删除认知条目「${item.title}」？`)) return
    setError('')
    try {
      await api.deleteDeviceDisplayItem(item.id)
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
            <Link to="/admin/display">屏柜学习</Link>
            {cognitionDevice && (
              <>
                <span> / </span>
                <Link
                  to={`/admin/display/cabinet-items/${cognitionDevice.cabinetDisplayItemId}/cognition-devices`}
                >
                  {cognitionDevice.cabinetDisplayItemTitle}
                </Link>
                <span> / </span>
                <span>{cognitionDevice.title} — 认知条目</span>
              </>
            )}
          </p>
          <h2 className="users-page__title">
            {cognitionDevice ? `${cognitionDevice.title} — 认知条目` : '认知条目'}
          </h2>
          {cognitionDevice && (
            <p className="users-page__desc">
              类型：{DEVICE_TYPE_LABELS[cognitionDevice.deviceType] ?? cognitionDevice.deviceType}
              {cognitionDevice.screenDeviceName ? ` · 关联 ${cognitionDevice.screenDeviceName}` : ''}
            </p>
          )}
        </div>
        <button type="button" className="users-page__btn users-page__btn--primary" onClick={() => setShowCreate(true)}>
          新增条目
        </button>
      </div>

      {message && <div className="users-page__message">{message}</div>}
      {error && <div className="users-page__error">{error}</div>}

      {loading ? (
        <p className="users-page__loading">加载中…</p>
      ) : !cognitionDevice ? (
        <p className="users-page__empty">
          认知设备不存在，<Link to="/admin/display">返回列表</Link>。
        </p>
      ) : (
        <div className="users-page__table-wrap">
          <table className="users-page__table">
            <thead>
              <tr>
                <th>图片</th>
                <th>名称</th>
                <th>描述摘要</th>
                <th>排序</th>
                <th>状态</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {items.length === 0 ? (
                <tr>
                  <td colSpan={7} className="users-page__empty-cell">
                    暂无认知条目
                  </td>
                </tr>
              ) : (
                items.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <img className="device-display-items__thumb" src={imageUrl(item.imageUrl)} alt={item.title} />
                    </td>
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
            <h3>新增认知条目</h3>
            <label>
              条目名称
              <input
                value={createForm.title}
                onChange={(e) => setCreateForm({ ...createForm, title: e.target.value })}
                required
              />
            </label>
            <CabinetImageUploadField
              imageUrl={createForm.imageUrl}
              onChange={(url) => setCreateForm({ ...createForm, imageUrl: url })}
              uploadImage={api.uploadDeviceDisplayImage}
              disabled={creating}
            />
            <label>
              文字描述
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
            <h3>编辑认知条目</h3>
            <label>
              条目名称
              <input
                value={editForm.title}
                onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
                required
              />
            </label>
            <CabinetImageUploadField
              imageUrl={editForm.imageUrl}
              onChange={(url) => setEditForm({ ...editForm, imageUrl: url })}
              uploadImage={api.uploadDeviceDisplayImage}
              disabled={saving}
            />
            <label>
              文字描述
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
