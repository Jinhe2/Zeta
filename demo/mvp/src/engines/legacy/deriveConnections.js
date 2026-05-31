/**
 * 从配置中推导 connections（兼容旧格式）
 */
export function deriveConnections(config) {
  if (config.connections?.length) return config.connections

  const connections = []

  for (const gate of config.gates || []) {
    ;(gate.inputs || []).forEach((inp, idx) => {
      const fromId = typeof inp === 'object' ? inp.node : inp
      connections.push({
        from: fromId,
        to: gate.id,
        toInputIndex: idx,
        inverted: typeof inp === 'object' ? !!inp.inverted : false,
      })
    })
  }

  for (const timer of config.timers || []) {
    if (timer.input) {
      connections.push({
        from: timer.input,
        to: timer.id,
        toInputIndex: 0,
        inverted: false,
      })
    }
  }

  for (const output of config.outputs || []) {
    if (output.input) {
      connections.push({
        from: output.input,
        to: output.id,
        toInputIndex: 0,
        inverted: false,
      })
    }
  }

  return connections
}
