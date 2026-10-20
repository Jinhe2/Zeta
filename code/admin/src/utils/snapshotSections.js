/** 统一断面节点状态：1 满足，0 不满足，-1 表示节点无实际数据。 */
function normalizeSectionState(value) {
  const numericValue = Number(value)
  if (numericValue === -1) return -1
  return numericValue === 0 ? 0 : 1
}

function parseTimestampMs(ts) {
  const spaceIdx = ts.indexOf(' ')
  const timePart = spaceIdx >= 0 ? ts.substring(spaceIdx + 1) : ts
  const parts = timePart.split(/[:.]/)
  const h = parseInt(parts[0]) || 0
  const m = parseInt(parts[1]) || 0
  const s = parseInt(parts[2]) || 0
  const ms = parseInt(parts[3]) || 0
  return ((h * 60 + m) * 60 + s) * 1000 + ms
}

/** 将 v2.3 snapshot JSON 转换成与历史接口一致的断面结构。 */
export function parseSnapshotSections(snapshotJson) {
  let data
  if (typeof snapshotJson === 'string') {
    try { data = JSON.parse(snapshotJson) } catch { return [] }
  } else {
    data = snapshotJson
  }

  const nodes = data?.nodes ?? []
  const timestamps = data?.timestamps ?? []
  const channels = data?.channels ?? []
  if (!timestamps.length || !nodes.length) return []

  const baseTime = parseTimestampMs(timestamps[0])
  return timestamps.map((ts, k) => {
    const states = {}
    const effectiveValues = {}
    for (let i = 0; i < nodes.length && i < channels.length; i += 1) {
      const nodeId = nodes[i].id
      const channel = channels[i] ?? {}
      const values = channel.values
      states[nodeId] = values && k < values.length ? normalizeSectionState(values[k]) : 0

      if (
        Array.isArray(channel.effectiveValues)
        && channel.effectiveValueType === 'fundamental_rms'
        && k < channel.effectiveValues.length
      ) {
        const rawValue = channel.effectiveValues[k]
        effectiveValues[nodeId] = {
          value: typeof rawValue === 'number' && Number.isFinite(rawValue) ? rawValue : null,
          effectiveValueType: channel.effectiveValueType,
          unit: typeof channel.unit === 'string' ? channel.unit : '',
        }
      }
    }
    const elapsedSec = (parseTimestampMs(ts) - baseTime) / 1000
    return {
      id: `section-${k}`,
      label: `T = ${elapsedSec.toFixed(3)} s`,
      time: elapsedSec,
      timestamp: ts,
      states,
      effectiveValues,
    }
  })
}

/** 将一个有效值格式化为节点副标题；空值保留为 --。 */
export function formatEffectiveValue(entry) {
  if (!entry || entry.effectiveValueType !== 'fundamental_rms') return null
  const value = entry.value
  const hasNumber = typeof value === 'number' && Number.isFinite(value)
  if (!hasNumber) return '--'
  const numberText = String(Number((Object.is(value, -0) ? 0 : value).toFixed(3)))
  const unit = typeof entry.unit === 'string' ? entry.unit.trim() : ''
  return unit ? `${numberText} ${unit}` : numberText
}

/** 将服务端节点有效值映射转换为图组件可直接显示的文本映射。 */
export function formatNodeEffectiveValues(effectiveValues) {
  if (!effectiveValues || typeof effectiveValues !== 'object') return null
  const result = {}
  for (const [nodeId, entry] of Object.entries(effectiveValues)) {
    const text = formatEffectiveValue(entry)
    if (text != null) result[nodeId] = text
  }
  return Object.keys(result).length > 0 ? result : null
}

/**
 * 将原始快照中的有效值合并进历史 sections 响应。
 * 用于兼容尚未返回 effectiveValues 的旧版 sections 接口。
 */
export function mergeSnapshotEffectiveValues(sections, snapshotJson) {
  if (!Array.isArray(sections) || sections.length === 0) return []
  const rawSections = parseSnapshotSections(snapshotJson)
  if (rawSections.length === 0) return sections
  const rawById = new Map(rawSections.map((section) => [section.id, section]))

  return sections.map((section, index) => {
    const rawSection = rawById.get(section.id) ?? rawSections[index]
    const rawEffectiveValues = rawSection?.effectiveValues
    if (!rawEffectiveValues || Object.keys(rawEffectiveValues).length === 0) {
      return section
    }
    return {
      ...section,
      effectiveValues: {
        ...(section.effectiveValues ?? {}),
        ...rawEffectiveValues,
      },
    }
  })
}
