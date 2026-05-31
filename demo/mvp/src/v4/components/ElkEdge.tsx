import { BaseEdge, type EdgeProps } from '@xyflow/react'

interface ElkEdgeData {
  path?: string
  v4Highlighted?: boolean
  v4Dimmed?: boolean
}

/** 使用 ELK 正交路由 sections 渲染的边 */
export default function ElkEdge({ id, data, style, markerEnd }: EdgeProps) {
  const d = data as ElkEdgeData | undefined
  const path = d?.path ?? ''
  const highlighted = d?.v4Highlighted ?? false
  const dimmed = d?.v4Dimmed ?? false

  return (
    <BaseEdge
      id={id}
      path={path}
      className={[
        highlighted ? 'v4-edge--highlighted' : '',
        dimmed ? 'v4-edge--dimmed' : '',
      ]
        .filter(Boolean)
        .join(' ')}
      style={{
        stroke: highlighted ? '#ffd54f' : '#7ab4e0',
        strokeWidth: highlighted ? 3 : 2,
        opacity: dimmed ? 0.2 : 1,
        strokeLinejoin: 'miter',
        strokeLinecap: 'butt',
        ...style,
      }}
      markerEnd={markerEnd}
    />
  )
}
