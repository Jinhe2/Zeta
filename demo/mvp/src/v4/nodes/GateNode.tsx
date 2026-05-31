import { Handle, Position, type NodeProps } from '@xyflow/react'
import type { V4NodeData } from './types'
import './v4Nodes.css'

function gateSymbol(type?: string): string {
  return type === 'OR' ? '≥1' : '&'
}

export default function GateNode({ data }: NodeProps) {
  const d = data as V4NodeData
  return (
    <div className="v4-node v4-node--gate">
      <Handle type="target" position={Position.Left} className="v4-handle" />
      <div className="v4-node__symbol">{gateSymbol(d.gateType)}</div>
      <div className="v4-node__gate-id">{d.nodeId}</div>
      {d.inverted && <span className="v4-node__invert" aria-hidden />}
      <Handle type="source" position={Position.Right} className="v4-handle" />
    </div>
  )
}
