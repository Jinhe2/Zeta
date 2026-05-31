import { Handle, Position, type NodeProps } from '@xyflow/react'

export default function OutputNode({ data }: NodeProps) {
  return (
    <div className="v2-node v2-node--output">
      <Handle type="target" position={Position.Left} className="v2-handle" />
      <div className="v2-node__label">{String(data.label ?? '')}</div>
    </div>
  )
}
