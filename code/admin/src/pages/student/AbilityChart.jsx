const LABELS = ['逻辑分析', '接线调试', '图纸识读', '故障排查', '规范操作', '综合应用']
const VALUES = [0.82, 0.65, 0.74, 0.58, 0.71, 0.68]

function polarPoint(cx, cy, radius, index, total) {
  const angle = (Math.PI * 2 * index) / total - Math.PI / 2
  return {
    x: cx + radius * Math.cos(angle),
    y: cy + radius * Math.sin(angle),
  }
}

export default function AbilityChart() {
  const cx = 120
  const cy = 120
  const maxR = 88
  const levels = [0.25, 0.5, 0.75, 1]

  const valuePoints = VALUES.map((v, i) =>
    polarPoint(cx, cy, maxR * v, i, LABELS.length),
  )
  const polygon = valuePoints.map((p) => `${p.x},${p.y}`).join(' ')

  return (
    <div className="ability-chart">
      <h3 className="ability-chart__title">能力图</h3>
      <svg viewBox="0 0 240 240" className="ability-chart__svg" aria-label="学员能力雷达图">
        {levels.map((level) => {
          const points = LABELS.map((_, i) => polarPoint(cx, cy, maxR * level, i, LABELS.length))
          const d = points.map((p) => `${p.x},${p.y}`).join(' ')
          return (
            <polygon
              key={level}
              points={d}
              fill="none"
              stroke="rgba(142, 197, 248, 0.18)"
              strokeWidth="1"
            />
          )
        })}

        {LABELS.map((label, i) => {
          const outer = polarPoint(cx, cy, maxR, i, LABELS.length)
          const labelPos = polarPoint(cx, cy, maxR + 18, i, LABELS.length)
          return (
            <g key={label}>
              <line
                x1={cx}
                y1={cy}
                x2={outer.x}
                y2={outer.y}
                stroke="rgba(142, 197, 248, 0.12)"
                strokeWidth="1"
              />
              <text
                x={labelPos.x}
                y={labelPos.y}
                textAnchor="middle"
                dominantBaseline="middle"
                className="ability-chart__label"
              >
                {label}
              </text>
            </g>
          )
        })}

        <polygon
          points={polygon}
          fill="rgba(74, 158, 255, 0.35)"
          stroke="#5eb3ff"
          strokeWidth="2"
        />
        {valuePoints.map((p, i) => (
          <circle key={i} cx={p.x} cy={p.y} r="3.5" fill="#7ec8ff" />
        ))}
      </svg>
    </div>
  )
}
