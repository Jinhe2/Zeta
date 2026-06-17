import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../../../../api/client'
import CabinetImageUploadField from '../../../../components/CabinetImageUploadField'
import '../UsersPage.css'
import './CabinetDisplayItemsPage.css'

const EMPTY_CREATE = {
  title: '',
  imageUrl: '',
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

export default function CabinetDisplayItemsPage() {
  const { cabinetId } = useParams()
  const cabinetIdNum = Number(cabinetId)

  const [cabinet, setCabinet] = useState(null)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_CREATE)
  const [creating, setCreating] = useState(false)

  const [editingItem, setEditingItem] = useState(null)
  const [editForm, setEditForm] = useState({
    title: '',
    imageUrl: '',
    content: '',
    sortOrder: 0,
    enabled: true,
  })
  const [saving, setSaving] = useState(false)

  const loadData = useCallback(async () => {
    if (!cabinetIdNum) return
    setLoading(true)
    setError('')
    setItems([])
    try {
      const [cabinetData, itemData] = await Promise.all([
        api.getKnowledgeCabinet(cabinetIdNum),
        api.listCabinetDisplayItems(cabinetIdNum),
      ])
      setCabinet(cabinetData)
      setItems(itemData)
    } catch (err) {
      setError(err.message || '加载屏柜认知条目失败')
      setCabinet(null)
      setItems([])
    } finally {
      setLoading(false)
    }
  }, [cabinetIdNum])

  useEffect(() => {
    loadData()
  }, [loadData])

  if (!cabinetId || Number.isNaN(cabinetIdNum)) {
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
      await api.createCabinetDisplayItem(cabinetIdNum, {
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
      await api.updateCabinetDisplayItem(editingItem.id, {
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
      await api.deleteCabinetDisplayItem(item.id)
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
            <Link to="/admin/display">屏柜认知</Link>
            <span> / </span>
            <span>{cabinet?.name ?? '认知条目'}</span>
          </p>
          <h2 className="users-page__title">
            {cabinet ? `${cabinet.name} — 认知条目` : '屏柜认知条目'}
          </h2>
          <p className="users-page__desc">
            每条认知包含一张图片与一段文字描述（如正视图、侧视图等）。学员端按排序展示已启用条目。
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
      ) : !cabinet ? (
        <p className="users-page__empty">
          屏柜不存在，<Link to="/admin/display">返回列表</Link>。
        </p>
      ) : (
        <>
          {cabinet.description || cabinet.code ? (
            <p className="users-page__desc">
              编码：{cabinet.code}
              {cabinet.description ? ` · ${cabinet.description}` : ''}
            </p>
          ) : null}

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
                        <img
                          className="cabinet-display-items__thumb"
                          src={item.imageUrl}
                          alt={item.title}
                        />
                      </td>
                      <td>{item.title}</td>
                      <td>{previewContent(item.content)}</td>
                      <td>{item.sortOrder}</td>
                      <td>{item.enabled ? '启用' : '停用'}</td>
                      <td>{formatDate(item.createdAt)}</td>
                      <td className="users-page__actions">
                        <Link
                          className="users-page__link"
                          to={`/admin/display/cabinet-items/${item.id}/cognition-devices`}
                        >
                          子设备
                        </Link>
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
        </>
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
                placeholder="如：正视图、侧视图"
                required
              />
            </label>
            <CabinetImageUploadField
              imageUrl={createForm.imageUrl}
              onChange={(url) => setCreateForm({ ...createForm, imageUrl: url })}
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
