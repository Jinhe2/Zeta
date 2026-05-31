import type { BuiltGraph, LayerAssignment } from './types'
import { topologicalSort } from './topoSort'

/**
 * 层级分配（ELK Layer Assignment 思想）：
 * - Input 固定 Layer 0
 * - 其余节点 Layer = max(parent.layer) + 1
 * - Output 自然落在最右层（由拓扑深度决定）
 */
export function calculateLayers(graph: BuiltGraph): LayerAssignment {
  const layers = new Map<string, number>()
  const order = topologicalSort(graph)

  for (const node of graph.nodes) {
    if (node.type === 'input') {
      layers.set(node.id, 0)
    }
  }

  for (const id of order) {
    const node = graph.nodeMap.get(id)!
    if (node.type === 'input') continue

    const parents = graph.inEdges.get(id) ?? []
    if (parents.length === 0) {
      layers.set(id, 0)
      continue
    }

    let maxParent = -1
    for (const edge of parents) {
      maxParent = Math.max(maxParent, layers.get(edge.source) ?? 0)
    }
    layers.set(id, maxParent + 1)
  }

  // 强制 Input 在最左
  for (const node of graph.nodes) {
    if (node.type === 'input') layers.set(node.id, 0)
  }

  const maxLayer = Math.max(0, ...layers.values())

  const layerNodes = new Map<number, string[]>()
  for (const node of graph.nodes) {
    const L = layers.get(node.id) ?? 0
    const list = layerNodes.get(L) ?? []
    list.push(node.id)
    layerNodes.set(L, list)
  }

  for (const [, ids] of layerNodes) {
    ids.sort((a, b) => a.localeCompare(b))
  }

  return { layers, layerNodes, maxLayer }
}
