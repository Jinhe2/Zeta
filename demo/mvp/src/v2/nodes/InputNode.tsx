import { Handle, Position, type NodeProps } from '@xyflow/react'

export default function InputNode({ data }: NodeProps) {
  return (
    <div className="v2-node v2-node--input">
      <div className="v2-node__label">{String(data.label ?? '')}</div>
      <Handle type="source" position={Position.Right} className="v2-handle" />
    </div>
  )
}
