import type { GraphData } from '../graph/types'
import { adaptZetaConfig } from './adaptZetaConfig'

function isGraphData(value: unknown): value is GraphData {
  if (typeof value !== 'object' || value === null) return false
  const obj = value as Record<string, unknown>
  return Array.isArray(obj.nodes) && Array.isArray(obj.edges)
}

/**
 * 解析输入 JSON：
 * - 原生 Graph JSON（nodes + edges）直接使用
 * - 继电保护 zeta 配置自动转换（含 V3 展示字段）
 */
export function parseGraphJson(config: unknown): GraphData {
  if (isGraphData(config)) {
    return {
      nodes: config.nodes.map((n) => ({
        id: n.id,
        name: n.name,
        type: n.type,
        data: n.data,
      })),
      edges: config.edges.map((e) => ({
        id: e.id,
        source: e.source,
        target: e.target,
      })),
    }
  }
  return adaptZetaConfig(config as Parameters<typeof adaptZetaConfig>[0])
}
