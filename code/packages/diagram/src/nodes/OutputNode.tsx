import { Handle, Position, type NodeProps } from '@xyflow/react'
import type { V4NodeData } from './types'
import NodeSectionState from './NodeSectionState'
import './v4Nodes.css'

export default function OutputNode({ data }: NodeProps) {
  const d = data as V4NodeData
  return (
    <div className="v4-node v4-node--output">
      <NodeSectionState satisfied={d.sectionSatisfied} />
      <Handle type="target" position={Position.Left} className="v4-handle" />
      <div className="v4-node__text">
        <span>{d.label}</span>
        <span className="v4-node__sep" />
        <span className="v4-node__ref">{d.channelRef ?? '-'}</span>
      </div>
    </div>
  )
}
