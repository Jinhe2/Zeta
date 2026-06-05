import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import './CoachModePage.css'

const COACH_ENTRIES = [
  { id: 'cabinet', label: '屏柜认知', offset: 0 },
  { id: 'logic', label: '逻辑原理', offset: 1 },
  { id: 'operation', label: '屏柜操作', offset: 2 },
  { id: 'drawing', label: '图纸学习', offset: 3 },
  { id: 'circuit', label: '回路学习', offset: 4 },
]

export default function CoachModePage() {
  const navigate = useNavigate()
  const { session } = useAuth()

  const displayName = session?.displayName || '学员'
  const levelLabel = '初级学员 Lv.3'
  const progress = 62

  return (
    <div className="coach-mode">
      <header className="coach-mode__header">
        <div className="coach-mode__user">
          <img
            className="coach-mode__avatar"
            src="/images/student-avatar.svg"
            alt=""
            width={52}
            height={52}
          />
          <div className="coach-mode__user-meta">
            <div className="coach-mode__user-name">{displayName}</div>
            <div className="coach-mode__user-level">{levelLabel}</div>
            <div className="coach-mode__progress">
              <div className="coach-mode__progress-bar" style={{ width: `${progress}%` }} />
            </div>
            <div className="coach-mode__progress-text">学习进度 {progress}%</div>
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
            aria-label="关闭"
            onClick={() => navigate('/student')}
          >
            ✕
          </button>
        </div>
      </header>

      <main className="coach-mode__main">
        <div className="coach-mode__entries">
          {COACH_ENTRIES.map((entry) => (
            <button
              key={entry.id}
              type="button"
              className={`coach-mode__circle-btn coach-mode__circle-btn--${entry.id} coach-mode__circle-btn--offset-${entry.offset}`}
              onClick={() => {
                if (entry.id === 'cabinet') navigate('/student/modes/coach/cabinet')
              }}
            >
              {entry.label}
            </button>
          ))}
        </div>
      </main>
    </div>
  )
}
