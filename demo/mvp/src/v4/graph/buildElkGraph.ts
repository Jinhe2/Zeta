import type { ElkExtendedEdge, ElkNode, LayoutOptions } from 'elkjs/lib/elk-api'
import type { GraphData } from './types'
import { getNodeDimensions } from './nodeSizing'
import {
  ELK_NODE_NODE_SPACING,
  ELK_NODE_NODE_SPACING_MIN,
  INPUT_COMPACT_SPACING_STEP,
  INPUT_COMPACT_SPACING_THRESHOLD,
} from '../nodes/constants'

/** ELK Layered 布局基础参数（LR 方向 + 正交路由） */
const ELK_LAYOUT_BASE: LayoutOptions = {
  'elk.algorithm': 'layered',
  'elk.direction': 'RIGHT',
  'elk.layered.nodePlacement.strategy': 'BRANDES_KOEPF',
  'elk.layered.crossingMinimization.strategy': 'LAYER_SWEEP',
  'elk.layered.spacing.nodeNodeBetweenLayers': '120',
  'elk.spacing.edgeNode': '28',
  'elk.spacing.edgeEdge': '16',
  'elk.layered.spacing.edgeNodeBetweenLayers': '28',
  'elk.edgeRouting': 'ORTHOGONAL',
  'elk.layered.unnecessaryBendpoints': 'true',
  'elk.padding': '[top=40,left=40,bottom=40,right=40]',
}

/** input 数量多时缩小同层 nodeNode 间距，压缩左侧输入列 */
export function resolveInputNodeSpacing(data: GraphData): number {
  const inputCount = data.nodes.filter((n) => n.type === 'input').length
  if (inputCount <= INPUT_COMPACT_SPACING_THRESHOLD) {
    return ELK_NODE_NODE_SPACING
  }
  const reduced =
    ELK_NODE_NODE_SPACING -
    (inputCount - INPUT_COMPACT_SPACING_THRESHOLD) * INPUT_COMPACT_SPACING_STEP
  return Math.max(ELK_NODE_NODE_SPACING_MIN, reduced)
}

export function buildElkLayoutOptions(data: GraphData): LayoutOptions {
  return {
    ...ELK_LAYOUT_BASE,
    'elk.spacing.nodeNode': String(resolveInputNodeSpacing(data)),
  }
}

/** @deprecated 固定间距版本；布局请用 buildElkLayoutOptions(data) */
export const ELK_LAYOUT_OPTIONS: LayoutOptions = {
  ...ELK_LAYOUT_BASE,
  'elk.spacing.nodeNode': String(ELK_NODE_NODE_SPACING),
}

function layerConstraint(type: GraphData['nodes'][0]['type']): LayoutOptions | undefined {
  if (type === 'input') {
    return { 'org.eclipse.elk.layered.layering.layerConstraint': 'FIRST' }
  }
  if (type === 'output') {
    return { 'org.eclipse.elk.layered.layering.layerConstraint': 'LAST' }
  }
  return undefined
}

/**
 * 将 Graph JSON 转为 ELK 图结构。
 */
export function buildElkGraph(data: GraphData): ElkNode {
  return {
    id: 'root',
    layoutOptions: buildElkLayoutOptions(data),
    children: data.nodes.map((node) => {
      const { width, height } = getNodeDimensions(node, data.edges)
      return {
        id: node.id,
        width,
        height,
        labels: [{ text: node.name }],
        layoutOptions: layerConstraint(node.type),
      }
    }),
    edges: data.edges.map(
      (edge): ElkExtendedEdge => ({
        id: edge.id,
        sources: [edge.source],
        targets: [edge.target],
      }),
    ),
  }
}

export type { ElkNode, ElkExtendedEdge }
