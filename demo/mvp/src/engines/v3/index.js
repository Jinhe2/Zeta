import { parseConfigV3 } from './parseConfig.js'
import { layoutInputColumn } from './layoutInputs.js'
import { layoutAllGateColumns } from './layoutGates.js'
import { applyColumnSpaceAround } from './alignColumns.js'
import { createConnections } from './connections.js'
import { ensureContactNodeHeights } from './nodeContactSize.js'
import { routeConnections } from './routeWires.js'
import { computeBounds } from '../v1/nodeSizing.js'

/** V3 引擎：节点布局 + connections + 按列顺序连线 */
export function buildDiagramV3(rawConfig) {
  const parsed = parseConfigV3(rawConfig)
  if (!parsed.ok) {
    throw new Error(parsed.error)
  }

  const { config, nodes } = parsed
  const inputLayout = layoutInputColumn(config, nodes)
  const placedNodes = { ...inputLayout.nodes }
  const gateLayout = layoutAllGateColumns(config, nodes, placedNodes)
  applyColumnSpaceAround(gateLayout.nodes)

  const connections = createConnections(config, gateLayout.nodes)
  ensureContactNodeHeights(gateLayout.nodes, connections)
  applyColumnSpaceAround(gateLayout.nodes)

  const routed = routeConnections(connections, gateLayout.nodes)

  return {
    config,
    nodes: gateLayout.nodes,
    wires: routed.wires,
    crossings: routed.crossings,
    bounds: computeBounds(gateLayout.nodes),
    connections,
    meta: {
      engine: 'v3',
      phase: 'route-wires',
      inputCount: inputLayout.inputIds.length,
      gateCount: gateLayout.gateIds.length,
      gateLayerCount: gateLayout.gateLayerCount,
      connectionCount: connections.total,
      routedCount: connections.routed,
      skippedConnectionCount: connections.skippedCount,
      unplacedGateIds: gateLayout.unplacedGateIds,
      summary: parsed.summary,
    },
  }
}

export { parseConfigV3, createConnections }
