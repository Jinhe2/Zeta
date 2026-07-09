import { Handle, Position, type NodeProps } from '@xyflow/react'
import type { V4NodeData } from './types'
import NodeSectionState from './NodeSectionState'
import { sectionStateClass } from './sectionStateClass'
import './v4Nodes.css'

function gateSymbol(type?: string): string {
  return type === 'OR' ? '≥1' : '&'
}

export default function GateNode({ data }: NodeProps) {
  const d = data as V4NodeData
  return (
    <div className={`v4-node v4-node--gate${sectionStateClass(d.sectionSatisfied)}`}>
      <NodeSectionState satisfied={d.sectionSatisfied} />
      <Handle type="target" position={Position.Left} className="v4-handle" />
      <div className="v4-node__symbol">{gateSymbol(d.gateType)}</div>
      {d.inverted && <span className="v4-node__invert" aria-hidden />}
      <Handle type="source" position={Position.Right} className="v4-handle" />
    </div>
  )
}
