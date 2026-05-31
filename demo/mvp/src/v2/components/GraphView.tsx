import { useMemo } from 'react'
import {
  ReactFlow,
  Background,
  Controls,
  BackgroundVariant,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'

import InputNode from '../nodes/InputNode'
import GateNode from '../nodes/GateNode'
import OutputNode from '../nodes/OutputNode'
import OrthogonalEdge from './OrthogonalEdge'
import { runLayoutPipeline, type GraphData } from '../graph'
import './GraphView.css'

const nodeTypes = {
  input: InputNode,
  gate: GateNode,
  output: OutputNode,
}

const edgeTypes = {
  orthogonal: OrthogonalEdge,
}

export interface GraphViewProps {
  data: GraphData
}

export default function GraphView({ data }: GraphViewProps) {
  const pipeline = useMemo(() => runLayoutPipeline(data), [data])

  const { reactFlowNodes, reactFlowEdges, crossingCount, nodeHits, layout } = pipeline

  return (
    <div className="v2-graph-view">
      <div className="v2-stats-bar">
        <span>节点 {reactFlowNodes.length}</span>
        <span>连线 {reactFlowEdges.length}</span>
        <span>层间交叉 {crossingCount}</span>
        <span>穿节点 {nodeHits.length}</span>
        <span>
          画布 {Math.round(layout.width)}×{Math.round(layout.height)}
        </span>
      </div>
      <div className="v2-flow-pane">
        <ReactFlow
          nodes={reactFlowNodes}
          edges={reactFlowEdges}
          nodeTypes={nodeTypes}
          edgeTypes={edgeTypes}
          nodesDraggable={false}
          nodesConnectable={false}
          elementsSelectable={false}
          zoomOnDoubleClick={false}
          fitView
          fitViewOptions={{ padding: 0.15 }}
          proOptions={{ hideAttribution: true }}
        >
          <Background variant={BackgroundVariant.Dots} gap={20} size={1} color="#1e3448" />
          <Controls showInteractive={false} />
        </ReactFlow>
      </div>
    </div>
  )
}
