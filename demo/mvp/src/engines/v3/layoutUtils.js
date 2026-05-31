import { NODE_GAP } from './constants.js'

export function getGateInputIds(gate) {
  return (gate.inputs || []).map((inp) => (typeof inp === 'object' ? inp.node : inp))
}

export function columnRightEdge(placedNodes, layer) {
  const layerNodes = Object.values(placedNodes).filter((n) => n.layer === layer)
  if (!layerNodes.length) return 0
  return Math.max(...layerNodes.map((n) => n.x + n.w))
}

export function barycenterY(nodeIds, placedNodes) {
  const refs = nodeIds.map((id) => placedNodes[id]).filter(Boolean)
  if (!refs.length) return 0
  return refs.reduce((s, n) => s + n.y + n.h / 2, 0) / refs.length
}

export function fixColumnOverlap(nodeIds, nodes) {
  if (!nodeIds?.length) return
  nodeIds.sort((a, b) => nodes[a].y - nodes[b].y)
  for (let i = 1; i < nodeIds.length; i++) {
    const prev = nodes[nodeIds[i - 1]]
    const curr = nodes[nodeIds[i]]
    const minY = prev.y + prev.h + NODE_GAP
    if (curr.y < minY) curr.y = minY
  }
}
