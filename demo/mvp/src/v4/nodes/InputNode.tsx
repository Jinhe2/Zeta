import { Handle, Position, type NodeProps } from '@xyflow/react'
import type { V4NodeData } from './types'
import './v4Nodes.css'

export default function InputNode({ data }: NodeProps) {
  const d = data as V4NodeData
  return (
    <div className="v4-node v4-node--input">
      <div className="v4-node__text">
        <span className="v4-node__threshold">{d.threshold ?? '-'}</span>
        <span className="v4-node__sep" />
        <span>{d.label}</span>
        <span className="v4-node__id">{d.nodeId}</span>
        <span className="v4-node__sep" />
        <span className="v4-node__value">{d.displayValue ?? '-'}</span>
      </div>
      <Handle type="source" position={Position.Right} className="v4-handle" />
    </div>
  )
}
