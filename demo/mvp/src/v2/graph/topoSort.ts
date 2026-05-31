import type { BuiltGraph } from './types'

/**
 * Kahn 拓扑排序：保证 DAG 中每个节点在其所有父节点之后出现。
 * 若存在环则抛出错误。
 */
export function topologicalSort(graph: BuiltGraph): string[] {
  const inDegree = new Map<string, number>()
  for (const node of graph.nodes) {
    inDegree.set(node.id, (graph.inEdges.get(node.id) ?? []).length)
  }

  const queue: string[] = []
  for (const [id, deg] of inDegree) {
    if (deg === 0) queue.push(id)
  }

  queue.sort((a, b) => a.localeCompare(b))

  const order: string[] = []
  while (queue.length > 0) {
    const id = queue.shift()!
    order.push(id)

    for (const edge of graph.outEdges.get(id) ?? []) {
      const next = edge.target
      const deg = (inDegree.get(next) ?? 0) - 1
      inDegree.set(next, deg)
      if (deg === 0) {
        queue.push(next)
        queue.sort((a, b) => a.localeCompare(b))
      }
    }
  }

  if (order.length !== graph.nodes.length) {
    throw new Error('图存在环路，无法进行 DAG 布局')
  }

  return order
}
