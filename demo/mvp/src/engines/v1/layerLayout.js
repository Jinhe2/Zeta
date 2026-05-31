import {
  COL_MARGIN,
  NODE_GAP,
  MIN_V_GAP,
} from './constants.js'
import { applyNodeSize, computeBounds } from './nodeSizing.js'
import {
  assignLayers,
  buildAdjacency,
  computeLeafOrder,
  findReachableFromOutputs,
  groupByLayer,
  minimizeLeafCrossings,
} from './graph.js'

function barycenter(id, nodes, refs) {
  if (!refs.length) return nodes[id].y + nodes[id].h / 2
  return refs.reduce((s, n) => s + n.y + n.h / 2, 0) / refs.length
}

function sortLayerByBarycenter(layerIds, nodes, preds, succs, direction) {
  layerIds.sort((a, b) => {
    const refNodes = (id) => {
      if (direction === 'pred') {
        return (preds[id] || []).map((c) => nodes[c.from]).filter(Boolean)
      }
      return (succs[id] || []).map((toId) => nodes[toId]).filter(Boolean)
    }
    return barycenter(a, nodes, refNodes(a)) - barycenter(b, nodes, refNodes(b))
  })
}

function assignColumnY(layerIds, nodes, startY = 40) {
  let y = startY
  for (const id of layerIds) {
    nodes[id].y = y
    y += nodes[id].h + NODE_GAP
  }
  return y
}

function centerOnPredecessors(id, nodes, preds) {
  const ps = preds[id] || []
  if (!ps.length) return
  const avg = ps.reduce((s, c) => s + nodes[c.from].y + nodes[c.from].h / 2, 0) / ps.length
  nodes[id].y = avg - nodes[id].h / 2
}

function fixColumnOverlap(layerIds, nodes) {
  if (!layerIds?.length) return
  layerIds.sort((a, b) => nodes[a].y - nodes[b].y)
  for (let i = 1; i < layerIds.length; i++) {
    const prev = nodes[layerIds[i - 1]]
    const curr = nodes[layerIds[i]]
    const minY = prev.y + prev.h + NODE_GAP
    if (curr.y < minY) curr.y = minY
  }
}

function xRangesOverlap(a, b) {
  return a.x < b.x + b.w && b.x < a.x + a.w
}

function fixCrossColumnCollisions(columns, nodes) {
  const layerKeys = Object.keys(columns).map(Number).sort((a, b) => a - b)
  for (let i = 0; i < layerKeys.length; i++) {
    for (let j = i + 1; j < layerKeys.length; j++) {
      for (const idA of columns[layerKeys[i]]) {
        for (const idB of columns[layerKeys[j]]) {
          const a = nodes[idA]
          const b = nodes[idB]
          if (!xRangesOverlap(a, b)) continue
          const aBottom = a.y + a.h + NODE_GAP
          const bBottom = b.y + b.h + NODE_GAP
          if (a.y < bBottom && b.y < aBottom) {
            if (a.y <= b.y) b.y = aBottom
            else a.y = bBottom
          }
        }
      }
    }
  }
}

function countWiresBetween(colA, colB, connections) {
  const setA = new Set(colA)
  const setB = new Set(colB)
  let count = 0
  for (const c of connections) {
    if ((setA.has(c.from) && setB.has(c.to)) || (setB.has(c.from) && setA.has(c.to))) count++
  }
  return count
}

function assignColumnX(columns, nodes, connections) {
  const layerKeys = Object.keys(columns).map(Number).sort((a, b) => a - b)
  let x = 0

  for (let i = 0; i < layerKeys.length; i++) {
    const layer = layerKeys[i]
    const colIds = columns[layer]
    const maxW = Math.max(...colIds.map((id) => nodes[id].w))

    for (const id of colIds) nodes[id].x = x

    if (i < layerKeys.length - 1) {
      const nextLayer = layerKeys[i + 1]
      const wireCount = countWiresBetween(colIds, columns[nextLayer], connections)
      const margin = Math.max(COL_MARGIN, COL_MARGIN + wireCount * 12)
      x += maxW + margin
    }
  }
}

function handleIsolated(config, nodes, connections, preds, layers, columns, reachable) {
  const isolated = Object.keys(nodes).filter((id) => !reachable.has(id))
  if (!isolated.length) return

  let maxY = Math.max(...Object.values(nodes).map((n) => n.y + n.h), 0) + MIN_V_GAP * 2
  for (const id of isolated) {
    const layer = layers[id]
    if (!columns[layer]) columns[layer] = []
    if (!columns[layer].includes(id)) columns[layer].push(id)
    nodes[id].y = maxY
    maxY += nodes[id].h + NODE_GAP
  }
}

/**
 * Sugiyama 风格分层布局：
 * 1. DAG 拓扑最长路径分层
 * 2. 叶节点序初始化
 * 3. 多轮前向/后向重心传播排序
 * 4. 同列/跨列碰撞修复
 */
export function computeLayerLayout(config, nodes, connections) {
  const nodeIds = Object.keys(nodes)
  const { preds, succs } = buildAdjacency(connections)
  const inputIds = (config.inputs || []).map((i) => i.id)
  const outputIds = (config.outputs || []).map((o) => o.id)

  for (const id of nodeIds) applyNodeSize(nodes[id], connections)

  const layers = assignLayers(nodeIds, preds, inputIds)
  const columns = groupByLayer(nodeIds, layers)
  const maxLayer = Math.max(...Object.values(layers), 0)

  let leafOrder = computeLeafOrder(config, preds)
  leafOrder = minimizeLeafCrossings(leafOrder, connections)

  const layer0Ids = [...(columns[0] || [])]
  layer0Ids.sort((a, b) => {
    const ia = leafOrder.indexOf(a)
    const ib = leafOrder.indexOf(b)
    return (ia === -1 ? 999 : ia) - (ib === -1 ? 999 : ib)
  })
  columns[0] = layer0Ids
  assignColumnY(layer0Ids, nodes)

  const PROPAGATION_ROUNDS = 14
  for (let round = 0; round < PROPAGATION_ROUNDS; round++) {
    for (let L = 1; L <= maxLayer; L++) {
      if (!columns[L]?.length) continue
      sortLayerByBarycenter(columns[L], nodes, preds, succs, 'pred')
      assignColumnY(columns[L], nodes)
    }
    for (let L = maxLayer - 1; L >= 0; L--) {
      if (!columns[L]?.length) continue
      sortLayerByBarycenter(columns[L], nodes, preds, succs, 'succ')
      assignColumnY(columns[L], nodes)
    }
  }

  for (let L = 1; L <= maxLayer; L++) {
    for (const id of columns[L] || []) centerOnPredecessors(id, nodes, preds)
    fixColumnOverlap(columns[L], nodes)
  }

  for (let pass = 0; pass < 6; pass++) {
    for (let L = 0; L <= maxLayer; L++) fixColumnOverlap(columns[L], nodes)
    fixCrossColumnCollisions(columns, nodes)
  }

  for (let L = 0; L <= maxLayer; L++) fixColumnOverlap(columns[L], nodes)

  assignColumnX(columns, nodes, connections)

  const reachable = findReachableFromOutputs(outputIds, preds)
  handleIsolated(config, nodes, connections, preds, layers, columns, reachable)

  for (let L = 0; L <= maxLayer; L++) fixColumnOverlap(columns[L], nodes)

  for (const id of nodeIds) {
    nodes[id].layer = layers[id]
  }

  return {
    nodes,
    layers,
    columns,
    preds,
    succs,
    connections,
    bounds: computeBounds(nodes),
  }
}
