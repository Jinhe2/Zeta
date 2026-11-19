import { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import IedCommunicationStatus from '../../components/IedCommunicationStatus'
import { useCoachLearningAvailability } from './CoachLearningAvailability'
import './TabletShell.css'
import './CoachModePage.css'
import './CoachVirtualCircuit.css'

function ModuleIcon({ type }) {
  const common = { viewBox: '0 0 48 48', width: 40, height: 40, fill: 'none', stroke: 'currentColor', strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round' }
  if (type === 'CABINET') return <svg {...common}><rect x="8" y="4" width="32" height="40" rx="3" /><path d="M8 18h32M8 30h32" /><circle cx="24" cy="11" r="2" fill="currentColor" /><circle cx="24" cy="24" r="2" fill="currentColor" /><circle cx="24" cy="37" r="2" fill="currentColor" /></svg>
  if (type === 'DRAWING') return <svg {...common}><rect x="6" y="6" width="36" height="36" rx="2" /><path d="M6 16h36M16 6v36M22 26l6-4 6 8M22 34h14" /></svg>
  if (type === 'CIRCUIT') return <svg {...common}><path d="M4 24h10M22 24h4M34 24h10M24 8v10M24 30v10" /><rect x="14" y="18" width="8" height="12" rx="1" /><circle cx="30" cy="24" r="4" /><circle cx="24" cy="8" r="2" fill="currentColor" /><circle cx="24" cy="40" r="2" fill="currentColor" /></svg>
  if (type === 'SAMPLING') return <svg {...common}><circle cx="24" cy="28" r="14" /><path d="M24 28V18M24 28l8-4M24 4v6M16 6l4 5M32 6l-4 5" /><circle cx="24" cy="28" r="2" fill="currentColor" /></svg>
  return <svg {...common}><rect x="16" y="14" width="16" height="20" rx="2" /><path d="M4 20h12M4 28h12M32 24h12" /><text x="24" y="28" textAnchor="middle" fill="currentColor" stroke="none" fontSize="14" fontWeight="bold">&amp;</text></svg>
}

const PRESENTATION = {
  CABINET: { id: 'cabinet', route: '/student/modes/coach/cabinet' },
  DRAWING: { id: 'drawing', route: '/student/modes/coach/drawing' },
  CIRCUIT: { id: 'circuit', route: '/student/modes/coach/circuit' },
  SAMPLING: { id: 'sampling', route: '/student/modes/coach/sampling' },
  LOGIC: { id: 'logic', route: '/student/modes/panorama', navigateState: { from: 'coach', section: 'logic' } },
}

export default function CoachModePage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { logout } = useAuth()
  const availability = useCoachLearningAvailability()
  const [path, setPath] = useState([])
  const [loading, setLoading] = useState(true)
  const [busyCode, setBusyCode] = useState('')
  const [message, setMessage] = useState(() => location.state?.coachNotice || '')
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await api.getStudentLearningPath()
      setPath(data?.modules || [])
    } catch (err) {
      setError(err.message || '学习进度加载失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(load, 0)
    return () => window.clearTimeout(timer)
  }, [load])
  useEffect(() => {
    if (!location.state?.coachNotice) return
    navigate(location.pathname, { replace: true, state: null })
  }, [location.pathname, location.state, navigate])

  const entries = useMemo(() => path.map((module) => {
    const presentation = PRESENTATION[module.code] || { id: module.code.toLowerCase(), route: null }
    if (module.code !== 'CIRCUIT') return { ...module, ...presentation }
    if (availability.status !== 'ready') return { ...module, ...presentation, availabilityPending: true }
    return availability.hasVirtualCircuit
      ? { ...module, ...presentation, name: '虚回路学习', description: '查看装置间 GOOSE、SV 虚端子连接与实时链路状态', route: '/student/modes/coach/virtual-circuit' }
      : { ...module, ...presentation }
  }), [availability.hasVirtualCircuit, availability.status, path])

  const openModule = async (entry) => {
    if (entry.status === 'LOCKED' || !entry.route || entry.availabilityPending || busyCode) return
    setBusyCode(entry.code)
    setError('')
    try {
      const data = await api.visitStudentLearningModule(entry.code)
      setPath(data?.modules || path)
      navigate(entry.route, { state: entry.navigateState })
    } catch (err) {
      setError(err.message || '暂时无法进入该模块')
    } finally { setBusyCode('') }
  }

  const completeModule = async (entry) => {
    if (busyCode || !window.confirm(`确认已完成“${entry.name}”本阶段学习？`)) return
    setBusyCode(entry.code)
    setError('')
    try {
      const data = await api.completeStudentLearningModule(entry.code)
      setPath(data?.modules || [])
      const unlocked = (data?.modules || []).find((item) => item.status === 'AVAILABLE' && item.code !== entry.code)
      setMessage(unlocked ? `已完成“${entry.name}”，“${unlocked.name}”已解锁。` : `已完成“${entry.name}”。`)
    } catch (err) {
      setError(err.message || '完成状态保存失败')
    } finally { setBusyCode('') }
  }

  return <div className="tablet-shell">
    <header className="tablet-shell__header">
      <div className="tablet-shell__header-left"><button type="button" className="tablet-shell__back" onClick={() => navigate('/student')}>← 返回上级</button></div>
      <h1>教练模式</h1>
      <div className="tablet-shell__header-actions"><button type="button" className="tablet-shell__logout" onClick={async () => { await logout(); navigate('/login', { replace: true }) }}>退出登录</button></div>
    </header>
    <main className="tablet-shell__main coach-mode__main"><div className="coach-mode__content">
      {message && <p className="coach-mode__notice" role="status">{message}</p>}
      {error && <p className="coach-mode__notice coach-mode__notice--error" role="alert">{error} <button type="button" onClick={load}>重试</button></p>}
      {loading ? <p className="coach-mode__loading">正在加载学习进度…</p> : <div className="coach-mode__floating">
        {entries.map((entry, index) => {
          const locked = entry.status === 'LOCKED'
          const unavailable = entry.availabilityPending || (entry.code === 'CIRCUIT' && availability.status === 'error')
          const disabled = locked || unavailable || busyCode === entry.code
          const reasons = (entry.lockedReasons || []).join('、')
          return <article key={entry.code} className={`coach-mode__card coach-mode__card--${entry.id} coach-mode__card--${entry.status.toLowerCase()}${disabled ? ' coach-mode__card--disabled' : ''}`}>
            <span className="coach-mode__step">第 {index + 1} 阶段</span>
            <button type="button" className="coach-mode__card-main" disabled={disabled} onClick={() => openModule(entry)}>
              <span className="coach-mode__card-icon"><ModuleIcon type={entry.code} /></span>
              <span className="coach-mode__card-label">{entry.name}</span>
              <span className="coach-mode__card-desc">{locked ? `完成${reasons || '前置阶段'}后解锁` : unavailable ? (availability.error || '正在确认学习方式…') : entry.description}</span>
            </button>
            <span className={`coach-mode__status coach-mode__status--${entry.status.toLowerCase()}`}>{entry.status === 'LOCKED' ? '🔒 未解锁' : entry.status === 'AVAILABLE' ? '已解锁' : entry.status === 'IN_PROGRESS' ? '学习中' : '✓ 已完成'}</span>
            {entry.status === 'IN_PROGRESS' && <button type="button" className="coach-mode__complete" disabled={busyCode === entry.code} onClick={() => completeModule(entry)}>{busyCode === entry.code ? '保存中…' : '完成本阶段学习'}</button>}
          </article>
        })}
      </div>}
    </div></main>
    <IedCommunicationStatus />
  </div>
}
