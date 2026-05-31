import { DOT_R, RING_R } from './diagramConstants'

export default function DiagramWire({ wire, highlighted }) {
  const { path, srcX, srcY, tgtX, tgtY } = wire

  return (
    <g className={`wire ${highlighted ? 'highlighted' : ''}`}>
      <path
        d={path}
        fill="none"
        className="wire-path"
        strokeWidth={highlighted ? 2.5 : 2}
      />
      <circle cx={srcX} cy={srcY} r={DOT_R} className="wire-dot" />
      <circle cx={srcX} cy={srcY} r={RING_R} className="wire-ring" />
      <circle cx={tgtX} cy={tgtY} r={DOT_R} className="wire-dot" />
      <circle cx={tgtX} cy={tgtY} r={RING_R} className="wire-ring" />
    </g>
  )
}
