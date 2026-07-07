import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import './CoachModePage.css'

/* ── 各模块 SVG 图标 ── */

function IconCabinet() {
  return (
    <svg viewBox="0 0 48 48" width="40" height="40" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="8" y="4" width="32" height="40" rx="3" />
      <line x1="8" y1="18" x2="40" y2="18" />
      <line x1="8" y1="30" x2="40" y2="30" />
      <circle cx="24" cy="11" r="2" fill="currentColor" />
      <circle cx="24" cy="24" r="2" fill="currentColor" />
      <circle cx="24" cy="37" r="2" fill="currentColor" />
    </svg>
  )
}

function IconLogic() {
  return (
    <svg viewBox="0 0 48 48" width="40" height="40" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="16" y="14" width="16" height="20" rx="2" />
      <line x1="4" y1="20" x2="16" y2="20" />
      <line x1="4" y1="28" x2="16" y2="28" />
      <line x1="32" y1="24" x2="44" y2="24" />
      <text x="24" y="28" textAnchor="middle" fill="currentColor" stroke="none" fontSize="14" fontWeight="bold">&amp;</text>
    </svg>
  )
}

function IconOperation() {
  return (
    <svg viewBox="0 0 48 48" width="40" height="40" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="24" cy="28" r="14" />
      <line x1="24" y1="28" x2="24" y2="18" />
      <line x1="24" y1="28" x2="32" y2="24" />
      <circle cx="24" cy="28" r="2" fill="currentColor" />
      <line x1="24" y1="4" x2="24" y2="10" />
      <line x1="16" y1="6" x2="20" y2="11" />
      <line x1="32" y1="6" x2="28" y2="11" />
    </svg>
  )
}

function IconDrawing() {
  return (
    <svg viewBox="0 0 48 48" width="40" height="40" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="6" y="6" width="36" height="36" rx="2" />
      <line x1="6" y1="16" x2="42" y2="16" />
      <line x1="16" y1="6" x2="16" y2="42" />
      <polyline points="22,26 28,22 34,30" />
      <line x1="22" y1="34" x2="36" y2="34" />
    </svg>
  )
}

function IconCircuit() {
  return (
    <svg viewBox="0 0 48 48" width="40" height="40" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <line x1="4" y1="24" x2="14" y2="24" />
      <rect x="14" y="18" width="8" height="12" rx="1" />
      <line x1="22" y1="24" x2="26" y2="24" />
      <circle cx="30" cy="24" r="4" />
      <line x1="34" y1="24" x2="44" y2="24" />
      <line x1="24" y1="8" x2="24" y2="18" />
      <line x1="24" y1="30" x2="24" y2="40" />
      <circle cx="24" cy="8" r="2" fill="currentColor" />
      <circle cx="24" cy="40" r="2" fill="currentColor" />
    </svg>
  )
}

const COACH_ENTRIES = [
  {
    id: 'cabinet',
    label: '屏柜学习',
    Icon: IconCabinet,
    desc: '认识屏柜结构、保护装置、压板与端子排',
    route: '/student/modes/coach/cabinet',
  },
  {
    id: 'logic',
    label: '逻辑原理',
    Icon: IconLogic,
    desc: '了解继电保护逻辑框图与动作原理，进行采样值测试与信号校验',
    route: '/student/modes/panorama',
    navigateState: { from: 'coach' },
  },
  {
    id: 'operation',
    label: '采样测试',
    Icon: IconOperation,
    desc: '对保护装置进行采样值测试与信号校验',
    route: null,
  },
  {
    id: 'drawing',
    label: '图纸学习',
    Icon: IconDrawing,
    desc: '识读二次回路图纸与接线图',
    route: null,
  },
  {
    id: 'circuit',
    label: '回路学习',
    Icon: IconCircuit,
    desc: '掌握保护回路、控制回路与信号回路',
    route: null,
  },
]

export default function CoachModePage() {
  const navigate = useNavigate()
  const { session } = useAuth()

  const displayName = session?.displayName || '学员'
  const levelLabel = '初级学员 Lv.3'
  const progress = 62

  return (
    <div className="coach-mode">
      <div className="coach-mode__bg-decor" aria-hidden="true">
        <div className="coach-mode__bg-circle coach-mode__bg-circle--1" />
        <div className="coach-mode__bg-circle coach-mode__bg-circle--2" />
        <div className="coach-mode__bg-circle coach-mode__bg-circle--3" />
      </div>

      <header className="coach-mode__header">
        <div className="coach-mode__user">
          <div className="coach-mode__user-meta">
            <div className="coach-mode__user-name">{displayName}</div>
            <div className="coach-mode__user-level">{levelLabel}</div>
          </div>
          <div className="coach-mode__progress-wrap">
            <div className="coach-mode__progress">
              <div className="coach-mode__progress-bar" style={{ width: `${progress}%` }} />
            </div>
            <span className="coach-mode__progress-text">{progress}%</span>
          </div>
        </div>

        <div className="coach-mode__title-badge">
          <div className="coach-mode__title-badge-inner">
            <h1 className="coach-mode__title">教练模式</h1>
          </div>
        </div>

        <div className="coach-mode__actions">
          <button
            type="button"
            className="coach-mode__close"
            aria-label="返回"
            onClick={() => navigate('/student')}
          >
            ✕
          </button>
        </div>
      </header>

      <main className="coach-mode__main">
        <div className="coach-mode__grid">
          {COACH_ENTRIES.map((entry) => {
            const { Icon } = entry
            return (
              <button
                key={entry.id}
                type="button"
                className={`coach-mode__card coach-mode__card--${entry.id}${!entry.route ? ' coach-mode__card--disabled' : ''}`}
                onClick={() => {
                  if (entry.route) navigate(entry.route, { state: entry.navigateState })
                }}
                disabled={!entry.route}
              >
                <span className="coach-mode__card-icon">
                  <Icon />
                </span>
                <span className="coach-mode__card-label">{entry.label}</span>
                <span className="coach-mode__card-desc">{entry.desc}</span>
                {!entry.route && <span className="coach-mode__card-soon">即将开放</span>}
              </button>
            )
          })}
        </div>
      </main>
    </div>
  )
}
