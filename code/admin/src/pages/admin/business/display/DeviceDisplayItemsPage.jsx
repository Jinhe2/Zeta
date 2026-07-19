/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api, imageUrl, videoUrl } from '../../../../api/client'
import CabinetImageUploadField from '../../../../components/CabinetImageUploadField'
import CognitionVideoUploadField from '../../../../components/CognitionVideoUploadField'
import ImageRegionEditor from '../../../../components/ImageRegionEditor'
import { DEFAULT_REGION, normalizeRegion } from '../../../../utils/imageRegionUtils'
import '../UsersPage.css'
import './DeviceDisplayItemsPage.css'

const EMPTY_CREATE = {
  title: '',
  mediaType: 'IMAGE',
  videoPath: '',
  imageId: null,
  imageUrl: '',
  hasImage: false,
  leftPercent: null,
  topPercent: null,
  widthPercent: null,
  heightPercent: null,
  content: '',
  sortOrder: 0,
  enabled: true,
  terminalOperation: { terminalStripId: '', terminals: [] },
}

const DEVICE_TYPE_LABELS = {
  IED: 'IED 设备外观',
  IED_OPERATION: 'IED 设备操作',
  OTHER_DEVICE: '其他设备',
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

function hasHighlightRegion(form) {
  return (
    form.leftPercent != null
    && form.topPercent != null
    && form.widthPercent != null
    && form.heightPercent != null
  )
}

function roundPercent(value) {
  return Math.round(Number(value) * 1000) / 1000
}

function regionFields(region) {
  if (!region) {
    return {
      leftPercent: null,
      topPercent: null,
      widthPercent: null,
      heightPercent: null,
    }
  }
  const normalized = normalizeRegion(region)
  return {
    leftPercent: roundPercent(normalized.leftPercent),
    topPercent: roundPercent(normalized.topPercent),
    widthPercent: roundPercent(normalized.widthPercent),
    heightPercent: roundPercent(normalized.heightPercent),
  }
}

function formToRegion(form) {
  if (!hasHighlightRegion(form)) return null
  return normalizeRegion({
    leftPercent: form.leftPercent,
    topPercent: form.topPercent,
    widthPercent: form.widthPercent,
    heightPercent: form.heightPercent,
  })
}

function BaselineSettingsPreview({ cognitionDeviceId }) {
  const [state, setState] = useState({ loading: true, items: [], error: '' })

  useEffect(() => {
    let cancelled = false
    api.listKnowledgeCognitionDeviceBaselineSettings(cognitionDeviceId)
      .then((items) => !cancelled && setState({ loading: false, items, error: '' }))
      .catch((err) => !cancelled && setState({ loading: false, items: [], error: err.message || '加载基准定值失败' }))
    return () => { cancelled = true }
  }, [cognitionDeviceId])

  return (
    <div className="device-display-items__baseline-preview">
      <span>关联 IED 基准定值（只读）</span>
      {state.loading && <p>加载中…</p>}
      {state.error && <p className="users-page__error">{state.error}</p>}
      {!state.loading && !state.error && (
        <table className="users-page__table">
          <thead><tr><th>定值描述</th><th>基准值</th></tr></thead>
          <tbody>{state.items.length ? state.items.map((item, index) => (
            <tr key={`${item.description}-${index}`}><td>{item.description || '—'}</td><td>{item.baselineValue}</td></tr>
          )) : <tr><td colSpan={2}>暂无基准定值</td></tr>}</tbody>
        </table>
      )}
    </div>
  )
}

function HighlightRegionField({ form, setForm, previewSrc, disabled }) {
  const enabled = hasHighlightRegion(form)
  const region = formToRegion(form)

  return (
    <div className="device-display-items__highlight-field">
      <label className="users-page__checkbox">
        <input
          type="checkbox"
          checked={enabled}
          onChange={(e) =>
            setForm((current) => ({
              ...current,
              ...regionFields(e.target.checked ? DEFAULT_REGION : null),
            }))
          }
          disabled={disabled}
        />
        设置图片高亮区域
      </label>
      {enabled && (
        <>
          <p className="device-display-items__highlight-hint">拖动黄框调整高亮显示的位置和大小</p>
          <ImageRegionEditor
            imageUrl={previewSrc}
            region={region}
            onChange={(nextRegion) =>
              setForm((current) => ({
                ...current,
                ...regionFields(nextRegion),
              }))
            }
            readOnly={disabled}
          />
        </>
      )}
    </div>
  )
}

export default function DeviceDisplayItemsPage() {
  const { cognitionDeviceId } = useParams()
  const cognitionDeviceIdNum = Number(cognitionDeviceId)

  const [cognitionDevice, setCognitionDevice] = useState(null)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [imageVersion, setImageVersion] = useState(() => Date.now())

  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_CREATE)
  const [creating, setCreating] = useState(false)

  const [editingItem, setEditingItem] = useState(null)
  const [editForm, setEditForm] = useState({
    title: '',
    mediaType: 'IMAGE',
    videoPath: '',
    imageId: null,
    imageUrl: '',
    hasImage: false,
    content: '',
    sortOrder: 0,
    enabled: true,
    leftPercent: null,
    topPercent: null,
    widthPercent: null,
    heightPercent: null,
    terminalOperation: { terminalStripId: '', terminals: [] },
  })
  const [saving, setSaving] = useState(false)
  const [terminalStrips, setTerminalStrips] = useState([])
  const [terminals, setTerminals] = useState([])

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
      const cabinetItem = await api.getCabinetDisplayItem(deviceData.cabinetDisplayItemId)
      const [strips, terminalData] = await Promise.all([
        api.listTerminalStrips(cabinetItem.cabinetId),
        api.listTerminals(cabinetItem.cabinetId),
      ])
      setCognitionDevice(deviceData)
      setItems(itemData)
      setTerminalStrips(strips)
      setTerminals(terminalData)
    } catch (err) {
      setError(err.message || '加载认知条目失败')
      setCognitionDevice(null)
      setItems([])
      setTerminalStrips([])
      setTerminals([])
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
    if (createForm.mediaType === 'IMAGE' && !createForm.imageId && !createForm.imageUrl) {
      setError('请上传认知图片')
      return
    }
    if (createForm.mediaType === 'VIDEO' && !createForm.videoPath) {
      setError('请上传认知视频')
      return
    }
    if (!validateTerminalOperation(createForm)) return
    setCreating(true)
    setError('')
    try {
      const payload = { ...createForm }
      delete payload.hasImage
      await api.createCognitionDeviceDisplayItem(cognitionDeviceIdNum, {
        ...payload,
        sortOrder: Number(createForm.sortOrder),
      })
      setShowCreate(false)
      setCreateForm(EMPTY_CREATE)
      setImageVersion(Date.now())
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
      mediaType: item.mediaType || 'IMAGE',
      videoPath: item.videoPath || '',
      imageId: null,
      imageUrl: item.imageUrl,
      hasImage: (item.mediaType || 'IMAGE') === 'IMAGE',
      content: item.content,
      sortOrder: item.sortOrder,
      enabled: item.enabled,
      leftPercent: item.leftPercent ?? null,
      topPercent: item.topPercent ?? null,
      widthPercent: item.widthPercent ?? null,
      heightPercent: item.heightPercent ?? null,
      terminalOperation: item.terminalOperation
        ? { terminalStripId: String(item.terminalOperation.terminalStripId), terminals: item.terminalOperation.terminals.map((terminal) => ({ terminalId: terminal.terminalId, meaning: terminal.meaning })) }
        : { terminalStripId: '', terminals: [] },
    })
  }

  const handleUpdate = async (e) => {
    e.preventDefault()
    if (!editingItem) return
    if (editForm.mediaType === 'IMAGE' && !editForm.hasImage && !editForm.imageId && !editForm.imageUrl) {
      setError('请上传认知图片')
      return
    }
    if (editForm.mediaType === 'VIDEO' && !editForm.videoPath) {
      setError('请上传认知视频')
      return
    }
    if (!validateTerminalOperation(editForm)) return
    setSaving(true)
    setError('')
    try {
      const payload = { ...editForm }
      delete payload.hasImage
      await api.updateDeviceDisplayItem(editingItem.id, {
        ...payload,
        sortOrder: Number(editForm.sortOrder),
      })
      setEditingItem(null)
      setImageVersion(Date.now())
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

  const cleanupDraftVideo = (path, savedPath = '') => {
    if (path && path !== savedPath) api.deleteUnreferencedCognitionVideo(path).catch(() => {})
  }

  const validateTerminalOperation = (form) => {
    if (form.mediaType !== 'TERMINAL_OPERATION') return true
    if (!form.terminalOperation?.terminalStripId || !form.terminalOperation.terminals?.length) {
      setError('请选择端子排和至少一个端子')
      return false
    }
    if (form.terminalOperation.terminals.some((terminal) => !terminal.meaning?.trim())) {
      setError('需要操作的端子含义不能为空')
      return false
    }
    return true
  }

  const terminalOperationField = (form, setForm, disabled) => {
    const operation = form.terminalOperation || { terminalStripId: '', terminals: [] }
    const selected = new Map(operation.terminals.map((terminal) => [Number(terminal.terminalId), terminal.meaning]))
    const stripTerminals = terminals.filter((terminal) => Number(terminal.terminalStripId) === Number(operation.terminalStripId))
    const updateOperation = (next) => setForm((current) => ({ ...current, terminalOperation: next }))
    return <>
      <label>端子排<select value={operation.terminalStripId} disabled={disabled} required onChange={(e) => updateOperation({ terminalStripId: e.target.value, terminals: [] })}>
        <option value="">请选择端子排</option>
        {terminalStrips.map((strip) => <option key={strip.id} value={strip.id}>{strip.name}（{strip.labelPrefix}）</option>)}
      </select></label>
      {operation.terminalStripId && <div className="device-display-items__terminal-operation">
        <p>勾选需要接线的端子，并填写测试仪端子的表示含义。</p>
        <div className="device-display-items__terminal-table-wrap"><table className="users-page__table"><thead><tr><th>选择</th><th>端子编号</th><th>端子表示含义</th></tr></thead><tbody>
          {stripTerminals.map((terminal) => {
            const checked = selected.has(Number(terminal.id))
            return <tr key={terminal.id}><td><input type="checkbox" checked={checked} disabled={disabled} onChange={(e) => {
              const next = operation.terminals.filter((entry) => Number(entry.terminalId) !== Number(terminal.id))
              if (e.target.checked) next.push({ terminalId: terminal.id, meaning: '' })
              updateOperation({ ...operation, terminals: next })
            }} /></td><td>{terminal.terminalLabel}</td><td>{checked && <input value={selected.get(Number(terminal.id)) || ''} disabled={disabled} required onChange={(e) => updateOperation({ ...operation, terminals: operation.terminals.map((entry) => Number(entry.terminalId) === Number(terminal.id) ? { ...entry, meaning: e.target.value } : entry) })} />}</td></tr>
          })}
        </tbody></table></div>
      </div>}
    </>
  }

  const closeCreate = () => {
    cleanupDraftVideo(createForm.videoPath)
    setShowCreate(false)
    setCreateForm(EMPTY_CREATE)
  }

  const closeEdit = () => {
    cleanupDraftVideo(editForm.videoPath, editingItem?.videoPath)
    setEditingItem(null)
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
                <th>媒体</th>
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
                      {item.mediaType === 'IED_BASELINE_SETTING' ? (
                        <span className="device-display-items__media-badge">定值整定</span>
                      ) : item.mediaType === 'TERMINAL_OPERATION' ? (
                        <span className="device-display-items__media-badge">端子操作{item.terminalOperation ? ` · ${item.terminalOperation.terminalStripName || ''} · ${item.terminalOperation.terminals.length} 个端子` : ''}</span>
                      ) : item.mediaType === 'VIDEO' ? (
                        <span className="device-display-items__media-badge">视频</span>
                      ) : (
                        <img
                          className="device-display-items__thumb"
                          src={imageUrl('device-display', item.id, imageVersion)}
                          alt={item.title}
                        />
                      )}
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
        <div className="users-page__overlay">
          <form className="users-page__dialog" onSubmit={handleCreate}>
            <h3>新增认知条目</h3>
            <label>
              条目名称
              <input
                value={createForm.title}
                onChange={(e) => setCreateForm({ ...createForm, title: e.target.value })}
                required
              />
            </label>
            <label>
              媒体类型
              <select
                value={createForm.mediaType}
                onChange={(e) => {
                  const mediaType = e.target.value
                  if (mediaType !== 'VIDEO') cleanupDraftVideo(createForm.videoPath)
                  setCreateForm((current) => ({
                    ...current,
                    mediaType,
                    videoPath: mediaType === 'VIDEO' ? current.videoPath : '',
                    ...(mediaType === 'VIDEO' ? regionFields(null) : {}),
                  }))
                }}
              >
                <option value="IMAGE">图片</option>
                <option value="VIDEO">视频</option>
                {cognitionDevice?.deviceType === 'TERMINAL_GROUP' && <option value="TERMINAL_OPERATION">端子操作</option>}
                {cognitionDevice?.deviceType === 'IED_OPERATION' && <option value="IED_BASELINE_SETTING">定值整定</option>}
              </select>
            </label>
            {createForm.mediaType === 'IED_BASELINE_SETTING' ? (
              <BaselineSettingsPreview cognitionDeviceId={cognitionDevice.id} />
            ) : createForm.mediaType === 'TERMINAL_OPERATION' ? terminalOperationField(createForm, setCreateForm, creating) : createForm.mediaType === 'IMAGE' ? (
              <CabinetImageUploadField
                imageUrl={createForm.imageUrl}
                onChange={(url, result) => setCreateForm((current) => ({
                  ...current,
                  imageUrl: url,
                  imageId: result?.imageId ?? null,
                  hasImage: Boolean(url || result?.imageId),
                }))}
                uploadImage={api.uploadDeviceDisplayImage}
                disabled={creating}
                renderPreviewExtra={(previewSrc) => (
                  <HighlightRegionField form={createForm} setForm={setCreateForm} previewSrc={previewSrc} disabled={creating} />
                )}
              />
            ) : (
              <CognitionVideoUploadField
                value={createForm.videoPath}
                onChange={(videoPath) => setCreateForm((current) => ({ ...current, videoPath }))}
                disabled={creating}
              />
            )}
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
              <button type="button" className="users-page__btn" onClick={closeCreate}>
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
        <div className="users-page__overlay">
          <form className="users-page__dialog" onSubmit={handleUpdate}>
            <h3>编辑认知条目</h3>
            <label>
              条目名称
              <input
                value={editForm.title}
                onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
                required
              />
            </label>
            <label>
              媒体类型
              <select
                value={editForm.mediaType}
                onChange={(e) => {
                  const mediaType = e.target.value
                  if (mediaType !== 'VIDEO') cleanupDraftVideo(editForm.videoPath, editingItem?.videoPath)
                  setEditForm((current) => ({
                    ...current,
                    mediaType,
                    videoPath: mediaType === 'VIDEO' || current.videoPath === editingItem?.videoPath
                      ? current.videoPath
                      : '',
                    ...(mediaType === 'VIDEO' ? regionFields(null) : {}),
                  }))
                }}
              >
                <option value="IMAGE">图片</option>
                <option value="VIDEO">视频</option>
                {cognitionDevice?.deviceType === 'TERMINAL_GROUP' && <option value="TERMINAL_OPERATION">端子操作</option>}
                {cognitionDevice?.deviceType === 'IED_OPERATION' && <option value="IED_BASELINE_SETTING">定值整定</option>}
              </select>
            </label>
            {editForm.mediaType === 'IED_BASELINE_SETTING' ? (
              <BaselineSettingsPreview cognitionDeviceId={cognitionDevice.id} />
            ) : editForm.mediaType === 'TERMINAL_OPERATION' ? terminalOperationField(editForm, setEditForm, saving) : editForm.mediaType === 'IMAGE' ? (
              <CabinetImageUploadField
                imageUrl={editForm.imageUrl}
                previewUrl={editingItem && editingItem.mediaType !== 'VIDEO' ? imageUrl('device-display', editingItem.id, imageVersion) : ''}
                onChange={(url, result) => setEditForm((current) => ({
                  ...current,
                  imageUrl: url,
                  imageId: result?.imageId ?? null,
                  hasImage: Boolean(url || result?.imageId || (editingItem?.id && editingItem.mediaType !== 'VIDEO')),
                }))}
                uploadImage={api.uploadDeviceDisplayImage}
                disabled={saving}
                renderPreviewExtra={(previewSrc) => (
                  <HighlightRegionField form={editForm} setForm={setEditForm} previewSrc={previewSrc} disabled={saving} />
                )}
              />
            ) : (
              <CognitionVideoUploadField
                value={editForm.videoPath}
                previewUrl={editingItem?.mediaType === 'VIDEO' ? videoUrl('device-display', editingItem.id) : ''}
                onChange={(videoPath) => setEditForm((current) => ({ ...current, videoPath }))}
                disabled={saving}
              />
            )}
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
              <button type="button" className="users-page__btn" onClick={closeEdit}>
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
