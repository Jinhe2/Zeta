import { normalizeConfig, buildNodeRegistry } from './normalizeConfig.js'

/**
 * 解析并校验 V3 配置 JSON，返回规范化后的 config 与摘要信息。
 */
export function parseConfigV3(rawConfig) {
  if (!rawConfig || typeof rawConfig !== 'object') {
    return { ok: false, error: '配置为空或格式无效' }
  }

  if (!rawConfig.inputs && !rawConfig.gates && !rawConfig.outputs) {
    return { ok: false, error: '无效的保护逻辑配置：缺少 inputs / gates / outputs' }
  }

  try {
    const config = normalizeConfig(rawConfig)
    const nodes = buildNodeRegistry(config)

    const counts = { input: 0, gate: 0, timer: 0, output: 0 }
    const nodeIdsByKind = { input: [], gate: [], timer: [], output: [] }

    for (const [id, node] of Object.entries(nodes)) {
      counts[node.kind] = (counts[node.kind] || 0) + 1
      nodeIdsByKind[node.kind].push(id)
    }

    for (const kind of Object.keys(nodeIdsByKind)) {
      nodeIdsByKind[kind].sort()
    }

    return {
      ok: true,
      config,
      nodes,
      summary: {
        name: config.name || '(未命名)',
        description: config.description || '',
        version: config.version || '-',
        inputCount: counts.input,
        gateCount: counts.gate,
        timerCount: counts.timer,
        outputCount: counts.output,
        connectionCount: config.connections.length,
        settingCount: config.settings.length,
        nodeIdsByKind,
      },
    }
  } catch (err) {
    return { ok: false, error: err.message || '配置解析失败' }
  }
}
