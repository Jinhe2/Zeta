/**
 * 从配置中推导 connections（兼容旧格式）
 * - 如果 config.connections 已存在，直接返回
 * - 否则从 gates[].inputs, timers[].input, outputs[].input 推导
 */
function deriveConnections(config) {
  if (config.connections) return config.connections;

  const connections = [];

  // From gates[].inputs
  for (const gate of config.gates || []) {
    (gate.inputs || []).forEach((inp, idx) => {
      const fromId = typeof inp === 'object' ? inp.node : inp;
      connections.push({
        from: fromId,
        to: gate.id,
        toInputIndex: idx,
      });
    });
  }

  // From timers[].input
  for (const timer of config.timers || []) {
    if (timer.input) {
      connections.push({
        from: timer.input,
        to: timer.id,
        toInputIndex: 0,
      });
    }
  }

  // From outputs[].input
  for (const output of config.outputs || []) {
    if (output.input) {
      connections.push({
        from: output.input,
        to: output.id,
        toInputIndex: 0,
      });
    }
  }

  return connections;
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = { deriveConnections };
}
