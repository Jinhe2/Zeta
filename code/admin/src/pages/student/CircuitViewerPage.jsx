import { useNavigate, useParams } from 'react-router-dom'
import { CIRCUIT_DATA } from '../../config/circuitData'
import './TabletShell.css'
import './CircuitViewerPage.css'

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
