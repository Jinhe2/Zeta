import type { BuiltGraph, GraphData, GraphEdge, GraphNode } from './types'

/**
 * 将 GraphData 解析为带邻接表的 BuiltGraph，供后续拓扑/分层使用。
 */
export function buildGraph(data: GraphData): BuiltGraph {
  const nodeMap = new Map<string, GraphNode>()
  for (const node of data.nodes) {
    nodeMap.set(node.id, node)
  }

  const outEdges = new Map<string, GraphEdge[]>()
  const inEdges = new Map<string, GraphEdge[]>()

  for (const edge of data.edges) {
    if (!nodeMap.has(edge.source) || !nodeMap.has(edge.target)) {
      throw new Error(`边 ${edge.id} 引用了不存在的节点`)
    }
    const outs = outEdges.get(edge.source) ?? []
    outs.push(edge)
    outEdges.set(edge.source, outs)

    const ins = inEdges.get(edge.target) ?? []
    ins.push(edge)
    inEdges.set(edge.target, ins)
  }

  return {
    nodes: [...data.nodes],
    edges: [...data.edges],
    nodeMap,
    outEdges,
    inEdges,
  }
}

/**
 * Fan-Out：为出度 > 1 的节点插入隐藏 __split__ 节点，使下游分支在布局上自然展开。
 */
export function insertSplitNodes(graph: BuiltGraph): BuiltGraph {
  const newNodes: GraphNode[] = [...graph.nodes]
  const newEdges: GraphEdge[] = []
  const splitIds = new Set<string>()

  for (const node of graph.nodes) {
    if (node.hidden || node.type === '__split__' || node.type === '__merge__') continue
    const outs = graph.outEdges.get(node.id) ?? []
    if (outs.length <= 1) {
      newEdges.push(...outs)
      continue
    }

    const splitId = `${node.id}__split__`
    if (!splitIds.has(splitId)) {
      splitIds.add(splitId)
      newNodes.push({
        id: splitId,
        name: '',
        type: '__split__',
        hidden: true,
      })
    }

    newEdges.push({
      id: `${node.id}->${splitId}`,
      source: node.id,
      target: splitId,
    })

    for (const edge of outs) {
      newEdges.push({
        id: `${splitId}->${edge.target}`,
        source: splitId,
        target: edge.target,
      })
    }
  }

  return buildGraph({ nodes: newNodes, edges: newEdges })
}

/**
 * Fan-In：为入度 > 1 的节点插入隐藏 __merge__ 节点，汇聚多路上游。
 */
export function insertMergeNodes(graph: BuiltGraph): BuiltGraph {
  const targetsWithMerge = new Set<string>()

  for (const node of graph.nodes) {
    if (node.type === '__split__' || node.type === '__merge__') continue
    if ((graph.inEdges.get(node.id) ?? []).length > 1) {
      targetsWithMerge.add(node.id)
    }
  }

  if (targetsWithMerge.size === 0) return graph

  const newNodes: GraphNode[] = [...graph.nodes]
  const newEdges: GraphEdge[] = []

  for (const targetId of targetsWithMerge) {
    newNodes.push({
      id: `${targetId}__merge__`,
      name: '',
      type: '__merge__',
      hidden: true,
    })
  }

  for (const edge of graph.edges) {
    if (targetsWithMerge.has(edge.target)) {
      const mergeId = `${edge.target}__merge__`
      newEdges.push({
        id: `${edge.source}->${mergeId}`,
        source: edge.source,
        target: mergeId,
      })
    } else {
      newEdges.push(edge)
    }
  }

  for (const targetId of targetsWithMerge) {
    const mergeId = `${targetId}__merge__`
    newEdges.push({
      id: `${mergeId}->${targetId}`,
      source: mergeId,
      target: targetId,
    })
  }

  return buildGraph({ nodes: newNodes, edges: newEdges })
}
