import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, imageUrl, videoUrl } from '../../../../api/client'
import CabinetImageUploadField from '../../../../components/CabinetImageUploadField'
import CognitionVideoUploadField from '../../../../components/CognitionVideoUploadField'
import { normalizeSortOrder } from '../../../../utils/sortOrder'
import '../UsersPage.css'
import './SamplingItemsPage.css'

const CHANNEL_CODES = ['Ua', 'Ub', 'Uc', 'Un', 'Ia', 'Ib', 'Ic', 'In']
const WIRING_ONLY_CODES = new Set(['Un', 'In'])

function emptyChannels() {
  return CHANNEL_CODES.map((outputCode) => ({ outputCode, terminalStripId: '', terminalId: '', baselineMagnitude: '', baselineAngle: '' }))
}

function emptyForm() {
  return {
    title: '', mediaType: 'IMAGE', imageId: null, imageUrl: '', videoPath: '', content: '',
    sortOrder: '', enabled: true, channels: emptyChannels(), digitalConfig: { iedDeviceId: '', channels: [] },
  }
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
    digitalConfig: item.digitalConfig ? {
      iedDeviceId: item.digitalConfig.iedDeviceId || '',
      channels: (item.digitalConfig.channels || []).map((channel) => ({
        ...channel,
        baselineMagnitude: channel.baselineMagnitude ?? '',
        baselineAngle: channel.baselineAngle ?? '',
      })),
    } : { iedDeviceId: '', channels: [] },
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

function DigitalSamplingFields({
  devices, candidates, loading, candidatesLoaded, config, selectedIds, candidateById,
  onDeviceChange, onToggle, onUpdate,
}) {
  return <div className="sampling-items__digital">
    <label>装置选择<select required value={config.iedDeviceId} onChange={(event) => onDeviceChange(event.target.value)}>
      {devices.length === 0 && <option value="">当前屏柜暂无装置</option>}
      {devices.map((device) => <option key={device.id} value={device.id}>{device.displayName}（{device.iedName}）</option>)}
    </select></label>
    <div className="sampling-items__digital-title">通道选择</div>
    {loading ? <p>正在加载数字化采样通道…</p> : candidates.length === 0 ? <p className="sampling-items__digital-empty">当前装置没有已配置的采样通道。</p> : candidates.map((association) => <section key={association.associationId} className={`sampling-items__association${association.valid ? '' : ' sampling-items__association--invalid'}`}>
      <header><strong>{association.category === 'VOLTAGE' ? '电压' : '电流'} · {association.controlName || association.controlReference || `组合 ${association.associationId}`}</strong><span>{association.sourceIedName || '未知发送装置'}</span>{!association.valid && <em>{association.invalidReason}</em>}</header>
      {association.channels?.map((channel) => {
        const selected = selectedIds.has(Number(channel.channelId))
        const configured = config.channels.find((entry) => Number(entry.samplingSignalChannelId) === Number(channel.channelId))
        return <div key={channel.channelId} className="sampling-items__digital-channel">
          <label className="users-page__checkbox"><input type="checkbox" checked={selected} disabled={!channel.valid} onChange={(event) => onToggle(channel, event.target.checked)} /><span><b>{channel.description || '未命名通道'}</b></span></label>
          {selected && <><label>基准幅值<input type="number" min="0" step="any" required value={configured?.baselineMagnitude ?? ''} onChange={(event) => onUpdate(channel.channelId, { baselineMagnitude: event.target.value })} /></label><label>基准相角（°）<input type="number" step="any" required value={configured?.baselineAngle ?? ''} onChange={(event) => onUpdate(channel.channelId, { baselineAngle: event.target.value })} /></label></>}
        </div>
      })}
    </section>)}
    {!loading && config.channels.filter((channel) => channel.valid === false || (candidatesLoaded && !candidateById.has(Number(channel.samplingSignalChannelId)))).map((channel) => <div key={`invalid-${channel.samplingSignalChannelId}`} className="sampling-items__invalid-channel">
      <div><strong>{channel.telemetryDescription || '失效通道'}</strong><em>{channel.invalidReason || '配置失效，请删除后重新选择'}</em></div>
      <button type="button" className="users-page__link users-page__link--danger" onClick={() => onToggle({ channelId: channel.samplingSignalChannelId }, false)}>删除</button>
    </div>)}
  </div>
}

export default function SamplingItemsPage() {
  const { cabinetId } = useParams()
  const [cabinet, setCabinet] = useState(null)
  const [items, setItems] = useState([])
  const [strips, setStrips] = useState([])
  const [terminals, setTerminals] = useState([])
  const [digitalDevices, setDigitalDevices] = useState([])
  const [digitalCandidates, setDigitalCandidates] = useState([])
  const [digitalLoading, setDigitalLoading] = useState(false)
  const [digitalCandidatesLoaded, setDigitalCandidatesLoaded] = useState(false)
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
      const [cabinetData, itemData, stripData, terminalData, deviceData] = await Promise.all([
        api.getKnowledgeCabinet(cabinetId), api.listSamplingTestItems(cabinetId),
        api.listTerminalStrips(cabinetId), api.listTerminals(cabinetId),
        api.listDigitalSamplingDevices(cabinetId),
      ])
      setCabinet(cabinetData); setItems(itemData); setStrips(stripData); setTerminals(terminalData); setDigitalDevices(deviceData)
    } catch (err) { setError(err.message || '加载采样测试配置失败') } finally { setLoading(false) }
  }, [cabinetId])

  useEffect(() => {
    const timer = window.setTimeout(load, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  const terminalById = useMemo(() => new Map(terminals.map((terminal) => [Number(terminal.id), terminal])), [terminals])
  const candidateChannelById = useMemo(() => {
    const result = new Map()
    digitalCandidates.forEach((association) => association.channels?.forEach((channel) => {
      result.set(Number(channel.channelId), { ...channel, association })
    }))
    return result
  }, [digitalCandidates])
  const selectedDigitalIds = useMemo(() => new Set(
    form.digitalConfig.channels.map((channel) => Number(channel.samplingSignalChannelId)),
  ), [form.digitalConfig.channels])
  const otherConfigurationTypes = useMemo(() => new Set(items
    .filter((entry) => entry.id !== dialog?.item?.id)
    .map((entry) => entry.mediaType)
    .filter((type) => type === 'SAMPLING_CONFIGURATION' || type === 'DIGITAL_SAMPLING_CONFIGURATION')),
  [dialog?.item?.id, items])

  const closeDialog = () => {
    if (form.videoPath && form.videoPath !== dialog?.item?.videoPath) api.deleteUnreferencedCognitionVideo(form.videoPath).catch(() => {})
    setDialog(null); setForm(emptyForm())
  }

  const openCreate = () => {
    const next = emptyForm()
    next.digitalConfig.iedDeviceId = digitalDevices[0]?.id || ''
    setForm(next); setDialog({ mode: 'create', item: null })
  }
  const openEdit = (item) => { setForm(itemToForm(item)); setDialog({ mode: 'edit', item }) }

  useEffect(() => {
    if (!dialog || form.mediaType !== 'DIGITAL_SAMPLING_CONFIGURATION' || !form.digitalConfig.iedDeviceId) {
      queueMicrotask(() => { setDigitalCandidates([]); setDigitalCandidatesLoaded(false) })
      return undefined
    }
    let cancelled = false
    queueMicrotask(() => { if (!cancelled) { setDigitalLoading(true); setDigitalCandidatesLoaded(false) } })
    api.listDigitalSamplingChannels(cabinetId, form.digitalConfig.iedDeviceId)
      .then((data) => { if (!cancelled) { setDigitalCandidates(data.associations || []); setDigitalCandidatesLoaded(true) } })
      .catch((err) => { if (!cancelled) { setDigitalCandidates([]); setError(err.message || '加载数字化采样通道失败') } })
      .finally(() => { if (!cancelled) setDigitalLoading(false) })
    return () => { cancelled = true }
  }, [cabinetId, dialog, form.digitalConfig.iedDeviceId, form.mediaType])

  const updateChannel = (outputCode, patch) => setForm((current) => ({
    ...current,
    channels: current.channels.map((channel) => channel.outputCode === outputCode ? { ...channel, ...patch } : channel),
  }))

  const updateDigitalChannel = (channelId, patch) => setForm((current) => ({
    ...current,
    digitalConfig: {
      ...current.digitalConfig,
      channels: current.digitalConfig.channels.map((channel) => (
        Number(channel.samplingSignalChannelId) === Number(channelId) ? { ...channel, ...patch } : channel
      )),
    },
  }))

  const toggleDigitalChannel = (candidate, checked) => setForm((current) => ({
    ...current,
    digitalConfig: {
      ...current.digitalConfig,
      channels: checked
        ? [...current.digitalConfig.channels, {
            samplingSignalChannelId: candidate.channelId,
            baselineMagnitude: '', baselineAngle: '', valid: candidate.valid,
          }]
        : current.digitalConfig.channels.filter((channel) => Number(channel.samplingSignalChannelId) !== Number(candidate.channelId)),
    },
  }))

  const validate = () => {
    if (form.mediaType === 'DIGITAL_SAMPLING_CONFIGURATION') {
      if (!form.digitalConfig.iedDeviceId) { setError('请选择数字化采样装置'); return false }
      if (form.digitalConfig.channels.length === 0) { setError('请至少选择一个数字化采样通道'); return false }
      const invalid = form.digitalConfig.channels.find((channel) => channel.valid === false)
      if (invalid) { setError('存在已失效的数字化采样通道，请删除后重新选择'); return false }
      const missing = form.digitalConfig.channels.find((channel) => channel.baselineMagnitude === '' || channel.baselineAngle === '')
      if (missing) { setError('每个数字化采样通道都必须填写基准幅值和相角'); return false }
      if (form.digitalConfig.channels.some((channel) => Number(channel.baselineMagnitude) < 0)) { setError('基准幅值不能小于 0'); return false }
      return true
    }
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
    sortOrder: normalizeSortOrder(form.sortOrder),
    enabled: form.enabled,
    channels: form.mediaType === 'SAMPLING_CONFIGURATION' ? form.channels.map((channel) => ({
      outputCode: channel.outputCode,
      terminalId: Number(channel.terminalId),
      baselineMagnitude: WIRING_ONLY_CODES.has(channel.outputCode) ? null : Number(channel.baselineMagnitude),
      baselineAngle: WIRING_ONLY_CODES.has(channel.outputCode) ? null : Number(channel.baselineAngle),
    })) : [],
    digitalConfig: form.mediaType === 'DIGITAL_SAMPLING_CONFIGURATION' ? {
      iedDeviceId: Number(form.digitalConfig.iedDeviceId),
      channels: form.digitalConfig.channels.map((channel) => ({
        samplingSignalChannelId: Number(channel.samplingSignalChannelId),
        baselineMagnitude: Number(channel.baselineMagnitude),
        baselineAngle: Number(channel.baselineAngle),
      })),
    } : null,
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
    <div className="users-page__header"><div><p className="users-page__breadcrumb"><Link to="/admin/sampling-tests">采样测试</Link><span> / </span><span>{cabinet?.name || '屏柜配置'}</span></p><h2 className="users-page__title">{cabinet ? `${cabinet.name} — 采样测试` : '采样测试配置'}</h2><p className="users-page__desc">普通采样配置维护端子接线；数字化采样配置维护装置、SV 通道及目标量值。</p></div><button type="button" className="users-page__btn users-page__btn--primary" onClick={openCreate}>新增条目</button></div>
    {message && <div className="users-page__message">{message}</div>}{error && <div className="users-page__error">{error}</div>}
    {loading ? <p className="users-page__loading">加载中…</p> : <div className="users-page__table-wrap"><table className="users-page__table"><thead><tr><th>类型</th><th>名称</th><th>说明摘要</th><th>排序</th><th>状态</th><th>操作</th></tr></thead><tbody>
      {items.length === 0 ? <tr><td colSpan={6} className="users-page__empty-cell">暂无采样测试条目</td></tr> : items.map((item) => <tr key={item.id}><td>{item.mediaType === 'SAMPLING_CONFIGURATION' ? <span className="sampling-items__badge">采样配置</span> : item.mediaType === 'DIGITAL_SAMPLING_CONFIGURATION' ? <span className="sampling-items__badge sampling-items__badge--digital">数字化采样配置</span> : item.mediaType === 'VIDEO' ? <span className="sampling-items__badge">视频</span> : <img className="device-display-items__thumb" src={imageUrl('sampling-test', item.id)} alt={item.title} />}</td><td>{item.title}</td><td>{item.content.length > 50 ? `${item.content.slice(0, 50)}…` : item.content}</td><td>{item.sortOrder}</td><td>{item.enabled ? '启用' : '停用'}</td><td className="users-page__actions"><button type="button" className="users-page__link" onClick={() => openEdit(item)}>编辑</button><button type="button" className="users-page__link users-page__link--danger" onClick={() => remove(item)}>删除</button></td></tr>)}
    </tbody></table></div>}

    {dialog && <div className="users-page__overlay"><form className="users-page__dialog sampling-items__dialog" onSubmit={submit}><h3>{dialog.mode === 'create' ? '新增采样测试条目' : '编辑采样测试条目'}</h3>
      <label>条目名称<input required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></label>
      <label>条目类型<select value={form.mediaType} onChange={(e) => {
        const mediaType = e.target.value
        if (mediaType !== 'VIDEO' && form.videoPath !== dialog.item?.videoPath) api.deleteUnreferencedCognitionVideo(form.videoPath).catch(() => {})
        setForm((current) => ({
          ...current,
          mediaType,
          videoPath: mediaType === 'VIDEO' ? current.videoPath : '',
          digitalConfig: mediaType === 'DIGITAL_SAMPLING_CONFIGURATION'
            ? { iedDeviceId: current.digitalConfig.iedDeviceId || digitalDevices[0]?.id || '', channels: current.digitalConfig.channels }
            : current.digitalConfig,
        }))
      }}><option value="IMAGE">图片</option><option value="VIDEO">视频</option><option value="SAMPLING_CONFIGURATION" disabled={otherConfigurationTypes.has('DIGITAL_SAMPLING_CONFIGURATION')}>采样配置</option><option value="DIGITAL_SAMPLING_CONFIGURATION" disabled={otherConfigurationTypes.has('SAMPLING_CONFIGURATION')}>数字化采样配置</option></select></label>
      {form.mediaType === 'IMAGE' ? <CabinetImageUploadField imageUrl={form.imageUrl} previewUrl={dialog.item?.mediaType === 'IMAGE' ? imageUrl('sampling-test', dialog.item.id) : ''} uploadImage={api.uploadDeviceDisplayImage} disabled={saving} onChange={(url, result) => setForm((current) => ({ ...current, imageUrl: url, imageId: result?.imageId ?? null }))} /> : form.mediaType === 'VIDEO' ? <CognitionVideoUploadField value={form.videoPath} previewUrl={dialog.item?.mediaType === 'VIDEO' ? videoUrl('sampling-test', dialog.item.id) : ''} disabled={saving} onChange={(videoPath) => setForm((current) => ({ ...current, videoPath }))} /> : form.mediaType === 'DIGITAL_SAMPLING_CONFIGURATION' ? <DigitalSamplingFields
        devices={digitalDevices} candidates={digitalCandidates} loading={digitalLoading} candidatesLoaded={digitalCandidatesLoaded}
        config={form.digitalConfig} selectedIds={selectedDigitalIds} candidateById={candidateChannelById}
        onDeviceChange={(iedDeviceId) => {
          if (form.digitalConfig.channels.length > 0 && !window.confirm('切换装置将清空已选通道和基准值，是否继续？')) return
          setForm((current) => ({ ...current, digitalConfig: { iedDeviceId, channels: [] } }))
        }}
        onToggle={toggleDigitalChannel} onUpdate={updateDigitalChannel}
      /> : <table className="sampling-items__channel-table"><thead><tr><th>通道</th><th>端子排</th><th>端子</th><th>基准幅值</th><th>基准角度（°）</th></tr></thead><tbody>
        {form.channels.map((channel) => {
          const eligible = terminals.filter((terminal) => Number(terminal.terminalStripId) === Number(channel.terminalStripId) && (WIRING_ONLY_CODES.has(channel.outputCode) ? terminal.signalType === 'END' : (terminal.signalType === 'ANALOG' && terminal.iedSignalRef)))
          return <tr key={channel.outputCode}><td className="sampling-items__channel-code">{channel.outputCode}</td><td><select required value={channel.terminalStripId} onChange={(e) => updateChannel(channel.outputCode, { terminalStripId: e.target.value, terminalId: '' })}><option value="">请选择</option>{strips.map((strip) => <option key={strip.id} value={strip.id}>{strip.name}（{strip.labelPrefix}）</option>)}</select></td><td><select required value={channel.terminalId} onChange={(e) => updateChannel(channel.outputCode, { terminalId: e.target.value })}><option value="">请选择</option>{eligible.map((terminal) => <option key={terminal.id} value={terminal.id}>{terminal.terminalLabel}{terminal.description ? ` — ${terminal.description}` : ''}</option>)}</select></td>{WIRING_ONLY_CODES.has(channel.outputCode) ? <td colSpan={2}>公共端，仅校验接线</td> : <><td><input type="number" min="0" step="any" required value={channel.baselineMagnitude} onChange={(e) => updateChannel(channel.outputCode, { baselineMagnitude: e.target.value })} /></td><td><input type="number" step="any" required value={channel.baselineAngle} onChange={(e) => updateChannel(channel.outputCode, { baselineAngle: e.target.value })} /></td></>}</tr>
        })}
      </tbody></table>}
      <label>说明文字<textarea rows={6} required value={form.content} onChange={(e) => setForm({ ...form, content: e.target.value })} /></label>
      <label>排序<input type="number" value={form.sortOrder} placeholder="不填则自动排到末尾" onChange={(e) => setForm({ ...form, sortOrder: e.target.value })} /></label>
      <label className="users-page__checkbox"><input type="checkbox" checked={form.enabled} onChange={(e) => setForm({ ...form, enabled: e.target.checked })} />启用</label>
      {form.mediaType === 'SAMPLING_CONFIGURATION' && <p>已选端子：{form.channels.filter((channel) => channel.terminalId).map((channel) => `${channel.outputCode}=${terminalById.get(Number(channel.terminalId))?.terminalLabel || channel.terminalId}`).join('，') || '无'}</p>}
      <div className="users-page__dialog-actions"><button type="button" className="users-page__btn" onClick={closeDialog}>取消</button><button type="submit" className="users-page__btn users-page__btn--primary" disabled={saving}>{saving ? '保存中…' : '保存'}</button></div>
    </form></div>}
  </div>
}
