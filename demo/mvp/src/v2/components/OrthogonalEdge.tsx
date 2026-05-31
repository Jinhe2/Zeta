import { BaseEdge, type EdgeProps } from '@xyflow/react'

interface OrthogonalEdgeData {
  path?: string
}

/**
 * 正交边：使用布局引擎预计算的 SVG path，禁止 Bezier / SmoothStep。
 */
export default function OrthogonalEdge({
  id,
  data,
  style,
  markerEnd,
}: EdgeProps) {
  const path = (data as OrthogonalEdgeData | undefined)?.path ?? ''
  return (
    <BaseEdge
      id={id}
      path={path}
      style={{ stroke: '#8aa4be', strokeWidth: 2, ...style }}
      markerEnd={markerEnd}
    />
  )
}
