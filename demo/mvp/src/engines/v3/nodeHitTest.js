/**
 * 线段与节点矩形的碰撞检测（端点节点 from/to 不参与检测）
 */

import { TURN_OFFSET } from './constants.js'

function hSegmentInteriorOverlap(y, x1, x2, nx, ny, nw, nh) {
  if (y <= ny || y >= ny + nh) return false
  const lo = Math.min(x1, x2)
  const hi = Math.max(x1, x2)
  const ox0 = Math.max(lo, nx)
  const ox1 = Math.min(hi, nx + nw)
  return ox1 - ox0 > 0.5
}

function vSegmentInteriorOverlap(x, y1, y2, nx, ny, nw, nh) {
  if (x <= nx || x >= nx + nw) return false
  const lo = Math.min(y1, y2)
  const hi = Math.max(y1, y2)
  const oy0 = Math.max(lo, ny)
  const oy1 = Math.min(hi, ny + nh)
  return oy1 - oy0 > 0.5
}

export function getWireSegments(wire) {
  const { srcX, tgtX, srcY, tgtY, turnX, bypassY, routeMode } = wire

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

/** 线段是否穿过任意非端点节点 */
export function wireHitsAnyNode(wire, nodes) {
  const segs = getWireSegments(wire)
  for (const node of Object.values(nodes)) {
    if (node.id === wire.from || node.id === wire.to) continue
    const { x: nx, y: ny, w: nw, h: nh } = node
    for (const seg of segs) {
      if (seg.type === 'H') {
        if (hSegmentInteriorOverlap(seg.y, seg.x1, seg.x2, nx, ny, nw, nh)) return true
      } else if (vSegmentInteriorOverlap(seg.x, seg.y1, seg.y2, nx, ny, nw, nh)) {
        return true
      }
    }
  }
  return false
}

/** 返回所有被穿过的节点 id */
export function wireHitNodeIds(wire, nodes) {
  const hit = []
  const segs = getWireSegments(wire)
  for (const node of Object.values(nodes)) {
    if (node.id === wire.from || node.id === wire.to) continue
    const { x: nx, y: ny, w: nw, h: nh } = node
    for (const seg of segs) {
      const hits = seg.type === 'H'
        ? hSegmentInteriorOverlap(seg.y, seg.x1, seg.x2, nx, ny, nw, nh)
        : vSegmentInteriorOverlap(seg.x, seg.y1, seg.y2, nx, ny, nw, nh)
      if (hits) {
        hit.push(node.id)
        break
      }
    }
  }
  return hit
}

/** 各列 x 范围 */
export function getLayerBounds(nodes) {
  const byLayer = {}
  for (const node of Object.values(nodes)) {
    const L = node.layer ?? 0
    if (!byLayer[L]) byLayer[L] = { left: Infinity, right: -Infinity }
    byLayer[L].left = Math.min(byLayer[L].left, node.x)
    byLayer[L].right = Math.max(byLayer[L].right, node.x + node.w)
  }
  return byLayer
}

/** 相邻列之间的走廊中点 x */
export function corridorMidX(nodes, layerLeft, layerRight) {
  const bounds = getLayerBounds(nodes)
  const left = bounds[layerLeft]
  const right = bounds[layerRight]
  if (!left || !right) return null
  return (left.right + right.left) / 2
}
