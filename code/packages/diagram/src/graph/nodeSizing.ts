import type { GraphData, GraphNode } from './types'
import {
  CONN_SPACING,
  CORNER_M,
  GATE_CONN_SPACING,
  GATE_SMALL_HEIGHT,
  GATE_WIDTH,
  INPUT_HEIGHT,
  INPUT_WIDE_WIDTH,
  INPUT_WIDTH,
  OUTPUT_HEIGHT,
  OUTPUT_WIDTH,
  TIMER_HEIGHT,
  TIMER_WIDTH,
  WIDE_LABEL_LEN,
} from '../nodes/constants'

function countConnections(nodeId: string, edges: GraphData['edges']): number {
  let n = 0
  for (const e of edges) {
    if (e.source === nodeId || e.target === nodeId) n++
  }
  return Math.max(n, 1)
}

/** gate 按 fan-in / fan-out 较大值自适应高度 */
function countGateConnections(nodeId: string, edges: GraphData['edges']): number {
  let inCount = 0
  let outCount = 0
  for (const e of edges) {
    if (e.target === nodeId) inCount++
    if (e.source === nodeId) outCount++
  }
  return Math.max(inCount, outCount, 1)
}

function adaptiveGateHeight(conn: number): number {
  return Math.max(GATE_SMALL_HEIGHT, CORNER_M * 2 + (conn - 1) * GATE_CONN_SPACING)
}

function visualTextWidth(text: string): number {
  let width = 0
  for (const char of text) {
    if (/[\u2E80-\u9FFF\uF900-\uFAFF\uFF00-\uFFEF]/u.test(char)) {
      width += 12
    } else if (/[A-Z]/.test(char)) {
      width += 8
    } else if (/[0-9]/.test(char)) {
      width += 7
    } else if (/\s/.test(char)) {
      width += 4
    } else {
      width += 7
    }
  }
  return width
}

function adaptiveOutputWidth(node: GraphNode): number {
  const channelRef = typeof node.data?.channelRef === 'string' ? node.data.channelRef : '-'
  const textWidth = visualTextWidth(node.name) + 8 + visualTextWidth(channelRef)
  return Math.max(OUTPUT_WIDTH, Math.ceil(textWidth + 28))
}

/** 按 V3 规则计算节点宽高 */
export function getNodeDimensions(
  node: GraphNode,
  edges: GraphData['edges'],
): { width: number; height: number } {
  const conn = countConnections(node.id, edges)

  switch (node.type) {
    case 'input': {
      const width = node.name.length > WIDE_LABEL_LEN ? INPUT_WIDE_WIDTH : INPUT_WIDTH
      const height = Math.max(INPUT_HEIGHT, CORNER_M * 2 + (conn - 1) * CONN_SPACING)
      return { width, height }
    }
    case 'gate': {
      const gateConn = countGateConnections(node.id, edges)
      return {
        width: GATE_WIDTH,
        height: adaptiveGateHeight(gateConn),
      }
    }
    case 'timer':
      return { width: TIMER_WIDTH, height: TIMER_HEIGHT }
    case 'output':
      return { width: adaptiveOutputWidth(node), height: OUTPUT_HEIGHT }
    default:
      return { width: INPUT_WIDTH, height: INPUT_HEIGHT }
  }
}
