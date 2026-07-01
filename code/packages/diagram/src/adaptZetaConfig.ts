import type { GraphData, GraphEdge, GraphNode } from './graph/types'

export interface ZetaSetting {
  id: string
  defaultValue?: string | number
}

export interface ZetaInput {
  id: string
  name: string
  thresholdRef?: string
  thresholdValue?: string | number
  baseValue?: string | number
}

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

export interface ZetaTimer {
  id: string
  name: string
  input: string
  delayRef?: string
  delayValue?: string | number
}

export interface ZetaOutput {
  id: string
  name: string
  input: string
  channelRef?: string
}

export interface ZetaConfig {
  inputs?: ZetaInput[]
  gates?: ZetaGate[]
  timers?: ZetaTimer[]
  outputs?: ZetaOutput[]
  settings?: ZetaSetting[]
  displayState?: Record<string, string>
}

function thresholdLabel(input: ZetaInput, settings: Record<string, ZetaSetting>): string {
  if (input.thresholdValue != null) return String(input.thresholdValue)
  if (input.thresholdRef && settings[input.thresholdRef]) {
    return String(settings[input.thresholdRef].defaultValue ?? '-')
  }
  if (input.baseValue != null) return String(input.baseValue)
  return '-'
}

/** 继电保护 JSON → Graph JSON（含 V3 展示字段） */
export function adaptZetaConfig(config: ZetaConfig): GraphData {
  const nodes: GraphNode[] = []
  const edges: GraphEdge[] = []
  const nodeIds = new Set<string>()
  const settings = Object.fromEntries((config.settings ?? []).map((s) => [s.id, s]))
  const displayState = config.displayState ?? {}

  function addNode(node: GraphNode) {
    if (nodeIds.has(node.id)) return
    nodeIds.add(node.id)
    nodes.push(node)
  }

  function addEdge(source: string, target: string, inverted?: boolean) {
    if (!source || !target) return
    const id = `${source}->${target}`
    if (edges.some((e) => e.id === id)) return
    edges.push({ id, source, target, inverted })
  }

  for (const input of config.inputs ?? []) {
    addNode({
      id: input.id,
      name: input.name,
      type: 'input',
      data: {
        displayValue: displayState[input.id] ?? '-',
        threshold: thresholdLabel(input, settings),
      },
    })
  }

  for (const gate of config.gates ?? []) {
    addNode({
      id: gate.id,
      name: gate.id,
      type: 'gate',
      data: { gateType: gate.type, inverted: gate.inverted ?? false },
    })
    for (const inp of gate.inputs ?? []) {
      addEdge(inp.node, gate.id, inp.inverted ?? false)
    }
  }

  for (const timer of config.timers ?? []) {
    const delaySetting = timer.delayRef ? settings[timer.delayRef] : null
    addNode({
      id: timer.id,
      name: timer.name,
      type: 'timer',
      data: {
        delayValue: delaySetting?.defaultValue ?? timer.delayValue ?? '-',
      },
    })
    addEdge(timer.input, timer.id)
  }

  for (const output of config.outputs ?? []) {
    addNode({
      id: output.id,
      name: output.name,
      type: 'output',
      data: { channelRef: output.channelRef ?? '-' },
    })
    addEdge(output.input, output.id)
  }

  return { nodes, edges }
}
