/** 对外 JSON 图数据（v2.md 规范） */
export type GraphNodeType = 'input' | 'gate' | 'output' | '__split__' | '__merge__'

export interface GraphNode {
  id: string
  name: string
  type: GraphNodeType
  hidden?: boolean
  data?: Record<string, unknown>
}

export interface GraphEdge {
  id: string
  source: string
  target: string
}

export interface GraphData {
  nodes: GraphNode[]
  edges: GraphEdge[]
}

/** 内部图结构：邻接表 + 索引 */
export interface BuiltGraph {
  nodes: GraphNode[]
  edges: GraphEdge[]
  nodeMap: Map<string, GraphNode>
  /** 出边：source -> edges */
  outEdges: Map<string, GraphEdge[]>
  /** 入边：target -> edges */
  inEdges: Map<string, GraphEdge[]>
}

export interface LayerAssignment {
  /** nodeId -> layer index（Input=0，Output 在最右层） */
  layers: Map<string, number>
  /** layer -> node ids（有序） */
  layerNodes: Map<number, string[]>
  maxLayer: number
}

export interface NodePosition {
  id: string
  x: number
  y: number
  width: number
  height: number
  layer: number
}

export interface LayoutResult {
  positions: Map<string, NodePosition>
  width: number
  height: number
}

export interface RoutedEdge {
  id: string
  source: string
  target: string
  /** SVG path d 属性 */
  path: string
  /** 正交折点，供 ReactFlow 自定义边使用 */
  points: Array<{ x: number; y: number }>
}

export interface LayoutConstants {
  NODE_WIDTH: number
  NODE_HEIGHT: number
  HORIZONTAL_GAP: number
  VERTICAL_GAP: number
}

export const LAYOUT: LayoutConstants = {
  NODE_WIDTH: 220,
  NODE_HEIGHT: 80,
  HORIZONTAL_GAP: 350,
  VERTICAL_GAP: 120,
}
