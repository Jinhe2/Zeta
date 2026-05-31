import {
  CORNER_M,
  CONN_SPACING,
  GATE_SMALL_HEIGHT,
  GATE_WIDTH,
  INPUT_HEIGHT,
  INPUT_WIDTH,
  INPUT_WIDE_WIDTH,
  OUTPUT_HEIGHT,
  OUTPUT_WIDTH,
  TIMER_HEIGHT,
  TIMER_WIDTH,
  WIDE_LABEL_LEN,
} from './constants.js'

export function countConnections(nodeId, connections) {
  let inCount = 0
  let outCount = 0
  for (const c of connections) {
    if (c.from === nodeId) outCount++
    if (c.to === nodeId) inCount++
  }
  return Math.max(inCount, outCount, 1)
}

export function getBaseWidth(node) {
  switch (node.kind) {
    case 'input':
      return (node.name?.length ?? 0) > WIDE_LABEL_LEN ? INPUT_WIDE_WIDTH : INPUT_WIDTH
    case 'gate':
      return GATE_WIDTH
    case 'timer':
      return TIMER_WIDTH
    case 'output':
      return OUTPUT_WIDTH
    default:
      return INPUT_WIDTH
  }
}

export function getBaseHeight(node) {
  switch (node.kind) {
    case 'input':
      return INPUT_HEIGHT
    case 'gate':
      return GATE_SMALL_HEIGHT
    case 'timer':
      return TIMER_HEIGHT
    case 'output':
      return OUTPUT_HEIGHT
    default:
      return INPUT_HEIGHT
  }
}

export function applyNodeSize(node, connections) {
  const w = getBaseWidth(node)
  const baseH = getBaseHeight(node)
  const connCount = countConnections(node.id, connections)
  const h = Math.max(baseH, CORNER_M * 2 + (connCount - 1) * CONN_SPACING)
  node.w = w
  node.h = h
  node.connCount = connCount
}

export function computeBounds(nodes, padding = 80) {
  const xs = Object.values(nodes).map((n) => (n.x ?? 0) + (n.w ?? 0))
  const ys = Object.values(nodes).map((n) => (n.y ?? 0) + (n.h ?? 0))
  return {
    width: (xs.length ? Math.max(...xs) : 400) + padding,
    height: (ys.length ? Math.max(...ys) : 300) + padding,
  }
}
