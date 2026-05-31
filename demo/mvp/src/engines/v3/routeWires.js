import {
  CORNER_M,
  CONN_SPACING,
  TURN_OFFSET,
  TURN_MIN_GAP,
} from './constants.js'
import { CONTACT_GAP } from './nodeContactSize.js'
import {
  getWireSegments,
  wireHitsAnyNode,
  wireHitNodeIds,
  corridorMidX,
} from './nodeHitTest.js'

function getSrcX(node) {
  return node.x + node.w
}

function getTgtX(node) {
  return node.x
}

export function getColumnNodeOrder(nodes) {
  const byLayer = {}
  for (const node of Object.values(nodes)) {
    const layer = node.layer ?? 0
    if (!byLayer[layer]) byLayer[layer] = []
    byLayer[layer].push(node)
  }
  return Object.keys(byLayer)
    .map(Number)
    .sort((a, b) => a - b)
    .flatMap((layer) => byLayer[layer].sort((a, b) => a.y - b.y).map((n) => n.id))
}

function contactYForIndex(node, index, total) {
  const minY = node.y + CORNER_M
  const maxY = node.y + node.h - CORNER_M
  if (minY > maxY) return node.y + node.h / 2
  if (total <= 1) return (minY + maxY) / 2
  return minY + (index * (maxY - minY)) / (total - 1)
}

function clampContactY(y, node) {
  const minY = node.y + CORNER_M
  const maxY = node.y + node.h - CORNER_M
  if (minY > maxY) return node.y + node.h / 2
  return Math.max(minY, Math.min(y, maxY))
}

function segmentCrossPoint(sa, sb) {
  if (sa.type === 'H' && sb.type === 'V') {
    if (
      sb.x > Math.min(sa.x1, sa.x2) && sb.x < Math.max(sa.x1, sa.x2) &&
      sa.y > sb.y1 && sa.y < sb.y2
    ) return true
  }
  if (sa.type === 'V' && sb.type === 'H') {
    if (
      sa.x > Math.min(sb.x1, sb.x2) && sa.x < Math.max(sb.x1, sb.x2) &&
      sb.y > sa.y1 && sb.y < sa.y2
    ) return true
  }
  return false
}

function hSegmentsOverlap(sa, sb) {
  if (sa.type !== 'H' || sb.type !== 'H') return false
  if (Math.abs(sa.y - sb.y) >= CONN_SPACING) return false
  return Math.max(sa.x1, sb.x1) < Math.min(sa.x2, sb.x2)
}

function vSegmentsOverlap(sa, sb) {
  if (sa.type !== 'V' || sb.type !== 'V') return false
  if (Math.abs(sa.x - sb.x) >= TURN_MIN_GAP) return false
  return Math.max(sa.y1, sb.y1) < Math.min(sa.y2, sb.y2)
}

function segmentsCoincide(sa, sb) {
  return hSegmentsOverlap(sa, sb) || vSegmentsOverlap(sa, sb)
}

function countCrossingsBetween(wireA, wireB) {
  if (wireA.from === wireB.from || wireA.to === wireB.to) return 0
  const segsA = getWireSegments(wireA)
  const segsB = getWireSegments(wireB)
  let count = 0
  for (const sa of segsA) {
    for (const sb of segsB) {
      if (segmentCrossPoint(sa, sb)) count++
    }
  }
  return count
}

function wireWireValid(wire, existing, nodes) {
  if (wireHitsAnyNode(wire, nodes)) return false
  for (const other of existing) {
    for (const sa of getWireSegments(wire)) {
      for (const sb of getWireSegments(other)) {
        if (segmentsCoincide(sa, sb)) return false
      }
    }
    if (countCrossingsBetween(wire, other) > 1) return false
  }
  return true
}

function clampTurnX(turnX, srcX, tgtX) {
  if (turnX == null) return null
  return Math.max(srcX, Math.min(turnX, tgtX))
}

const CHANNEL_PAD = 2

function bypassYCandidates(wire, nodes) {
  const { srcX, tgtX, srcY, tgtY } = wire
  const lo = Math.min(srcX, tgtX)
  const hi = Math.max(srcX, tgtX)
  const set = new Set()

  set.add(srcY)
  set.add(tgtY)
  set.add((srcY + tgtY) / 2)

  let minY = Infinity
  let maxY = -Infinity
  for (const node of Object.values(nodes)) {
    minY = Math.min(minY, node.y)
    maxY = Math.max(maxY, node.y + node.h)
    if (node.id === wire.from || node.id === wire.to) continue
    const nx = node.x
    const nw = node.w
    if (nx + nw <= lo || nx >= hi) continue
    set.add(node.y - CHANNEL_PAD)
    set.add(node.y + node.h + CHANNEL_PAD)
  }

  set.add(minY - CHANNEL_PAD)
  set.add(maxY + CHANNEL_PAD)

  return [...set].sort((a, b) => {
    const costA = Math.abs(a - srcY) + Math.abs(a - tgtY)
    const costB = Math.abs(b - srcY) + Math.abs(b - tgtY)
    return costA - costB
  })
}

/** 沿源/目标边竖线绕行：M srcX srcY V bypassY H tgtX V tgtY */
function findClearBypassY(wire, nodes) {
  for (const bypassY of bypassYCandidates(wire, nodes)) {
    const test = { ...wire, routeMode: 'vhv', bypassY, turnX: null }
    if (!wireHitsAnyNode(test, nodes)) return bypassY
  }
  return null
}

function applyVHVRoute(wire, bypassY) {
  wire.routeMode = 'vhv'
  wire.bypassY = bypassY
  wire.turnX = null
}

function applyHVHRoute(wire, turnX) {
  wire.routeMode = 'hvh'
  wire.bypassY = null
  wire.turnX = turnX
}

/** 在列间走廊中搜索不穿节点的 turnX（优先早转弯，缩短首段水平线） */
function findClearTurnX(wire, nodes) {
  const { srcX, tgtX, srcY, tgtY } = wire
  if (Math.abs(srcY - tgtY) < 1) return null
  if (tgtX <= srcX) return srcX

  const fromLayer = nodes[wire.from].layer ?? 0
  const toLayer = nodes[wire.to].layer ?? 0
  const lo = Math.min(fromLayer, toLayer)
  const hi = Math.max(fromLayer, toLayer)

  const candidates = new Set()

  for (let L = lo; L < hi; L++) {
    const mid = corridorMidX(nodes, L, L + 1)
    if (mid != null) candidates.add(mid)
  }

  for (let x = srcX + TURN_OFFSET; x <= tgtX - TURN_OFFSET; x += 6) {
    candidates.add(x)
  }

  candidates.add(tgtX - TURN_OFFSET)
  candidates.add(srcX + (tgtX - srcX) * 0.35)
  candidates.add(srcX + (tgtX - srcX) * 0.65)

  const sorted = [...candidates]
    .filter((x) => x >= srcX + TURN_OFFSET && x <= tgtX - TURN_OFFSET)
    .sort((a, b) => a - b)

  for (const turnX of sorted) {
    const test = { ...wire, turnX: clampTurnX(turnX, srcX, tgtX) }
    if (!wireHitsAnyNode(test, nodes)) return test.turnX
  }

  return null
}

/** 穷举接触点与 turnX，寻找不穿节点的路径 */
function exhaustiveClearRoute(wire, nodes, existing) {
  const from = nodes[wire.from]
  const to = nodes[wire.to]
  const srcSteps = Math.max(connectionsSteps(from), 8)
  const tgtSteps = Math.max(connectionsSteps(to), 8)

  function yAt(node, i, total) {
    const minY = node.y + CORNER_M
    const maxY = node.y + node.h - CORNER_M
    if (total <= 1) return (minY + maxY) / 2
    return minY + (i / (total - 1)) * (maxY - minY)
  }

  const srcTotal = srcSteps
  const tgtTotal = tgtSteps

  for (let si = 0; si < srcTotal; si++) {
    for (let ti = 0; ti < tgtTotal; ti++) {
      const trial = {
        ...wire,
        srcY: yAt(from, si, srcTotal),
        tgtY: yAt(to, ti, tgtTotal),
        srcX: getSrcX(from),
        tgtX: getTgtX(to),
        turnX: null,
      }

      if (Math.abs(trial.srcY - trial.tgtY) < 1) {
        if (!wireHitsAnyNode(trial, nodes) && wireWireValid(trial, existing, nodes)) {
          Object.assign(wire, trial)
          return true
        }
      }

      const turnX = findClearTurnX(trial, nodes)
      if (turnX != null) {
        applyHVHRoute(trial, turnX)
        if (wireWireValid(trial, existing, nodes)) {
          Object.assign(wire, trial)
          return true
        }
      }

      const bypassY = findClearBypassY(trial, nodes)
      if (bypassY != null) {
        applyVHVRoute(trial, bypassY)
        if (wireWireValid(trial, existing, nodes)) {
          Object.assign(wire, trial)
          return true
        }
      }
    }
  }
  return false
}

function connectionsSteps(node) {
  const span = node.h - CORNER_M * 2
  if (span <= 0) return 1
  return Math.min(16, Math.max(4, Math.floor(span / CONTACT_GAP) + 1))
}

function assignGeometry(wire, nodes) {
  const from = nodes[wire.from]
  const to = nodes[wire.to]
  wire.srcX = getSrcX(from)
  wire.tgtX = getTgtX(to)

  if (Math.abs(wire.srcY - wire.tgtY) < 1) {
    applyHVHRoute(wire, null)
    return
  }

  const turnX = findClearTurnX(wire, nodes)
  if (turnX != null) {
    applyHVHRoute(wire, turnX)
    return
  }

  const bypassY = findClearBypassY(wire, nodes)
  if (bypassY != null) {
    applyVHVRoute(wire, bypassY)
    return
  }

  applyHVHRoute(wire, null)
}

function buildPath(wire) {
  const { srcX, tgtX, srcY, tgtY, turnX, bypassY, routeMode } = wire

  if (routeMode === 'vhv' && bypassY != null) {
    return `M ${srcX} ${srcY} V ${bypassY} H ${tgtX} V ${tgtY}`
  }

  if (turnX == null) {
    if (Math.abs(srcY - tgtY) < 1) {
      return `M ${srcX} ${srcY} H ${tgtX}`
    }
    const tx = Math.min(srcX + TURN_OFFSET, tgtX - 1)
    return `M ${srcX} ${srcY} H ${tx} V ${tgtY} H ${tgtX}`
  }
  return `M ${srcX} ${srcY} H ${turnX} V ${tgtY} H ${tgtX}`
}

function findAllCrossings(wires) {
  const crossings = []
  for (let i = 0; i < wires.length; i++) {
    for (let j = i + 1; j < wires.length; j++) {
      const a = wires[i]
      const b = wires[j]
      if (a.from === b.from || a.to === b.to) continue
      for (const sa of getWireSegments(a)) {
        for (const sb of getWireSegments(b)) {
          if (segmentCrossPoint(sa, sb)) {
            crossings.push(
              sa.type === 'H' ? { x: sb.x, y: sa.y } : { x: sa.x, y: sb.y },
            )
          }
        }
      }
    }
  }
  return crossings
}

function tryAdjustWire(wire, nodes, attempt) {
  const from = nodes[wire.from]
  const to = nodes[wire.to]
  const step = CONTACT_GAP

  if (attempt % 4 === 0) {
    wire.srcY = clampContactY(wire.srcY + step, from)
    wire.tgtY = clampContactY(wire.tgtY + step, to)
  } else if (attempt % 4 === 1) {
    wire.srcY = clampContactY(wire.srcY - step, from)
    wire.tgtY = clampContactY(wire.tgtY - step, to)
  } else if (attempt % 4 === 2) {
    wire.srcY = clampContactY(wire.srcY + step, from)
  } else {
    wire.tgtY = clampContactY(wire.tgtY + step, to)
  }

  assignGeometry(wire, nodes)
}

function routeSingleWire(wire, nodes, existing) {
  assignGeometry(wire, nodes)
  if (wireWireValid(wire, existing, nodes)) return

  for (let i = 0; i < 80; i++) {
    tryAdjustWire(wire, nodes, i)
    if (wireWireValid(wire, existing, nodes)) return
  }

  if (exhaustiveClearRoute(wire, nodes, existing)) return
}

function resolveAll(wires, nodes) {
  for (let iter = 0; iter < 120; iter++) {
    let changed = false
    for (let i = 0; i < wires.length; i++) {
      const wi = wires[i]
      const prior = wires.slice(0, i)
      if (wireWireValid(wi, prior, nodes)) continue
      tryAdjustWire(wi, nodes, iter + i)
      if (wireWireValid(wi, prior, nodes)) {
        changed = true
        continue
      }
      if (exhaustiveClearRoute(wi, nodes, prior)) {
        changed = true
        continue
      }
      const bypassY = findClearBypassY(wi, nodes)
      if (bypassY != null) {
        applyVHVRoute(wi, bypassY)
        if (wireWireValid(wi, prior, nodes)) changed = true
      }
    }
    if (!changed) break
  }
}

function sortOutgoing(outgoing, nodes) {
  return [...outgoing].sort((a, b) => {
    const dy = nodes[a.to].y - nodes[b.to].y
    if (dy !== 0) return dy
    return a.toInputIndex - b.toInputIndex
  })
}

/**
 * 按列顺序逐节点、逐下游画线；保证不穿节点。
 */
export function routeConnections(connections, nodes) {
  const completed = []
  const srcUsed = {}
  const tgtUsed = {}
  const columnOrder = getColumnNodeOrder(nodes)

  for (const nodeId of columnOrder) {
    for (const conn of sortOutgoing(connections.byFrom[nodeId] || [], nodes)) {
      const from = nodes[conn.from]
      const to = nodes[conn.to]

      const srcIdx = srcUsed[conn.from] || 0
      srcUsed[conn.from] = srcIdx + 1
      const tgtIdx = tgtUsed[conn.to] || 0
      tgtUsed[conn.to] = tgtIdx + 1

      const wire = {
        ...conn,
        srcY: contactYForIndex(from, srcIdx, connections.outboundCount[conn.from] || 1),
        tgtY: contactYForIndex(to, tgtIdx, connections.inboundCount[conn.to] || 1),
        turnX: null,
        routeMode: 'hvh',
        bypassY: null,
      }

      routeSingleWire(wire, nodes, completed)

      conn.status = 'routed'
      conn.srcY = wire.srcY
      conn.tgtY = wire.tgtY
      conn.turnX = wire.turnX
      conn.srcX = wire.srcX
      conn.tgtX = wire.tgtX
      conn.path = buildPath(wire)

      completed.push(wire)
    }
  }

  resolveAll(completed, nodes)

  for (const w of completed) {
    w.path = buildPath(w)
    const conn = connections.items.find((c) => c.id === w.id)
    if (conn) {
      conn.path = w.path
      conn.srcY = w.srcY
      conn.tgtY = w.tgtY
      conn.turnX = w.turnX
      conn.hitNodes = wireHitNodeIds(w, nodes)
    }
  }

  connections.routed = completed.length
  connections.pending = connections.total - completed.length
  connections.wires = completed
  connections.nodeHits = completed
    .filter((w) => wireHitNodeIds(w, nodes).length > 0)
    .map((w) => ({ id: w.id, from: w.from, to: w.to, hit: wireHitNodeIds(w, nodes) }))

  const wires = completed.map((w) => ({ ...w, path: buildPath(w) }))

  return {
    wires,
    crossings: findAllCrossings(completed),
    connections,
  }
}

export { getWireSegments as getSegments, wireHitsAnyNode, wireHitNodeIds, wireWireValid as wireIsValid }
