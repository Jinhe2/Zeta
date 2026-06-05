import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import './StudentSubpageLayout.css'

export default function StudentSubpageBar({ title }) {
  const navigate = useNavigate()
  const { session } = useAuth()

  const displayName = session?.displayName || '学员'
  const levelLabel = '初级学员 Lv.3'

  return (
    <header className="student-subpage__bar">
      <h1 className="student-subpage__bar-title">{title}</h1>
      <div className="student-subpage__bar-right">
        <span className="student-subpage__user-name">{displayName}</span>
        <span className="student-subpage__user-level">{levelLabel}</span>
        <button
          type="button"
          className="student-subpage__close"
          aria-label="关闭"
          onClick={() => navigate('/student')}
        >
          ✕
        </button>
      </div>
    </header>
  )
}
