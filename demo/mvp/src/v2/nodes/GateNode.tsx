import { Handle, Position, type NodeProps } from '@xyflow/react'

export default function GateNode({ data }: NodeProps) {
  const gateType = String(data.gateType ?? 'GATE')
  return (
    <div className="v2-node v2-node--gate">
      <Handle type="target" position={Position.Left} className="v2-handle" />
      <div className="v2-node__type">{gateType}</div>
      <div className="v2-node__label">{String(data.label ?? '')}</div>
      <Handle type="source" position={Position.Right} className="v2-handle" />
    </div>
  )
}
