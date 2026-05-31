import { normalizeConfig, buildNodeRegistry } from './normalizeConfig.js'
import { computeLayerLayout } from './layerLayout.js'
import { routeWiresV1 } from './wireRouter.js'

export function buildDiagramV1(rawConfig) {
  const config = normalizeConfig(rawConfig)
  const nodes = buildNodeRegistry(config)
  const connections = (config.connections || []).map((c) => ({ ...c }))

  const layout = computeLayerLayout(config, nodes, connections)
  const routed = routeWiresV1(layout.nodes, connections)

  return {
    config,
    nodes: layout.nodes,
    wires: routed.wires,
    crossings: routed.crossings,
    bounds: layout.bounds,
    meta: { engine: 'v1', layers: layout.layers },
  }
}
