import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import StructureCognitionContent from './cabinet/StructureCognitionContent'
import DeviceCognitionContent from './cabinet/DeviceCognitionContent'
import PlateCognitionContent from './cabinet/PlateCognitionContent'
import TerminalCognitionContent from './cabinet/TerminalCognitionContent'
import './CabinetCognitionPage.css'

const SECTIONS = [
  { id: 'structure', label: '结构认知' },
  { id: 'device', label: '设备认知' },
  { id: 'plate', label: '压板认知' },
  { id: 'terminal', label: '端子排认知' },
  { id: 'apparatus', label: '装置认知' },
]

export default function CabinetCognitionPage() {
  const navigate = useNavigate()
  const { session } = useAuth()

  const displayName = session?.displayName || '学员'
  const levelLabel = '初级学员 Lv.3'
  const [activeSection, setActiveSection] = useState(SECTIONS[0].id)
  const currentSection = SECTIONS.find((s) => s.id === activeSection)

  const renderSectionContent = () => {
    if (activeSection === 'structure') {
      return <StructureCognitionContent />
    }
    if (activeSection === 'device') {
      return <DeviceCognitionContent />
    }
    if (activeSection === 'plate') {
      return <PlateCognitionContent />
    }
    if (activeSection === 'terminal') {
      return <TerminalCognitionContent />
    }
    return (
      <div className="cabinet-page__content-placeholder">
        <h2>{currentSection?.label}</h2>
        <p>内容开发中，后续逐步补充。</p>
      </div>
    )
  }

  return (
    <div className="cabinet-page">
      <header className="cabinet-page__header">
        <div className="cabinet-page__user">
          <div className="cabinet-page__user-name">{displayName}</div>
          <div className="cabinet-page__user-level">{levelLabel}</div>
        </div>

        <div className="cabinet-page__title-badge">
          <div className="cabinet-page__title-badge-inner">
            <h1 className="cabinet-page__title">屏柜学习</h1>
          </div>
        </div>

        <div className="cabinet-page__actions">
          <button
            type="button"
            className="cabinet-page__close"
            aria-label="关闭"
            onClick={() => navigate('/student/modes/coach')}
          >
            ✕
          </button>
        </div>
      </header>

      <main className="cabinet-page__main">
        <nav className="cabinet-page__nav" aria-label="屏柜学习分类">
          {SECTIONS.map((section) => (
            <button
              key={section.id}
              type="button"
              className={`cabinet-page__nav-btn${activeSection === section.id ? ' cabinet-page__nav-btn--active' : ''}`}
              onClick={() => setActiveSection(section.id)}
            >
              {section.label}
            </button>
          ))}
        </nav>

        <section className="cabinet-page__content" aria-live="polite">
          {renderSectionContent()}
        </section>
      </main>
    </div>
  )
}
