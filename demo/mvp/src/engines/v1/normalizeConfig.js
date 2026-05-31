import { deriveConnections } from './deriveConnections'

function normalizeGateInputs(gate) {
  return (gate.inputs || []).map((inp) =>
    typeof inp === 'object' ? inp : { node: inp, inverted: false },
  )
}

export function normalizeConfig(raw) {
  const config = JSON.parse(JSON.stringify(raw))
  config.gates = (config.gates || []).map((gate) => ({
    ...gate,
    inputs: normalizeGateInputs(gate),
    inverted: !!gate.inverted,
  }))
  config.inputs = config.inputs || []
  config.timers = config.timers || []
  config.outputs = config.outputs || []
  config.settings = config.settings || []
  config.connections = deriveConnections(config)
  return config
}

export function buildNodeRegistry(config) {
  const settingsMap = Object.fromEntries(
    (config.settings || []).map((s) => [s.id, s]),
  )
  const displayState = config.displayState || {}
  const nodes = {}

  for (const inp of config.inputs) {
    nodes[inp.id] = {
      ...inp,
      kind: 'input',
      displayValue: displayState[inp.id] ?? '-',
    }
  }

  for (const gate of config.gates) {
    nodes[gate.id] = { ...gate, kind: 'gate' }
  }

  for (const timer of config.timers) {
    const delaySetting = timer.delayRef ? settingsMap[timer.delayRef] : null
    nodes[timer.id] = {
      ...timer,
      kind: 'timer',
      delayValue: delaySetting?.defaultValue ?? timer.delayValue ?? '-',
    }
  }

  for (const output of config.outputs) {
    nodes[output.id] = { ...output, kind: 'output' }
  }

  return nodes
}

export function getInputThresholdLabel(node, settingsMap) {
  if (node.thresholdValue != null) return String(node.thresholdValue)
  if (node.thresholdRef && settingsMap[node.thresholdRef]) {
    return String(settingsMap[node.thresholdRef].defaultValue ?? '-')
  }
  if (node.baseValue != null) return String(node.baseValue)
  return '-'
}
