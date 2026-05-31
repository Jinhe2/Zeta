import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  ReactFlow,
  Background,
  BackgroundVariant,
  ReactFlowProvider,
  useReactFlow,
  type Node,
  type Edge,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'

import InputNode from '../nodes/InputNode'
import GateNode from '../nodes/GateNode'
import TimerNode from '../nodes/TimerNode'
import OutputNode from '../nodes/OutputNode'
import ElkEdge from './ElkEdge'
import { runElkPipeline, type GraphData } from '../graph'
import type { ReactFlowLayoutResult } from '../graph/toReactFlow'
import './GraphView.css'

const nodeTypes = {
  input: InputNode,
  gate: GateNode,
  timer: TimerNode,
  output: OutputNode,
}

const edgeTypes = {
  elk: ElkEdge,
}

export interface GraphViewProps {
  data: GraphData
}

function ZoomControls() {
  const { zoomIn, zoomOut, fitView } = useReactFlow()

  return (
    <div className="v4-zoom-controls">
      <button type="button" aria-label="放大" onClick={() => zoomIn()}>
        +
      </button>
      <button type="button" aria-label="缩小" onClick={() => zoomOut()}>
        −
      </button>
      <button
        type="button"
        aria-label="适应画布"
        onClick={() => fitView({ padding: 0.15 })}
      >
        ⊡
      </button>
    </div>
  )
}

function GraphFlow({ result }: { result: ReactFlowLayoutResult }) {
  const [selectedId, setSelectedId] = useState<string | null>(null)

  useEffect(() => {
    setSelectedId(null)
  }, [result])

  const relatedIds = useMemo(() => {
    if (!selectedId) return new Set<string>()
    const ids = new Set<string>([selectedId])
    for (const edge of result.edges) {
      if (edge.source === selectedId) ids.add(edge.target)
      if (edge.target === selectedId) ids.add(edge.source)
    }
    return ids
  }, [selectedId, result.edges])

  const displayNodes = useMemo((): Node[] => {
    return result.nodes.map((node) => {
      const isSelected = node.id === selectedId
      const isDimmed = selectedId != null && !relatedIds.has(node.id)
      const isRelated = selectedId != null && relatedIds.has(node.id) && !isSelected
      const className = [
        isSelected ? 'v4-rf-node--selected' : '',
        isRelated ? 'v4-rf-node--related' : '',
        isDimmed ? 'v4-rf-node--dimmed' : '',
      ]
        .filter(Boolean)
        .join(' ')
      return {
        ...node,
        className,
        zIndex: isSelected ? 2 : isRelated ? 1 : 0,
      }
    })
  }, [result.nodes, selectedId, relatedIds])

  const displayEdges = useMemo((): Edge[] => {
    return result.edges.map((edge) => {
      const highlighted =
        selectedId != null && (edge.source === selectedId || edge.target === selectedId)
      const dimmed =
        selectedId != null && edge.source !== selectedId && edge.target !== selectedId
      return {
        ...edge,
        data: { ...(edge.data ?? {}), v4Highlighted: highlighted, v4Dimmed: dimmed },
        zIndex: highlighted ? 10 : 0,
      }
    })
  }, [result.edges, selectedId])

  const onNodeClick = useCallback((_: React.MouseEvent, node: Node) => {
    setSelectedId((prev) => (prev === node.id ? null : node.id))
  }, [])

  const onPaneClick = useCallback(() => {
    setSelectedId(null)
  }, [])

  return (
    <>
      <div className="v4-stats-bar">
        <span className="v4-engine-tag">ELK Layout</span>
        <span>节点 {result.stats.nodeCount}</span>
        <span>连线 {result.stats.edgeCount}</span>
        <span>
          画布 {Math.round(result.stats.width)}×{Math.round(result.stats.height)}
        </span>
        <span>布局 {result.stats.layoutMs.toFixed(1)}ms</span>
        <ZoomControls />
      </div>

      <div className="v4-flow-pane">
        <ReactFlow
          nodes={displayNodes}
          edges={displayEdges}
          nodeTypes={nodeTypes}
          edgeTypes={edgeTypes}
          nodesDraggable={false}
          nodesConnectable={false}
          elementsSelectable={false}
          zoomOnDoubleClick={false}
          fitView
          fitViewOptions={{ padding: 0.15 }}
          proOptions={{ hideAttribution: true }}
          onNodeClick={onNodeClick}
          onPaneClick={onPaneClick}
        >
          <Background variant={BackgroundVariant.Dots} gap={20} size={1} color="#1a2340" />
        </ReactFlow>
      </div>
    </>
  )
}

export default function GraphView({ data }: GraphViewProps) {
  const [result, setResult] = useState<ReactFlowLayoutResult | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    runElkPipeline(data)
      .then((layout) => {
        if (!cancelled) {
          setResult(layout)
          setLoading(false)
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : String(err))
          setLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [data])

  return (
    <div className="v4-graph-view">
      {(loading || error) && (
        <div className="v4-stats-bar">
          <span className="v4-engine-tag">ELK Layout</span>
          {loading && <span>布局计算中…</span>}
          {error && <span className="v4-stats-error">错误：{error}</span>}
        </div>
      )}

      {loading && <div className="v4-loading">ELK 正在计算节点与连线位置…</div>}

      {!loading && result && (
        <ReactFlowProvider>
          <GraphFlow result={result} />
        </ReactFlowProvider>
      )}
    </div>
  )
}
