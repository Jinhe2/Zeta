import {
  CORNER_M,
  CONN_SPACING,
  TURN_OFFSET,
  TURN_STEP,
  TURN_MIN_GAP,
} from './constants.js'

function getSrcX(node) {
  return node.x + node.w
}

function getTgtX(node) {
  return node.x
}

function buildSuccs(connections) {
  const succs = {}
  for (const conn of connections) {
    if (!succs[conn.from]) succs[conn.from] = []
    succs[conn.from].push(conn.to)
  }
  return succs
}

/** 下游节点（含目标）的最小 x，连线所有 x 坐标不得大于此值 */
function downstreamMinX(toId, nodes, succs) {
  let minX = nodes[toId]?.x ?? Infinity
  const queue = [toId]
  const visited = new Set([toId])
  while (queue.length) {
    const id = queue.shift()
    for (const next of succs[id] || []) {
      if (visited.has(next)) continue
      visited.add(next)
      minX = Math.min(minX, nodes[next]?.x ?? Infinity)
      queue.push(next)
    }
  }
  return minX
}

function wireXCap(w, nodes, succs) {
  return downstreamMinX(w.to, nodes, succs)
}

function minTurnXFor(w, nodes) {
  return getSrcX(nodes[w.from]) + TURN_OFFSET
}

function maxTurnXFor(w, nodes, succs) {
  const cap = wireXCap(w, nodes, succs)
  const tgtX = getTgtX(nodes[w.to])
  return Math.min(cap, tgtX) - TURN_OFFSET
}

function clampTurnX(w, nodes, succs) {
  if (w.turnX == null) return
  const minX = minTurnXFor(w, nodes)
  const maxX = maxTurnXFor(w, nodes, succs)
  if (maxX < minX) {
    w.turnX = null
    return
  }
  w.turnX = Math.max(minX, Math.min(w.turnX, maxX))
}

function clampAllTurnXs(wires, nodes, succs) {
  for (const w of wires) clampTurnX(w, nodes, succs)
}

function assignConnectionYs(nodes, wires) {
  const outgoing = {}
  const incoming = {}

  for (const w of wires) {
    if (!outgoing[w.from]) outgoing[w.from] = []
    if (!incoming[w.to]) incoming[w.to] = []
    outgoing[w.from].push(w)
    incoming[w.to].push(w)
  }

  for (const list of Object.values(outgoing)) {
    list.sort((a, b) => {
      const dy = (nodes[a.to]?.y ?? 0) - (nodes[b.to]?.y ?? 0)
      return dy !== 0 ? dy : a.to.localeCompare(b.to)
    })
    list.forEach((w, idx) => {
      w.srcY = nodes[w.from].y + CORNER_M + idx * CONN_SPACING
    })
  }

  for (const list of Object.values(incoming)) {
    list.sort((a, b) => {
      const dy = (nodes[a.from]?.y ?? 0) - (nodes[b.from]?.y ?? 0)
      return dy !== 0 ? dy : a.from.localeCompare(b.from)
    })
    list.forEach((w, idx) => {
      w.tgtY = nodes[w.to].y + CORNER_M + idx * CONN_SPACING
    })
  }
}

function computeTurnX(w, nodes, succs) {
  const srcNode = nodes[w.from]
  const tgtNode = nodes[w.to]
  const srcX = getSrcX(srcNode)
  const tgtX = getTgtX(tgtNode)
  const cap = wireXCap(w, nodes, succs)

  if (Math.abs(w.srcY - w.tgtY) < 1) return null

  let turnX = Math.min(tgtX - TURN_OFFSET, cap - TURN_OFFSET)
  const minTurnX = srcX + TURN_OFFSET
  if (turnX < minTurnX) {
    turnX = Math.max(minTurnX, srcX + (Math.min(tgtX, cap) - srcX) / 2)
  }
  if (Math.min(tgtX, cap) - turnX < TURN_OFFSET) turnX = Math.min(tgtX, cap) - TURN_OFFSET
  if (turnX - srcX < TURN_OFFSET) turnX = srcX + TURN_OFFSET
  turnX = Math.min(turnX, cap)
  if (turnX > maxTurnXFor(w, nodes, succs) || turnX < minTurnXFor(w, nodes)) return null
  return turnX
}

function assignInitialTurnXs(wires, nodes, succs) {
  const byTarget = {}
  for (const w of wires) {
    if (!byTarget[w.to]) byTarget[w.to] = []
    byTarget[w.to].push(w)
  }

  for (const list of Object.values(byTarget)) {
    list.sort((a, b) => a.srcY - b.srcY)
    list.forEach((w, idx) => {
      w.turnX = computeTurnX(w, nodes, succs)
      if (w.turnX != null && idx > 0 && list[0].turnX != null) {
        const staggered = list[0].turnX + idx * TURN_STEP
        w.turnX = Math.min(staggered, maxTurnXFor(w, nodes, succs))
      }
      clampTurnX(w, nodes, succs)
    })
  }
}

function getSegments(w, nodes) {
  const srcX = getSrcX(nodes[w.from])
  const tgtX = getTgtX(nodes[w.to])
  const { srcY, tgtY, turnX } = w

  if (turnX == null || Math.abs(srcY - tgtY) < 1) {
    return [{ type: 'H', y: srcY, x1: srcX, x2: tgtX }]
  }
  return [
    { type: 'H', y: srcY, x1: srcX, x2: turnX },
    { type: 'V', x: turnX, y1: Math.min(srcY, tgtY), y2: Math.max(srcY, tgtY) },
    { type: 'H', y: tgtY, x1: turnX, x2: tgtX },
  ]
}

function segmentsOverlap(a, b) {
  if (a.type === 'H' && b.type === 'H') {
    if (Math.abs(a.y - b.y) >= CONN_SPACING) return false
    return Math.max(a.x1, b.x1) < Math.min(a.x2, b.x2)
  }
  if (a.type === 'V' && b.type === 'V') {
    if (Math.abs(a.x - b.x) >= 20) return false
    return Math.max(a.y1, b.y1) < Math.min(a.y2, b.y2)
  }
  if (a.type === 'H' && b.type === 'V') {
    return a.y > b.y1 && a.y < b.y2 && b.x > Math.min(a.x1, a.x2) && b.x < Math.max(a.x1, a.x2)
  }
  if (a.type === 'V' && b.type === 'H') {
    return b.y > a.y1 && b.y < a.y2 && a.x > Math.min(b.x1, b.x2) && a.x < Math.max(b.x1, b.x2)
  }
  return false
}

function resolveSegmentOverlaps(wires, nodes, succs) {
  for (let iter = 0; iter < 200; iter++) {
    let changed = false
    for (let i = 0; i < wires.length; i++) {
      for (let j = i + 1; j < wires.length; j++) {
        const segsA = getSegments(wires[i], nodes)
        const segsB = getSegments(wires[j], nodes)
        for (const sa of segsA) {
          for (const sb of segsB) {
            if (!segmentsOverlap(sa, sb)) continue
            if (sa.type === 'H' && sb.type === 'H') {
              if (wires[i].srcY <= wires[j].srcY) {
                wires[j].srcY += CONN_SPACING
                wires[j].tgtY += CONN_SPACING
              } else {
                wires[i].srcY += CONN_SPACING
                wires[i].tgtY += CONN_SPACING
              }
              changed = true
            } else if (sa.type === 'V' && sb.type === 'V') {
              if ((wires[i].turnX ?? 0) <= (wires[j].turnX ?? 0)) {
                wires[j].turnX = (wires[j].turnX ?? 0) + 20
                clampTurnX(wires[j], nodes, succs)
              } else {
                wires[i].turnX = (wires[i].turnX ?? 0) + 20
                clampTurnX(wires[i], nodes, succs)
              }
              changed = true
            }
          }
        }
      }
    }
    if (!changed) break
  }
}

function enforceTurnXGap(wires, nodes, succs) {
  for (let iter = 0; iter < 50; iter++) {
    const withTurn = wires.filter((w) => w.turnX != null).sort((a, b) => a.turnX - b.turnX)
    let changed = false
    for (let i = 1; i < withTurn.length; i++) {
      const prev = withTurn[i - 1]
      const curr = withTurn[i]
      if (curr.turnX - prev.turnX >= TURN_MIN_GAP) continue

      const desired = prev.turnX + TURN_MIN_GAP
      const currMax = maxTurnXFor(curr, nodes, succs)
      if (desired <= currMax) {
        curr.turnX = desired
        changed = true
        continue
      }

      const shiftedPrev = curr.turnX - TURN_MIN_GAP
      const prevMin = minTurnXFor(prev, nodes)
      const prevMax = maxTurnXFor(prev, nodes, succs)
      if (shiftedPrev >= prevMin && shiftedPrev <= prevMax) {
        prev.turnX = shiftedPrev
        changed = true
      }
    }
    clampAllTurnXs(wires, nodes, succs)
    if (!changed) break
  }
}

function enforceTurnYGap(wires) {
  const corners = []
  for (let i = 0; i < wires.length; i++) {
    const w = wires[i]
    if (w.turnX == null) continue
    corners.push({ wireIdx: i, x: w.turnX, y: w.srcY })
    corners.push({ wireIdx: i, x: w.turnX, y: w.tgtY })
  }
  corners.sort((a, b) => a.y - b.y)

  for (let iter = 0; iter < 50; iter++) {
    let changed = false
    for (let i = 1; i < corners.length; i++) {
      const prev = corners[i - 1]
      const curr = corners[i]
      if (prev.wireIdx === curr.wireIdx) continue
      if (Math.abs(curr.x - prev.x) < TURN_MIN_GAP && Math.abs(curr.y - prev.y) < TURN_MIN_GAP) {
        const w = wires[curr.wireIdx]
        const delta = TURN_MIN_GAP - (curr.y - prev.y)
        w.tgtY += delta
        if (w.srcY === curr.y) w.srcY += delta
        curr.y += delta
        changed = true
      }
    }
    if (!changed) break
  }
}

function getWireXs(w, nodes) {
  const srcX = getSrcX(nodes[w.from])
  const tgtX = getTgtX(nodes[w.to])
  const { turnX } = w
  if (turnX == null || Math.abs(w.srcY - w.tgtY) < 1) {
    return [srcX, tgtX]
  }
  return [srcX, turnX, tgtX]
}

function enforceWireXCap(wires, nodes, succs) {
  for (const w of wires) {
    clampTurnX(w, nodes, succs)
    const cap = wireXCap(w, nodes, succs)
    for (const x of getWireXs(w, nodes)) {
      if (x > cap + 0.001) {
        if (w.turnX != null && w.turnX > cap) w.turnX = maxTurnXFor(w, nodes, succs)
        clampTurnX(w, nodes, succs)
      }
    }
  }
}
function buildPath(w, nodes) {
  const srcX = getSrcX(nodes[w.from])
  const tgtX = getTgtX(nodes[w.to])
  const { srcY, tgtY, turnX } = w
  if (turnX == null || Math.abs(srcY - tgtY) < 1) {
    return `M ${srcX} ${srcY} H ${tgtX}`
  }
  return `M ${srcX} ${srcY} H ${turnX} V ${tgtY} H ${tgtX}`
}

function findCrossings(wires, nodes) {
  const crossings = []
  for (let i = 0; i < wires.length; i++) {
    for (let j = i + 1; j < wires.length; j++) {
      const a = wires[i]
      const b = wires[j]
      if (a.from === b.from || a.to === b.to) continue
      const segsA = getSegments(a, nodes)
      const segsB = getSegments(b, nodes)
      for (const sa of segsA) {
        for (const sb of segsB) {
          if (sa.type === 'H' && sb.type === 'V') {
            if (
              sb.x > Math.min(sa.x1, sa.x2) && sb.x < Math.max(sa.x1, sa.x2) &&
              sa.y > sb.y1 && sa.y < sb.y2
            ) crossings.push({ x: sb.x, y: sa.y })
          } else if (sa.type === 'V' && sb.type === 'H') {
            if (
              sa.x > Math.min(sb.x1, sb.x2) && sa.x < Math.max(sb.x1, sb.x2) &&
              sb.y > sa.y1 && sb.y < sa.y2
            ) crossings.push({ x: sa.x, y: sb.y })
          }
        }
      }
    }
  }
  return crossings
}

export function routeWiresV1(nodes, connections) {
  const succs = buildSuccs(connections)
  const wires = connections.map((c) => ({ ...c }))
  wires.sort((a, b) => {
    const sy = (nodes[a.from]?.y ?? 0) - (nodes[b.from]?.y ?? 0)
    if (sy !== 0) return sy
    return (nodes[a.to]?.y ?? 0) - (nodes[b.to]?.y ?? 0)
  })

  assignConnectionYs(nodes, wires)
  assignInitialTurnXs(wires, nodes, succs)
  resolveSegmentOverlaps(wires, nodes, succs)
  enforceTurnXGap(wires, nodes, succs)
  enforceTurnYGap(wires)
  resolveSegmentOverlaps(wires, nodes, succs)
  enforceWireXCap(wires, nodes, succs)

  const routed = wires.map((w) => ({
    ...w,
    path: buildPath(w, nodes),
    srcX: getSrcX(nodes[w.from]),
    tgtX: getTgtX(nodes[w.to]),
  }))

  return { wires: routed, crossings: findCrossings(wires, nodes) }
}

export { findCrossings }
