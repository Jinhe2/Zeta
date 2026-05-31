/**
 * 第一步：从配置数据创建 connections 对象，汇总全部逻辑连线。
 *
 * connections 结构：
 * - items: 全部待画连线（已渲染节点之间）
 * - byFrom / byTo: 按源/目标索引
 * - outboundCount / inboundCount: 各节点出入度
 * - skipped: 因节点未渲染而跳过的连线
 */

function cloneConnection(raw, index) {
  return {
    id: `conn-${index}`,
    from: raw.from,
    to: raw.to,
    toInputIndex: raw.toInputIndex ?? 0,
    inverted: !!raw.inverted,
    status: 'pending',
  }
}

/**
 * @param {object} config - normalizeConfig 后的配置（含 connections）
 * @param {Record<string, object>} placedNodes - 已渲染到图上的节点
 */
export function createConnections(config, placedNodes) {
  const placedIds = new Set(Object.keys(placedNodes))
  const rawList = config.connections || []

  /** @type {object[]} */
  const items = []
  /** @type {object[]} */
  const skipped = []

  rawList.forEach((raw, index) => {
    const fromPlaced = placedIds.has(raw.from)
    const toPlaced = placedIds.has(raw.to)
    if (!fromPlaced || !toPlaced) {
      skipped.push({
        ...cloneConnection(raw, index),
        reason: !fromPlaced && !toPlaced
          ? 'both-unplaced'
          : !fromPlaced
            ? 'from-unplaced'
            : 'to-unplaced',
      })
      return
    }
    items.push(cloneConnection(raw, index))
  })

  const byFrom = {}
  const byTo = {}
  const outboundCount = {}
  const inboundCount = {}

  for (const conn of items) {
    if (!byFrom[conn.from]) byFrom[conn.from] = []
    byFrom[conn.from].push(conn)
    if (!byTo[conn.to]) byTo[conn.to] = []
    byTo[conn.to].push(conn)
    outboundCount[conn.from] = (outboundCount[conn.from] || 0) + 1
    inboundCount[conn.to] = (inboundCount[conn.to] || 0) + 1
  }

  for (const list of Object.values(byFrom)) {
    list.sort((a, b) => a.toInputIndex - b.toInputIndex || a.to.localeCompare(b.to))
  }
  for (const list of Object.values(byTo)) {
    list.sort((a, b) => a.toInputIndex - b.toInputIndex || a.from.localeCompare(b.from))
  }

  return {
    items,
    byFrom,
    byTo,
    outboundCount,
    inboundCount,
    total: items.length,
    skipped,
    skippedCount: skipped.length,
    rawCount: rawList.length,
    pending: items.length,
    routed: 0,
  }
}
