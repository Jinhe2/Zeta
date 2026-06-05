import StudentSubpageBar from './StudentSubpageBar'
import './StudentSubpageLayout.css'
import './MistakesPage.css'

const MOCK_MISTAKES = [
  {
    id: 1,
    topic: '过流保护逻辑',
    question: '过流保护动作后，重合闸未闭锁的原因是什么？',
    wrongAnswer: '仅检查了保护定值，未核对重合闸闭锁回路',
    date: '2026-05-28',
    count: 2,
  },
  {
    id: 2,
    topic: '线路保护基础',
    question: '距离保护Ⅰ段与Ⅱ段时间配合原则是什么？',
    wrongAnswer: '误认为两段可共用同一时间定值',
    date: '2026-05-26',
    count: 1,
  },
  {
    id: 3,
    topic: '故障排查',
    question: '保护装置告警「CT断线」时应优先检查什么？',
    wrongAnswer: '直接更换保护插件，未检查CT回路',
    date: '2026-05-24',
    count: 3,
  },
  {
    id: 4,
    topic: '图纸识读',
    question: '屏柜端子图中跳闸回路的接点类型如何识别？',
    wrongAnswer: '混淆常开与常闭接点在逻辑中的位置',
    date: '2026-05-22',
    count: 1,
  },
]

const MOCK_SUGGESTIONS = [
  {
    id: 1,
    title: '强化逻辑回路分析',
    desc: '错题集中在保护动作与闭锁逻辑，建议进入教练模式复习「过流保护逻辑」相关步骤。',
    action: '前往教练模式',
  },
  {
    id: 2,
    title: '巩固图纸识读能力',
    desc: '端子图与回路图识读有误，建议查阅资源库中的图纸与说明书对照学习。',
    action: '打开资源库',
  },
  {
    id: 3,
    title: '针对性测评巩固',
    desc: '「故障排查」类题目错误次数较多，建议完成一次专项测评检验掌握情况。',
    action: '进入测评模式',
  },
]

export default function MistakesPage() {
  return (
    <div className="student-subpage mistakes-page">
      <StudentSubpageBar title="错题本" />

      <div className="mistakes-page__body">
        <section className="mistakes-page__mistakes" aria-label="我的错题">
          <h2 className="mistakes-page__section-title">我的错题</h2>
          <ul className="mistakes-list">
            {MOCK_MISTAKES.map((item) => (
              <li key={item.id} className="mistakes-item">
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
                <span className="suggestions-item__action">{item.action}</span>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </div>
  )
}
