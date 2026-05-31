import { normalizeConfig, buildNodeRegistry } from './normalizeConfig'
import { computeLayout } from './layout'
import { routeWires } from './wireRouting'

export function buildDiagram(rawConfig) {
  const config = normalizeConfig(rawConfig)
  const nodes = buildNodeRegistry(config)
  const connections = (config.connections || []).map((c) => ({ ...c }))

  const layout = computeLayout(config, nodes, connections)
  const routed = routeWires(layout.nodes, layout.connections || [])

  return {
    config,
    nodes: layout.nodes || {},
    wires: Array.isArray(routed?.wires) ? routed.wires : [],
    crossings: Array.isArray(routed?.crossings) ? routed.crossings : [],
    bounds: layout.bounds || { width: 800, height: 600 },
    meta: { engine: 'legacy' },
  }
}

export { deriveConnections } from './deriveConnections'
export { normalizeConfig, buildNodeRegistry, getInputThresholdLabel } from './normalizeConfig'
