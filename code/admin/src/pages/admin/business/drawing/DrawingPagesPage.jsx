import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api, imageUrl } from '../../../../api/client'
import CabinetImageUploadField from '../../../../components/CabinetImageUploadField'
import '../UsersPage.css'
import '../display/CabinetDisplayItemsPage.css'

const EMPTY_FORM = { title: '', imageId: null, imageUrl: '', hasImage: false, sortOrder: 0, enabled: true }
const TYPE_LABELS = { BLUEPRINT: '蓝图', WHITEPRINT: '白图' }

function formatDate(iso) {
  return iso ? new Date(iso).toLocaleString('zh-CN') : '—'
}

export default function DrawingPagesPage() {
  const { groupId } = useParams()
  const groupIdNum = Number(groupId)
  const [group, setGroup] = useState(null)
  const [pages, setPages] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [imageVersion, setImageVersion] = useState(0)
  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_FORM)
  const [editingPage, setEditingPage] = useState(null)
  const [editForm, setEditForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)

  const loadData = useCallback(async () => {
    if (!groupIdNum) return
    setLoading(true)
    setError('')
    try {
      const [groupData, pageData] = await Promise.all([
        api.getDrawingGroup(groupIdNum),
        api.listDrawingPages(groupIdNum),
      ])
      setGroup(groupData)
      setPages(pageData)
    } catch (err) {
      setError(err.message || '加载图纸失败')
      setGroup(null)
      setPages([])
    } finally {
      setLoading(false)
    }
  }, [groupIdNum])

  useEffect(() => {
    const timer = window.setTimeout(loadData, 0)
    return () => window.clearTimeout(timer)
  }, [loadData])

  if (!groupId || Number.isNaN(groupIdNum)) return <Navigate to="/admin/drawing-learning" replace />

  const flash = (text) => {
    setMessage(text)
    setTimeout(() => setMessage(''), 3000)
  }

  const saveCreate = async (e) => {
    e.preventDefault()
    if (!createForm.imageId && !createForm.imageUrl) {
      setError('请上传图纸图片')
      return
    }
    setSaving(true)
    setError('')
    try {
      const payload = { ...createForm, sortOrder: Number(createForm.sortOrder) }
      delete payload.hasImage
      await api.createDrawingPage(groupIdNum, payload)
      setShowCreate(false)
      setCreateForm(EMPTY_FORM)
      setImageVersion((version) => version + 1)
      flash('图纸创建成功')
      await loadData()
    } catch (err) {
      setError(err.message || '创建失败')
    } finally {
      setSaving(false)
    }
  }

  const saveEdit = async (e) => {
    e.preventDefault()
    if (!editingPage) return
    if (!editForm.hasImage && !editForm.imageId && !editForm.imageUrl) {
      setError('请上传图纸图片')
      return
    }
    setSaving(true)
    setError('')
    try {
      const payload = { ...editForm, sortOrder: Number(editForm.sortOrder) }
      delete payload.hasImage
      await api.updateDrawingPage(editingPage.id, payload)
      setEditingPage(null)
      setImageVersion((version) => version + 1)
      flash('图纸已更新')
      await loadData()
    } catch (err) {
      setError(err.message || '更新失败')
    } finally {
      setSaving(false)
    }
  }

  const deletePage = async (page) => {
    if (!window.confirm(`确定删除图纸「${page.title}」及其认知条目？`)) return
    setError('')
    try {
      await api.deleteDrawingPage(page.id)
      flash('图纸已删除')
      await loadData()
    } catch (err) {
      setError(err.message || '删除失败')
    }
  }

  const renderDialog = (title, form, setForm, onSubmit, onCancel, previewPage) => (
    <div className="users-page__overlay">
      <form className="users-page__dialog" onSubmit={onSubmit}>
        <h3>{title}</h3>
        <label>
          图纸标题
          <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
        </label>
        <CabinetImageUploadField
          imageUrl={form.imageUrl}
          previewUrl={previewPage ? imageUrl('drawing-page', previewPage.id, imageVersion) : ''}
          onChange={(url, result) => setForm((current) => ({
            ...current,
            imageUrl: url,
            imageId: result?.imageId ?? null,
            hasImage: Boolean(url || result?.imageId || previewPage?.id),
          }))}
          disabled={saving}
        />
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

  return (
    <div className="users-page">
      <div className="users-page__header">
        <div>
          <p className="users-page__breadcrumb">
            <Link to="/admin/drawing-learning">图纸学习</Link>
            {group && <><span> / </span><Link to={`/admin/drawing-learning/cabinets/${group.cabinetId}`}>{group.cabinetName}</Link></>}
            <span> / </span><span>{group?.name ?? '图纸'}</span>
          </p>
          <h2 className="users-page__title">{group ? `${TYPE_LABELS[group.drawingType]} — ${group.name}` : '图纸列表'}</h2>
          <p className="users-page__desc">每张图纸上传一张图片，认知条目会在该图片上圈定讲解区域。</p>
        </div>
        <button type="button" className="users-page__btn users-page__btn--primary" onClick={() => setShowCreate(true)}>新增图纸</button>
      </div>
      {message && <div className="users-page__message">{message}</div>}
      {error && <div className="users-page__error">{error}</div>}
      {loading ? <p className="users-page__loading">加载中…</p> : (
        <div className="users-page__table-wrap">
          <table className="users-page__table">
            <thead><tr><th>图片</th><th>标题</th><th>排序</th><th>认知条目数</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>
              {pages.length === 0 ? <tr><td colSpan={7} className="users-page__empty-cell">暂无图纸</td></tr> : pages.map((page) => (
                <tr key={page.id}>
                  <td><img className="cabinet-display-items__thumb" src={imageUrl('drawing-page', page.id, imageVersion)} alt={page.title} /></td>
                  <td>{page.title}</td>
                  <td>{page.sortOrder}</td>
                  <td>{page.cognitionItemCount}</td>
                  <td>{page.enabled ? '启用' : '停用'}</td>
                  <td>{formatDate(page.createdAt)}</td>
                  <td className="users-page__actions">
                    <Link className="users-page__link" to={`/admin/drawing-learning/pages/${page.id}/items`}>认知条目</Link>
                    <button type="button" className="users-page__link" onClick={() => {
                      setEditingPage(page)
                      setEditForm({
                        title: page.title,
                        imageId: null,
                        imageUrl: page.imageUrl,
                        hasImage: true,
                        sortOrder: page.sortOrder,
                        enabled: page.enabled,
                      })
                    }}>编辑</button>
                    <button type="button" className="users-page__link users-page__link--danger" onClick={() => deletePage(page)}>删除</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {showCreate && renderDialog('新增图纸', createForm, setCreateForm, saveCreate, () => setShowCreate(false))}
      {editingPage && renderDialog('编辑图纸', editForm, setEditForm, saveEdit, () => setEditingPage(null), editingPage)}
    </div>
  )
}
