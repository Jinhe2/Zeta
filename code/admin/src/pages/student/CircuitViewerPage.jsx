import { useNavigate, useParams } from 'react-router-dom'
import './TabletShell.css'
import './CircuitViewerPage.css'

// 回路数据配置（与 CircuitLearningPage 保持一致）
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

export default function CircuitViewerPage() {
  const navigate = useNavigate()
  const { category, name } = useParams()

  // 查找对应的回路文件
  const findCircuit = () => {
    for (const cat of CIRCUIT_DATA) {
      if (cat.category === category) {
        const circuit = cat.circuits.find((c) => c.name === name)
        if (circuit) {
          return circuit
        }
      }
    }
    return null
  }

  const circuit = findCircuit()

  if (!circuit) {
    return (
      <div className="tablet-shell">
        <header className="tablet-shell__header">
          <div className="tablet-shell__header-left">
            <button
              type="button"
              className="tablet-shell__back"
              onClick={() => navigate('/student/modes/coach/circuit')}
            >
              ← 返回上级
            </button>
            <button type="button" className="tablet-shell__home" onClick={() => navigate('/student')}>
              返回首页
            </button>
          </div>
          <h1>回路不存在</h1>
          <div className="tablet-shell__header-actions" />
        </header>
        <main className="tablet-shell__main circuit-viewer">
          <div className="circuit-viewer__error">
            <p>未找到对应的回路文件</p>
          </div>
        </main>
      </div>
    )
  }

  return (
    <div className="tablet-shell">
      <header className="tablet-shell__header">
        <div className="tablet-shell__header-left">
          <button
            type="button"
            className="tablet-shell__back"
            onClick={() => navigate('/student/modes/coach/circuit')}
          >
            ← 返回上级
          </button>
          <button type="button" className="tablet-shell__home" onClick={() => navigate('/student')}>
            返回首页
          </button>
        </div>
        <h1>{circuit.name}</h1>
        <div className="tablet-shell__header-actions" />
      </header>

      <main className="tablet-shell__main circuit-viewer">
        <iframe
          src={circuit.file}
          className="circuit-viewer__iframe"
          title={circuit.name}
          frameBorder="0"
        />
      </main>
    </div>
  )
}
