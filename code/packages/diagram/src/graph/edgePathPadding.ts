import type { ElkEdgeSection } from 'elkjs/lib/elk-api'

/** 出线/入线水平 stub，避免转弯贴节点边沿 */
export const EDGE_STUB_MIN = 32

export interface Point {
  x: number
  y: number
}

export interface NodeRect {
  x: number
  y: number
  w: number
  h: number
}

const ON_EDGE = 4

/** 逐段收集 ELK section 折点（保留 ELK 为每条边分配的路径） */
export function collectSectionPoints(sections: ElkEdgeSection[]): Point[] {
  if (!sections.length) return []

  const points: Point[] = []
  for (const section of sections) {
    const seg = [section.startPoint, ...(section.bendPoints ?? []), section.endPoint]
    for (const p of seg) {
      if (points.length === 0) {
        points.push({ ...p })
        continue
      }
      const tail = points[points.length - 1]
      if (Math.abs(tail.x - p.x) < 0.5 && Math.abs(tail.y - p.y) < 0.5) continue
      points.push({ ...p })
    }
  }
  return points
}

/** 将路径首尾转弯推离节点边沿 */
export function padEdgePathPoints(
  points: Point[],
  sourceId: string,
  targetId: string,
  nodeRects: Map<string, NodeRect>,
  stub = EDGE_STUB_MIN,
): Point[] {
  if (points.length < 2) return points

  const out = points.map((p) => ({ ...p }))
  const src = nodeRects.get(sourceId)
  const tgt = nodeRects.get(targetId)

  if (src) pushStubFromSource(out, src, stub)
  if (tgt) pushStubToTarget(out, tgt, stub)

  return dedupeCollinear(out)
}

function pushStubFromSource(points: Point[], src: NodeRect, stub: number): void {
  const p0 = points[0]
  const p1 = points[1]
  const srcRight = src.x + src.w
  const srcLeft = src.x

  if (isHorizontal(p0, p1)) {
    if (p1.x >= p0.x && near(p0.x, srcRight)) {
      points[1] = { x: Math.max(p1.x, srcRight + stub), y: p0.y }
    } else if (p1.x < p0.x && near(p0.x, srcLeft)) {
      points[1] = { x: Math.min(p1.x, srcLeft - stub), y: p0.y }
    }
    return
  }

  if (isVertical(p0, p1)) {
    if (near(p0.x, srcRight)) {
      const stubX = srcRight + stub
      points.splice(1, 0, { x: stubX, y: p0.y }, { x: stubX, y: p1.y })
    } else if (near(p0.x, srcLeft)) {
      const stubX = srcLeft - stub
      points.splice(1, 0, { x: stubX, y: p0.y }, { x: stubX, y: p1.y })
    }
  }
}

function pushStubToTarget(points: Point[], tgt: NodeRect, stub: number): void {
  const n = points.length
  const pn = points[n - 1]
  const pn1 = points[n - 2]
  const tgtLeft = tgt.x
  const tgtRight = tgt.x + tgt.w

  if (isHorizontal(pn1, pn)) {
    if (pn1.x < pn.x && near(pn.x, tgtLeft)) {
      points[n - 2] = { x: Math.min(pn1.x, tgtLeft - stub), y: pn.y }
    } else if (pn1.x > pn.x && near(pn.x, tgtRight)) {
      points[n - 2] = { x: Math.max(pn1.x, tgtRight + stub), y: pn.y }
    }
    return
  }

  if (isVertical(pn1, pn)) {
    if (near(pn.x, tgtLeft)) {
      const stubX = tgtLeft - stub
      points.splice(n - 1, 0, { x: stubX, y: pn.y }, { x: stubX, y: pn1.y })
    } else if (near(pn.x, tgtRight)) {
      const stubX = tgtRight + stub
      points.splice(n - 1, 0, { x: stubX, y: pn.y }, { x: stubX, y: pn1.y })
    }
  }
}

/** 严格正交化：消除斜线，但不合并不同边的路径 */
export function toOrthogonalPoints(points: Point[]): Point[] {
  if (points.length < 2) return points.map((p) => ({ ...p }))

  const out: Point[] = [{ ...points[0] }]
  for (let i = 1; i < points.length; i++) {
    const prev = out[out.length - 1]
    const next = points[i]
    for (const p of connectOrthogonal(prev, next)) {
      out.push(p)
    }
  }
  return dedupeCollinear(out)
}

function connectOrthogonal(a: Point, b: Point): Point[] {
  if (isHorizontal(a, b) || isVertical(a, b)) {
    return [{ ...b }]
  }

  const hFirst: Point = { x: b.x, y: a.y }
  const dx = Math.abs(b.x - a.x)
  const dy = Math.abs(b.y - a.y)

  if (dx >= dy) {
    return [hFirst, { ...b }]
  }
  return [{ x: a.x, y: b.y }, { ...b }]
}

function isHorizontal(a: Point, b: Point): boolean {
  return Math.abs(a.y - b.y) < 0.5
}

function isVertical(a: Point, b: Point): boolean {
  return Math.abs(a.x - b.x) < 0.5
}

function near(a: number, b: number): boolean {
  return Math.abs(a - b) <= ON_EDGE
}

function dedupeCollinear(points: Point[]): Point[] {
  if (points.length <= 2) return points.map((p) => ({ ...p }))

  const out: Point[] = [{ ...points[0] }]
  for (let i = 1; i < points.length; i++) {
    const prev = out[out.length - 1]
    const curr = points[i]
    const next = points[i + 1]
    if (Math.abs(prev.x - curr.x) < 0.5 && Math.abs(prev.y - curr.y) < 0.5) continue
    if (next && collinear(prev, curr, next)) continue
    out.push({ ...curr })
  }
  return out
}

function collinear(a: Point, b: Point, c: Point): boolean {
  return (isHorizontal(a, b) && isHorizontal(b, c)) || (isVertical(a, b) && isVertical(b, c))
}

export function pointsToSvgPath(points: Point[]): string {
  const ortho = dedupeCollinear(toOrthogonalPoints(points))
  if (ortho.length < 2) return ''

  let d = `M ${ortho[0].x} ${ortho[0].y}`
  for (let i = 1; i < ortho.length; i++) {
    const prev = ortho[i - 1]
    const curr = ortho[i]
    if (isHorizontal(prev, curr)) {
      d += ` H ${curr.x}`
    } else if (isVertical(prev, curr)) {
      d += ` V ${curr.y}`
    }
  }
  return d
}

/** ELK 路径 → 正交 SVG（不做跨边合并或目标列对齐） */
export function buildOrthogonalEdgePath(
  sections: ElkEdgeSection[] | undefined,
  sourceId: string,
  targetId: string,
  nodeRects: Map<string, NodeRect>,
): string {
  const raw = collectSectionPoints(sections ?? [])
  const padded = padEdgePathPoints(raw, sourceId, targetId, nodeRects)
  return pointsToSvgPath(padded)
}
