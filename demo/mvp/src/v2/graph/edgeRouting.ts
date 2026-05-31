import type { BuiltGraph, LayoutResult, NodePosition, RoutedEdge } from './types'
import { getNodeCenterLeft, getNodeCenterRight } from './layout'

const TURN_OFFSET = 30
const CHANNEL_PAD = 4

interface SegmentH {
  type: 'H'
  y: number
  x1: number
  x2: number
}

interface SegmentV {
  type: 'V'
  x: number
  y1: number
  y2: number
}

type Segment = SegmentH | SegmentV

interface WireGeom {
  srcX: number
  srcY: number
  tgtX: number
  tgtY: number
  turnX: number | null
  bypassY: number | null
  routeMode: 'hvh' | 'vhv'
}

function hInterior(y: number, x1: number, x2: number, pos: NodePosition): boolean {
  if (y <= pos.y || y >= pos.y + pos.height) return false
  const lo = Math.min(x1, x2)
  const hi = Math.max(x1, x2)
  return Math.min(hi, pos.x + pos.width) - Math.max(lo, pos.x) > 0.5
}

function vInterior(x: number, y1: number, y2: number, pos: NodePosition): boolean {
  if (x <= pos.x || x >= pos.x + pos.width) return false
  const lo = Math.min(y1, y2)
  const hi = Math.max(y1, y2)
  return Math.min(hi, pos.y + pos.height) - Math.max(lo, pos.y) > 0.5
}

function getSegments(geom: WireGeom): Segment[] {
  const { srcX, tgtX, srcY, tgtY, turnX, bypassY, routeMode } = geom

  if (routeMode === 'vhv' && bypassY != null) {
    return [
      { type: 'V', x: srcX, y1: Math.min(srcY, bypassY), y2: Math.max(srcY, bypassY) },
      { type: 'H', y: bypassY, x1: srcX, x2: tgtX },
      { type: 'V', x: tgtX, y1: Math.min(bypassY, tgtY), y2: Math.max(bypassY, tgtY) },
    ]
  }

  if (turnX == null) {
    if (Math.abs(srcY - tgtY) < 1) {
      return [{ type: 'H', y: srcY, x1: srcX, x2: tgtX }]
    }
    const tx = Math.min(srcX + TURN_OFFSET, tgtX - 1)
    return [
      { type: 'H', y: srcY, x1: srcX, x2: tx },
      { type: 'V', x: tx, y1: Math.min(srcY, tgtY), y2: Math.max(srcY, tgtY) },
      { type: 'H', y: tgtY, x1: tx, x2: tgtX },
    ]
  }

  return [
    { type: 'H', y: srcY, x1: srcX, x2: turnX },
    { type: 'V', x: turnX, y1: Math.min(srcY, tgtY), y2: Math.max(srcY, tgtY) },
    { type: 'H', y: tgtY, x1: turnX, x2: tgtX },
  ]
}

function hitsNode(
  geom: WireGeom,
  positions: Map<string, NodePosition>,
  from: string,
  to: string,
): boolean {
  const segs = getSegments(geom)
  for (const [id, pos] of positions) {
    if (id === from || id === to) continue
    for (const seg of segs) {
      if (seg.type === 'H') {
        if (hInterior(seg.y, seg.x1, seg.x2, pos)) return true
      } else if (vInterior(seg.x, seg.y1, seg.y2, pos)) {
        return true
      }
    }
  }
  return false
}

function findClearTurnX(geom: WireGeom, positions: Map<string, NodePosition>, from: string, to: string): number | null {
  const { srcX, tgtX } = geom
  if (tgtX <= srcX) return srcX

  const candidates = new Set<number>()
  for (let x = srcX + TURN_OFFSET; x <= tgtX - TURN_OFFSET; x += 8) {
    candidates.add(x)
  }
  candidates.add(srcX + (tgtX - srcX) * 0.35)
  candidates.add(srcX + (tgtX - srcX) * 0.65)
  candidates.add(tgtX - TURN_OFFSET)

  for (const turnX of [...candidates].sort((a, b) => a - b)) {
    const test = { ...geom, turnX, routeMode: 'hvh' as const, bypassY: null }
    if (!hitsNode(test, positions, from, to)) return turnX
  }
  return null
}

function bypassCandidates(geom: WireGeom, positions: Map<string, NodePosition>): number[] {
  const { srcX, tgtX, srcY, tgtY } = geom
  const lo = Math.min(srcX, tgtX)
  const hi = Math.max(srcX, tgtX)
  const set = new Set<number>([srcY, tgtY, (srcY + tgtY) / 2])

  let minY = Infinity
  let maxY = -Infinity
  for (const pos of positions.values()) {
    minY = Math.min(minY, pos.y)
    maxY = Math.max(maxY, pos.y + pos.height)
    if (pos.x + pos.width <= lo || pos.x >= hi) continue
    set.add(pos.y - CHANNEL_PAD)
    set.add(pos.y + pos.height + CHANNEL_PAD)
  }
  set.add(minY - CHANNEL_PAD)
  set.add(maxY + CHANNEL_PAD)

  return [...set].sort(
    (a, b) => Math.abs(a - srcY) + Math.abs(a - tgtY) - (Math.abs(b - srcY) + Math.abs(b - tgtY)),
  )
}

function findClearBypassY(
  geom: WireGeom,
  positions: Map<string, NodePosition>,
  from: string,
  to: string,
): number | null {
  for (const bypassY of bypassCandidates(geom, positions)) {
    const test = { ...geom, bypassY, turnX: null, routeMode: 'vhv' as const }
    if (!hitsNode(test, positions, from, to)) return bypassY
  }
  return null
}

function buildPathD(geom: WireGeom): string {
  const { srcX, tgtX, srcY, tgtY, turnX, bypassY, routeMode } = geom
  if (routeMode === 'vhv' && bypassY != null) {
    return `M ${srcX} ${srcY} V ${bypassY} H ${tgtX} V ${tgtY}`
  }
  if (turnX == null) {
    if (Math.abs(srcY - tgtY) < 1) return `M ${srcX} ${srcY} H ${tgtX}`
    const tx = Math.min(srcX + TURN_OFFSET, tgtX - 1)
    return `M ${srcX} ${srcY} H ${tx} V ${tgtY} H ${tgtX}`
  }
  return `M ${srcX} ${srcY} H ${turnX} V ${tgtY} H ${tgtX}`
}

function pathToPoints(path: string): Array<{ x: number; y: number }> {
  const points: Array<{ x: number; y: number }> = []
  const tokens = path.match(/[MLHV][^MLHV]*/g) ?? []
  let cx = 0
  let cy = 0
  for (const token of tokens) {
    const cmd = token[0]
    const nums = token
      .slice(1)
      .trim()
      .split(/[\s,]+/)
      .map(Number)
    if (cmd === 'M') {
      cx = nums[0]
      cy = nums[1]
      points.push({ x: cx, y: cy })
    } else if (cmd === 'H') {
      cx = nums[0]
      points.push({ x: cx, y: cy })
    } else if (cmd === 'V') {
      cy = nums[0]
      points.push({ x: cx, y: cy })
    } else if (cmd === 'L') {
      cx = nums[0]
      cy = nums[1]
      points.push({ x: cx, y: cy })
    }
  }
  return points
}

function routeSingleEdge(
  source: string,
  target: string,
  positions: Map<string, NodePosition>,
): WireGeom {
  const srcPos = positions.get(source)!
  const tgtPos = positions.get(target)!
  const src = getNodeCenterRight(srcPos)
  const tgt = getNodeCenterLeft(tgtPos)

  let geom: WireGeom = {
    srcX: src.x,
    srcY: src.y,
    tgtX: tgt.x,
    tgtY: tgt.y,
    turnX: null,
    bypassY: null,
    routeMode: 'hvh',
  }

  if (Math.abs(geom.srcY - geom.tgtY) < 1) {
    if (!hitsNode(geom, positions, source, target)) return geom
  }

  const turnX = findClearTurnX(geom, positions, source, target)
  if (turnX != null) {
    geom = { ...geom, turnX, routeMode: 'hvh', bypassY: null }
    if (!hitsNode(geom, positions, source, target)) return geom
  }

  const bypassY = findClearBypassY(geom, positions, source, target)
  if (bypassY != null) {
    return { ...geom, bypassY, turnX: null, routeMode: 'vhv' }
  }

  return geom
}

/**
 * 正交连线路由：H-V-H 优先，必要时 V-H-V 绕行，确保不穿节点。
 */
export function routeEdges(
  graph: BuiltGraph,
  layout: LayoutResult,
): RoutedEdge[] {
  return graph.edges.map((edge) =>
    routeVisiblePair(edge.source, edge.target, layout.positions, edge.id),
  )
}

export function routeVisiblePair(
  source: string,
  target: string,
  positions: Map<string, NodePosition>,
  id?: string,
): RoutedEdge {
  const geom = routeSingleEdge(source, target, positions)
  const path = buildPathD(geom)
  return {
    id: id ?? `${source}->${target}`,
    source,
    target,
    path,
    points: pathToPoints(path),
  }
}

/** 检测路由结果是否仍有穿节点（调试用） */
export function findEdgeNodeHits(
  routed: RoutedEdge[],
  graph: BuiltGraph,
  layout: LayoutResult,
): Array<{ id: string; hits: string[] }> {
  const hits: Array<{ id: string; hits: string[] }> = []
  for (const edge of routed) {
    const geom = routeSingleEdge(edge.source, edge.target, layout.positions)
    const nodeHits: string[] = []
    for (const node of graph.nodes) {
      if (node.id === edge.source || node.id === edge.target) continue
      const pos = layout.positions.get(node.id)!
      const segs = getSegments(geom)
      for (const seg of segs) {
        const hit =
          seg.type === 'H'
            ? hInterior(seg.y, seg.x1, seg.x2, pos)
            : vInterior(seg.x, seg.y1, seg.y2, pos)
        if (hit) {
          nodeHits.push(node.id)
          break
        }
      }
    }
    if (nodeHits.length > 0) hits.push({ id: edge.id, hits: nodeHits })
  }
  return hits
}
