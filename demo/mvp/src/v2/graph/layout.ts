import type { BuiltGraph, LayoutResult, NodePosition } from './types'
import { LAYOUT } from './types'

const { NODE_WIDTH, NODE_HEIGHT, HORIZONTAL_GAP, VERTICAL_GAP } = LAYOUT

/** 隐藏辅助节点尺寸（不参与视觉，仅占位） */
const HIDDEN_SIZE = 4

function nodeSize(node: BuiltGraph['nodes'][0]): { w: number; h: number } {
  if (node.hidden || node.type === '__split__' || node.type === '__merge__') {
    return { w: HIDDEN_SIZE, h: HIDDEN_SIZE }
  }
  return { w: NODE_WIDTH, h: NODE_HEIGHT }
}

/**
 * 坐标分配（ELK Coordinate Assignment）：
 * x = layer * (NODE_WIDTH + HORIZONTAL_GAP)
 * y = rankInLayer * (NODE_HEIGHT + VERTICAL_GAP)
 * 保证任意两节点矩形不重叠。
 */
export function calculateNodePositions(
  graph: BuiltGraph,
  layers: Map<string, number>,
  layerOrder: Map<number, string[]>,
): LayoutResult {
  const positions = new Map<string, NodePosition>()
  let maxX = 0
  let maxY = 0

  for (const [layer, ids] of layerOrder) {
    ids.forEach((id, rank) => {
      const node = graph.nodeMap.get(id)!
      const { w, h } = nodeSize(node)
      const x = layer * (NODE_WIDTH + HORIZONTAL_GAP)
      const y = rank * (NODE_HEIGHT + VERTICAL_GAP)

      positions.set(id, { id, x, y, width: w, height: h, layer })
      maxX = Math.max(maxX, x + w)
      maxY = Math.max(maxY, y + h)
    })
  }

  // 未出现在 layerOrder 中的节点（不应发生）
  for (const node of graph.nodes) {
    if (!positions.has(node.id)) {
      const L = layers.get(node.id) ?? 0
      const { w, h } = nodeSize(node)
      positions.set(node.id, {
        id: node.id,
        x: L * (NODE_WIDTH + HORIZONTAL_GAP),
        y: 0,
        width: w,
        height: h,
        layer: L,
      })
    }
  }

  return {
    positions,
    width: maxX + HORIZONTAL_GAP,
    height: maxY + VERTICAL_GAP,
  }
}

export function getNodeCenterRight(pos: NodePosition): { x: number; y: number } {
  return { x: pos.x + pos.width, y: pos.y + pos.height / 2 }
}

export function getNodeCenterLeft(pos: NodePosition): { x: number; y: number } {
  return { x: pos.x, y: pos.y + pos.height / 2 }
}
