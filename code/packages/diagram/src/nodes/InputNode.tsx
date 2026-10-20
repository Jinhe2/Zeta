import { Handle, Position, type NodeProps } from '@xyflow/react'
import type { V4NodeData } from './types'
import NodeSectionState from './NodeSectionState'
import { sectionStateClass } from './sectionStateClass'
import './v4Nodes.css'

export default function InputNode({ data }: NodeProps) {
  const d = data as V4NodeData
  return (
    <div className={`v4-node v4-node--input${sectionStateClass(d.sectionSatisfied)}`}>
      <NodeSectionState satisfied={d.sectionSatisfied} />
      <div className="v4-node__text">
        <span className="v4-node__input-label">{d.label}</span>
        {d.effectiveValueText != null && (
          <>
            <span className="v4-node__sep" />
            <span className="v4-node__value v4-node__effective-value">
              有效值 {d.effectiveValueText}
            </span>
          </>
        )}
      </div>
      <Handle type="source" position={Position.Right} className="v4-handle" />
    </div>
  )
}
