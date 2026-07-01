import type { Edge, Node } from '@xyflow/react'
import type { ElkEdgeSection, ElkExtendedEdge, ElkNode } from 'elkjs/lib/elk-api'
import type { GraphData, ElkLayoutStats } from './types'
import { NODE_HEIGHT, NODE_WIDTH } from './types'
import {
  buildOrthogonalEdgePath,
  type NodeRect,
} from './edgePathPadding'
import type { V4NodeData } from '../nodes/types'

export interface ReactFlowLayoutResult {
  nodes: Node[]
  edges: Edge[]
  stats: ElkLayoutStats
}

function buildNodeRectMap(elkGraph: ElkNode): Map<string, NodeRect> {
  const map = new Map<string, NodeRect>()
  for (const child of elkGraph.children ?? []) {
    map.set(child.id, {
      x: child.x ?? 0,
      y: child.y ?? 0,
      w: child.width ?? NODE_WIDTH,
      h: child.height ?? NODE_HEIGHT,
    })
  }
  return map
}

function buildNodeData(meta: GraphData['nodes'][0] | undefined): V4NodeData {
  const d = meta?.data ?? {}
  return {
    label: meta?.name ?? meta?.id ?? '',
    nodeId: meta?.id ?? '',
    displayValue: d.displayValue as string | undefined,
    threshold: d.threshold as string | undefined,
    gateType: d.gateType as string | undefined,
    inverted: d.inverted as boolean | undefined,
    delayValue: d.delayValue as string | number | undefined,
    channelRef: d.channelRef as string | undefined,
  }
}

/** 将 ELK edge sections 转为严格正交 SVG path */
export function sectionsToPath(
  sections: ElkEdgeSection[] | undefined,
  sourceId: string,
  targetId: string,
  nodeRects: Map<string, NodeRect>,
): string {
  return buildOrthogonalEdgePath(sections, sourceId, targetId, nodeRects)
}

/**
 * 将 ELK 布局结果转为 ReactFlow nodes + edges。
 */
export function toReactFlow(
  elkGraph: ElkNode,
  source: GraphData,
  stats: ElkLayoutStats,
): ReactFlowLayoutResult {
  const nodeMap = new Map(source.nodes.map((n) => [n.id, n]))
  const nodeRects = buildNodeRectMap(elkGraph)

  const nodes: Node[] = (elkGraph.children ?? []).map((child) => {
    const meta = nodeMap.get(child.id)
    const w = child.width ?? NODE_WIDTH
    const h = child.height ?? NODE_HEIGHT
    return {
      id: child.id,
      type: meta?.type ?? 'gate',
      position: { x: child.x ?? 0, y: child.y ?? 0 },
      style: { width: w, height: h },
      data: buildNodeData(meta),
      draggable: false,
      selectable: false,
      connectable: false,
    }
  })

  const edgeMap = new Map(source.edges.map((e) => [e.id, e]))

  const edges: Edge[] = (elkGraph.edges ?? []).map((edge: ElkExtendedEdge) => {
    const sourceId = edge.sources[0]
    const targetId = edge.targets[0]
    const path = sectionsToPath(edge.sections, sourceId, targetId, nodeRects)
    const graphEdge = edgeMap.get(edge.id)
    return {
      id: edge.id,
      source: sourceId,
      target: targetId,
      type: 'elk',
      data: { path, inverted: graphEdge?.inverted ?? false },
      selectable: false,
    }
  })

  return { nodes, edges, stats }
}
