import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api, imageUrl } from '../../../../api/client'
import ImageRegionEditor from '../../../../components/ImageRegionEditor'
import { DEFAULT_REGION, normalizeRegion } from '../../../../utils/imageRegionUtils'
import '../UsersPage.css'
import '../display/DeviceDisplayItemsPage.css'

const EMPTY_FORM = {
  title: '',
  content: '',
  hasRegion: false,
  leftPercent: null,
  topPercent: null,
  widthPercent: null,
  heightPercent: null,
  sortOrder: 0,
  enabled: true,
}

function formatDate(iso) {
  return iso ? new Date(iso).toLocaleString('zh-CN') : '—'
}

function roundPercent(value) {
  return Math.round(Number(value) * 1000) / 1000
}

function formToRegion(form) {
  if (!form.hasRegion) return null
  return normalizeRegion({
    leftPercent: form.leftPercent,
    topPercent: form.topPercent,
    widthPercent: form.widthPercent,
    heightPercent: form.heightPercent,
  })
}

function regionFields(region) {
  const normalized = normalizeRegion(region)
  return {
    leftPercent: roundPercent(normalized.leftPercent),
    topPercent: roundPercent(normalized.topPercent),
    widthPercent: roundPercent(normalized.widthPercent),
    heightPercent: roundPercent(normalized.heightPercent),
  }
}

export default function DrawingCognitionItemsPage() {
  const { pageId } = useParams()
  const pageIdNum = Number(pageId)
  const [page, setPage] = useState(null)
  const [group, setGroup] = useState(null)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [imageVersion] = useState(0)
  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_FORM)
  const [editingItem, setEditingItem] = useState(null)
  const [editForm, setEditForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)

  const loadData = useCallback(async () => {
    if (!pageIdNum) return
    setLoading(true)
    setError('')
    try {
      const [pageData, itemData] = await Promise.all([
        api.getDrawingPage(pageIdNum),
        api.listDrawingCognitionItems(pageIdNum),
      ])
      const groupData = await api.getDrawingGroup(pageData.groupId)
      setPage(pageData)
      setGroup(groupData)
      setItems(itemData)
    } catch (err) {
      setError(err.message || '加载认知条目失败')
      setPage(null)
      setGroup(null)
      setItems([])
    } finally {
      setLoading(false)
    }
  }, [pageIdNum])

  useEffect(() => {
    const timer = window.setTimeout(loadData, 0)
    return () => window.clearTimeout(timer)
  }, [loadData])

  if (!pageId || Number.isNaN(pageIdNum)) return <Navigate to="/admin/drawing-learning" replace />

  const flash = (text) => {
    setMessage(text)
    setTimeout(() => setMessage(''), 3000)
  }

  const saveCreate = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      const payload = { ...createForm, sortOrder: Number(createForm.sortOrder) }
      delete payload.hasRegion
      await api.createDrawingCognitionItem(pageIdNum, payload)
      setShowCreate(false)
      setCreateForm(EMPTY_FORM)
      flash('认知条目创建成功')
      await loadData()
    } catch (err) {
      setError(err.message || '创建失败')
    } finally {
      setSaving(false)
    }
  }

  const saveEdit = async (e) => {
    e.preventDefault()
    if (!editingItem) return
    setSaving(true)
    setError('')
    try {
      const payload = { ...editForm, sortOrder: Number(editForm.sortOrder) }
      delete payload.hasRegion
      await api.updateDrawingCognitionItem(editingItem.id, payload)
      setEditingItem(null)
      flash('认知条目已更新')
      await loadData()
    } catch (err) {
      setError(err.message || '更新失败')
    } finally {
      setSaving(false)
    }
  }

  const deleteItem = async (item) => {
    if (!window.confirm(`确定删除认知条目「${item.title}」？`)) return
    setError('')
    try {
      await api.deleteDrawingCognitionItem(item.id)
      flash('认知条目已删除')
      await loadData()
    } catch (err) {
      setError(err.message || '删除失败')
    }
  }

  const renderDialog = (title, form, setForm, onSubmit, onCancel) => {
    const previewSrc = page ? imageUrl('drawing-page', page.id, imageVersion) : ''
    return (
      <div className="users-page__overlay">
        <form className="users-page__dialog" onSubmit={onSubmit}>
          <h3>{title}</h3>
          <label>
            条目标题
            <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
          </label>
          <label>
            文字说明
            <textarea rows={6} value={form.content} onChange={(e) => setForm({ ...form, content: e.target.value })} required />
          </label>
          <div className="device-display-items__highlight-field">
            <label className="users-page__checkbox">
              <input
                type="checkbox"
                checked={form.hasRegion}
                onChange={(e) => setForm((current) => ({
                  ...current,
                  hasRegion: e.target.checked,
                  ...(e.target.checked ? regionFields(DEFAULT_REGION) : {
                    leftPercent: null,
                    topPercent: null,
                    widthPercent: null,
                    heightPercent: null,
                  }),
                }))}
                disabled={saving}
              />
              设置图片高亮区域
            </label>
            {form.hasRegion && (
              <>
                <p className="device-display-items__highlight-hint">拖动黄框调整图纸上的高亮区域</p>
                <ImageRegionEditor
                  imageUrl={previewSrc}
                  region={formToRegion(form)}
                  onChange={(nextRegion) => setForm((current) => ({ ...current, ...regionFields(nextRegion) }))}
                  readOnly={saving}
                />
              </>
            )}
          </div>
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
            <button type="submit" className="users-page__btn users-page__btn--primary" disabled={saving}>{saving ? '保存中…' : '保存'}</button>
          </div>
        </form>
      </div>
    )
  }

  return (
    <div className="users-page">
      <div className="users-page__header">
        <div>
          <p className="users-page__breadcrumb">
            <Link to="/admin/drawing-learning">图纸学习</Link>
            {group && <><span> / </span><Link to={`/admin/drawing-learning/cabinets/${group.cabinetId}`}>{group.cabinetName}</Link><span> / </span><Link to={`/admin/drawing-learning/groups/${group.id}/pages`}>{group.name}</Link></>}
            <span> / </span><span>{page?.title ?? '认知条目'}</span>
          </p>
          <h2 className="users-page__title">{page ? `${page.title} — 认知条目` : '图纸认知条目'}</h2>
          <p className="users-page__desc">每个认知条目对应图纸上的一个高亮区域和一段文字说明。</p>
        </div>
        <button type="button" className="users-page__btn users-page__btn--primary" onClick={() => {
          setCreateForm(EMPTY_FORM)
          setShowCreate(true)
        }}>新增条目</button>
      </div>
      {message && <div className="users-page__message">{message}</div>}
      {error && <div className="users-page__error">{error}</div>}
      {loading ? <p className="users-page__loading">加载中…</p> : (
        <div className="users-page__table-wrap">
          <table className="users-page__table">
            <thead><tr><th>标题</th><th>说明</th><th>区域</th><th>排序</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>
              {items.length === 0 ? <tr><td colSpan={7} className="users-page__empty-cell">暂无认知条目</td></tr> : items.map((item) => (
                <tr key={item.id}>
                  <td>{item.title}</td>
                  <td>{item.content.length > 42 ? `${item.content.slice(0, 42)}…` : item.content}</td>
                  <td>{item.leftPercent == null ? '未设置' : `${item.leftPercent}, ${item.topPercent}, ${item.widthPercent}, ${item.heightPercent}`}</td>
                  <td>{item.sortOrder}</td>
                  <td>{item.enabled ? '启用' : '停用'}</td>
                  <td>{formatDate(item.createdAt)}</td>
                  <td className="users-page__actions">
                    <button type="button" className="users-page__link" onClick={() => {
                      setEditingItem(item)
                      setEditForm({
                        title: item.title,
                        content: item.content,
                        hasRegion: item.leftPercent != null,
                        leftPercent: item.leftPercent,
                        topPercent: item.topPercent,
                        widthPercent: item.widthPercent,
                        heightPercent: item.heightPercent,
                        sortOrder: item.sortOrder,
                        enabled: item.enabled,
                      })
                    }}>编辑</button>
                    <button type="button" className="users-page__link users-page__link--danger" onClick={() => deleteItem(item)}>删除</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {showCreate && renderDialog('新增图纸认知条目', createForm, setCreateForm, saveCreate, () => setShowCreate(false))}
      {editingItem && renderDialog('编辑图纸认知条目', editForm, setEditForm, saveEdit, () => setEditingItem(null))}
    </div>
  )
}
