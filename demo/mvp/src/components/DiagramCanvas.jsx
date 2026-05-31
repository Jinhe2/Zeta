import { useMemo, useState, useCallback, useEffect } from 'react'
import DiagramNode from './DiagramNode'
import DiagramWire from './DiagramWire'
import { BRIDGE_R } from './diagramConstants'
import './LogicDiagram.css'

function prepareDiagram(config, buildDiagram, engineId) {
  if (!config) return { kind: 'empty' }
  try {
    const diagram = buildDiagram(config)
    const nodes = diagram.nodes && typeof diagram.nodes === 'object' ? diagram.nodes : {}
    const wires = Array.isArray(diagram.wires) ? diagram.wires : []
    const crossings = Array.isArray(diagram.crossings) ? diagram.crossings : []
    const bounds = diagram.bounds || { width: 800, height: 600 }
    const nodeList = Object.values(nodes)
    const firstNode = nodeList[0]

    return {
      kind: 'ready',
      nodes,
      wires,
      crossings,
      bounds: {
        width: Number.isFinite(bounds.width) ? bounds.width : 800,
        height: Number.isFinite(bounds.height) ? bounds.height : 600,
      },
      settings: diagram.config?.settings || [],
      meta: diagram.meta || null,
      stats: {
        engineId,
        resolvedEngine: diagram.meta?.engine ?? engineId,
        nodeCount: nodeList.length,
        wireCount: wires.length,
        crossingCount: crossings.length,
        layoutFingerprint: firstNode
          ? `${Math.round(firstNode.x)}-${Math.round(firstNode.y)}-${Math.round(bounds.width)}x${Math.round(bounds.height)}`
          : 'empty',
      },
    }
  } catch (err) {
    console.error(`[${engineId}]`, err)
    return { kind: 'error', message: err.message }
  }
}

export default function DiagramCanvas({
  config,
  buildDiagram,
  engineId,
  engineLabel,
  onNodeSelect,
}) {
  const [selectedId, setSelectedId] = useState(null)

  useEffect(() => {
    setSelectedId(null)
  }, [config, engineId, buildDiagram])

  const renderData = useMemo(
    () => prepareDiagram(config, buildDiagram, engineId),
    [config, buildDiagram, engineId],
  )

  const handleNodeClick = useCallback(
    (id) => {
      setSelectedId((prev) => (prev === id ? null : id))
      onNodeSelect?.(id)
    },
    [onNodeSelect],
  )

  if (renderData.kind === 'empty') {
    return <div className="diagram-empty">请选择或上传 JSON 配置文件</div>
  }
  if (renderData.kind === 'error') {
    return (
      <div className="diagram-error">
        [{engineLabel}] 渲染失败：{renderData.message}
      </div>
    )
  }

  const { nodes, wires, crossings, bounds, settings, stats } = renderData
  const nodeList = Object.values(nodes)
  const relatedIds = new Set()

  if (selectedId) {
    relatedIds.add(selectedId)
    for (const w of wires) {
      if (w.from === selectedId) relatedIds.add(w.to)
      if (w.to === selectedId) relatedIds.add(w.from)
    }
  }

  const settingsMap = Object.fromEntries(settings.map((s) => [s.id, s]))
  const normalWires = wires.filter(
    (w) => !selectedId || (w.from !== selectedId && w.to !== selectedId),
  )
  const highlightWires = wires.filter(
    (w) => selectedId && (w.from === selectedId || w.to === selectedId),
  )

  return (
    <div className={`diagram-viewport diagram-viewport--${engineId}`}>
      <div className="diagram-overlay-badge" aria-hidden>
        {engineLabel}
      </div>

      <div className="diagram-stats-bar">
        <span>引擎 {stats.resolvedEngine}</span>
        <span>节点 {stats.nodeCount}</span>
        <span>连线 {stats.wireCount}</span>
        <span>交叉 {stats.crossingCount}</span>
        <span>布局 {stats.layoutFingerprint}</span>
      </div>

      <svg
        className="diagram-canvas"
        width={bounds.width}
        height={bounds.height}
        viewBox={`0 0 ${bounds.width} ${bounds.height}`}
      >
        <text
          x={bounds.width - 16}
          y={bounds.height - 16}
          textAnchor="end"
          className="diagram-watermark"
        >
          {engineLabel} ENGINE
        </text>

        {normalWires.map((wire, idx) => (
          <DiagramWire key={`w-${wire.from}-${wire.to}-${idx}`} wire={wire} highlighted={false} />
        ))}

        {crossings.map((c, idx) => (
          <path
            key={`cross-${idx}`}
            d={`M ${c.x - BRIDGE_R} ${c.y} a ${BRIDGE_R} ${BRIDGE_R} 0 0 1 ${BRIDGE_R * 2} 0`}
            fill="none"
            stroke="var(--wire)"
            strokeWidth="2"
          />
        ))}

        {nodeList.map((node) => (
          <DiagramNode
            key={node.id}
            node={node}
            settingsMap={settingsMap}
            selected={node.id === selectedId}
            dimmed={selectedId != null && !relatedIds.has(node.id)}
            onClick={() => handleNodeClick(node.id)}
          />
        ))}

        {highlightWires.map((wire, idx) => (
          <DiagramWire key={`hw-${wire.from}-${wire.to}-${idx}`} wire={wire} highlighted />
        ))}
      </svg>
    </div>
  )
}
