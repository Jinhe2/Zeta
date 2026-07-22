import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import StudentSubpageBar from './StudentSubpageBar'
import './StudentSubpageLayout.css'
import './MistakesPage.css'

const LOGIC_LIST_STATE = { from: 'coach', section: 'logic' }

const MOCK_MISTAKES = [
  {
    id: 1,
    topic: '过流保护逻辑',
    question: '过流保护动作后，重合闸未闭锁的原因是什么？',
    wrongAnswer: '仅检查了保护定值，未核对重合闸闭锁回路',
    date: '2026-05-28',
    count: 2,
    related: '过流保护逻辑',
    logicKeyword: '过流',
  },
  {
    id: 2,
    topic: '线路保护基础',
    question: '距离保护Ⅰ段与Ⅱ段时间配合原则是什么？',
    wrongAnswer: '误认为两段可共用同一时间定值',
    date: '2026-05-26',
    count: 1,
    related: '距离保护逻辑',
    logicKeyword: '距离',
  },
  {
    id: 3,
    topic: '故障排查',
    question: '保护装置告警「CT断线」时应优先检查什么？',
    wrongAnswer: '直接更换保护插件，未检查CT回路',
    date: '2026-05-24',
    count: 3,
    related: '保护逻辑清单',
    logicKeyword: '',
  },
  {
    id: 4,
    topic: '图纸识读',
    question: '屏柜端子图中跳闸回路的接点类型如何识别？',
    wrongAnswer: '混淆常开与常闭接点在逻辑中的位置',
    date: '2026-05-22',
    count: 1,
    related: '跳闸回路逻辑',
    logicKeyword: '跳',
  },
]

const MOCK_SUGGESTIONS = [
  {
    id: 1,
    title: '强化逻辑回路分析',
    desc: '错题集中在保护动作与闭锁逻辑，建议进入逻辑原理查看相关保护逻辑。',
    action: '查看逻辑原理',
    logicKeyword: '过流',
  },
  {
    id: 2,
    title: '巩固图纸识读能力',
    desc: '端子图与回路图识读有误，建议先从保护逻辑清单选择相关逻辑框图学习。',
    action: '打开逻辑清单',
    logicKeyword: '',
  },
  {
    id: 3,
    title: '针对性测评巩固',
    desc: '「故障排查」类题目错误次数较多，先跳转到逻辑原理串联一次模拟业务流。',
    action: '开始复习',
    logicKeyword: 'CT',
  },
]

export default function MistakesPage() {
  const navigate = useNavigate()
  const [navigatingId, setNavigatingId] = useState(null)

  const openLogicPrinciple = async (logicKeyword, sourceId) => {
    const keyword = logicKeyword?.trim()
    if (!keyword) {
      navigate('/student/modes/panorama', { state: LOGIC_LIST_STATE })
      return
    }

    setNavigatingId(sourceId)
    try {
      const list = await api.listProtectionLogics()
      const matched = list.find((item) => {
        const text = `${item.title ?? ''} ${item.code ?? ''} ${item.category ?? ''} ${item.description ?? ''}`
        return text.includes(keyword)
      })
      if (matched?.id) {
        navigate(`/student/modes/panorama/${matched.id}`, { state: LOGIC_LIST_STATE })
      } else {
        navigate('/student/modes/panorama', { state: LOGIC_LIST_STATE })
      }
    } catch {
      navigate('/student/modes/panorama', { state: LOGIC_LIST_STATE })
    } finally {
      setNavigatingId(null)
    }
  }

  const handleItemKeyDown = (event, item) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      openLogicPrinciple(item.logicKeyword, `mistake-${item.id}`)
    }
  }

  return (
    <div className="student-subpage mistakes-page">
      <StudentSubpageBar title="错题本" />

      <div className="mistakes-page__body">
        <section className="mistakes-page__mistakes" aria-label="我的错题">
          <h2 className="mistakes-page__section-title">我的错题</h2>
          <ul className="mistakes-list">
            {MOCK_MISTAKES.map((item) => (
              <li
                key={item.id}
                className="mistakes-item"
                role="button"
                tabIndex={0}
                onClick={() => openLogicPrinciple(item.logicKeyword, `mistake-${item.id}`)}
                onKeyDown={(event) => handleItemKeyDown(event, item)}
                aria-label={`查看错题关联逻辑原理：${item.question}`}
              >
                <div className="mistakes-item__head">
                  <span className="mistakes-item__topic">{item.topic}</span>
                  <span className="mistakes-item__meta">
                    {item.date} · 错 {item.count} 次
                  </span>
                </div>
                <p className="mistakes-item__question">{item.question}</p>
                <p className="mistakes-item__wrong">
                  <span>错误原因：</span>
                  {item.wrongAnswer}
                </p>
                <button
                  type="button"
                  className="mistakes-item__link"
                  disabled={navigatingId === `mistake-${item.id}`}
                  onClick={(event) => {
                    event.stopPropagation()
                    openLogicPrinciple(item.logicKeyword, `mistake-${item.id}`)
                  }}
                >
                  {navigatingId === `mistake-${item.id}` ? '匹配逻辑中…' : `关联逻辑：${item.related}`}
                </button>
              </li>
            ))}
          </ul>
        </section>

        <section className="mistakes-page__suggestions" aria-label="学习建议">
          <h2 className="mistakes-page__section-title">学习建议</h2>
          <ul className="suggestions-list">
            {MOCK_SUGGESTIONS.map((item) => (
              <li key={item.id} className="suggestions-item">
                <div className="suggestions-item__main">
                  <h3 className="suggestions-item__title">{item.title}</h3>
                  <p className="suggestions-item__desc">{item.desc}</p>
                </div>
                <button
                  type="button"
                  className="suggestions-item__action"
                  disabled={navigatingId === `suggestion-${item.id}`}
                  onClick={() => openLogicPrinciple(item.logicKeyword, `suggestion-${item.id}`)}
                >
                  {navigatingId === `suggestion-${item.id}` ? '跳转中…' : item.action}
                </button>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </div>
  )
}
