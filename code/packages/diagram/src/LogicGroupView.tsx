import { useMemo } from 'react'
import GraphView from './components/GraphView'
import type { GraphData } from './graph/types'

export interface LogicGroupItem {
  /** 基础逻辑框图 ID（作为节点 id） */
  id: string
  /** 基础逻辑名称（节点显示文本） */
  name: string
}

export interface LogicGroupViewProps {
  /** 按组合顺序排列的基础逻辑 */
  items: LogicGroupItem[]
  /** 节点状态：id → 是否成功；为 null 时不展示状态 */
  nodeStates?: Record<string, boolean> | null
  /** 点击节点回调（进入对应基础逻辑框图） */
  onNodeClick?: (id: string) => void
  className?: string
}

/** 组合逻辑视图：把多个基础逻辑按顺序串联渲染成一张框图。 */
export default function LogicGroupView({
  items,
  nodeStates,
  onNodeClick,
  className,
}: LogicGroupViewProps) {
  const data = useMemo<GraphData>(() => {
    const nodes = items.map((item) => ({
      id: item.id,
      name: item.name,
      type: 'logic' as const,
      data: {},
    }))
    const edges: GraphData['edges'] = []
    for (let i = 0; i < items.length - 1; i++) {
      edges.push({
        id: `${items[i].id}->${items[i + 1].id}`,
        source: items[i].id,
        target: items[i + 1].id,
      })
    }
    return { nodes, edges }
  }, [items])

  if (data.nodes.length === 0) return null

  return (
    <GraphView
      data={data}
      showDevInfo={false}
      nodeStates={nodeStates}
      onNodeSelect={
        onNodeClick
          ? (id: string | null) => {
              if (id) onNodeClick(id)
            }
          : undefined
      }
      className={className}
    />
  )
}
