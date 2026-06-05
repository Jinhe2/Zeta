import { useMemo, useState } from 'react'
import StudentSubpageBar from './StudentSubpageBar'
import './StudentSubpageLayout.css'
import './TasksPage.css'

const TABS = [
  { id: 'all', label: '全部' },
  { id: 'pending', label: '待完成' },
  { id: 'completed', label: '已完成' },
]

const MOCK_TASKS = [
  {
    id: 1,
    title: '完成教练模式 · 线路保护基础',
    mode: '教练模式',
    deadline: '2026-06-05',
    status: 'pending',
  },
  {
    id: 2,
    title: '过流保护逻辑全景浏览',
    mode: '全景模式',
    deadline: '2026-06-03',
    status: 'pending',
  },
  {
    id: 3,
    title: '继电保护综合测评',
    mode: '测评模式',
    deadline: '2026-05-28',
    status: 'pending',
  },
  {
    id: 4,
    title: '查阅调试大纲第一章',
    mode: '资源库',
    deadline: '2026-05-25',
    status: 'completed',
    completedAt: '2026-05-24',
  },
  {
    id: 5,
    title: '错题本复习 · 故障排查',
    mode: '错题本',
    deadline: '2026-05-22',
    status: 'completed',
    completedAt: '2026-05-21',
  },
  {
    id: 6,
    title: '教练模式 · 距离保护入门',
    mode: '教练模式',
    deadline: '2026-05-20',
    status: 'completed',
    completedAt: '2026-05-19',
  },
]

function filterTasks(tasks, tab) {
  if (tab === 'pending') return tasks.filter((t) => t.status === 'pending')
  if (tab === 'completed') return tasks.filter((t) => t.status === 'completed')
  return tasks
}

export default function TasksPage() {
  const [activeTab, setActiveTab] = useState('all')

  const tasks = useMemo(() => filterTasks(MOCK_TASKS, activeTab), [activeTab])

  return (
    <div className="student-subpage tasks-page">
      <StudentSubpageBar title="任务中心" />

      <div className="student-subpage__body">
        <nav className="student-subpage__tabs" aria-label="任务分类">
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
          {tasks.length === 0 ? (
            <p className="tasks-page__empty">暂无任务</p>
          ) : (
            <ul className="tasks-list">
              {tasks.map((task) => (
                <li
                  key={task.id}
                  className={`tasks-item tasks-item--${task.status}`}
                >
                  <div className="tasks-item__main">
                    <h3 className="tasks-item__title">{task.title}</h3>
                    <div className="tasks-item__meta">
                      <span className="tasks-item__mode">{task.mode}</span>
                      {task.status === 'pending' ? (
                        <span className="tasks-item__deadline">截止 {task.deadline}</span>
                      ) : (
                        <span className="tasks-item__done">完成于 {task.completedAt}</span>
                      )}
                    </div>
                  </div>
                  <span className={`tasks-item__status tasks-item__status--${task.status}`}>
                    {task.status === 'pending' ? '待完成' : '已完成'}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </main>
      </div>
    </div>
  )
}
