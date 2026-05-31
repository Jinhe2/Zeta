import {
  CORNER_M,
  CONN_SPACING,
  GATE_SMALL_HEIGHT,
  GATE_WIDTH,
  INPUT_HEIGHT,
  INPUT_WIDTH,
  INPUT_WIDE_WIDTH,
  MIN_V_GAP,
  NODE_GAP,
  OUTPUT_HEIGHT,
  OUTPUT_WIDTH,
  TIMER_HEIGHT,
  TIMER_WIDTH,
  WIDE_LABEL_LEN,
  COL_MARGIN,
} from './constants'

function getBaseWidth(node) {
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

function getBaseHeight(node) {
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

function assignLayers(config, connections) {
  const layers = {}
  const inEdges = {}

  for (const conn of connections) {
    if (!inEdges[conn.to]) inEdges[conn.to] = []
    inEdges[conn.to].push(conn)
  }

  for (const inp of config.inputs) layers[inp.id] = 0

  const allIds = [
    ...config.inputs.map((n) => n.id),
    ...config.gates.map((n) => n.id),
    ...config.timers.map((n) => n.id),
    ...config.outputs.map((n) => n.id),
  ]

  let changed = true
  while (changed) {
    changed = false
    for (const id of allIds) {
      if (layers[id] != null) continue
      const preds = inEdges[id] || []
      if (preds.length === 0) {
        layers[id] = 0
        changed = true
        continue
      }
      const predLayers = preds.map((c) => layers[c.from]).filter((l) => l != null)
      if (predLayers.length === preds.length) {
        layers[id] = Math.max(...predLayers) + 1
        changed = true
      }
    }
  }

  for (const id of allIds) {
    if (layers[id] == null) layers[id] = 0
  }

  return layers
}

function computeLeafOrder(config, connections) {
  const order = []
  const visited = new Set()
  const inEdges = {}
  for (const conn of connections) {
    if (!inEdges[conn.to]) inEdges[conn.to] = []
    inEdges[conn.to].push(conn)
  }

  function dfs(nodeId) {
    if (visited.has(nodeId)) return
    visited.add(nodeId)
    const edges = (inEdges[nodeId] || []).slice().sort((a, b) => a.toInputIndex - b.toInputIndex)
    for (const conn of edges) dfs(conn.from)
    order.push(nodeId)
  }

  for (const out of config.outputs) dfs(out.id)
  for (const inp of config.inputs) {
    if (!visited.has(inp.id)) order.push(inp.id)
  }

  const inputSet = new Set(config.inputs.map((i) => i.id))
  return order.filter((id) => inputSet.has(id))
}

function groupByLayer(nodes, layers) {
  const columns = {}
  for (const [id, layer] of Object.entries(layers)) {
    if (!nodes[id]) continue
    if (!columns[layer]) columns[layer] = []
    columns[layer].push(id)
  }
  return columns
}

function countConnections(nodeId, connections) {
  let inCount = 0
  let outCount = 0
  for (const c of connections) {
    if (c.from === nodeId) outCount++
    if (c.to === nodeId) inCount++
  }
  return Math.max(inCount, outCount, 1)
}

function computeNodeSize(node, connections) {
  const w = getBaseWidth(node)
  const baseH = getBaseHeight(node)
  const connCount = countConnections(node.id, connections)
  const h = Math.max(baseH, CORNER_M * 2 + (connCount - 1) * CONN_SPACING)
  return { w, h, connCount }
}

function sortColumnNodes(colIds, nodes, connections, leafOrder, layer) {
  if (layer === 0) {
    const orderMap = Object.fromEntries(leafOrder.map((id, i) => [id, i]))
    colIds.sort((a, b) => (orderMap[a] ?? 999) - (orderMap[b] ?? 999))
    return
  }

  const inEdges = {}
  for (const conn of connections) {
    if (!inEdges[conn.to]) inEdges[conn.to] = []
    inEdges[conn.to].push(conn)
  }

  colIds.sort((a, b) => {
    const avgY = (id) => {
      const preds = (inEdges[id] || []).map((c) => nodes[c.from]?.y ?? 0)
      return preds.length ? preds.reduce((s, v) => s + v, 0) / preds.length : 0
    }
    return avgY(a) - avgY(b)
  })
}

function placeColumn(colIds, nodes, connections, startY) {
  let y = startY
  for (const id of colIds) {
    const size = computeNodeSize(nodes[id], connections)
    nodes[id].w = size.w
    nodes[id].h = size.h
    nodes[id].x = 0
    nodes[id].y = y
    y += size.h + NODE_GAP
  }
  return y
}

function fixSameColumnOverlap(columns, nodes) {
  for (const colIds of Object.values(columns)) {
    colIds.sort((a, b) => nodes[a].y - nodes[b].y)
    for (let i = 1; i < colIds.length; i++) {
      const prev = nodes[colIds[i - 1]]
      const curr = nodes[colIds[i]]
      const minY = prev.y + prev.h + NODE_GAP
      if (curr.y < minY) curr.y = minY
    }
  }
}

function xRangesOverlap(a, b) {
  return a.x < b.x + b.w && b.x < a.x + a.w
}

function fixCrossColumnOverlap(columns, nodes, layers) {
  const sortedLayers = Object.keys(columns)
    .map(Number)
    .sort((a, b) => a - b)

  for (let li = 0; li < sortedLayers.length - 1; li++) {
    for (let lj = li + 1; lj < sortedLayers.length; lj++) {
      const colA = columns[sortedLayers[li]]
      const colB = columns[sortedLayers[lj]]
      for (const idA of colA) {
        for (const idB of colB) {
          const a = nodes[idA]
          const b = nodes[idB]
          if (!xRangesOverlap(a, b)) continue
          if (a.y + a.h + NODE_GAP > b.y && b.y + b.h + NODE_GAP > a.y) {
            if (a.y <= b.y) {
              b.y = a.y + a.h + NODE_GAP
            } else {
              a.y = b.y + b.h + NODE_GAP
            }
          }
        }
      }
    }
  }
}

function countWiresBetween(colA, colB, connections) {
  let count = 0
  const setA = new Set(colA)
  const setB = new Set(colB)
  for (const c of connections) {
    if ((setA.has(c.from) && setB.has(c.to)) || (setB.has(c.from) && setA.has(c.to))) count++
  }
  return count
}

function computeColumnXs(columns, nodes, connections) {
  const sortedLayers = Object.keys(columns)
    .map(Number)
    .sort((a, b) => a - b)

  const colXs = {}
  let x = 0

  for (let i = 0; i < sortedLayers.length; i++) {
    const layer = sortedLayers[i]
    colXs[layer] = x
    const colIds = columns[layer]
    const maxW = Math.max(...colIds.map((id) => nodes[id].w))

    if (i < sortedLayers.length - 1) {
      const nextLayer = sortedLayers[i + 1]
      const wireCount = countWiresBetween(colIds, columns[nextLayer], connections)
      const margin = Math.max(COL_MARGIN, COL_MARGIN + wireCount * 12)
      x += maxW + margin
    }
  }

  return colXs
}

function applyColumnX(columns, nodes, colXs, fractionalLayers) {
  for (const [layerStr, colIds] of Object.entries(columns)) {
    const layer = Number(layerStr)
    const x = colXs[layer] ?? 0
    for (const id of colIds) {
      nodes[id].x = x + (fractionalLayers[id] ?? 0) * 40
    }
  }
}

function findReachableFromOutputs(config, connections) {
  const reachable = new Set(config.outputs.map((o) => o.id))
  let changed = true
  while (changed) {
    changed = false
    for (const c of connections) {
      if (reachable.has(c.to) && !reachable.has(c.from)) {
        reachable.add(c.from)
        changed = true
      }
    }
  }
  return reachable
}

function repositionInternalNodes(columns, nodes, connections, layers) {
  const inEdges = {}
  for (const conn of connections) {
    if (!inEdges[conn.to]) inEdges[conn.to] = []
    inEdges[conn.to].push(conn)
  }

  const sortedLayers = Object.keys(columns)
    .map(Number)
    .sort((a, b) => a - b)
    .filter((l) => l > 0)

  for (const layer of sortedLayers) {
    for (const id of columns[layer]) {
      if (nodes[id].kind === 'input') continue
      const preds = inEdges[id] || []
      if (!preds.length) continue
      const avgCenter =
        preds.reduce((s, c) => s + nodes[c.from].y + nodes[c.from].h / 2, 0) / preds.length
      nodes[id].y = avgCenter - nodes[id].h / 2
    }
  }
}

function minimizeLeafCrossings(leafOrder, connections, nodes) {
  if (leafOrder.length < 3) return leafOrder

  function countCrossings(order) {
    const pos = Object.fromEntries(order.map((id, i) => [id, i]))
    let cross = 0
    for (let i = 0; i < connections.length; i++) {
      for (let j = i + 1; j < connections.length; j++) {
        const a = connections[i]
        const b = connections[j]
        if (a.from === b.from || a.to === b.to) continue
        const p1 = pos[a.from]
        const p2 = pos[b.from]
        const q1 = pos[a.to]
        const q2 = pos[b.to]
        if (p1 == null || p2 == null || q1 == null || q2 == null) continue
        if ((p1 - p2) * (q1 - q2) < 0) cross++
      }
    }
    return cross
  }

  let best = [...leafOrder]
  let bestCross = countCrossings(best)

  for (let i = 0; i < best.length - 1; i++) {
    const swapped = [...best]
    ;[swapped[i], swapped[i + 1]] = [swapped[i + 1], swapped[i]]
    const cross = countCrossings(swapped)
    if (cross < bestCross) {
      best = swapped
      bestCross = cross
    }
  }

  return best
}

export function computeLayout(config, nodes, connections) {
  const layers = assignLayers(config, connections)
  let leafOrder = computeLeafOrder(config, connections)
  leafOrder = minimizeLeafCrossings(leafOrder, connections, nodes)

  const columns = groupByLayer(nodes, layers)

  for (const id of Object.keys(nodes)) {
    const size = computeNodeSize(nodes[id], connections)
    nodes[id].w = size.w
    nodes[id].h = size.h
  }

  const layer0 = columns[0] || []
  sortColumnNodes(layer0, nodes, connections, leafOrder, 0)
  placeColumn(layer0, nodes, connections, 40)

  const sortedLayers = Object.keys(columns)
    .map(Number)
    .sort((a, b) => a - b)
    .filter((l) => l > 0)

  for (const layer of sortedLayers) {
    sortColumnNodes(columns[layer], nodes, connections, leafOrder, layer)
    for (const id of columns[layer]) {
      if (nodes[id].y == null || Number.isNaN(nodes[id].y)) {
        nodes[id].y = 40
      }
    }
    repositionInternalNodes(columns, nodes, connections, layers)
  }

  for (let iter = 0; iter < 8; iter++) {
    repositionInternalNodes(columns, nodes, connections, layers)
    fixSameColumnOverlap(columns, nodes)
    fixCrossColumnOverlap(columns, nodes, layers)
  }

  fixSameColumnOverlap(columns, nodes)

  const reachable = findReachableFromOutputs(config, connections)
  const isolated = Object.keys(nodes).filter((id) => !reachable.has(id))
  if (isolated.length) {
    let maxY = Math.max(...Object.values(nodes).map((n) => n.y + n.h), 0) + MIN_V_GAP * 2
    for (const id of isolated) {
      const layer = layers[id]
      if (!columns[layer]) columns[layer] = []
      if (!columns[layer].includes(id)) columns[layer].push(id)
      nodes[id].y = maxY
      maxY += nodes[id].h + NODE_GAP
    }
    fixSameColumnOverlap(columns, nodes)
  }

  const colXs = computeColumnXs(columns, nodes, connections)
  applyColumnX(columns, nodes, colXs, {})

  const xs = Object.values(nodes).map((n) => (n.x ?? 0) + (n.w ?? 0))
  const ys = Object.values(nodes).map((n) => (n.y ?? 0) + (n.h ?? 0))
  const bounds = {
    width: (xs.length ? Math.max(...xs) : 400) + 80,
    height: (ys.length ? Math.max(...ys) : 300) + 80,
  }

  return { nodes, connections, columns, layers, bounds }
}
