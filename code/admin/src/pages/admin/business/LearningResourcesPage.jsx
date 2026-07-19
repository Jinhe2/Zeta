import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../../../api/client'
import './LearningResourcesPage.css'

const TYPES = [
  { value: 'DEBUG_OUTLINE', label: '调试大纲', accept: 'application/pdf,.pdf' },
  { value: 'MANUAL', label: '说明书', accept: 'application/pdf,.pdf' },
  { value: 'DRAWING', label: '图纸', accept: 'application/pdf,.pdf' },
  { value: 'VIDEO_COURSE', label: '视频微课', accept: 'video/mp4,.mp4' },
]
const EMPTY_FORM = { name: '', description: '', resourceType: 'DEBUG_OUTLINE', scope: 'ALL', cabinetId: '', file: null }
const typeLabel = (value) => TYPES.find((type) => type.value === value)?.label || value
const formatSize = (size) => {
  if (!Number.isFinite(size)) return '—'
  if (size < 1024 * 1024) return `${Math.ceil(size / 1024)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

export default function LearningResourcesPage() {
  const [resources, setResources] = useState([])
  const [cabinets, setCabinets] = useState([])
  const [typeFilter, setTypeFilter] = useState('')
  const [cabinetFilter, setCabinetFilter] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [items, cabinetItems] = await Promise.all([
        api.listLearningResources(typeFilter || undefined, cabinetFilter && cabinetFilter !== 'all' ? cabinetFilter : undefined),
        api.listKnowledgeCabinets(),
      ])
      setResources(items)
      setCabinets(cabinetItems)
    } catch (err) {
      setError(err.message || '加载学习资料失败')
    } finally {
      setLoading(false)
    }
  }, [typeFilter, cabinetFilter])

  useEffect(() => {
    const timer = window.setTimeout(load, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  const selectedType = useMemo(
    () => TYPES.find((type) => type.value === form.resourceType) || TYPES[0],
    [form.resourceType],
  )

  const openCreate = () => {
    setEditing('create')
    setForm(EMPTY_FORM)
    setError('')
  }

  const openEdit = (item) => {
    setEditing(item)
    setForm({
      name: item.name,
      description: item.description,
      resourceType: item.resourceType,
      scope: item.cabinetId ? 'CABINET' : 'ALL',
      cabinetId: item.cabinetId ? String(item.cabinetId) : '',
      file: null,
    })
    setError('')
  }

  const submit = async (event) => {
    event.preventDefault()
    if (!editing) return
    if (editing === 'create' && !form.file) {
      setError('请选择要上传的资料文件')
      return
    }
    if (form.scope === 'CABINET' && !form.cabinetId) {
      setError('请选择适用屏柜')
      return
    }
    const changingMediaKind = editing !== 'create'
      && (editing.resourceType === 'VIDEO_COURSE') !== (form.resourceType === 'VIDEO_COURSE')
    if (changingMediaKind && !form.file) {
      setError('PDF 与视频资料之间切换时必须上传新文件')
      return
    }
    const data = new FormData()
    data.append('name', form.name)
    data.append('description', form.description)
    data.append('resourceType', form.resourceType)
    if (form.scope === 'CABINET') data.append('cabinetId', form.cabinetId)
    if (form.file) data.append('file', form.file)
    setSaving(true)
    setError('')
    try {
      if (editing === 'create') await api.createLearningResource(data)
      else await api.updateLearningResource(editing.id, data)
      setEditing(null)
      setMessage(editing === 'create' ? '学习资料已上传' : '学习资料已保存')
      await load()
    } catch (err) {
      setError(err.message || '保存学习资料失败')
    } finally {
      setSaving(false)
    }
  }

  const remove = async (item) => {
    if (!window.confirm(`确定删除「${item.name}」及其文件吗？`)) return
    setError('')
    try {
      await api.deleteLearningResource(item.id)
      setMessage('学习资料已删除')
      await load()
    } catch (err) {
      setError(err.message || '删除学习资料失败')
    }
  }

  return (
    <div className="learning-resources-page">
      <div className="learning-resources-page__header">
        <div>
          <h2>学习资料</h2>
          <p>上传并维护面向所有屏柜或指定屏柜的 PDF 与视频微课。</p>
        </div>
        <button type="button" className="learning-resources-page__primary" onClick={openCreate}>上传学习资料</button>
      </div>

      <div className="learning-resources-page__filters">
        <label>分类<select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}><option value="">全部分类</option>{TYPES.map((type) => <option key={type.value} value={type.value}>{type.label}</option>)}</select></label>
        <label>屏柜<select value={cabinetFilter} onChange={(e) => setCabinetFilter(e.target.value)}><option value="">全部范围</option><option value="all">所有屏柜资料</option>{cabinets.map((cabinet) => <option key={cabinet.id} value={cabinet.id}>{cabinet.name}</option>)}</select></label>
      </div>
      {message && <div className="learning-resources-page__message">{message}</div>}
      {error && <div className="learning-resources-page__error">{error}</div>}
      {loading ? <p className="learning-resources-page__hint">加载中…</p> : resources.length === 0 ? <p className="learning-resources-page__hint">暂无学习资料。</p> : (
        <div className="learning-resources-page__table-wrap"><table><thead><tr><th>名称</th><th>分类</th><th>适用屏柜</th><th>说明</th><th>文件</th><th>更新时间</th><th>操作</th></tr></thead><tbody>
          {resources.filter((item) => cabinetFilter !== 'all' || !item.cabinetId).map((item) => <tr key={item.id}>
            <td>{item.name}</td><td>{typeLabel(item.resourceType)}</td><td>{item.cabinetName || '所有屏柜'}</td><td className="learning-resources-page__description">{item.description}</td><td>{item.originalFilename}<small>{formatSize(item.fileSize)}</small></td><td>{new Date(item.updatedAt).toLocaleString('zh-CN')}</td>
            <td><button type="button" onClick={() => openEdit(item)}>编辑</button><button type="button" className="learning-resources-page__danger" onClick={() => remove(item)}>删除</button></td>
          </tr>)}
        </tbody></table></div>
      )}

      {editing && <div className="learning-resources-page__overlay"><form className="learning-resources-page__dialog" onSubmit={submit}>
        <h3>{editing === 'create' ? '上传学习资料' : '编辑学习资料'}</h3>
        <label>名称<input value={form.name} maxLength="128" required onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
        <label>说明<textarea value={form.description} required rows="4" onChange={(e) => setForm({ ...form, description: e.target.value })} /></label>
        <label>分类<select value={form.resourceType} onChange={(e) => setForm({ ...form, resourceType: e.target.value, file: null })}>{TYPES.map((type) => <option key={type.value} value={type.value}>{type.label}</option>)}</select></label>
        <fieldset><legend>适用范围</legend><label className="learning-resources-page__radio"><input type="radio" checked={form.scope === 'ALL'} onChange={() => setForm({ ...form, scope: 'ALL', cabinetId: '' })} />所有屏柜</label><label className="learning-resources-page__radio"><input type="radio" checked={form.scope === 'CABINET'} onChange={() => setForm({ ...form, scope: 'CABINET' })} />指定屏柜</label>{form.scope === 'CABINET' && <select value={form.cabinetId} required onChange={(e) => setForm({ ...form, cabinetId: e.target.value })}><option value="">请选择屏柜</option>{cabinets.map((cabinet) => <option key={cabinet.id} value={cabinet.id}>{cabinet.name}</option>)}</select>}</fieldset>
        <label>文件{editing !== 'create' && '（留空则保留原文件）'}<input type="file" accept={selectedType.accept} required={editing === 'create'} onChange={(e) => setForm({ ...form, file: e.target.files?.[0] || null })} /><small>{selectedType.value === 'VIDEO_COURSE' ? '仅支持 MP4，最大 500MB' : '仅支持 PDF，最大 50MB'}</small></label>
        <div className="learning-resources-page__dialog-actions"><button type="button" disabled={saving} onClick={() => setEditing(null)}>取消</button><button type="submit" disabled={saving}>{saving ? '保存中…' : '保存'}</button></div>
      </form></div>}
    </div>
  )
}
