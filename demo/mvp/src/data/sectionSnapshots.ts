/** 实验断面：某一时刻全部节点的满足状态 */
export interface SectionSnapshot {
  id: string
  label: string
  time: number
  states: Record<string, boolean>
}

function hash(s: string): number {
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0
  return Math.abs(h)
}

function parseLooseBool(value: string): boolean {
  const v = value.trim().toLowerCase()
  if (v === '1' || v === 'true' || v === 'yes') return true
  if (v === '0' || v === 'false' || v === 'no' || v === '') return false
  const n = Number(v)
  return Number.isFinite(n) && n > 0
}

/**
 * 根据配置中的 sections 字段或节点列表生成断面数据（暂无后端时使用模拟数据）。
 */
export function buildSectionSnapshots(
  nodeIds: string[],
  config?: Record<string, unknown> | null,
): SectionSnapshot[] {
  const fromConfig = config?.sections
  if (Array.isArray(fromConfig) && fromConfig.length > 0) {
    return fromConfig.map((raw, idx) => {
      const item = raw as Record<string, unknown>
      const statesRaw = (item.states ?? item.nodeStates ?? {}) as Record<string, unknown>
      const states: Record<string, boolean> = {}
      for (const id of nodeIds) {
        const v = statesRaw[id]
        states[id] = typeof v === 'boolean' ? v : parseLooseBool(String(v ?? '0'))
      }
      const time = typeof item.time === 'number' ? item.time : idx * 0.5
      return {
        id: String(item.id ?? `section-${idx}`),
        label: String(item.label ?? `T = ${time.toFixed(1)} s`),
        time,
        states,
      }
    })
  }

  const displayState = (config?.displayState ?? {}) as Record<string, string>
  const times = [0, 0.2, 0.5, 1.0, 2.0, 5.0]

  return times.map((time, idx) => {
    const states: Record<string, boolean> = {}
    for (const id of nodeIds) {
      if (idx === 0 && id in displayState) {
        states[id] = parseLooseBool(displayState[id])
        continue
      }
      const base = hash(`${id}-${idx}`) % 100
      const threshold = 28 + idx * 12
      states[id] = base < threshold
    }
    return {
      id: `section-${idx}`,
      label: `T = ${time.toFixed(1)} s`,
      time,
      states,
    }
  })
}
