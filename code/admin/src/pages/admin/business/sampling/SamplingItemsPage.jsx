import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, imageUrl, videoUrl } from '../../../../api/client'
import CabinetImageUploadField from '../../../../components/CabinetImageUploadField'
import CognitionVideoUploadField from '../../../../components/CognitionVideoUploadField'
import '../UsersPage.css'
import './SamplingItemsPage.css'

const CHANNEL_CODES = ['Ua', 'Ub', 'Uc', 'Un', 'Ia', 'Ib', 'Ic', 'In']
const WIRING_ONLY_CODES = new Set(['Un', 'In'])

function emptyChannels() {
  return CHANNEL_CODES.map((outputCode) => ({ outputCode, terminalStripId: '', terminalId: '', baselineMagnitude: '', baselineAngle: '' }))
}

function emptyForm() {
  return { title: '', mediaType: 'IMAGE', imageId: null, imageUrl: '', videoPath: '', content: '', sortOrder: 0, enabled: true, channels: emptyChannels() }
}

function itemToForm(item) {
  const configured = new Map((item.channels || []).map((channel) => [channel.outputCode, channel]))
  return {
    title: item.title,
    mediaType: item.mediaType,
    imageId: null,
    imageUrl: item.imageUrl || '',
    videoPath: item.videoPath || '',
    content: item.content,
    sortOrder: item.sortOrder,
    enabled: item.enabled,
    channels: CHANNEL_CODES.map((outputCode) => {
      const channel = configured.get(outputCode) || {}
      return {
        outputCode,
        terminalStripId: channel.terminalStripId || '',
        terminalId: channel.terminalId || '',
        baselineMagnitude: channel.baselineMagnitude ?? '',
        baselineAngle: channel.baselineAngle ?? '',
      }
    }),
  }
}

export default function SamplingItemsPage() {
  const { cabinetId } = useParams()
  const [cabinet, setCabinet] = useState(null)
  const [items, setItems] = useState([])
  const [strips, setStrips] = useState([])
  const [terminals, setTerminals] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [dialog, setDialog] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [cabinetData, itemData, stripData, terminalData] = await Promise.all([
        api.getKnowledgeCabinet(cabinetId), api.listSamplingTestItems(cabinetId),
        api.listTerminalStrips(cabinetId), api.listTerminals(cabinetId),
      ])
      setCabinet(cabinetData); setItems(itemData); setStrips(stripData); setTerminals(terminalData)
    } catch (err) { setError(err.message || '加载采样测试配置失败') } finally { setLoading(false) }
  }, [cabinetId])

  useEffect(() => {
    const timer = window.setTimeout(load, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  const terminalById = useMemo(() => new Map(terminals.map((terminal) => [Number(terminal.id), terminal])), [terminals])

  const closeDialog = () => {
    if (form.videoPath && form.videoPath !== dialog?.item?.videoPath) api.deleteUnreferencedCognitionVideo(form.videoPath).catch(() => {})
    setDialog(null); setForm(emptyForm())
  }

  const openCreate = () => { setForm(emptyForm()); setDialog({ mode: 'create', item: null }) }
  const openEdit = (item) => { setForm(itemToForm(item)); setDialog({ mode: 'edit', item }) }

  const updateChannel = (outputCode, patch) => setForm((current) => ({
    ...current,
    channels: current.channels.map((channel) => channel.outputCode === outputCode ? { ...channel, ...patch } : channel),
  }))

  const validate = () => {
    if (form.mediaType !== 'SAMPLING_CONFIGURATION') return true
    if (form.channels.some((channel) => !channel.terminalId)) { setError('Ua、Ub、Uc、Un、Ia、Ib、Ic、In 必须全部关联端子'); return false }
    if (new Set(form.channels.map((channel) => String(channel.terminalId))).size !== CHANNEL_CODES.length) { setError('八个采样通道必须关联不同端子'); return false }
    const missingValue = form.channels.find((channel) => !WIRING_ONLY_CODES.has(channel.outputCode) && (channel.baselineMagnitude === '' || channel.baselineAngle === ''))
    if (missingValue) { setError(`${missingValue.outputCode} 必须填写基准幅值和角度`); return false }
    return true
  }

  const payload = () => ({
    title: form.title,
    mediaType: form.mediaType,
    imageId: form.imageId,
    imageUrl: form.imageUrl,
    videoPath: form.videoPath,
    content: form.content,
    sortOrder: Number(form.sortOrder) || 0,
    enabled: form.enabled,
    channels: form.mediaType === 'SAMPLING_CONFIGURATION' ? form.channels.map((channel) => ({
      outputCode: channel.outputCode,
      terminalId: Number(channel.terminalId),
      baselineMagnitude: WIRING_ONLY_CODES.has(channel.outputCode) ? null : Number(channel.baselineMagnitude),
      baselineAngle: WIRING_ONLY_CODES.has(channel.outputCode) ? null : Number(channel.baselineAngle),
    })) : [],
  })

  const submit = async (event) => {
    event.preventDefault(); setError(''); setMessage('')
    if (!validate()) return
    setSaving(true)
    try {
      if (dialog.mode === 'create') await api.createSamplingTestItem(cabinetId, payload())
      else await api.updateSamplingTestItem(dialog.item.id, payload())
      setMessage(dialog.mode === 'create' ? '采样测试条目创建成功' : '采样测试条目已更新')
      setDialog(null); setForm(emptyForm()); await load()
    } catch (err) { setError(err.message || '保存失败') } finally { setSaving(false) }
  }

  const remove = async (item) => {
    if (!window.confirm(`确定删除采样测试条目「${item.title}」？`)) return
    try { await api.deleteSamplingTestItem(item.id); setMessage('采样测试条目已删除'); await load() } catch (err) { setError(err.message || '删除失败') }
  }

  return <div className="users-page">
    <div className="users-page__header"><div><p className="users-page__breadcrumb"><Link to="/admin/sampling-tests">采样测试</Link><span> / </span><span>{cabinet?.name || '屏柜配置'}</span></p><h2 className="users-page__title">{cabinet ? `${cabinet.name} — 采样测试` : '采样测试配置'}</h2><p className="users-page__desc">普通条目展示图片或视频；采样配置条目维护八路接线及目标量值。</p></div><button type="button" className="users-page__btn users-page__btn--primary" onClick={openCreate}>新增条目</button></div>
    {message && <div className="users-page__message">{message}</div>}{error && <div className="users-page__error">{error}</div>}
    {loading ? <p className="users-page__loading">加载中…</p> : <div className="users-page__table-wrap"><table className="users-page__table"><thead><tr><th>类型</th><th>名称</th><th>说明摘要</th><th>排序</th><th>状态</th><th>操作</th></tr></thead><tbody>
      {items.length === 0 ? <tr><td colSpan={6} className="users-page__empty-cell">暂无采样测试条目</td></tr> : items.map((item) => <tr key={item.id}><td>{item.mediaType === 'SAMPLING_CONFIGURATION' ? <span className="sampling-items__badge">采样配置</span> : item.mediaType === 'VIDEO' ? <span className="sampling-items__badge">视频</span> : <img className="device-display-items__thumb" src={imageUrl('sampling-test', item.id)} alt={item.title} />}</td><td>{item.title}</td><td>{item.content.length > 50 ? `${item.content.slice(0, 50)}…` : item.content}</td><td>{item.sortOrder}</td><td>{item.enabled ? '启用' : '停用'}</td><td className="users-page__actions"><button type="button" className="users-page__link" onClick={() => openEdit(item)}>编辑</button><button type="button" className="users-page__link users-page__link--danger" onClick={() => remove(item)}>删除</button></td></tr>)}
    </tbody></table></div>}

    {dialog && <div className="users-page__overlay"><form className="users-page__dialog sampling-items__dialog" onSubmit={submit}><h3>{dialog.mode === 'create' ? '新增采样测试条目' : '编辑采样测试条目'}</h3>
      <label>条目名称<input required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></label>
      <label>条目类型<select value={form.mediaType} onChange={(e) => {
        const mediaType = e.target.value
        if (mediaType !== 'VIDEO' && form.videoPath !== dialog.item?.videoPath) api.deleteUnreferencedCognitionVideo(form.videoPath).catch(() => {})
        setForm((current) => ({ ...current, mediaType, videoPath: mediaType === 'VIDEO' ? current.videoPath : '' }))
      }}><option value="IMAGE">图片</option><option value="VIDEO">视频</option><option value="SAMPLING_CONFIGURATION">采样配置</option></select></label>
      {form.mediaType === 'IMAGE' ? <CabinetImageUploadField imageUrl={form.imageUrl} previewUrl={dialog.item?.mediaType === 'IMAGE' ? imageUrl('sampling-test', dialog.item.id) : ''} uploadImage={api.uploadDeviceDisplayImage} disabled={saving} onChange={(url, result) => setForm((current) => ({ ...current, imageUrl: url, imageId: result?.imageId ?? null }))} /> : form.mediaType === 'VIDEO' ? <CognitionVideoUploadField value={form.videoPath} previewUrl={dialog.item?.mediaType === 'VIDEO' ? videoUrl('sampling-test', dialog.item.id) : ''} disabled={saving} onChange={(videoPath) => setForm((current) => ({ ...current, videoPath }))} /> : <table className="sampling-items__channel-table"><thead><tr><th>通道</th><th>端子排</th><th>端子</th><th>基准幅值</th><th>基准角度（°）</th></tr></thead><tbody>
        {form.channels.map((channel) => {
          const eligible = terminals.filter((terminal) => Number(terminal.terminalStripId) === Number(channel.terminalStripId) && (WIRING_ONLY_CODES.has(channel.outputCode) ? terminal.signalType === 'END' : (terminal.signalType === 'ANALOG' && terminal.iedSignalRef)))
          return <tr key={channel.outputCode}><td className="sampling-items__channel-code">{channel.outputCode}</td><td><select required value={channel.terminalStripId} onChange={(e) => updateChannel(channel.outputCode, { terminalStripId: e.target.value, terminalId: '' })}><option value="">请选择</option>{strips.map((strip) => <option key={strip.id} value={strip.id}>{strip.name}（{strip.labelPrefix}）</option>)}</select></td><td><select required value={channel.terminalId} onChange={(e) => updateChannel(channel.outputCode, { terminalId: e.target.value })}><option value="">请选择</option>{eligible.map((terminal) => <option key={terminal.id} value={terminal.id}>{terminal.terminalLabel}{terminal.description ? ` — ${terminal.description}` : ''}</option>)}</select></td>{WIRING_ONLY_CODES.has(channel.outputCode) ? <td colSpan={2}>公共端，仅校验接线</td> : <><td><input type="number" min="0" step="any" required value={channel.baselineMagnitude} onChange={(e) => updateChannel(channel.outputCode, { baselineMagnitude: e.target.value })} /></td><td><input type="number" step="any" required value={channel.baselineAngle} onChange={(e) => updateChannel(channel.outputCode, { baselineAngle: e.target.value })} /></td></>}</tr>
        })}
      </tbody></table>}
      <label>说明文字<textarea rows={6} required value={form.content} onChange={(e) => setForm({ ...form, content: e.target.value })} /></label>
      <label>排序<input type="number" value={form.sortOrder} onChange={(e) => setForm({ ...form, sortOrder: e.target.value })} /></label>
      <label className="users-page__checkbox"><input type="checkbox" checked={form.enabled} onChange={(e) => setForm({ ...form, enabled: e.target.checked })} />启用</label>
      {form.mediaType === 'SAMPLING_CONFIGURATION' && <p>已选端子：{form.channels.filter((channel) => channel.terminalId).map((channel) => `${channel.outputCode}=${terminalById.get(Number(channel.terminalId))?.terminalLabel || channel.terminalId}`).join('，') || '无'}</p>}
      <div className="users-page__dialog-actions"><button type="button" className="users-page__btn" onClick={closeDialog}>取消</button><button type="submit" className="users-page__btn users-page__btn--primary" disabled={saving}>{saving ? '保存中…' : '保存'}</button></div>
    </form></div>}
  </div>
}
