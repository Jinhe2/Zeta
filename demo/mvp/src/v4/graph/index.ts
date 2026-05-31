import type { GraphData } from './types'
import { runElkLayout } from './elkLayout'
import { toReactFlow, type ReactFlowLayoutResult } from './toReactFlow'

/**
 * V4 流水线：Graph JSON → ELK Layout → ReactFlow nodes/edges
 */
export async function runElkPipeline(data: GraphData): Promise<ReactFlowLayoutResult> {
  const { elkGraph, stats } = await runElkLayout(data)
  return toReactFlow(elkGraph, data, stats)
}

export { buildElkGraph, ELK_LAYOUT_OPTIONS } from './buildElkGraph'
export { runElkLayout } from './elkLayout'
export { toReactFlow, sectionsToPath } from './toReactFlow'
export type { GraphData, GraphNode, GraphEdge, ElkLayoutStats } from './types'
