import { applyNodeSize, computeBounds } from '../v1/nodeSizing.js'
import { COL_START_Y, COL_X, NODE_GAP } from './constants.js'

/**
 * 第一步：将所有 input 节点排布在最左侧一列，纵向等间距堆叠。
 * 顺序与 JSON 中 inputs 数组一致。
 */
export function layoutInputColumn(config, nodes) {
  const connections = config.connections || []
  const inputIds = (config.inputs || []).map((inp) => inp.id)

  let y = COL_START_Y

  for (const id of inputIds) {
    const node = nodes[id]
    if (!node || node.kind !== 'input') continue

    applyNodeSize(node, connections)
    node.x = COL_X
    node.y = y
    node.layer = 0

    y += node.h + NODE_GAP
  }

  const placedNodes = {}
  for (const id of inputIds) {
    if (nodes[id]) placedNodes[id] = nodes[id]
  }

  return {
    nodes: placedNodes,
    inputIds,
    bounds: computeBounds(placedNodes),
  }
}
