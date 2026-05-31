import { COL_START_Y, INPUT_MIN_GAP } from './constants.js'

function columnContentHeight(column) {
  return column.reduce((sum, node) => sum + node.h, 0)
}

/** space-around 下相邻节点间距为 gap，列总高度 = contentH + n × gap */
function columnTrackHeight(contentH, n, minGap = 0) {
  if (n === 0) return 0
  return contentH + n * minGap
}

/**
 * 按 layer 分组，各列在统一纵向高度内以 space-around 模式分布。
 * 输入列（layer 0）相邻节点间距不小于 INPUT_MIN_GAP。
 */
export function applyColumnSpaceAround(placedNodes, startY = COL_START_Y) {
  const byLayer = {}

  for (const node of Object.values(placedNodes)) {
    const layer = node.layer ?? 0
    if (!byLayer[layer]) byLayer[layer] = []
    byLayer[layer].push(node)
  }

  const layers = Object.keys(byLayer)
    .map(Number)
    .sort((a, b) => a - b)

  if (!layers.length) {
    return { trackHeight: 0, startY }
  }

  for (const layer of layers) {
    byLayer[layer].sort((a, b) => a.y - b.y)
  }

  const trackHeight = Math.max(
    ...layers.map((layer) => {
      const column = byLayer[layer]
      const n = column.length
      const contentH = columnContentHeight(column)
      const minGap = layer === 0 ? INPUT_MIN_GAP : 0
      return columnTrackHeight(contentH, n, minGap)
    }),
    0,
  )

  for (const layer of layers) {
    const column = byLayer[layer]
    const n = column.length
    if (!n) continue

    const contentH = columnContentHeight(column)
    let gap = (trackHeight - contentH) / n
    if (layer === 0) {
      gap = Math.max(gap, INPUT_MIN_GAP)
    }

    let y = startY + gap / 2
    for (const node of column) {
      node.y = y
      y += node.h + gap
    }
  }

  return { trackHeight, startY }
}
