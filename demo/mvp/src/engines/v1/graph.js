/**
 * DAG 图构建、拓扑分层、叶节点序
 */
export function buildAdjacency(connections) {
  const preds = {}
  const succs = {}

  for (const conn of connections) {
    if (!preds[conn.to]) preds[conn.to] = []
    preds[conn.to].push(conn)
    if (!succs[conn.from]) succs[conn.from] = []
    succs[conn.from].push(conn.to)
  }

  for (const key of Object.keys(preds)) {
    preds[key].sort((a, b) => a.toInputIndex - b.toInputIndex)
  }

  return { preds, succs }
}

/** 最长路径分层：layer = 1 + max(前驱 layer)，输入节点固定为 0 */
export function assignLayers(nodeIds, preds, inputIds) {
  const layers = {}
  const inputSet = new Set(inputIds)

  for (const id of inputIds) layers[id] = 0

  for (const id of nodeIds) {
    if (!inputSet.has(id) && layers[id] == null) layers[id] = -1
  }

  let changed = true
  let guard = 0
  while (changed && guard++ < nodeIds.length + 2) {
    changed = false
    for (const id of nodeIds) {
      if (inputSet.has(id)) continue
      const ps = preds[id] || []
      if (!ps.length) {
        if (layers[id] !== 0) {
          layers[id] = 0
          changed = true
        }
        continue
      }
      const predLayers = ps.map((c) => layers[c.from]).filter((l) => l != null && l >= 0)
      if (predLayers.length < ps.length) continue
      const next = Math.max(...predLayers) + 1
      if (layers[id] !== next) {
        layers[id] = next
        changed = true
      }
    }
  }

  for (const id of nodeIds) {
    if (layers[id] == null || layers[id] < 0) layers[id] = 0
  }

  return layers
}

/** 从 output 反向 DFS，按 toInputIndex 确定叶节点顺序 */
export function computeLeafOrder(config, preds) {
  const order = []
  const visited = new Set()

  function dfs(nodeId) {
    if (visited.has(nodeId)) return
    visited.add(nodeId)
    for (const conn of preds[nodeId] || []) dfs(conn.from)
    order.push(nodeId)
  }

  for (const out of config.outputs || []) dfs(out.id)

  const inputSet = new Set((config.inputs || []).map((i) => i.id))
  return order.filter((id) => inputSet.has(id))
}

/** 从 output 反向标记可达节点 */
export function findReachableFromOutputs(outputIds, preds) {
  const reachable = new Set(outputIds)
  let changed = true
  while (changed) {
    changed = false
    for (const [to, edges] of Object.entries(preds)) {
      if (!reachable.has(to)) continue
      for (const e of edges) {
        if (!reachable.has(e.from)) {
          reachable.add(e.from)
          changed = true
        }
      }
    }
  }
  return reachable
}

export function groupByLayer(nodeIds, layers) {
  const columns = {}
  for (const id of nodeIds) {
    const layer = layers[id] ?? 0
    if (!columns[layer]) columns[layer] = []
    columns[layer].push(id)
  }
  return columns
}

export function countCrossings(layerOrder, connections) {
  const pos = Object.fromEntries(layerOrder.map((id, i) => [id, i]))
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

export function minimizeLeafCrossings(leafOrder, connections) {
  if (leafOrder.length < 3) return leafOrder
  let best = [...leafOrder]
  let bestCross = countCrossings(best, connections)

  for (let pass = 0; pass < 3; pass++) {
    let improved = false
    for (let i = 0; i < best.length - 1; i++) {
      const swapped = [...best]
      ;[swapped[i], swapped[i + 1]] = [swapped[i + 1], swapped[i]]
      const cross = countCrossings(swapped, connections)
      if (cross < bestCross) {
        best = swapped
        bestCross = cross
        improved = true
      }
    }
    if (!improved) break
  }
  return best
}
