import type { Edge, Node } from '@xyflow/react'
import type { BuiltGraph, GraphData, LayoutResult, RoutedEdge } from './types'
import { buildGraph, insertMergeNodes, insertSplitNodes } from './buildGraph'
import { calculateLayers } from './calculateLayers'
import { countCrossings, reduceCrossings } from './crossingReduction'
import { calculateNodePositions } from './layout'
import {
  routeEdges,
  findEdgeNodeHits,
  routeVisiblePair,
} from './edgeRouting'
import { topologicalSort } from './topoSort'

export interface PipelineResult {
  graph: BuiltGraph
  layout: LayoutResult
  routedEdges: RoutedEdge[]
  layerOrder: Map<number, string[]>
  crossingCount: number
  nodeHits: Array<{ id: string; hits: string[] }>
  reactFlowNodes: Node[]
  reactFlowEdges: Edge[]
}

function isVisibleNode(node: BuiltGraph['nodes'][0]): boolean {
  return !node.hidden && node.type !== '__split__' && node.type !== '__merge__'
}

/**
 * 将布局结果转为 ReactFlow 节点（隐藏 split/merge 不输出）。
 */
export function buildReactFlowNodes(
  graph: BuiltGraph,
  layout: LayoutResult,
): Node[] {
  return graph.nodes
    .filter(isVisibleNode)
    .map((node) => ({
      id: node.id,
      type: node.type,
      position: { x: layout.positions.get(node.id)!.x, y: layout.positions.get(node.id)!.y },
      style: { width: 220, height: 80 },
      data: { label: node.name, ...node.data },
      draggable: false,
      selectable: false,
      connectable: false,
    }))
}

/**
 * 将路由结果转为 ReactFlow 边（正交 Step 风格，使用预计算 path）。
 */
export function buildReactFlowEdges(routed: RoutedEdge[]): Edge[] {
  return routed.map((edge) => ({
    id: edge.id,
    source: edge.source,
    target: edge.target,
    type: 'orthogonal',
    data: { path: edge.path, points: edge.points },
    selectable: false,
  }))
}

/** 合并经过隐藏 split/merge 的边，并对可见端点重新路由 */
function collapseHiddenEdges(
  graph: BuiltGraph,
  layout: LayoutResult,
): RoutedEdge[] {
  const hiddenIds = new Set(
    graph.nodes.filter((n) => !isVisibleNode(n)).map((n) => n.id),
  )
  if (hiddenIds.size === 0) {
    return routeEdges(graph, layout)
  }

  const visiblePairs: Array<{ source: string; target: string; id: string }> = []
  const seen = new Set<string>()

  function walkForward(source: string, current: string): void {
    const outs = graph.outEdges.get(current) ?? []
    for (const edge of outs) {
      if (hiddenIds.has(edge.target)) {
        walkForward(source, edge.target)
      } else {
        const key = `${source}->${edge.target}`
        if (!seen.has(key)) {
          seen.add(key)
          visiblePairs.push({ source, target: edge.target, id: key })
        }
      }
    }
  }

  for (const node of graph.nodes) {
    if (!isVisibleNode(node)) continue
    const outs = graph.outEdges.get(node.id) ?? []
    for (const edge of outs) {
      if (hiddenIds.has(edge.target)) {
        walkForward(node.id, edge.target)
      } else {
        const key = `${edge.source}->${edge.target}`
        if (!seen.has(key)) {
          seen.add(key)
          visiblePairs.push({ source: edge.source, target: edge.target, id: edge.id })
        }
      }
    }
  }

  return visiblePairs.map(({ source, target, id }) =>
    routeVisiblePair(source, target, layout.positions, id),
  )
}

/**
 * 完整布局流水线：
 * buildGraph → split/merge → layers → crossing → positions → route → ReactFlow
 */
export function runLayoutPipeline(data: GraphData): PipelineResult {
  let graph = buildGraph(data)
  graph = insertSplitNodes(graph)
  graph = insertMergeNodes(graph)

  const assignment = calculateLayers(graph)
  const layerOrder = reduceCrossings(graph, assignment)
  const layout = calculateNodePositions(graph, assignment.layers, layerOrder)
  const routedEdges = collapseHiddenEdges(graph, layout)
  const crossingCount = countCrossings(graph, layerOrder)
  const nodeHits = findEdgeNodeHits(routedEdges, graph, layout)

  return {
    graph,
    layout,
    routedEdges,
    layerOrder,
    crossingCount,
    nodeHits,
    reactFlowNodes: buildReactFlowNodes(graph, layout),
    reactFlowEdges: buildReactFlowEdges(routedEdges),
  }
}

export {
  buildGraph,
  insertSplitNodes,
  insertMergeNodes,
  topologicalSort,
  calculateLayers,
  reduceCrossings,
  countCrossings,
  calculateNodePositions,
  routeEdges,
}

export type { GraphData, GraphNode, GraphEdge } from './types'
