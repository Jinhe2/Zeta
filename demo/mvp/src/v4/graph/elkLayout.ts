import ELK from 'elkjs/lib/elk.bundled.js'
import type { ElkNode } from 'elkjs/lib/elk-api'
import type { GraphData, ElkLayoutStats } from './types'
import { buildElkGraph } from './buildElkGraph'

const elk = new ELK()

export interface ElkLayoutResult {
  elkGraph: ElkNode
  stats: ElkLayoutStats
}

/**
 * 调用 ELK Layout Engine 计算节点与边的坐标。
 */
export async function runElkLayout(data: GraphData): Promise<ElkLayoutResult> {
  const input = buildElkGraph(data)
  const t0 = performance.now()
  const elkGraph = await elk.layout(input)
  const layoutMs = performance.now() - t0

  const children = elkGraph.children ?? []
  let maxX = 0
  let maxY = 0
  for (const child of children) {
    maxX = Math.max(maxX, (child.x ?? 0) + (child.width ?? 0))
    maxY = Math.max(maxY, (child.y ?? 0) + (child.height ?? 0))
  }

  return {
    elkGraph,
    stats: {
      nodeCount: children.length,
      edgeCount: (elkGraph.edges ?? []).length,
      width: maxX + 40,
      height: maxY + 40,
      layoutMs,
    },
  }
}
