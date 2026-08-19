import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import StudentSubpageBar from './StudentSubpageBar'
import './StudentSubpageLayout.css'
import './ProfilePage.css'

const TABS = [
  { id: 'account', label: '个人信息' },
]

const ROLE_LABELS = {
  student: '学员',
  teacher: '教师',
  admin: '管理员',
}

const MOCK_HISTORY = [
  { id: 1, deviceName: '线路保护装置', title: '教练模式 · 线路保护基础', time: '2026-05-28 14:30', duration: '42 分钟' },
  { id: 2, deviceName: '线路保护装置', title: '测评模式 · 继电保护综合测评', time: '2026-05-26 10:15', duration: '35 分钟' },
  { id: 3, deviceName: '母线保护装置', title: '全景模式 · 过流保护逻辑', time: '2026-05-24 16:08', duration: '28 分钟' },
]

const HISTORY_BY_DEVICE = MOCK_HISTORY.reduce((groups, item) => {
  const records = groups.get(item.deviceName) || []
  records.push(item)
  groups.set(item.deviceName, records)
  return groups
}, new Map())

const MOCK_SCORES = [
  { id: 1, name: '线路保护基础测评', score: 86, date: '2026-05-26' },
  { id: 2, name: '继电保护综合测评', score: 78, date: '2026-05-20' },
  { id: 3, name: '故障排查专项测评', score: 92, date: '2026-05-15' },
]

const MOCK_HONORS = [
  { id: 1, title: '入门学员', desc: '完成首次教练模式学习', earned: true },
  { id: 2, title: '测评达人', desc: '累计完成 5 次测评', earned: true },
  { id: 3, title: '逻辑大师', desc: '全景模式浏览全部保护逻辑', earned: false },
  { id: 4, title: '满分挑战', desc: '单次测评获得满分', earned: false },
]

function AccountTab({ session, onChangePassword }) {
  const displayName = session?.displayName || '学员'
  const username = session?.username || '未获取'
  const roleLabel = ROLE_LABELS[session?.role] || session?.role || '学员'

  const infoItems = [
    { label: '姓名', value: displayName },
    { label: '登录账号', value: username },
    { label: '用户角色', value: roleLabel },
  ]

  return (
    <div className="profile-panel">
      <section className="profile-account-card">
        <div className="profile-account-card__avatar" aria-hidden="true">
          {displayName.slice(0, 1)}
        </div>
        <div className="profile-account-card__summary">
          <span className="profile-account-card__eyebrow">当前登录学员</span>
          <h2>{displayName}</h2>
          <p>在个人中心查看账号基础信息，并维护登录密码。</p>
        </div>
        <button
          type="button"
          className="profile-account-card__action"
          onClick={onChangePassword}
        >
          修改密码
        </button>
      </section>

      <section className="profile-info-grid" aria-label="个人信息">
        {infoItems.map((item) => (
          <div key={item.label} className="profile-info-item">
            <span className="profile-info-item__label">{item.label}</span>
            <span className="profile-info-item__value">{item.value}</span>
          </div>
        ))}
      </section>
    </div>
  )
}

function LearningTab() {
  return (
    <div className="profile-panel">
      <div className="profile-stats">
        <div className="profile-stat-card">
          <span className="profile-stat-card__value">62%</span>
          <span className="profile-stat-card__label">总体学习进度</span>
        </div>
        <div className="profile-stat-card">
          <span className="profile-stat-card__value">18</span>
          <span className="profile-stat-card__label">累计学习时长(h)</span>
        </div>
        <div className="profile-stat-card">
          <span className="profile-stat-card__value">7</span>
          <span className="profile-stat-card__label">本周学习天数</span>
        </div>
        <div className="profile-stat-card">
          <span className="profile-stat-card__value">12</span>
          <span className="profile-stat-card__label">已完成任务</span>
        </div>
      </div>
      <section className="profile-section">
        <h3>近期学习动态</h3>
        <ul className="profile-timeline">
          <li>完成「过流保护逻辑」全景浏览</li>
          <li>教练模式学习进度 +8%</li>
          <li>测评「线路保护基础」得分 86 分</li>
        </ul>
      </section>
    </div>
  )
}

function HistoryTab() {
  return (
    <div className="profile-panel">
      <div className="profile-history-groups">
        {[...HISTORY_BY_DEVICE.entries()].map(([deviceName, records]) => (
          <section key={deviceName} className="profile-history-group">
            <header className="profile-history-group__header">
              <h3>{deviceName}</h3>
              <span>{records.length} 条记录</span>
            </header>
            <ul className="profile-history-list">
              {records.map((item) => (
                <li key={item.id} className="profile-history-item">
                  <div className="profile-history-item__main">
                    <span className="profile-history-item__device">学习装置：{item.deviceName}</span>
                    <span className="profile-history-item__title">{item.title}</span>
                    <span className="profile-history-item__time">{item.time}</span>
                  </div>
                  <span className="profile-history-item__duration">{item.duration}</span>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>
    </div>
  )
}

function ScoresTab() {
  return (
    <div className="profile-panel">
      <table className="profile-scores-table">
        <thead>
          <tr>
            <th>测评名称</th>
            <th>成绩</th>
            <th>日期</th>
          </tr>
        </thead>
        <tbody>
          {MOCK_SCORES.map((item) => (
            <tr key={item.id}>
              <td>{item.name}</td>
              <td>
                <span className={`profile-score profile-score--${item.score >= 90 ? 'high' : item.score >= 80 ? 'mid' : 'low'}`}>
                  {item.score}
                </span>
              </td>
              <td>{item.date}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function HonorsTab() {
  return (
    <div className="profile-panel">
      <div className="profile-honors-grid">
        {MOCK_HONORS.map((item) => (
          <div
            key={item.id}
            className={`profile-honor-card${item.earned ? '' : ' profile-honor-card--locked'}`}
          >
            <span className="profile-honor-card__icon">{item.earned ? '🏅' : '🔒'}</span>
            <span className="profile-honor-card__title">{item.title}</span>
            <span className="profile-honor-card__desc">{item.desc}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

const TAB_PANELS = {
  account: AccountTab,
  learning: LearningTab,
  history: HistoryTab,
  scores: ScoresTab,
  honors: HonorsTab,
}

export default function ProfilePage() {
  const { session } = useAuth()
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState('account')
  const ActivePanel = TAB_PANELS[activeTab]

  return (
    <div className="student-subpage profile-page">
      <StudentSubpageBar title="个人中心" />

      <div className="student-subpage__body">
        <nav className="student-subpage__tabs" aria-label="个人中心分类">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              type="button"
              className={`student-subpage__tab${activeTab === tab.id ? ' student-subpage__tab--active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </nav>

        <main className="student-subpage__content">
          <ActivePanel
            session={session}
            onChangePassword={() => navigate('/student/settings/password')}
          />
        </main>
      </div>
    </div>
  )
}
