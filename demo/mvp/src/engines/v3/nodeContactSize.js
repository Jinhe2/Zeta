import { CORNER_M } from './constants.js'
import { getBaseHeight } from '../v1/nodeSizing.js'

/** 同一节点上相邻接触点最小间距 */
export const CONTACT_GAP = 20

/** 根据出入线数量增大节点高度，保证接触点距上下边 ≥20px 且彼此 ≥20px */
export function ensureContactNodeHeights(nodes, connections) {
  for (const node of Object.values(nodes)) {
    const out = connections.outboundCount[node.id] || 0
    const inn = connections.inboundCount[node.id] || 0
    const count = Math.max(out, inn, 1)
    const baseH = getBaseHeight(node)
    const requiredH = CORNER_M * 2 + (count - 1) * CONTACT_GAP
    node.h = Math.max(node.h ?? baseH, baseH, requiredH)
  }
}
