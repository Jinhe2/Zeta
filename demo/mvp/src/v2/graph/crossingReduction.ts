import type { BuiltGraph, LayerAssignment } from './types'

const ITERATIONS = 4

/**
 * 计算节点在排序中的重心（Barycenter）：
 * 取所有已排序父节点在当前层序列中的索引平均值。
 */
function barycenter(
  nodeId: string,
  graph: BuiltGraph,
  layerOrder: Map<number, string[]>,
): number {
  const parents = graph.inEdges.get(nodeId) ?? []
  if (parents.length === 0) return 0

  let sum = 0
  let count = 0
  for (const edge of parents) {
    for (const [layer, ids] of layerOrder) {
      const idx = ids.indexOf(edge.source)
      if (idx >= 0) {
        sum += idx
        count++
        break
      }
    }
  }
  return count > 0 ? sum / count : 0
}

/** 按重心排序一层节点（稳定排序） */
function sortLayerByBarycenter(
  layer: number,
  layerOrder: Map<number, string[]>,
  graph: BuiltGraph,
  useParents: boolean,
): void {
  const ids = layerOrder.get(layer)
  if (!ids || ids.length <= 1) return

  const scored = ids.map((id, index) => ({
    id,
    index,
    weight: useParents
      ? barycenter(id, graph, layerOrder)
      : forwardBarycenter(id, graph, layerOrder),
  }))

  scored.sort((a, b) => {
    if (a.weight !== b.weight) return a.weight - b.weight
    return a.index - b.index
  })

  layerOrder.set(
    layer,
    scored.map((s) => s.id),
  )
}

/** 反向扫描：按子节点重心排序（用于自下而上优化） */
function forwardBarycenter(
  nodeId: string,
  graph: BuiltGraph,
  layerOrder: Map<number, string[]>,
): number {
  const children = graph.outEdges.get(nodeId) ?? []
  if (children.length === 0) return 0

  let sum = 0
  let count = 0
  for (const edge of children) {
    for (const [, ids] of layerOrder) {
      const idx = ids.indexOf(edge.target)
      if (idx >= 0) {
        sum += idx
        count++
        break
      }
    }
  }
  return count > 0 ? sum / count : 0
}

/**
 * Barycenter Crossing Reduction（Sugiyama 交叉最小化）：
 * 自上而下 + 自下而上交替，至少 3 轮迭代。
 */
export function reduceCrossings(
  graph: BuiltGraph,
  assignment: LayerAssignment,
): Map<number, string[]> {
  const layerOrder = new Map<number, string[]>()

  for (let L = 0; L <= assignment.maxLayer; L++) {
    layerOrder.set(L, [...(assignment.layerNodes.get(L) ?? [])])
  }

  for (let iter = 0; iter < ITERATIONS; iter++) {
    for (let L = 1; L <= assignment.maxLayer; L++) {
      sortLayerByBarycenter(L, layerOrder, graph, true)
    }
    for (let L = assignment.maxLayer - 1; L >= 0; L--) {
      sortLayerByBarycenter(L, layerOrder, graph, false)
    }
  }

  return layerOrder
}

/**
 * 统计两层之间边的交叉数（用于调试/指标）。
 */
export function countCrossings(
  graph: BuiltGraph,
  layerOrder: Map<number, string[]>,
): number {
  let crossings = 0
  const layers = [...layerOrder.keys()].sort((a, b) => a - b)

  for (let i = 0; i < layers.length - 1; i++) {
    const left = layerOrder.get(layers[i]) ?? []
    const right = layerOrder.get(layers[i + 1]) ?? []
    const leftIndex = new Map(left.map((id, idx) => [id, idx]))
    const rightIndex = new Map(right.map((id, idx) => [id, idx]))

    const edges: Array<[number, number]> = []
    for (const edge of graph.edges) {
      const li = leftIndex.get(edge.source)
      const ri = rightIndex.get(edge.target)
      if (li != null && ri != null) edges.push([li, ri])
    }

    for (let a = 0; a < edges.length; a++) {
      for (let b = a + 1; b < edges.length; b++) {
        const [l1, r1] = edges[a]
        const [l2, r2] = edges[b]
        if ((l1 - l2) * (r1 - r2) < 0) crossings++
      }
    }
  }

  return crossings
}
