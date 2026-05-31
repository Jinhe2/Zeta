import type { GraphData, GraphEdge, GraphNode } from '../graph/types'

/** 继电保护 JSON 配置（zeta 项目格式） */
export interface ZetaGateInput {
  node: string
  inverted?: boolean
}

export interface ZetaGate {
  id: string
  type: string
  inputs: ZetaGateInput[]
  inverted?: boolean
}

export interface ZetaInput {
  id: string
  name: string
}

export interface ZetaTimer {
  id: string
  name: string
  input: string
}

export interface ZetaOutput {
  id: string
  name: string
  input: string
}

export interface ZetaConfig {
  name?: string
  description?: string
  inputs?: ZetaInput[]
  gates?: ZetaGate[]
  timers?: ZetaTimer[]
  outputs?: ZetaOutput[]
}

/**
 * 将 zeta 继电保护 JSON 转为 v2 GraphData（input / gate / output）。
 */
export function adaptZetaConfig(config: ZetaConfig): GraphData {
  const nodes: GraphNode[] = []
  const edges: GraphEdge[] = []
  const nodeIds = new Set<string>()

  function addNode(node: GraphNode) {
    if (nodeIds.has(node.id)) return
    nodeIds.add(node.id)
    nodes.push(node)
  }

  function addEdge(source: string, target: string) {
    if (!source || !target) return
    const id = `${source}->${target}`
    if (edges.some((e) => e.id === id)) return
    edges.push({ id, source, target })
  }

  for (const input of config.inputs ?? []) {
    addNode({ id: input.id, name: input.name, type: 'input' })
  }

  for (const gate of config.gates ?? []) {
    const label = gate.inverted ? `${gate.type}!` : gate.type
    addNode({
      id: gate.id,
      name: label,
      type: 'gate',
      data: { gateType: gate.type, inverted: gate.inverted ?? false },
    })
    for (const inp of gate.inputs ?? []) {
      addEdge(inp.node, gate.id)
    }
  }

  for (const timer of config.timers ?? []) {
    addNode({
      id: timer.id,
      name: timer.name,
      type: 'gate',
      data: { gateType: 'TIMER' },
    })
    addEdge(timer.input, timer.id)
  }

  for (const output of config.outputs ?? []) {
    addNode({ id: output.id, name: output.name, type: 'output' })
    addEdge(output.input, output.id)
  }

  return { nodes, edges }
}
