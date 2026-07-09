import { useNavigate } from 'react-router-dom'
import './TabletShell.css'
import './CircuitLearningPage.css'

// 回路数据配置（未来可从 API 获取）
const CIRCUIT_DATA = [
  {
    category: '操作回路',
    circuits: [
      {
        name: '操作回路',
        file: '/kegong/操作回路/操作回路.html',
        description: '操作回路全节点流向可视化',
      },
      {
        name: '遥控回路',
        file: '/kegong/操作回路/遥控回路.html',
        description: '遥控回路全节点流向可视化',
      },
    ],
  },
]

export default function CircuitLearningPage() {
  const navigate = useNavigate()

  const handleCircuitClick = (category, circuitName) => {
    // 导航到回路查看器页面
    navigate(`/student/modes/coach/circuit/${encodeURIComponent(category)}/${encodeURIComponent(circuitName)}`)
  }

  return (
    <div className="tablet-shell circuit-learning-shell">
      <header className="tablet-shell__header">
        <div className="tablet-shell__header-left">
          <button
            type="button"
            className="tablet-shell__back"
            onClick={() => navigate('/student/modes/coach')}
          >
            ← 返回上级
          </button>
          <button type="button" className="tablet-shell__home" onClick={() => navigate('/student')}>
            返回首页
          </button>
        </div>
        <h1>回路学习</h1>
        <div className="tablet-shell__header-actions" />
      </header>

      <main className="tablet-shell__main circuit-learning">
        {CIRCUIT_DATA.map((category, idx) => (
          <section key={idx} className="circuit-category">
            <h2 className="circuit-category__title">{category.category}</h2>
            <div className="circuit-grid">
              {category.circuits.map((circuit, cidx) => (
                <div
                  key={cidx}
                  className="circuit-card"
                  onClick={() => handleCircuitClick(category.category, circuit.name)}
                >
                  <div className="circuit-card__icon">⚡</div>
                  <h3 className="circuit-card__title">{circuit.name}</h3>
                  <p className="circuit-card__desc">{circuit.description}</p>
                  <div className="circuit-card__action">点击查看 →</div>
                </div>
              ))}
            </div>
          </section>
        ))}
      </main>
    </div>
  )
}
