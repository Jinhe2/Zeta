import type { GraphData } from '../graph/types'

/** v2.md 规范 Demo 数据：Fan-Out + Fan-In 示例 */
export const sampleGraphData: GraphData = {
  nodes: [
    { id: 'IN_A', name: '输入 A', type: 'input' },
    { id: 'IN_B', name: '输入 B', type: 'input' },
    { id: 'IN_C', name: '输入 C', type: 'input' },
    { id: 'G1', name: 'AND', type: 'gate', data: { gateType: 'AND' } },
    { id: 'G2', name: 'OR', type: 'gate', data: { gateType: 'OR' } },
    { id: 'G3', name: 'AND', type: 'gate', data: { gateType: 'AND' } },
    { id: 'OUT', name: '保护动作', type: 'output' },
  ],
  edges: [
    { id: 'IN_A->G1', source: 'IN_A', target: 'G1' },
    { id: 'IN_B->G1', source: 'IN_B', target: 'G1' },
    { id: 'IN_A->G2', source: 'IN_A', target: 'G2' },
    { id: 'IN_C->G2', source: 'IN_C', target: 'G2' },
    { id: 'G1->G3', source: 'G1', target: 'G3' },
    { id: 'G2->G3', source: 'G2', target: 'G3' },
    { id: 'G3->OUT', source: 'G3', target: 'OUT' },
  ],
}
