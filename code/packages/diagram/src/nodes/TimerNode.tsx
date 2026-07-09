import { Handle, Position, type NodeProps } from '@xyflow/react'
import type { V4NodeData } from './types'
import NodeSectionState from './NodeSectionState'
import { sectionStateClass } from './sectionStateClass'
import './v4Nodes.css'

export default function TimerNode({ data }: NodeProps) {
  const d = data as V4NodeData
  return (
    <div className={`v4-node v4-node--timer${sectionStateClass(d.sectionSatisfied)}`}>
      <NodeSectionState satisfied={d.sectionSatisfied} />
      <Handle type="target" position={Position.Left} className="v4-handle" />
      <div className="v4-node__text">
        <span>{d.label}</span>
        <span className="v4-node__sep" />
        <span className="v4-node__value">{d.delayValue ?? '-'}s</span>
      </div>
      <Handle type="source" position={Position.Right} className="v4-handle" />
    </div>
  )
}
