/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api, imageUrl, videoUrl } from '../../../../api/client'
import CabinetImageUploadField from '../../../../components/CabinetImageUploadField'
import CognitionVideoUploadField from '../../../../components/CognitionVideoUploadField'
import ImageRegionEditor from '../../../../components/ImageRegionEditor'
import { DEFAULT_REGION, normalizeRegion } from '../../../../utils/imageRegionUtils'
import '../UsersPage.css'
import '../display/DeviceDisplayItemsPage.css'
import './LogicLearningPage.css'

const EMPTY_FORM = {
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
}

const NODE_TYPE_LABELS = {
  INPUT: '输入节点',
  TIMER: '延时节点',
  OUTPUT: '输出节点',
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

function hasItemImage(item) {
  return Boolean(item.hasImage || item.imageUrl)
}

function validateFormContent(form, setError) {
  if (form.mediaType === 'VIDEO' && !form.videoPath) {
    setError('请上传认知视频')
    return false
  }
  if (form.mediaType === 'TEXT' && !hasText(form.content)) {
    setError('请填写文字描述')
    return false
  }
  if (form.mediaType === 'IMAGE' && !hasFormImage(form)) {
    setError('请上传认知图片')
    return false
  }
  return true
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

function HighlightRegionField({ form, setForm, previewSrc, disabled }) {
  const enabled = hasHighlightRegion(form)
  const region = formToRegion(form)

  return (
    <div className="logic-learning__highlight-field">
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
          <p className="logic-learning__highlight-hint">拖动黄框调整高亮显示的位置和大小</p>
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

export default function LogicNodeItemsPage() {
  const { logicDiagramId, nodeId } = useParams()
  const logicDiagramIdNum = Number(logicDiagramId)
  const decodedNodeId = nodeId ?? ''
  const [logic, setLogic] = useState(null)
  const [device, setDevice] = useState(null)
  const [nodes, setNodes] = useState([])
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

  const selectedNode = useMemo(
    () => nodes.find((node) => node.nodeId === decodedNodeId) ?? null,
    [nodes, decodedNodeId],
  )

  const flash = (text) => {
    setMessage(text)
    setTimeout(() => setMessage(''), 3000)
  }

  const loadData = useCallback(async () => {
    if (!logicDiagramIdNum || !decodedNodeId) return
    setLoading(true)
    setError('')
    try {
      const [logicData, nodeData, itemData] = await Promise.all([
        api.getProtectionLogic(logicDiagramIdNum),
        api.listLogicLearningNodes(logicDiagramIdNum),
        api.listLogicNodeCognitionItems(logicDiagramIdNum, decodedNodeId),
      ])
      setLogic(logicData)
      setNodes(nodeData)
      setItems(itemData)
      if (logicData.deviceId) {
        const deviceData = await api.getKnowledgeDevice(logicData.deviceId)
        setDevice(deviceData)
      } else {
        setDevice(null)
      }
    } catch (err) {
      setError(err.message || '加载认知条目失败')
      setLogic(null)
      setDevice(null)
      setNodes([])
      setItems([])
    } finally {
      setLoading(false)
    }
  }, [logicDiagramIdNum, decodedNodeId])

  useEffect(() => {
    loadData()
  }, [loadData])

  if (!logicDiagramId || Number.isNaN(logicDiagramIdNum) || !decodedNodeId) {
    return <Navigate to="/admin/logic-learning" replace />
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    if (!validateFormContent(createForm, setError)) return
    setCreating(true)
    setError('')
    try {
      const payload = { ...createForm }
      delete payload.hasImage
      await api.createLogicNodeCognitionItem(logicDiagramIdNum, decodedNodeId, {
        ...payload,
        sortOrder: Number(createForm.sortOrder),
      })
      setShowCreate(false)
      setCreateForm(EMPTY_FORM)
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
      mediaType: item.mediaType || (hasItemImage(item) ? 'IMAGE' : 'TEXT'),
      videoPath: item.videoPath || '',
      imageId: null,
      imageUrl: item.imageUrl,
      hasImage: hasItemImage(item),
      removeImage: false,
      leftPercent: item.leftPercent ?? null,
      topPercent: item.topPercent ?? null,
      widthPercent: item.widthPercent ?? null,
      heightPercent: item.heightPercent ?? null,
      content: item.content,
      sortOrder: item.sortOrder,
      enabled: item.enabled,
    })
  }

  const handleUpdate = async (e) => {
    e.preventDefault()
    if (!editingItem) return
    if (!validateFormContent(editForm, setError)) return
    setSaving(true)
    setError('')
    try {
      const payload = { ...editForm }
      delete payload.hasImage
      await api.updateLogicNodeCognitionItem(editingItem.id, {
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
      await api.deleteLogicNodeCognitionItem(item.id)
      flash('认知条目已删除')
      await loadData()
    } catch (err) {
      setError(err.message || '删除失败')
    }
  }

  const cleanupDraftVideo = (path, savedPath = '') => {
    if (path && path !== savedPath) api.deleteUnreferencedCognitionVideo(path).catch(() => {})
  }

  const renderDialog = (mode) => {
    const isCreate = mode === 'create'
    const form = isCreate ? createForm : editForm
    const setForm = isCreate ? setCreateForm : setEditForm
    const busy = isCreate ? creating : saving
    const close = () => {
      cleanupDraftVideo(form.videoPath, isCreate ? '' : editingItem?.videoPath)
      if (isCreate) {
        setShowCreate(false)
        setCreateForm(EMPTY_FORM)
      } else {
        setEditingItem(null)
      }
    }

    return (
      <div className="users-page__overlay">
        <form
          className="users-page__dialog"
          onSubmit={isCreate ? handleCreate : handleUpdate}
        >
          <h3>{isCreate ? '新增认知条目' : '编辑认知条目'}</h3>
          <label>
            条目名称
            <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
          </label>
          <label>
            媒体类型
            <select
              value={form.mediaType}
              onChange={(e) => {
                const mediaType = e.target.value
                const savedPath = isCreate ? '' : editingItem?.videoPath
                if (mediaType !== 'VIDEO') cleanupDraftVideo(form.videoPath, savedPath)
                setForm((current) => ({
                  ...current,
                  mediaType,
                  videoPath: mediaType === 'VIDEO' || current.videoPath === savedPath
                    ? current.videoPath
                    : '',
                  ...(mediaType !== 'IMAGE' ? regionFields(null) : {}),
                }))
              }}
            >
              <option value="IMAGE">图片</option>
              <option value="VIDEO">视频</option>
              <option value="TEXT">仅文字</option>
            </select>
          </label>
          {form.mediaType === 'IMAGE' && (
            <CabinetImageUploadField
              imageUrl={form.imageUrl}
              previewUrl={!isCreate && editingItem && form.hasImage && editingItem.mediaType !== 'VIDEO'
                ? imageUrl('logic-node-cognition', editingItem.id, imageVersion)
                : ''}
              onChange={(url, result) => setForm((current) => ({
                ...current,
                imageUrl: url,
                imageId: result?.imageId ?? null,
                hasImage: Boolean(url || result?.imageId) || (current.hasImage && !result?.removeImage),
                removeImage: Boolean(result?.removeImage),
                ...(!url && result?.removeImage ? regionFields(null) : {}),
              }))}
              uploadImage={api.uploadDeviceDisplayImage}
              disabled={busy}
              allowClear
              renderPreviewExtra={(previewSrc) => (
                <HighlightRegionField form={form} setForm={setForm} previewSrc={previewSrc} disabled={busy} />
              )}
            />
          )}
          {form.mediaType === 'VIDEO' && (
            <CognitionVideoUploadField
              value={form.videoPath}
              previewUrl={!isCreate && editingItem?.mediaType === 'VIDEO'
                ? videoUrl('logic-node-cognition', editingItem.id)
                : ''}
              onChange={(videoPath) => setForm((current) => ({ ...current, videoPath }))}
              disabled={busy}
            />
          )}
          <label>
            文字描述
            <textarea
              rows={6}
              value={form.content}
              onChange={(e) => setForm({ ...form, content: e.target.value })}
            />
          </label>
          <label>
            排序
            <input
              type="number"
              value={form.sortOrder}
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

  return (
    <div className="users-page">
      <div className="users-page__header">
        <div>
          <p className="users-page__breadcrumb">
            <Link to="/admin/logic-learning">逻辑学习</Link>
            {device && (
              <>
                <span> / </span>
                <Link to={`/admin/logic-learning/cabinets/${device.cabinetId}/devices`}>{device.cabinetName}</Link>
                <span> / </span>
                <Link to={`/admin/logic-learning/devices/${device.id}/logics`}>{device.name}</Link>
              </>
            )}
            {logic && (
              <>
                <span> / </span>
                <Link to={`/admin/logic-learning/logics/${logic.id}/nodes`}>{logic.title}</Link>
              </>
            )}
            {selectedNode && (
              <>
                <span> / </span>
                <span>{selectedNode.nodeName} — 认知条目</span>
              </>
            )}
          </p>
          <h2 className="users-page__title">
            {selectedNode ? `${selectedNode.nodeName} — 认知条目` : '节点认知条目'}
          </h2>
          {selectedNode && (
            <p className="users-page__desc">
              {NODE_TYPE_LABELS[selectedNode.nodeType] ?? selectedNode.nodeType} · {selectedNode.nodeId}
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
      ) : !logic || !selectedNode ? (
        <p className="users-page__empty">
          逻辑节点不存在，<Link to={logic ? `/admin/logic-learning/logics/${logic.id}/nodes` : '/admin/logic-learning'}>返回列表</Link>。
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
                  <td colSpan={7} className="users-page__empty-cell">暂无认知条目</td>
                </tr>
              ) : (
                items.map((item) => (
                  <tr key={item.id}>
                    <td>
                      {item.mediaType === 'VIDEO' ? (
                        <span className="logic-learning__media-badge">视频</span>
                      ) : item.mediaType === 'TEXT' ? (
                        <span className="logic-learning__media-badge logic-learning__media-badge--text">文字</span>
                      ) : hasItemImage(item) ? (
                        <img
                          className="logic-learning__thumb"
                          src={imageUrl('logic-node-cognition', item.id, imageVersion)}
                          alt={item.title}
                        />
                      ) : (
                        <span className="logic-learning__thumb-placeholder">无图片</span>
                      )}
                    </td>
                    <td>{item.title}</td>
                    <td>{previewContent(item.content)}</td>
                    <td>{item.sortOrder}</td>
                    <td>{item.enabled ? '启用' : '停用'}</td>
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
