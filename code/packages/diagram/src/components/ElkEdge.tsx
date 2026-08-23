import { BaseEdge, type EdgeProps } from '@xyflow/react'

interface ElkEdgeData {
  path?: string
  inverted?: boolean
  v4Highlighted?: boolean
  v4Dimmed?: boolean
}

const INVERT_R = 5
const DIRECTION_ARROW_LENGTH = 11
const DIRECTION_ARROW_HALF_WIDTH = 5

interface PathPoint {
  x: number
  y: number
}

interface DirectionArrow extends PathPoint {
  angle: number
}

/** 解析本组件生成的 M/H/V 正交路径。 */
function getPathPoints(path: string): PathPoint[] {
  const points: PathPoint[] = []
  let x = 0
  let y = 0
  const tokens = path.trim().split(/\s+/)
  let i = 0

  while (i < tokens.length) {
    const cmd = tokens[i]
    if (cmd === 'M' && i + 2 < tokens.length) {
      x = parseFloat(tokens[i + 1])
      y = parseFloat(tokens[i + 2])
      i += 3
      points.push({ x, y })
    } else if (cmd === 'H' && i + 1 < tokens.length) {
      x = parseFloat(tokens[i + 1])
      i += 2
      points.push({ x, y })
    } else if (cmd === 'V' && i + 1 < tokens.length) {
      y = parseFloat(tokens[i + 1])
      i += 2
      points.push({ x, y })
    } else {
      i++
    }
  }

  return points.filter((point) => Number.isFinite(point.x) && Number.isFinite(point.y))
}

/** 返回路径长度中点及该处的前进方向。 */
function getDirectionArrow(path: string): DirectionArrow | null {
  const points = getPathPoints(path)
  if (points.length < 2) return null

  const segments = points.slice(1).map((end, index) => {
    const start = points[index]
    return {
      start,
      end,
      length: Math.abs(end.x - start.x) + Math.abs(end.y - start.y),
    }
  })
  const totalLength = segments.reduce((sum, segment) => sum + segment.length, 0)
  if (totalLength < DIRECTION_ARROW_LENGTH * 2) return null

  let remaining = totalLength / 2
  for (const segment of segments) {
    if (remaining <= segment.length && segment.length > 0) {
      const ratio = remaining / segment.length
      return {
        x: segment.start.x + (segment.end.x - segment.start.x) * ratio,
        y: segment.start.y + (segment.end.y - segment.start.y) * ratio,
        angle: Math.atan2(
          segment.end.y - segment.start.y,
          segment.end.x - segment.start.x,
        ) * (180 / Math.PI),
      }
    }
    remaining -= segment.length
  }

  return null
}

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
  const directionArrow = getDirectionArrow(path)
  const edgeColor = highlighted ? '#ffd54f' : '#7ab4e0'
  const edgeOpacity = dimmed ? 0.2 : 1

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
          stroke: edgeColor,
          strokeWidth: highlighted ? 3 : 2,
          opacity: edgeOpacity,
          strokeLinejoin: 'miter',
          strokeLinecap: 'butt',
          ...style,
        }}
        markerEnd={markerEnd}
      />
      {directionArrow && (
        <polygon
          points={`-${DIRECTION_ARROW_LENGTH / 2},-${DIRECTION_ARROW_HALF_WIDTH} ${DIRECTION_ARROW_LENGTH / 2},0 -${DIRECTION_ARROW_LENGTH / 2},${DIRECTION_ARROW_HALF_WIDTH}`}
          transform={`translate(${directionArrow.x} ${directionArrow.y}) rotate(${directionArrow.angle})`}
          fill={edgeColor}
          opacity={edgeOpacity}
          pointerEvents="none"
          aria-hidden="true"
        />
      )}
      {inverted && lastPt && (
        <circle
          cx={circleX}
          cy={circleY}
          r={INVERT_R}
          fill="#1a2332"
          stroke={edgeColor}
          strokeWidth={1.5}
        />
      )}
    </>
  )
}
