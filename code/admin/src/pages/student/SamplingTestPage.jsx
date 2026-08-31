import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { api, imageUrl, publicUrl, videoUrl } from '../../api/client'
import { useStudentCabinetId } from './studentCabinet'
import { actualOutputs, evaluateSamplingChannel, evaluateSamplingWiring, statesByTerminalId } from '../../utils/samplingValidation'
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
          setError(err.message || '采样测试加载失败')
        }
      } finally { if (!cancelled) setLoading(false) }
    }
    load()
    return () => { cancelled = true }
  }, [selectedCabinetId])

  const item = items[index] || null
  const isSampling = item?.mediaType === 'SAMPLING_CONFIGURATION'
  const activeConfiguration = useMemo(() => {
    if (isSampling) return item
    for (let itemIndex = index - 1; itemIndex >= 0; itemIndex -= 1) {
      if (items[itemIndex]?.mediaType === 'SAMPLING_CONFIGURATION') return items[itemIndex]
    }
    return items.slice(index + 1).find((candidate) => candidate.mediaType === 'SAMPLING_CONFIGURATION') || null
  }, [index, isSampling, item, items])
  const channels = useMemo(() => [...(activeConfiguration?.channels || [])].sort((a, b) => CHANNEL_CODES.indexOf(a.outputCode) - CHANNEL_CODES.indexOf(b.outputCode)), [activeConfiguration])
  const groups = useMemo(() => groupChannels(channels, cabinetTerminals), [cabinetTerminals, channels])
  const states = useMemo(() => statesByTerminalId(terminalData), [terminalData])
  const evaluations = useMemo(() => channels.map((channel) => {
    const raw = states.get(String(channel.terminalId))
    return { channel, result: evaluateSamplingChannel(channel, raw), wiring: evaluateSamplingWiring(channel, raw) }
  }), [channels, states])

  useEffect(() => {
    let cancelled = false
    queueMicrotask(() => {
      if (cancelled) return
      setTerminalData(null); setError(''); setCompleted(false)
      setAcknowledgedItemId(null)
      setDialog(isSampling ? 'instructions' : null)
    })
    return () => { cancelled = true }
  }, [index, isSampling, item?.id])

  const fetchStatus = useCallback(async () => {
    if (!cabinetId || !activeConfiguration || requestRef.current) return
    const terminalIds = channels.map((channel) => channel.terminalId)
    const request = api.triggerTerminalStatus(cabinetId, { terminalIds })
    requestRef.current = request
    try {
      const data = await request
      setTerminalData(data); setError('')
      const stateMap = statesByTerminalId(data)
      if (isSampling) {
        const allPassed = channels.length === CHANNEL_CODES.length && channels.every((channel) => evaluateSamplingChannel(channel, stateMap.get(String(channel.terminalId))).passed)
        if (allPassed) { setCompleted(true); setDialog('success') }
      }
    } catch (err) { setError(`端子状态读取失败：${err.message}`) } finally { if (requestRef.current === request) requestRef.current = null }
  }, [activeConfiguration, cabinetId, channels, isSampling])

  useEffect(() => {
    if (!activeConfiguration || (isSampling && acknowledgedItemId !== item?.id) || (isSampling && completed)) return undefined
    const initial = setTimeout(fetchStatus, 0)
    const timer = setInterval(fetchStatus, POLL_INTERVAL)
    return () => { clearTimeout(initial); clearInterval(timer) }
  }, [acknowledgedItemId, activeConfiguration, completed, fetchStatus, isSampling, item?.id])

  const requirements = channels.map((channel) => channel.outputCode === 'Un' || channel.outputCode === 'In'
    ? `${channel.outputCode}：接至 ${channel.terminalLabel}，仅校验接线`
    : `${channel.outputCode}：接至 ${channel.terminalLabel}，幅值 ${channel.baselineMagnitude}，角度 ${channel.baselineAngle}°`)

  return <div className="tablet-shell">
    <header className="tablet-shell__header"><div className="tablet-shell__header-left"><button type="button" className="tablet-shell__back" onClick={() => navigate('/student/modes/coach')}>← 返回上级</button><button type="button" className="tablet-shell__home" onClick={() => navigate('/student')}>返回首页</button></div><h1>采样测试</h1><div className="tablet-shell__header-actions"><button type="button" className="tablet-shell__logout" onClick={async () => { await logout(); navigate('/login', { replace: true }) }}>退出登录</button></div></header>
    <main className="tablet-shell__main sampling-page__main">
      {loading ? <p>加载中…</p> : error && !item ? <p>{error}</p> : !item ? <p>当前屏柜暂无采样测试条目。</p> : <div className="sampling-page__layout">
        <section className="sampling-page__terminals-panel sampling-page__panel" aria-label="端子连线状态">
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
        </section>
        <section className="sampling-page__content-panel sampling-page__panel">
          {item.mediaType === 'IMAGE' ? <img className="sampling-page__media" src={imageUrl('sampling-test', item.id)} alt={item.title} /> : item.mediaType === 'VIDEO' ? <video className="sampling-page__media" src={videoUrl('sampling-test', item.id)} controls /> : <div className="sampling-page__table-wrap"><table className="sampling-page__status-table"><thead><tr><th>项目</th><th>基准值</th><th>当前状态</th><th>当前值</th></tr></thead><tbody>{evaluations.map(({ channel, result, wiring }) => {
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
    {dialog === 'instructions' && <div className="sampling-page__overlay" role="dialog" aria-modal="true"><div className="sampling-page__dialog"><h2>采样操作提示</h2><p>请按照以下端子关联完成接线，并按基准值完成加量：</p><ul className="sampling-page__requirements">{requirements.map((requirement) => <li key={requirement}>{requirement}</li>)}</ul><button type="button" onClick={() => { setAcknowledgedItemId(item.id); setDialog(null) }}>开始检查</button></div></div>}
    {dialog === 'success' && <div className="sampling-page__overlay" role="dialog" aria-modal="true"><div className="sampling-page__dialog"><h2>加量成功</h2><p>八路端子接线及实时采样值均符合要求。</p><button type="button" onClick={() => setDialog(null)}>确定</button></div></div>}
  </div>
}
