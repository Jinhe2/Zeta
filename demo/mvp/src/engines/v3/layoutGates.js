import { applyNodeSize, computeBounds } from '../v1/nodeSizing.js'
import { COL_MARGIN } from './constants.js'
import {
  barycenterY,
  columnRightEdge,
  fixColumnOverlap,
  getGateInputIds,
} from './layoutUtils.js'

function selectEligibleGates(config, placedNodes) {
  const renderedIds = new Set(Object.keys(placedNodes))
  return (config.gates || []).filter((gate) => {
    if (renderedIds.has(gate.id)) return false
    const inputIds = getGateInputIds(gate)
    return inputIds.length > 0 && inputIds.every((id) => renderedIds.has(id))
  })
}

function layoutOneGateColumn(config, nodes, placedNodes, layer) {
  const connections = config.connections || []
  const eligible = selectEligibleGates(config, placedNodes)
  if (!eligible.length) return []

  const colX = columnRightEdge(placedNodes, layer - 1) + COL_MARGIN
  const columnGateIds = []

  eligible.sort((a, b) => {
    const ya = barycenterY(getGateInputIds(a), placedNodes)
    const yb = barycenterY(getGateInputIds(b), placedNodes)
    return ya - yb
  })

  for (const gate of eligible) {
    const node = nodes[gate.id]
    if (!node || node.kind !== 'gate') continue

    applyNodeSize(node, connections)
    const inputIds = getGateInputIds(gate)
    const centerY = barycenterY(inputIds, placedNodes)

    node.x = colX
    node.y = centerY - node.h / 2
    node.layer = layer

    placedNodes[gate.id] = node
    columnGateIds.push(gate.id)
  }

  fixColumnOverlap(columnGateIds, placedNodes)
  return columnGateIds
}

/**
 * 循环选取「全部输入均已渲染」且尚未放置的 gate，逐列排布，直到全部 gate 渲染完毕。
 * 输入列为 layer 0，第一批 gate 为 layer 1，依此类推。
 */
export function layoutAllGateColumns(config, nodes, placedNodes) {
  const allGateIds = (config.gates || []).map((g) => g.id)
  const placedGateIds = []
  let layer = 1

  while (placedGateIds.length < allGateIds.length) {
    const columnGateIds = layoutOneGateColumn(config, nodes, placedNodes, layer)
    if (!columnGateIds.length) break
    placedGateIds.push(...columnGateIds)
    layer++
  }

  const unplacedGateIds = allGateIds.filter((id) => !placedNodes[id])

  return {
    nodes: placedNodes,
    gateIds: placedGateIds,
    gateLayerCount: layer - 1,
    unplacedGateIds,
    bounds: computeBounds(placedNodes),
  }
}
