import { buildDiagram } from './legacy/index.js'
import { buildDiagramV1 } from './v1/index.js'
import { buildDiagramV3 } from './v3/index.js'

/** @typedef {'legacy' | 'v1' | 'v2' | 'v3' | 'v4'} EngineId */

export const ENGINE_REGISTRY = {
  legacy: {
    id: 'legacy',
    label: 'Legacy',
    title: '继电保护逻辑框图 · Legacy 引擎',
    path: '/',
    description: '初版自定义布局（最长路径分层）',
    buildDiagram,
  },
  v1: {
    id: 'v1',
    label: 'V1',
    title: '继电保护逻辑框图 · V1 引擎',
    path: '/v1',
    description: 'DAG 拓扑排序 + Sugiyama 增量传播分层',
    buildDiagram: buildDiagramV1,
  },
  v2: {
    id: 'v2',
    label: 'V2',
    title: '继电保护逻辑框图 · V2 引擎',
    path: '/v2',
    description: 'ReactFlow + TypeScript DAG 自动布局（Sugiyama + 正交路由）',
  },
  v3: {
    id: 'v3',
    label: 'V3',
    title: '继电保护逻辑框图 · V3 引擎',
    path: '/v3',
    description: '逐步实现 · 当前阶段：connections + 按列顺序连线',
    buildDiagram: buildDiagramV3,
  },
  v4: {
    id: 'v4',
    label: 'V4',
    title: '继电保护逻辑框图 · V4 引擎',
    path: '/v4',
    description: 'Graph JSON → ELK Layout → ReactFlow 渲染',
  },
}

export const ENGINE_LIST = Object.values(ENGINE_REGISTRY)

export function getEngine(id) {
  return ENGINE_REGISTRY[id] ?? ENGINE_REGISTRY.legacy
}
