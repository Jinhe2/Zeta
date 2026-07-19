import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api, imageUrl } from '../../../../api/client'
import ImageRegionEditor from '../../../../components/ImageRegionEditor'
import { DEFAULT_REGION, normalizeRegion } from '../../../../utils/imageRegionUtils'
import '../UsersPage.css'
import './CognitionDevicesPage.css'
import '../../../../components/ImageRegionEditor.css'

const DEVICE_TYPE_OPTIONS = [
  { value: 'IED', label: 'IED 设备' },
  { value: 'TERMINAL_GROUP', label: '端子组' },
  { value: 'PLATE_GROUP', label: '压板组' },
  { value: 'OTHER_DEVICE', label: '其他设备' },
]

const EMPTY_FORM = {
  deviceType: 'IED',
  screenDeviceId: '',
  title: '',
  leftPercent: DEFAULT_REGION.leftPercent,
  topPercent: DEFAULT_REGION.topPercent,
  widthPercent: DEFAULT_REGION.widthPercent,
  heightPercent: DEFAULT_REGION.heightPercent,
  sortOrder: 0,
  enabled: true,
}

function deviceTypeLabel(type) {
  return DEVICE_TYPE_OPTIONS.find((o) => o.value === type)?.label ?? type
}

export default function CognitionDevicesPage() {
  const { itemId } = useParams()
  const itemIdNum = Number(itemId)

  const [cabinetItem, setCabinetItem] = useState(null)
  const [cabinet, setCabinet] = useState(null)
  const [devices, setDevices] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_FORM)
  const [creating, setCreating] = useState(false)

  const [editingDevice, setEditingDevice] = useState(null)
  const [editForm, setEditForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)

  const [regionDraft, setRegionDraft] = useState(null)
  const [regionSaving, setRegionSaving] = useState(false)

  const selectedDevice = devices.find((d) => d.id === selectedId) ?? null
  const cabinetImageUrl = cabinetItem ? imageUrl('cabinet-display', cabinetItem.id, cabinetItem.id) : ''

  useEffect(() => {
    const device = devices.find((d) => d.id === selectedId)
    if (!device) {
      setRegionDraft(null)
      return
    }
    setRegionDraft(
      normalizeRegion({
        leftPercent: device.leftPercent,
        topPercent: device.topPercent,
        widthPercent: device.widthPercent,
        heightPercent: device.heightPercent,
      }),
    )
  }, [selectedId, devices])

  const loadData = useCallback(async () => {
    if (!itemIdNum) return
    setLoading(true)
    setError('')
    try {
      const item = await api.getCabinetDisplayItem(itemIdNum)
      setCabinetItem(item)
      const [deviceList, cabinetData] = await Promise.all([
        api.listCognitionDevices(itemIdNum),
        api.getKnowledgeCabinet(item.cabinetId),
      ])
      setDevices(deviceList)
      setCabinet(cabinetData)
      setSelectedId((prev) => {
        if (prev && deviceList.some((d) => d.id === prev)) return prev
        return deviceList[0]?.id ?? null
      })
    } catch (err) {
      setError(err.message || '加载认知设备失败')
      setCabinetItem(null)
      setDevices([])
    } finally {
      setLoading(false)
    }
  }, [itemIdNum])

  useEffect(() => {
    loadData()
  }, [loadData])

  if (!itemId || Number.isNaN(itemIdNum)) {
    return <Navigate to="/admin/display" replace />
  }

  const flash = (text) => {
    setMessage(text)
    setTimeout(() => setMessage(''), 3000)
  }

  const handleSaveRegion = async () => {
    if (!selectedDevice || !regionDraft) return
    setRegionSaving(true)
    setError('')
    try {
      await api.updateCognitionDevice(selectedDevice.id, {
        deviceType: selectedDevice.deviceType,
        screenDeviceId: selectedDevice.screenDeviceId,
        title: selectedDevice.title,
        ...regionDraft,
        sortOrder: selectedDevice.sortOrder,
        enabled: selectedDevice.enabled,
      })
      flash('区域已保存')
      await loadData()
    } catch (err) {
      setError(err.message || '保存区域失败')
    } finally {
      setRegionSaving(false)
    }
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    setCreating(true)
    setError('')
    try {
      const payload = {
        ...createForm,
        screenDeviceId: createForm.deviceType === 'IED' ? Number(createForm.screenDeviceId) : null,
        sortOrder: Number(createForm.sortOrder),
      }
      await api.createCognitionDevice(itemIdNum, payload)
      setShowCreate(false)
      setCreateForm(EMPTY_FORM)
      flash('认知设备创建成功')
      await loadData()
    } catch (err) {
      setError(err.message || '创建失败')
    } finally {
      setCreating(false)
    }
  }

  const openEdit = (device) => {
    setEditingDevice(device)
    setEditForm({
      deviceType: device.deviceType,
      screenDeviceId: device.screenDeviceId ?? '',
      title: device.title,
      leftPercent: device.leftPercent,
      topPercent: device.topPercent,
      widthPercent: device.widthPercent,
      heightPercent: device.heightPercent,
      sortOrder: device.sortOrder,
      enabled: device.enabled,
    })
  }

  const handleUpdate = async (e) => {
    e.preventDefault()
    if (!editingDevice) return
    setSaving(true)
    setError('')
    try {
      await api.updateCognitionDevice(editingDevice.id, {
        ...editForm,
        screenDeviceId: editForm.deviceType === 'IED' ? Number(editForm.screenDeviceId) : null,
        sortOrder: Number(editForm.sortOrder),
      })
      setEditingDevice(null)
      flash('认知设备已更新')
      await loadData()
    } catch (err) {
      setError(err.message || '更新失败')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (device) => {
    if (!window.confirm(`确定删除认知设备「${device.title}」？`)) return
    setError('')
    try {
      await api.deleteCognitionDevice(device.id)
      flash('认知设备已删除')
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
            {cabinetItem && (
              <>
                <span> / </span>
                <Link to={`/admin/display/cabinets/${cabinetItem.cabinetId}`}>
                  {cabinetItem.cabinetName ?? cabinet?.name ?? '屏柜'}
                </Link>
                <span> / </span>
                <span>{cabinetItem.title} — 子设备</span>
              </>
            )}
          </p>
          <h2 className="users-page__title">
            {cabinetItem ? `${cabinetItem.title} — 认知子设备` : '认知子设备'}
          </h2>
          <p className="users-page__desc">
            子设备隶属于本张屏柜学习图，在图上圈选区域；类型可为 IED 设备、其他设备、端子组或压板组。
          </p>
        </div>
        <button type="button" className="users-page__btn users-page__btn--primary" onClick={() => setShowCreate(true)}>
          新增子设备
        </button>
      </div>

      {message && <div className="users-page__message">{message}</div>}
      {error && <div className="users-page__error">{error}</div>}

      {loading ? (
        <p className="users-page__loading">加载中…</p>
      ) : !cabinetItem ? (
        <p className="users-page__empty">
          屏柜学习条目不存在，<Link to="/admin/display">返回列表</Link>。
        </p>
      ) : (
        <>
          <section className="cognition-devices__editor-section">
            <p className="cognition-devices__hint">选中下方子设备，拖动黄框调整其在图上的区域</p>
            {selectedDevice && regionDraft && cabinetImageUrl && (
              <>
                <ImageRegionEditor
                  key={selectedId}
                  imageUrl={cabinetImageUrl}
                  region={regionDraft}
                  onChange={setRegionDraft}
                />
                <div className="cognition-devices__region-actions">
                  <button
                    type="button"
                    className="users-page__btn users-page__btn--primary"
                    onClick={handleSaveRegion}
                    disabled={regionSaving}
                  >
                    {regionSaving ? '保存中…' : '保存区域'}
                  </button>
                </div>
              </>
            )}
            {!selectedDevice && (
              <p className="users-page__desc">请先新增或选择一个子设备</p>
            )}
          </section>

          <div className="users-page__table-wrap">
            <table className="users-page__table">
              <thead>
                <tr>
                  <th>名称</th>
                  <th>类型</th>
                  <th>关联 IED</th>
                  <th>区域</th>
                  <th>排序</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {devices.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="users-page__empty-cell">
                      暂无子设备
                    </td>
                  </tr>
                ) : (
                  devices.map((device) => (
                    <tr
                      key={device.id}
                      className={device.id === selectedId ? 'cognition-devices__row--active' : ''}
                      onClick={() => setSelectedId(device.id)}
                    >
                      <td>{device.title}</td>
                      <td>{deviceTypeLabel(device.deviceType)}</td>
                      <td>{device.screenDeviceName ?? '—'}</td>
                      <td>
                        {Math.round(device.leftPercent)}%, {Math.round(device.topPercent)}% ·{' '}
                        {Math.round(device.widthPercent)}×{Math.round(device.heightPercent)}%
                      </td>
                      <td>{device.sortOrder}</td>
                      <td>{device.enabled ? '启用' : '停用'}</td>
                      <td className="users-page__actions" onClick={(e) => e.stopPropagation()}>
                        <Link
                          className="users-page__link"
                          to={`/admin/display/cognition-devices/${device.id}/items`}
                        >
                          认知条目
                        </Link>
                        <button type="button" className="users-page__link" onClick={() => openEdit(device)}>
                          编辑
                        </button>
                        <button
                          type="button"
                          className="users-page__link users-page__link--danger"
                          onClick={() => handleDelete(device)}
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
        <DeviceFormDialog
          title="新增子设备"
          form={createForm}
          setForm={setCreateForm}
          cabinetDevices={cabinet?.devices ?? []}
          onClose={() => setShowCreate(false)}
          onSubmit={handleCreate}
          submitting={creating}
        />
      )}

      {editingDevice && (
        <DeviceFormDialog
          title="编辑子设备"
          form={editForm}
          setForm={setEditForm}
          cabinetDevices={cabinet?.devices ?? []}
          onClose={() => setEditingDevice(null)}
          onSubmit={handleUpdate}
          submitting={saving}
        />
      )}
    </div>
  )
}

function DeviceFormDialog({ title, form, setForm, cabinetDevices, onClose, onSubmit, submitting }) {
  return (
    <div className="users-page__overlay">
      <form className="users-page__dialog" onSubmit={onSubmit}>
        <h3>{title}</h3>
        <label>
          设备类型
          <select
            value={form.deviceType}
            onChange={(e) => setForm({ ...form, deviceType: e.target.value, screenDeviceId: '' })}
            required
          >
            {DEVICE_TYPE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </label>
        {form.deviceType === 'IED' && (
          <label>
            关联屏柜库 IED
            <select
              value={form.screenDeviceId}
              onChange={(e) => setForm({ ...form, screenDeviceId: e.target.value })}
              required
            >
              <option value="">请选择</option>
              {cabinetDevices.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name} ({d.code})
                </option>
              ))}
            </select>
          </label>
        )}
        <label>
          显示名称
          <input
            value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })}
            required
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
        <p className="cognition-devices__form-hint">区域坐标可在保存后于主界面拖动调整</p>
        <div className="users-page__dialog-actions">
          <button type="button" className="users-page__btn" onClick={onClose}>
            取消
          </button>
          <button type="submit" className="users-page__btn users-page__btn--primary" disabled={submitting}>
            {submitting ? '保存中…' : '保存'}
          </button>
        </div>
      </form>
    </div>
  )
}
