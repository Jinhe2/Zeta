import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import AbilityChart from './AbilityChart'
import ExitConfirmDialog from './ExitConfirmDialog'
import './TabletHome.css'

const DIALOG_MESSAGES = {
  logout: '您确定要退出系统吗？',
  close: '您确认退出登陆并关闭系统吗？',
}

const RESOURCE_ITEMS = [
  { id: 'outline', label: '调试大纲', path: '/student/resources/outline' },
  { id: 'manual', label: '说明书', path: '/student/resources/manual' },
  { id: 'drawing', label: '图纸', path: '/student/resources/drawing' },
  { id: 'video', label: '视频微课', path: '/student/resources/video' },
]

const BOTTOM_MENUS = [
  { id: 'resources', label: '资源库', expandable: true },
  { id: 'mistakes', label: '错题本', path: '/student/mistakes' },
  { id: 'tasks', label: '任务中心', path: '/student/tasks' },
]

const MODE_ENTRIES = [
  {
    id: 'coach',
    title: '教练模式',
    desc: '分步引导，循序渐进掌握操作要点',
    path: '/student/modes/coach',
    accent: 'coach',
  },
  {
    id: 'exam',
    title: '测评模式',
    desc: '模拟考核，检验学习成果',
    path: '/student/modes/exam',
    accent: 'exam',
  },
  // 全景模式已禁用 — 功能移入教练模式「采样测试」，后续可按需重新启用
  {
    id: 'panorama',
    title: '全景模式',
    desc: '逻辑全景与保护框图综合浏览',
    path: '/student/modes/panorama',
    accent: 'panorama',
    disabled: true,
  },
]

export default function TabletHome() {
  const { session, logout } = useAuth()
  const navigate = useNavigate()
  const [resourceOpen, setResourceOpen] = useState(false)
  const [confirmType, setConfirmType] = useState(null)

  const displayName = session?.displayName || '学员'
  const levelLabel = '初级学员 Lv.3'
  const progress = 62

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  const handleConfirm = async () => {
    setConfirmType(null)
    await handleLogout()
  }

  const handleBottomMenu = (menu) => {
    if (menu.expandable) {
      setResourceOpen((v) => !v)
      return
    }
    if (menu.path) navigate(menu.path)
  }

  return (
    <div className="tablet-home">
      <header className="tablet-home__header">
        <div className="tablet-home__user">
          <div className="tablet-home__user-meta">
            <div className="tablet-home__user-name">{displayName}</div>
            <div className="tablet-home__user-level">{levelLabel}</div>
            <div className="tablet-home__progress">
              <div className="tablet-home__progress-bar" style={{ width: `${progress}%` }} />
            </div>
            <div className="tablet-home__progress-text">学习进度 {progress}%</div>
          </div>
        </div>

        <div className="tablet-home__title-badge">
          <div className="tablet-home__title-badge-inner">
            <h1 className="tablet-home__title">智慧屏柜培训系统</h1>
          </div>
        </div>

        <div className="tablet-home__actions">
          <button
            type="button"
            className="tablet-home__text-btn"
            onClick={() => navigate('/student/profile')}
          >
            个人中心
          </button>
          <button
            type="button"
            className="tablet-home__icon-btn"
            aria-label="设置"
            onClick={() => navigate('/student/settings/password')}
          >
            ⚙
          </button>
          <button type="button" className="tablet-home__text-btn" onClick={() => setConfirmType('logout')}>
            退出登录
          </button>
          <button
            type="button"
            className="tablet-home__icon-btn tablet-home__icon-btn--close"
            onClick={() => setConfirmType('close')}
          >
            ✕
          </button>
        </div>
      </header>

      <main className="tablet-home__main">
        <section className="tablet-home__chart-area">
          <AbilityChart />
        </section>

        <section className="tablet-home__modes">
          {MODE_ENTRIES.map((mode) => (
            <button
              key={mode.id}
              type="button"
              className={`mode-card mode-card--${mode.accent}${mode.disabled ? ' mode-card--disabled' : ''}`}
              onClick={() => { if (!mode.disabled) navigate(mode.path) }}
              disabled={mode.disabled}
            >
              <span className="mode-card__title">{mode.title}</span>
              <span className="mode-card__desc">{mode.desc}</span>
              <span className="mode-card__arrow">{mode.disabled ? '暂未开放' : '进入 →'}</span>
            </button>
          ))}
        </section>
      </main>

      <footer className="tablet-home__footer">
        {resourceOpen && (
          <div className="tablet-home__resource-submenu">
            {RESOURCE_ITEMS.map((item) => (
              <button
                key={item.id}
                type="button"
                className="tablet-home__resource-item"
                onClick={() => navigate(item.path)}
              >
                {item.label}
              </button>
            ))}
          </div>
        )}

        <nav className="tablet-home__bottom-nav" aria-label="底部菜单">
          {BOTTOM_MENUS.map((menu) => (
            <button
              key={menu.id}
              type="button"
              className={`tablet-home__nav-item${menu.expandable && resourceOpen ? ' tablet-home__nav-item--active' : ''}`}
              onClick={() => handleBottomMenu(menu)}
            >
              {menu.label}
              {menu.expandable && (
                <span className="tablet-home__nav-caret">{resourceOpen ? '▲' : '▼'}</span>
              )}
            </button>
          ))}
        </nav>
      </footer>

      <ExitConfirmDialog
        open={confirmType !== null}
        message={confirmType ? DIALOG_MESSAGES[confirmType] : ''}
        onConfirm={handleConfirm}
        onCancel={() => setConfirmType(null)}
      />
    </div>
  )
}
