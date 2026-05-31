function getInputThresholdLabel(node, settingsMap) {
  if (node.thresholdValue != null) return String(node.thresholdValue)
  if (node.thresholdRef && settingsMap[node.thresholdRef]) {
    return String(settingsMap[node.thresholdRef].defaultValue ?? '-')
  }
  if (node.baseValue != null) return String(node.baseValue)
  return '-'
}

function gateSymbol(type) {
  return type === 'OR' ? '≥1' : '&'
}

export default function DiagramNode({ node, settingsMap, selected, dimmed, onClick }) {
  const { id, kind, x, y, w, h } = node
  const opacity = dimmed ? 0.35 : 1

  if (kind === 'gate') {
    const inverted = node.inverted
    return (
      <g
        className={`node gate ${selected ? 'selected' : ''}`}
        transform={`translate(${x}, ${y})`}
        opacity={opacity}
        onClick={onClick}
        style={{ cursor: 'pointer' }}
      >
        <rect width={w} height={h} rx="2" className="node-bg gate-bg" />
        <text x={w / 2} y={h / 2 - 4} textAnchor="middle" className="gate-symbol">
          {gateSymbol(node.type)}
        </text>
        <text x={w / 2} y={h / 2 + 10} textAnchor="middle" className="node-id gate-id">
          {id}
        </text>
        {inverted && (
          <circle cx={w + 5} cy={h / 2} r="5" className="invert-dot" />
        )}
      </g>
    )
  }

  if (kind === 'input') {
    const threshold = getInputThresholdLabel(node, settingsMap)
    return (
      <g
        className={`node input ${selected ? 'selected' : ''}`}
        transform={`translate(${x}, ${y})`}
        opacity={opacity}
        onClick={onClick}
        style={{ cursor: 'pointer' }}
      >
        <rect width={w} height={h} rx="2" className="node-bg input-bg" />
        <text x={8} y={h / 2 + 5} className="node-text">
          <tspan className="node-threshold">{threshold}</tspan>
          <tspan dx={8}>{node.name}</tspan>
          <tspan className="node-id" dx={6}>{id}</tspan>
          <tspan dx={12} className="node-value">{node.displayValue}</tspan>
        </text>
      </g>
    )
  }

  if (kind === 'timer') {
    return (
      <g
        className={`node timer ${selected ? 'selected' : ''}`}
        transform={`translate(${x}, ${y})`}
        opacity={opacity}
        onClick={onClick}
        style={{ cursor: 'pointer' }}
      >
        <rect width={w} height={h} rx="2" className="node-bg timer-bg" />
        <text x={8} y={h / 2 + 5} className="node-text">
          {node.name}
          <tspan className="node-id" dx={6}>{id}</tspan>
          <tspan dx={12} className="node-value">{node.delayValue}s</tspan>
        </text>
      </g>
    )
  }

  if (kind === 'output') {
    return (
      <g
        className={`node output ${selected ? 'selected' : ''}`}
        transform={`translate(${x}, ${y})`}
        opacity={opacity}
        onClick={onClick}
        style={{ cursor: 'pointer' }}
      >
        <rect width={w} height={h} rx="2" className="node-bg output-bg" />
        <text x={8} y={h / 2 + 5} className="node-text">
          {node.name}
          <tspan className="node-id" dx={6}>{id}</tspan>
          <tspan dx={12} className="node-ref">{node.channelRef || '-'}</tspan>
        </text>
      </g>
    )
  }

  return null
}
