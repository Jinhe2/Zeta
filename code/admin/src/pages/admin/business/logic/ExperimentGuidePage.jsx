/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api, imageUrl } from '../../../../api/client'
import CabinetImageUploadField from '../../../../components/CabinetImageUploadField'
import { normalizeSortOrder } from '../../../../utils/sortOrder'
import '../UsersPage.css'
import '../display/DeviceDisplayItemsPage.css'
import './LogicLearningPage.css'

const EMPTY_FORM = {
  type: 'IMAGE_TEXT',
  title: '',
  imageId: null,
  imageUrl: '',
  hasImage: false,
  content: '',
  sortOrder: '',
  enabled: true,
  showInWholeExperiment: true,
}

const TYPE_LABELS = {
  IMAGE_TEXT: '图片文字',
  SETTING_LIST: '定值整定',
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

function hasText(text) {
  return Boolean(text?.trim())
}

function hasFormImage(form) {
  return Boolean(form.hasImage || form.imageId || form.imageUrl)
}

export default function ExperimentGuidePage({ scopeType }) {
  const params = useParams()
  const rawId = scopeType === 'LOGIC_GROUP' ? params.groupId : params.logicDiagramId
  const scopeId = Number(rawId)
  const [scopeName, setScopeName] = useState('')
  const [device, setDevice] = useState(null)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [imageVersion, setImageVersion] = useState(() => Date.now())
  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_FORM)
  const [creating, setCreating] = useState(false)
  const [editingItem, setEditingItem] = useState(null)
  const [editForm, setEditForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)

  const flash = (text) => {
    setMessage(text)
    setTimeout(() => setMessage(''), 3000)
  }

  const loadData = useCallback(async () => {
    if (!scopeId) return
    setLoading(true)
    setError('')
    try {
      const itemData = await api.listExperimentGuides(scopeType, scopeId)
      setItems(itemData)
      let deviceData = null
      let name = ''
      if (scopeType === 'LOGIC_GROUP') {
        const group = await api.getLogicGroup(scopeId)
        name = group.name
        if (group.iedDeviceId) deviceData = await api.getKnowledgeDevice(group.iedDeviceId)
      } else {
        const logic = await api.getProtectionLogic(scopeId)
        name = logic.title
        if (logic.deviceId) deviceData = await api.getKnowledgeDevice(logic.deviceId)
      }
      setScopeName(name)
      setDevice(deviceData)
    } catch (err) {
      setError(err.message || '加载实验引导失败')
      setItems([])
      setScopeName('')
      setDevice(null)
    } finally {
      setLoading(false)
    }
  }, [scopeId, scopeType])

  useEffect(() => {
    loadData()
  }, [loadData])

  if (!rawId || Number.isNaN(scopeId)) {
    return <Navigate to="/admin/logic-learning" replace />
  }

  const validateForm = (form, setErr) => {
    if (form.type === 'IMAGE_TEXT') {
      if (!hasFormImage(form)) {
        setErr('请上传引导图片')
        return false
      }
      if (!hasText(form.content)) {
        setErr('请填写文字描述')
        return false
      }
    }
    return true
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    if (!validateForm(createForm, setError)) return
    setCreating(true)
    setError('')
    try {
      const payload = { ...createForm }
      delete payload.hasImage
      await api.createExperimentGuide(scopeType, scopeId, {
        ...payload,
        sortOrder: normalizeSortOrder(createForm.sortOrder),
      })
      setShowCreate(false)
      setCreateForm(EMPTY_FORM)
      setImageVersion(Date.now())
      flash('实验引导条目创建成功')
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
      type: item.type || 'IMAGE_TEXT',
      title: item.title,
      imageId: null,
      imageUrl: item.imageUrl || '',
      hasImage: Boolean(item.hasImage || item.imageUrl),
      removeImage: false,
      content: item.content || '',
      sortOrder: item.sortOrder,
      enabled: item.enabled,
      showInWholeExperiment: item.showInWholeExperiment !== false,
    })
  }

  const handleUpdate = async (e) => {
    e.preventDefault()
    if (!editingItem) return
    if (!validateForm(editForm, setError)) return
    setSaving(true)
    setError('')
    try {
      const payload = { ...editForm }
      delete payload.hasImage
      await api.updateExperimentGuide(editingItem.id, {
        ...payload,
        sortOrder: normalizeSortOrder(editForm.sortOrder),
      })
      setEditingItem(null)
      setImageVersion(Date.now())
      flash('实验引导条目已更新')
      await loadData()
    } catch (err) {
      setError(err.message || '更新失败')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (item) => {
    if (!window.confirm(`确定删除实验引导「${item.title}」？`)) return
    setError('')
    try {
      await api.deleteExperimentGuide(item.id)
      flash('实验引导条目已删除')
      await loadData()
    } catch (err) {
      setError(err.message || '删除失败')
    }
  }

  const renderDialog = (mode) => {
    const isCreate = mode === 'create'
    const form = isCreate ? createForm : editForm
    const setForm = isCreate ? setCreateForm : setEditForm
    const busy = isCreate ? creating : saving
    const close = () => {
      if (isCreate) {
        setShowCreate(false)
        setCreateForm(EMPTY_FORM)
      } else {
        setEditingItem(null)
      }
    }

    return (
      <div className="users-page__overlay">
        <form className="users-page__dialog" onSubmit={isCreate ? handleCreate : handleUpdate}>
          <h3>{isCreate ? '新增实验引导' : '编辑实验引导'}</h3>
          <label>
            引导类型
            <select
              value={form.type}
              onChange={(e) => setForm((current) => ({ ...current, type: e.target.value }))}
            >
              <option value="IMAGE_TEXT">图片文字</option>
              <option value="SETTING_LIST">定值整定</option>
            </select>
          </label>
          <label>
            引导标题
            <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
          </label>
          {form.type === 'IMAGE_TEXT' && (
            <CabinetImageUploadField
              imageUrl={form.imageUrl}
              previewUrl={!isCreate && editingItem && form.hasImage
                ? imageUrl('experiment-guide', editingItem.id, imageVersion)
                : ''}
              onChange={(url, result) => setForm((current) => ({
                ...current,
                imageUrl: url,
                imageId: result?.imageId ?? null,
                hasImage: Boolean(url || result?.imageId) || (current.hasImage && !result?.removeImage),
                removeImage: Boolean(result?.removeImage),
              }))}
              uploadImage={api.uploadExperimentGuideImage}
              disabled={busy}
              allowClear
            />
          )}
          <label>
            文字描述
            <textarea
              rows={6}
              value={form.content}
              onChange={(e) => setForm({ ...form, content: e.target.value })}
              placeholder={form.type === 'SETTING_LIST' ? '（可选）定值整定引导的文字说明' : '文字说明'}
            />
          </label>
          <label>
            排序
            <input
              type="number"
              value={form.sortOrder}
              placeholder="不填则自动排到末尾"
              onChange={(e) => setForm({ ...form, sortOrder: e.target.value })}
            />
          </label>
          <label className="users-page__checkbox">
            <input
              type="checkbox"
              checked={form.enabled}
              onChange={(e) => setForm({ ...form, enabled: e.target.checked })}
            />
            启用
          </label>
          {scopeType === 'LOGIC_DIAGRAM' && <label className="users-page__checkbox">
            <input type="checkbox" checked={form.showInWholeExperiment}
              onChange={(e) => setForm({ ...form, showInWholeExperiment: e.target.checked })} />
            在整组实验中显示
          </label>}
          <div className="users-page__dialog-actions">
            <button type="button" className="users-page__btn" onClick={close}>取消</button>
            <button type="submit" className="users-page__btn users-page__btn--primary" disabled={busy}>
              {busy ? '保存中…' : '保存'}
            </button>
          </div>
        </form>
      </div>
    )
  }

  const backTo = scopeType === 'LOGIC_GROUP'
    ? (device ? `/admin/logic-learning/devices/${device.id}/logic-groups` : '/admin/logic-learning')
    : (device ? `/admin/logic-learning/devices/${device.id}/logics` : '/admin/logic-learning')

  return (
    <div className="users-page">
      <div className="users-page__header">
        <div>
          <p className="users-page__breadcrumb">
            <Link to="/admin/logic-learning">逻辑学习</Link>
            {device && (
              <>
                <span> / </span>
                <Link to={`/admin/logic-learning/cabinets/${device.cabinetId}/devices`}>
                  {device.cabinetName}
                </Link>
                <span> / </span>
                <Link to={backTo}>{device.name}</Link>
              </>
            )}
            {scopeName && (
              <>
                <span> / </span>
                <span>{scopeName} — 实验引导</span>
              </>
            )}
          </p>
          <h2 className="users-page__title">{scopeName ? `${scopeName} — 实验引导` : '实验引导'}</h2>
          <p className="users-page__desc">为学员在开始实验前提供按序浏览的引导内容；「定值整定」类型将展示所属装置的完整定值清单表格。</p>
        </div>
        <button type="button" className="users-page__btn users-page__btn--primary" onClick={() => setShowCreate(true)}>
          新增引导项
        </button>
      </div>

      {message && <div className="users-page__message">{message}</div>}
      {error && <div className="users-page__error">{error}</div>}

      {loading ? (
        <p className="users-page__loading">加载中…</p>
      ) : (
        <div className="users-page__table-wrap">
          <table className="users-page__table">
            <thead>
              <tr>
                <th>类型</th>
                <th>标题</th>
                <th>描述摘要</th>
                <th>排序</th>
                <th>状态</th>
                {scopeType === 'LOGIC_DIAGRAM' && <th>在整组实验中显示</th>}
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {items.length === 0 ? (
                <tr>
                  <td colSpan={scopeType === 'LOGIC_DIAGRAM' ? 8 : 7} className="users-page__empty-cell">暂无实验引导，点击右上角「新增引导项」创建。</td>
                </tr>
              ) : (
                items.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <span className={`logic-learning__media-badge${item.type === 'SETTING_LIST' ? ' logic-learning__media-badge--text' : ''}`}>
                        {TYPE_LABELS[item.type] ?? item.type}
                      </span>
                    </td>
                    <td>{item.title}</td>
                    <td>{previewContent(item.content)}</td>
                    <td>{item.sortOrder}</td>
                    <td>{item.enabled ? '启用' : '停用'}</td>
                    {scopeType === 'LOGIC_DIAGRAM' && <td>{item.showInWholeExperiment !== false ? '是' : '否'}</td>}
                    <td>{formatDate(item.createdAt)}</td>
                    <td className="users-page__actions">
                      <button type="button" className="users-page__link" onClick={() => openEdit(item)}>编辑</button>
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

      {showCreate && renderDialog('create')}
      {editingItem && renderDialog('edit')}
    </div>
  )
}
