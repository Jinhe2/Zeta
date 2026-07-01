/** Graph JSON 数据格式（V4 输入） */
export type GraphNodeType = 'input' | 'gate' | 'timer' | 'output'

export interface GraphNode {
  id: string
  name: string
  type: GraphNodeType
  data?: Record<string, unknown>
}

export interface GraphEdge {
  id: string
  source: string
  target: string
  inverted?: boolean
}

export interface GraphData {
  nodes: GraphNode[]
  edges: GraphEdge[]
}

export const NODE_WIDTH = 232
export const NODE_HEIGHT = 32

export interface ElkLayoutStats {
  nodeCount: number
  edgeCount: number
  width: number
  height: number
  layoutMs: number
}
