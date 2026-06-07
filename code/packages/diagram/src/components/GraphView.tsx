import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  ReactFlow,
  Background,
  BackgroundVariant,
  ReactFlowProvider,
  useReactFlow,
  type Connection,
  type Edge,
  type Node,
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
  /** 是否展示布局引擎等开发信息，学员界面应设为 false */
  showDevInfo?: boolean
  /** 断面节点状态：nodeId → 是否满足；为 null 时不展示断面状态 */
  nodeStates?: Record<string, boolean> | null
  /** 编辑模式：允许拖拽连线、点击边删除 */
  editable?: boolean
  /** 受控选中节点（编辑模式） */
  selectedNodeId?: string | null
  onNodeSelect?: (id: string | null) => void
  onConnect?: (connection: Connection) => void
  onEdgeClick?: (event: React.MouseEvent, edge: Edge) => void
  className?: string
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

function GraphFlow({
  result,
  showDevInfo,
  nodeStates,
  editable = false,
  selectedNodeId,
  onNodeSelect,
  onConnect,
  onEdgeClick,
}: {
  result: ReactFlowLayoutResult
  showDevInfo: boolean
  nodeStates?: Record<string, boolean> | null
  editable?: boolean
  selectedNodeId?: string | null
  onNodeSelect?: (id: string | null) => void
  onConnect?: (connection: Connection) => void
  onEdgeClick?: (event: React.MouseEvent, edge: Edge) => void
}) {
  const [internalSelectedId, setInternalSelectedId] = useState<string | null>(null)
  const isControlled = onNodeSelect != null
  const selectedId = isControlled ? (selectedNodeId ?? null) : internalSelectedId

  useEffect(() => {
    if (!isControlled) {
      setInternalSelectedId(null)
    }
  }, [result, isControlled])

  const setSelectedId = useCallback(
    (id: string | null) => {
      if (isControlled) {
        onNodeSelect?.(id)
      } else {
        setInternalSelectedId(id)
      }
    },
    [isControlled, onNodeSelect],
  )

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
      const sectionSatisfied =
        nodeStates != null && node.id in nodeStates ? nodeStates[node.id] : null
      const className = [
        isSelected ? 'v4-rf-node--selected' : '',
        isRelated ? 'v4-rf-node--related' : '',
        isDimmed ? 'v4-rf-node--dimmed' : '',
        sectionSatisfied === true ? 'v4-rf-node--section-ok' : '',
        sectionSatisfied === false ? 'v4-rf-node--section-fail' : '',
      ]
        .filter(Boolean)
        .join(' ')
      return {
        ...node,
        className,
        connectable: editable && node.type !== 'input',
        data: {
          ...(node.data ?? {}),
          sectionSatisfied,
        },
        zIndex: isSelected ? 2 : isRelated ? 1 : 0,
      }
    })
  }, [result.nodes, selectedId, relatedIds, nodeStates, editable])

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

  const handleNodeClick = useCallback(
    (_: React.MouseEvent, node: Node) => {
      setSelectedId(selectedId === node.id ? null : node.id)
    },
    [selectedId, setSelectedId],
  )

  const handlePaneClick = useCallback(() => {
    setSelectedId(null)
  }, [setSelectedId])

  return (
    <>
      <div className="v4-stats-bar">
        {showDevInfo ? (
          <>
            <span className="v4-engine-tag">ELK Layout</span>
            <span>节点 {result.stats.nodeCount}</span>
            <span>连线 {result.stats.edgeCount}</span>
            <span>
              画布 {Math.round(result.stats.width)}×{Math.round(result.stats.height)}
            </span>
            <span>布局 {result.stats.layoutMs.toFixed(1)}ms</span>
          </>
        ) : (
          <>
            <span className="v4-stats-label">逻辑框图</span>
            <span>节点 {result.stats.nodeCount}</span>
            <span>连线 {result.stats.edgeCount}</span>
          </>
        )}
        <ZoomControls />
      </div>

      <div className="v4-flow-pane">
        <ReactFlow
          nodes={displayNodes}
          edges={displayEdges}
          nodeTypes={nodeTypes}
          edgeTypes={edgeTypes}
          nodesDraggable={false}
          nodesConnectable={editable}
          elementsSelectable={editable}
          zoomOnDoubleClick={false}
          fitView
          fitViewOptions={{ padding: 0.15 }}
          proOptions={{ hideAttribution: true }}
          onNodeClick={handleNodeClick}
          onPaneClick={handlePaneClick}
          onConnect={editable ? onConnect : undefined}
          onEdgeClick={editable ? onEdgeClick : undefined}
        >
          <Background variant={BackgroundVariant.Dots} gap={20} size={1} color="#1a2340" />
        </ReactFlow>
      </div>
    </>
  )
}

export default function GraphView({
  data,
  showDevInfo = true,
  nodeStates = null,
  editable = false,
  selectedNodeId,
  onNodeSelect,
  onConnect,
  onEdgeClick,
  className,
}: GraphViewProps) {
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

  const rootClass = ['v4-graph-view', className].filter(Boolean).join(' ')

  return (
    <div className={rootClass}>
      {(loading || error) && (
        <div className="v4-stats-bar">
          {showDevInfo && <span className="v4-engine-tag">ELK Layout</span>}
          {!showDevInfo && loading && <span className="v4-stats-label">逻辑框图</span>}
          {loading && <span>{showDevInfo ? '布局计算中…' : '正在加载…'}</span>}
          {error && <span className="v4-stats-error">错误：{error}</span>}
        </div>
      )}

      {loading && (
        <div className="v4-loading">
          {showDevInfo ? 'ELK 正在计算节点与连线位置…' : '正在生成逻辑框图…'}
        </div>
      )}

      {!loading && result && (
        <ReactFlowProvider>
          <GraphFlow
            result={result}
            showDevInfo={showDevInfo}
            nodeStates={nodeStates}
            editable={editable}
            selectedNodeId={selectedNodeId}
            onNodeSelect={onNodeSelect}
            onConnect={onConnect}
            onEdgeClick={onEdgeClick}
          />
        </ReactFlowProvider>
      )}
    </div>
  )
}
