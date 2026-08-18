import { Handle, Position, type NodeProps } from '@xyflow/react'
import type { V4NodeData } from './types'
import NodeSectionState from './NodeSectionState'
import { sectionStateClass } from './sectionStateClass'
import './v4Nodes.css'

/** 组合逻辑成员节点：一个基础逻辑框图在组合中的单元。 */
export default function LogicNode({ data }: NodeProps) {
  const d = data as V4NodeData
  return (
    <div className={`v4-node v4-node--logic${sectionStateClass(d.sectionSatisfied)}`}>
      <NodeSectionState satisfied={d.sectionSatisfied} />
      <Handle type="target" position={Position.Left} className="v4-handle" />
      <div className="v4-node__text">
        <span>{d.label}</span>
      </div>
      <Handle type="source" position={Position.Right} className="v4-handle" />
    </div>
  )
}
