import { useState } from 'react'
import StudentSubpageBar from './StudentSubpageBar'
import './StudentSubpageLayout.css'
import './ProfilePage.css'

const TABS = [
  { id: 'learning', label: '学习情况' },
  { id: 'history', label: '历史记录' },
  { id: 'scores', label: '学习成绩' },
  { id: 'honors', label: '荣誉成就' },
]

const MOCK_HISTORY = [
  { id: 1, title: '教练模式 · 线路保护基础', time: '2026-05-28 14:30', duration: '42 分钟' },
  { id: 2, title: '测评模式 · 继电保护综合测评', time: '2026-05-26 10:15', duration: '35 分钟' },
  { id: 3, title: '全景模式 · 过流保护逻辑', time: '2026-05-24 16:08', duration: '28 分钟' },
]

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
      <ul className="profile-history-list">
        {MOCK_HISTORY.map((item) => (
          <li key={item.id} className="profile-history-item">
            <div className="profile-history-item__main">
              <span className="profile-history-item__title">{item.title}</span>
              <span className="profile-history-item__time">{item.time}</span>
            </div>
            <span className="profile-history-item__duration">{item.duration}</span>
          </li>
        ))}
      </ul>
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
  learning: LearningTab,
  history: HistoryTab,
  scores: ScoresTab,
  honors: HonorsTab,
}

export default function ProfilePage() {
  const [activeTab, setActiveTab] = useState('learning')
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
          <ActivePanel />
        </main>
      </div>
    </div>
  )
}
