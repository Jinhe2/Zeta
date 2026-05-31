import {
  CORNER_M,
  CONN_SPACING,
  TURN_OFFSET,
  TURN_STEP,
  TURN_MIN_GAP,
  BRIDGE_R,
} from './constants'

function getSrcX(node) {
  return node.x + node.w
}

function getTgtX(node) {
  return node.x
}

function assignConnectionYs(nodes, connections) {
  const outgoing = {}
  const incoming = {}

  for (const conn of connections) {
    if (!outgoing[conn.from]) outgoing[conn.from] = []
    if (!incoming[conn.to]) incoming[conn.to] = []
    outgoing[conn.from].push(conn)
    incoming[conn.to].push(conn)
  }

  for (const conns of Object.values(outgoing)) {
    conns.sort((a, b) => {
      const dy = (nodes[a.to]?.y ?? 0) - (nodes[b.to]?.y ?? 0)
      return dy !== 0 ? dy : a.to.localeCompare(b.to)
    })
    conns.forEach((conn, idx) => {
      const node = nodes[conn.from]
      conn.srcY = node.y + CORNER_M + idx * CONN_SPACING
    })
  }

  for (const conns of Object.values(incoming)) {
    conns.sort((a, b) => {
      const dy = (nodes[a.from]?.y ?? 0) - (nodes[b.from]?.y ?? 0)
      return dy !== 0 ? dy : a.from.localeCompare(b.from)
    })
    conns.forEach((conn, idx) => {
      const node = nodes[conn.to]
      conn.tgtY = node.y + CORNER_M + idx * CONN_SPACING
    })
  }
}

function initialTurnX(conn, nodes) {
  const srcNode = nodes[conn.from]
  const tgtNode = nodes[conn.to]
  const srcX = getSrcX(srcNode)
  const tgtX = getTgtX(tgtNode)

  if (Math.abs(conn.srcY - conn.tgtY) < 1) {
    return null
  }

  let turnX = tgtX - TURN_OFFSET
  const minTurnX = srcX + TURN_OFFSET
  if (turnX < minTurnX) {
    turnX = Math.max(minTurnX, srcX + (tgtX - srcX) / 2)
  }
  if (tgtX - turnX < TURN_OFFSET) turnX = tgtX - TURN_OFFSET
  if (turnX - srcX < TURN_OFFSET) turnX = srcX + TURN_OFFSET

  return turnX
}

function assignTurnXs(connections, nodes) {
  const byTarget = {}
  for (const conn of connections) {
    if (!byTarget[conn.to]) byTarget[conn.to] = []
    byTarget[conn.to].push(conn)
  }

  for (const conns of Object.values(byTarget)) {
    conns.sort((a, b) => a.srcY - b.srcY)
    let baseTurn = null
    conns.forEach((conn, idx) => {
      conn.turnX = initialTurnX(conn, nodes)
      if (conn.turnX == null) return
      if (baseTurn == null) {
        baseTurn = conn.turnX
      } else {
        conn.turnX = baseTurn + idx * TURN_STEP
      }
    })
  }

  for (let iter = 0; iter < 50; iter++) {
    const turnXs = connections.map((c) => c.turnX).filter((x) => x != null).sort((a, b) => a - b)
    let changed = false
    for (let i = 1; i < turnXs.length; i++) {
      if (turnXs[i] - turnXs[i - 1] < TURN_MIN_GAP) {
        turnXs[i] = turnXs[i - 1] + TURN_MIN_GAP
        changed = true
      }
    }
    if (!changed) break
    let ti = 0
    for (const conn of connections) {
      if (conn.turnX != null) conn.turnX = turnXs[ti++]
    }
  }
}

function getSegments(conn, nodes) {
  const srcNode = nodes[conn.from]
  const tgtNode = nodes[conn.to]
  const srcX = getSrcX(srcNode)
  const tgtX = getTgtX(tgtNode)
  const { srcY, tgtY, turnX } = conn

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
    return a.y >= b.y1 && a.y <= b.y2 && b.x >= Math.min(a.x1, a.x2) && b.x <= Math.max(a.x1, a.x2)
  }
  if (a.type === 'V' && b.type === 'H') {
    return b.y >= a.y1 && b.y <= a.y2 && a.x >= Math.min(b.x1, b.x2) && a.x <= Math.max(b.x1, b.x2)
  }
  return false
}

function resolveOverlaps(connections, nodes) {
  for (let iter = 0; iter < 200; iter++) {
    let changed = false
    for (let i = 0; i < connections.length; i++) {
      for (let j = i + 1; j < connections.length; j++) {
        const segsA = getSegments(connections[i], nodes)
        const segsB = getSegments(connections[j], nodes)
        for (const sa of segsA) {
          for (const sb of segsB) {
            if (!segmentsOverlap(sa, sb)) continue
            if (sa.type === 'H' && sb.type === 'H') {
              if (connections[i].srcY <= connections[j].srcY) {
                connections[j].srcY += CONN_SPACING
                connections[j].tgtY += CONN_SPACING
              } else {
                connections[i].srcY += CONN_SPACING
                connections[i].tgtY += CONN_SPACING
              }
              changed = true
            } else if (sa.type === 'V' && sb.type === 'V') {
              const ci = connections[i].turnX ?? 0
              const cj = connections[j].turnX ?? 0
              if (ci <= cj) connections[j].turnX = cj + 20
              else connections[i].turnX = ci + 20
              changed = true
            }
          }
        }
      }
    }
    if (!changed) break
  }
}

function buildPath(conn, nodes) {
  const srcNode = nodes[conn.from]
  const tgtNode = nodes[conn.to]
  const srcX = getSrcX(srcNode)
  const tgtX = getTgtX(tgtNode)
  const { srcY, tgtY, turnX } = conn

  if (turnX == null || Math.abs(srcY - tgtY) < 1) {
    return `M ${srcX} ${srcY} H ${tgtX}`
  }

  return `M ${srcX} ${srcY} H ${turnX} V ${tgtY} H ${tgtX}`
}

function findCrossings(connections, nodes) {
  const crossings = []
  for (let i = 0; i < connections.length; i++) {
    for (let j = i + 1; j < connections.length; j++) {
      const a = connections[i]
      const b = connections[j]
      if (a.from === b.from || a.to === b.to) continue

      const segsA = getSegments(a, nodes)
      const segsB = getSegments(b, nodes)
      for (const sa of segsA) {
        for (const sb of segsB) {
          if (sa.type === 'H' && sb.type === 'V') {
            if (
              sb.x > Math.min(sa.x1, sa.x2) &&
              sb.x < Math.max(sa.x1, sa.x2) &&
              sa.y > sb.y1 &&
              sa.y < sb.y2
            ) {
              crossings.push({ x: sb.x, y: sa.y, i, j })
            }
          } else if (sa.type === 'V' && sb.type === 'H') {
            if (
              sa.x > Math.min(sb.x1, sb.x2) &&
              sa.x < Math.max(sb.x1, sb.x2) &&
              sb.y > sa.y1 &&
              sb.y < sa.y2
            ) {
              crossings.push({ x: sa.x, y: sb.y, i, j })
            }
          }
        }
      }
    }
  }
  return crossings
}

export function routeWires(nodes, connections) {
  const wired = connections.map((c) => ({ ...c }))
  wired.sort((a, b) => {
    const sy = (nodes[a.from]?.y ?? 0) - (nodes[b.from]?.y ?? 0)
    if (sy !== 0) return sy
    return (nodes[a.to]?.y ?? 0) - (nodes[b.to]?.y ?? 0)
  })

  assignConnectionYs(nodes, wired)
  assignTurnXs(wired, nodes)
  resolveOverlaps(wired, nodes)

  const crossings = findCrossings(wired, nodes)

  const routed = wired.map((conn) => ({
    ...conn,
    path: buildPath(conn, nodes),
    srcX: getSrcX(nodes[conn.from]),
    tgtX: getTgtX(nodes[conn.to]),
  }))

  return { wires: routed, crossings }
}

export { BRIDGE_R, findCrossings }
