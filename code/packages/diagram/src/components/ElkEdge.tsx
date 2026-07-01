import { BaseEdge, type EdgeProps } from '@xyflow/react'

interface ElkEdgeData {
  path?: string
  inverted?: boolean
  v4Highlighted?: boolean
  v4Dimmed?: boolean
}

const INVERT_R = 5

/** 从正交 SVG path 提取终点坐标 */
function getLastPoint(path: string): { x: number; y: number } | null {
  let x = 0, y = 0
  const tokens = path.trim().split(/\s+/)
  let i = 0
  let found = false
  while (i < tokens.length) {
    const cmd = tokens[i]
    if (cmd === 'M' && i + 2 < tokens.length) {
      x = parseFloat(tokens[i + 1])
      y = parseFloat(tokens[i + 2])
      i += 3
      found = true
    } else if (cmd === 'H' && i + 1 < tokens.length) {
      x = parseFloat(tokens[i + 1])
      i += 2
      found = true
    } else if (cmd === 'V' && i + 1 < tokens.length) {
      y = parseFloat(tokens[i + 1])
      i += 2
      found = true
    } else {
      i++
    }
  }
  return found ? { x, y } : null
}

/** 使用 ELK 正交路由 sections 渲染的边 */
export default function ElkEdge({ id, data, style, markerEnd }: EdgeProps) {
  const d = data as ElkEdgeData | undefined
  const path = d?.path ?? ''
  const inverted = d?.inverted ?? false
  const highlighted = d?.v4Highlighted ?? false
  const dimmed = d?.v4Dimmed ?? false

  // path 终点就在 node 左边缘，圆圈中心内缩半径使其右边缘与 node 相切
  const lastPt = inverted ? getLastPoint(path) : null
  const circleX = lastPt ? lastPt.x - INVERT_R : 0
  const circleY = lastPt ? lastPt.y : 0

  return (
    <>
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
      {inverted && lastPt && (
        <circle
          cx={circleX}
          cy={circleY}
          r={INVERT_R}
          fill="#1a2332"
          stroke={highlighted ? '#ffd54f' : '#7ab4e0'}
          strokeWidth={1.5}
        />
      )}
    </>
  )
}
