import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { api, imageUrl, publicUrl, videoUrl } from '../../api/client'
import { useStudentCabinetId } from './studentCabinet'
import { actualOutputs, evaluateSamplingChannel, evaluateSamplingWiring, statesByTerminalId } from '../../utils/samplingValidation'
import { DIGITAL_LINK_STYLES, digitalLinkState, evaluateDigitalSamplingChannel, indexDigitalSamplingResponse } from '../../utils/digitalSampling'
import DigitalSamplingCircuit from './DigitalSamplingCircuit'
import './TabletShell.css'
import './CabinetCognitionPage.css'
import './SamplingTestPage.css'

const CHANNEL_CODES = ['Ua', 'Ub', 'Uc', 'Un', 'Ia', 'Ib', 'Ic', 'In']
const POLL_INTERVAL = 2000

function terminalNumber(terminalLabel) {
  const matches = String(terminalLabel ?? '').match(/\d+/g)
  return matches?.[matches.length - 1] ?? String(terminalLabel ?? '')
}

function terminalLineImagePath(output) {
  const phase = String(output?.phase || output?.output_code?.slice(1) || '').toUpperCase()
  if (phase === 'A') return 'images/terminal/line_yellow.svg'
  if (phase === 'B') return 'images/terminal/line_green.svg'
  if (phase === 'C') return 'images/terminal/line_red.svg'
  return 'images/terminal/line_black.svg'
}

function groupChannels(channels, cabinetTerminals) {
  const groups = new Map()
  channels.forEach((channel, index) => {
    const key = String(channel.terminalStripId ?? 'unknown')
    if (!groups.has(key)) groups.set(key, { key, name: channel.terminalStripName || '未命名端子排', prefix: channel.terminalStripLabelPrefix || '', channels: [], order: index })
    groups.get(key).channels.push(channel)
  })
  return Array.from(groups.values()).map((group) => {
    const terminals = cabinetTerminals.filter((terminal) => String(terminal.terminalStripId) === group.key)
    return { ...group, terminals: terminals.length > 0 ? terminals : group.channels.map((channel) => ({ id: channel.terminalId, terminalLabel: channel.terminalLabel })) }
  }).sort((a, b) => {
    const aVoltage = a.channels.some((channel) => channel.outputCode.startsWith('U'))
    const bVoltage = b.channels.some((channel) => channel.outputCode.startsWith('U'))
    return aVoltage === bVoltage ? a.order - b.order : aVoltage ? -1 : 1
  })
}

function currentStatusText(channel, result, wiring, statusError) {
  if (statusError) return '状态读取失败'
  if (!wiring.passed) return wiring.wiringText
  if (channel.outputCode === 'Un' || channel.outputCode === 'In') return '接线正确'
  if (result.passed) return '接线正确，量值符合'
  return `接线正确，${result.message.replace(`${channel.outputCode}：`, '')}`
}

export default function SamplingTestPage() {
  const navigate = useNavigate()
  const { logout } = useAuth()
  const selectedCabinetId = useStudentCabinetId()
  const [cabinetId, setCabinetId] = useState(null)
  const [items, setItems] = useState([])
  const [cabinetTerminals, setCabinetTerminals] = useState([])
  const [index, setIndex] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [terminalData, setTerminalData] = useState(null)
  const [digitalData, setDigitalData] = useState(null)
  const [digitalTopology, setDigitalTopology] = useState(null)
  const [digitalTopologyError, setDigitalTopologyError] = useState('')
  const [dialog, setDialog] = useState(null)
  const [completed, setCompleted] = useState(false)
  const [acknowledgedItemId, setAcknowledgedItemId] = useState(null)
  const requestRef = useRef(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      if (selectedCabinetId === undefined) {
        setLoading(true)
        return
      }

      setLoading(true); setError('')
      try {
        const resolvedCabinetId = selectedCabinetId
        if (!resolvedCabinetId) throw new Error('未找到当前屏柜')
        const [data, terminalList] = await Promise.all([
          api.listKnowledgeSamplingTestItems(resolvedCabinetId),
          api.listTerminals(resolvedCabinetId),
        ])
        if (!cancelled) {
          requestRef.current = null
          setCabinetId(resolvedCabinetId)
          setItems(data)
          setCabinetTerminals(terminalList)
          setIndex(0)
          setTerminalData(null)
          setDigitalData(null)
          setDigitalTopology(null)
          setCompleted(false)
          setAcknowledgedItemId(null)
          setDialog(null)
        }
      } catch (err) {
        if (!cancelled) {
          setCabinetId(null)
          setItems([])
          setCabinetTerminals([])
          setTerminalData(null)
          setDigitalData(null)
          setDigitalTopology(null)
          setError(err.message || '采样测试加载失败')
        }
      } finally { if (!cancelled) setLoading(false) }
    }
    load()
    return () => { cancelled = true }
  }, [selectedCabinetId])

  const item = items[index] || null
  const configurationTypes = useMemo(() => new Set(items
    .map((entry) => entry.mediaType)
    .filter((type) => type === 'SAMPLING_CONFIGURATION' || type === 'DIGITAL_SAMPLING_CONFIGURATION')),
  [items])
  const mixedConfiguration = configurationTypes.size > 1
  const digitalMode = configurationTypes.has('DIGITAL_SAMPLING_CONFIGURATION') && !mixedConfiguration
  const configurationType = digitalMode ? 'DIGITAL_SAMPLING_CONFIGURATION' : 'SAMPLING_CONFIGURATION'
  const isConfiguration = item?.mediaType === configurationType
  const activeConfiguration = useMemo(() => {
    if (isConfiguration) return item
    for (let itemIndex = index - 1; itemIndex >= 0; itemIndex -= 1) {
      if (items[itemIndex]?.mediaType === configurationType) return items[itemIndex]
    }
    return items.slice(index + 1).find((candidate) => candidate.mediaType === configurationType) || null
  }, [configurationType, index, isConfiguration, item, items])
  const channels = useMemo(() => digitalMode
    ? [...(activeConfiguration?.digitalConfig?.channels || [])].sort((a, b) => a.sortOrder - b.sortOrder)
    : [...(activeConfiguration?.channels || [])].sort((a, b) => CHANNEL_CODES.indexOf(a.outputCode) - CHANNEL_CODES.indexOf(b.outputCode)),
  [activeConfiguration, digitalMode])
  const groups = useMemo(() => digitalMode ? [] : groupChannels(channels, cabinetTerminals), [cabinetTerminals, channels, digitalMode])
  const states = useMemo(() => statesByTerminalId(terminalData), [terminalData])
  const evaluations = useMemo(() => digitalMode ? [] : channels.map((channel) => {
    const raw = states.get(String(channel.terminalId))
    return { channel, result: evaluateSamplingChannel(channel, raw), wiring: evaluateSamplingWiring(channel, raw) }
  }), [channels, digitalMode, states])
  const digitalIndex = useMemo(() => indexDigitalSamplingResponse(digitalData), [digitalData])
  const associationStates = useMemo(() => {
    const result = new Map()
    channels.forEach((channel) => {
      const associationId = Number(channel.samplingSignalAssociationId)
      const association = digitalIndex.associations.get(associationId)
      result.set(associationId, digitalLinkState(channel.valid, association?.control_block ?? association?.controlBlock))
    })
    return result
  }, [channels, digitalIndex])
  const digitalEvaluations = useMemo(() => channels.map((channel) => {
    const state = associationStates.get(Number(channel.samplingSignalAssociationId)) || 'gray'
    const live = digitalIndex.channels.get(Number(channel.samplingSignalChannelId))
    return { channel, state, live, result: evaluateDigitalSamplingChannel(channel, live, state) }
  }), [associationStates, channels, digitalIndex.channels])

  useEffect(() => {
    let cancelled = false
    queueMicrotask(() => {
      if (cancelled) return
      setTerminalData(null); setError(''); setCompleted(false)
      setDigitalData(null)
      setAcknowledgedItemId(null)
      setDialog(isConfiguration ? 'instructions' : null)
    })
    return () => { cancelled = true }
  }, [index, isConfiguration, item?.id])

  useEffect(() => {
    if (!digitalMode || !activeConfiguration?.id) {
      queueMicrotask(() => { setDigitalTopology(null); setDigitalTopologyError('') })
      return undefined
    }
    let cancelled = false
    queueMicrotask(() => {
      if (!cancelled) { setDigitalTopology(null); setDigitalTopologyError('') }
    })
    api.getDigitalSamplingTopology(activeConfiguration.id)
      .then((data) => { if (!cancelled) setDigitalTopology(data) })
      .catch((err) => { if (!cancelled) setDigitalTopologyError(err.message || '数字化采样虚回路加载失败') })
    return () => { cancelled = true }
  }, [activeConfiguration?.id, digitalMode])

  const fetchStatus = useCallback(async () => {
    if (!cabinetId || !activeConfiguration || requestRef.current) return
    const request = digitalMode
      ? api.triggerSamplingSignalStatus(cabinetId, activeConfiguration.digitalConfig?.iedDeviceId)
      : api.triggerTerminalStatus(cabinetId, { terminalIds: channels.map((channel) => channel.terminalId) })
    requestRef.current = request
    try {
      const data = await request
      if (digitalMode) setDigitalData(data)
      else setTerminalData(data)
      setError('')
      if (isConfiguration) {
        const stateMap = statesByTerminalId(data)
        const indexed = indexDigitalSamplingResponse(data)
        const allPassed = digitalMode
          ? channels.length > 0 && activeConfiguration.digitalConfig?.valid && channels.every((channel) => {
              const association = indexed.associations.get(Number(channel.samplingSignalAssociationId))
              const link = digitalLinkState(channel.valid, association?.control_block ?? association?.controlBlock)
              return evaluateDigitalSamplingChannel(channel, indexed.channels.get(Number(channel.samplingSignalChannelId)), link).passed
            })
          : channels.length === CHANNEL_CODES.length && channels.every((channel) => evaluateSamplingChannel(channel, stateMap.get(String(channel.terminalId))).passed)
        if (allPassed) { setCompleted(true); setDialog('success') }
      }
    } catch (err) { setError(`${digitalMode ? '采样信号' : '端子'}状态读取失败：${err.message}`) } finally { if (requestRef.current === request) requestRef.current = null }
  }, [activeConfiguration, cabinetId, channels, digitalMode, isConfiguration])

  useEffect(() => {
    if (!activeConfiguration || (isConfiguration && acknowledgedItemId !== item?.id) || (isConfiguration && completed)) return undefined
    const initial = setTimeout(fetchStatus, 0)
    const timer = setInterval(fetchStatus, POLL_INTERVAL)
    return () => { clearTimeout(initial); clearInterval(timer) }
  }, [acknowledgedItemId, activeConfiguration, completed, fetchStatus, isConfiguration, item?.id])

  const requirements = digitalMode
    ? channels.map((channel) => `${channel.telemetryDescription || '未命名通道'}：幅值 ${channel.baselineMagnitude}，相角 ${channel.baselineAngle}°`)
    : channels.map((channel) => channel.outputCode === 'Un' || channel.outputCode === 'In'
      ? `${channel.outputCode}：接至 ${channel.terminalLabel}，仅校验接线`
      : `${channel.outputCode}：接至 ${channel.terminalLabel}，幅值 ${channel.baselineMagnitude}，角度 ${channel.baselineAngle}°`)

  return <div className="tablet-shell">
    <header className="tablet-shell__header"><div className="tablet-shell__header-left"><button type="button" className="tablet-shell__back" onClick={() => navigate('/student/modes/coach')}>← 返回上级</button><button type="button" className="tablet-shell__home" onClick={() => navigate('/student')}>返回首页</button></div><h1>采样测试</h1><div className="tablet-shell__header-actions"><button type="button" className="tablet-shell__logout" onClick={async () => { await logout(); navigate('/login', { replace: true }) }}>退出登录</button></div></header>
    <main className="tablet-shell__main sampling-page__main">
      {loading ? <p>加载中…</p> : mixedConfiguration ? <p className="sampling-page__fatal">采样测试同时存在普通和数字化配置，请联系管理员统一配置类型。</p> : error && !item ? <p>{error}</p> : !item ? <p>当前屏柜暂无采样测试条目。</p> : <div className={`sampling-page__layout${digitalMode ? ' sampling-page__layout--digital' : ''}`}>
        {digitalMode ? <section className="sampling-page__terminals-panel sampling-page__panel" aria-label="数字化采样虚回路状态">
          <div className="sampling-page__panel-title">数字化采样虚回路</div>
          {!activeConfiguration ? <p className="sampling-page__empty">当前测试没有数字化采样配置。</p> : digitalTopologyError ? <p className="sampling-page__empty sampling-page__fatal">{digitalTopologyError}</p> : !digitalTopology ? <p className="sampling-page__empty">正在构建采样虚回路…</p> : <DigitalSamplingCircuit topology={digitalTopology} associationStates={associationStates} />}
        </section> : <section className="sampling-page__terminals-panel sampling-page__panel" aria-label="端子连线状态">
          <div className="sampling-page__panel-title">端子连线状态</div>
          {groups.length === 0 ? <p className="sampling-page__empty">当前条目附近没有可显示的采样端子配置。</p> : <div className="sampling-page__strips">
            {groups.map((group) => {
              const channelByTerminalId = new Map(group.channels.map((channel) => [String(channel.terminalId), channel]))
              return <section key={group.key} className="sampling-page__strip terminal-wiring-status">
                <div className="terminal-wiring-status__header"><img className="terminal-wiring-status__tag" src={publicUrl('images/terminal/terminal_tag.svg')} alt={`端子排 ${group.prefix || group.name}`} /><span className="terminal-wiring-status__tag-label">{String(group.prefix || group.name).replace(/-+$/, '')}</span></div>
                <div className="terminal-wiring-status__list">{group.terminals.map((terminal) => {
                  const channel = channelByTerminalId.get(String(terminal.id))
                  const raw = channel ? states.get(String(channel.terminalId)) : null
                  const outputs = actualOutputs(raw)
                  const multiple = raw?.connection_status === 'MULTIPLE' || outputs.length > 1
                  const output = raw?.connection_status === 'CONNECTED' && outputs.length === 1 ? outputs[0] : null
                  const wiring = channel ? evaluateSamplingWiring(channel, raw) : null
                  return <div key={terminal.id} className="terminal-wiring-status__item">
                    {(output || multiple) && <img className="terminal-wiring-status__line" src={publicUrl(multiple ? 'images/terminal/line_gray.svg' : terminalLineImagePath(output))} alt={multiple ? '多路接入' : wiring?.wiringText} />}
                    {channel && <span className={`terminal-wiring-status__output${multiple || (output && !wiring?.passed) ? ' terminal-wiring-status__output--multiple' : ''}`}>{multiple ? '多路接入' : output ? `${output.output_code} · ${wiring?.wiringText}` : wiring?.wiringText}</span>}
                    <span className="terminal-wiring-status__terminal-clip"><img className="terminal-wiring-status__terminal" src={publicUrl('images/terminal/terminal_ang.svg')} alt={`端子 ${terminal.terminalLabel}`} /></span>
                    <span className="terminal-wiring-status__label">{terminalNumber(terminal.terminalLabel)}</span>
                  </div>
                })}</div>
              </section>
            })}
          </div>}
        </section>}
        <section className="sampling-page__content-panel sampling-page__panel">
          {item.mediaType === 'IMAGE' ? <img className="sampling-page__media" src={imageUrl('sampling-test', item.id)} alt={item.title} /> : item.mediaType === 'VIDEO' ? <video className="sampling-page__media" src={videoUrl('sampling-test', item.id)} controls /> : item.mediaType === 'DIGITAL_SAMPLING_CONFIGURATION' ? <div className="sampling-page__table-wrap"><table className="sampling-page__status-table"><thead><tr><th>项目</th><th>基准值</th><th>当前状态</th><th>当前值</th></tr></thead><tbody>{digitalEvaluations.map(({ channel, state, live, result }) => {
            const currentValue = !error && live?.realtime?.magnitude != null && live?.realtime?.angle != null ? `${live.realtime.magnitude} ∠ ${live.realtime.angle}°` : '—'
            const status = !channel.valid ? '配置失效' : error ? '状态读取失败' : DIGITAL_LINK_STYLES[state].label
            const passed = !error && result.passed
            return <tr key={channel.samplingSignalChannelId} title={result.message} className={live || error || !channel.valid ? (passed ? 'sampling-page__row--passed' : 'sampling-page__row--failed') : ''}><td>{channel.telemetryDescription || '未命名通道'}</td><td>{channel.baselineMagnitude} ∠ {channel.baselineAngle}°</td><td className={`sampling-page__link-state sampling-page__link-state--${channel.valid && !error ? state : 'gray'}`}>{status}</td><td>{currentValue}</td></tr>
          })}</tbody></table></div> : <div className="sampling-page__table-wrap"><table className="sampling-page__status-table"><thead><tr><th>项目</th><th>基准值</th><th>当前状态</th><th>当前值</th></tr></thead><tbody>{evaluations.map(({ channel, result, wiring }) => {
            const raw = states.get(String(channel.terminalId))
            const wiringOnly = channel.outputCode === 'Un' || channel.outputCode === 'In'
            const currentValue = !error && !wiringOnly && raw?.realtime?.type === 'ANALOG' && raw.realtime.magnitude != null && raw.realtime.angle != null
              ? `${raw.realtime.magnitude} ∠ ${raw.realtime.angle}°`
              : '—'
            const passed = !error && result.passed
            return <tr key={channel.outputCode} className={raw || error ? (passed ? 'sampling-page__row--passed' : 'sampling-page__row--failed') : ''}><td>{channel.outputCode}</td><td>{wiringOnly ? '仅接线' : `${channel.baselineMagnitude} ∠ ${channel.baselineAngle}°`}</td><td>{currentStatusText(channel, result, wiring, error)}</td><td>{currentValue}</td></tr>
          })}</tbody></table></div>}
        </section>
        <aside className="sampling-page__text-panel sampling-page__panel"><h2>{item.title}</h2><p className="sampling-page__description">{item.content}</p><div className="sampling-page__actions"><button type="button" disabled={index === 0} onClick={() => setIndex((current) => current - 1)}>上一步</button><button type="button" disabled={index >= items.length - 1} onClick={() => setIndex((current) => current + 1)}>下一步</button></div></aside>
      </div>}
    </main>
    {dialog === 'instructions' && <div className="sampling-page__overlay" role="dialog" aria-modal="true"><div className="sampling-page__dialog"><h2>采样操作提示</h2><p>{digitalMode ? '请按照以下基准值完成数字化采样加量：' : '请按照以下端子关联完成接线，并按基准值完成加量：'}</p><ul className="sampling-page__requirements">{requirements.map((requirement) => <li key={requirement}>{requirement}</li>)}</ul><button type="button" onClick={() => { setAcknowledgedItemId(item.id); setDialog(null) }}>开始检查</button></div></div>}
    {dialog === 'success' && <div className="sampling-page__overlay" role="dialog" aria-modal="true"><div className="sampling-page__dialog"><h2>加量成功</h2><p>{digitalMode ? '所选采样通道的链路及实时量值均符合要求。' : '八路端子接线及实时采样值均符合要求。'}</p><button type="button" onClick={() => setDialog(null)}>确定</button></div></div>}
  </div>
}
