import { useCallback, useEffect, useState } from 'react'
import { ZetaGraphView } from '@zeta/diagram'
import {
  addNodeToConfig,
  connectNodesInConfig,
  deleteNodeFromConfig,
  disconnectEdgeInConfig,
  findNodeMeta,
  updateNodeInConfig,
} from './configMutations'
import NodePropertyPanel from './NodePropertyPanel'
import SettingsPanel from './SettingsPanel'
import './LogicVisualEditor.css'

function hasNodes(config) {
  return (
    (config.inputs?.length ?? 0) +
      (config.gates?.length ?? 0) +
      (config.timers?.length ?? 0) +
      (config.outputs?.length ?? 0) >
    0
  )
}

export default function LogicVisualEditor({ config, onConfigChange }) {
  const [selectedId, setSelectedId] = useState(null)
  const [rightTab, setRightTab] = useState('node')

  useEffect(() => {
    if (selectedId && !findNodeMeta(config, selectedId)) {
      setSelectedId(null)
    }
  }, [config, selectedId])

  const selectedMeta = selectedId ? findNodeMeta(config, selectedId) : null

  const applyConfig = useCallback(
    (next) => {
      onConfigChange(next)
    },
    [onConfigChange],
  )

  const handleAdd = (kind) => {
    const next = addNodeToConfig(config, kind)
    applyConfig(next)
    const listKey = kind === 'input' ? 'inputs' : kind === 'gate' ? 'gates' : kind === 'timer' ? 'timers' : 'outputs'
    const added = next[listKey]?.[next[listKey].length - 1]
    if (added?.id) {
      setSelectedId(added.id)
      setRightTab('node')
    }
  }

  const handleUpdate = (item) => {
    if (!selectedMeta) return
    applyConfig(updateNodeInConfig(config, selectedMeta.kind, selectedMeta.item.id, item))
  }

  const handleDelete = () => {
    if (!selectedMeta) return
    if (!window.confirm(`确定删除节点「${selectedMeta.item.id}」？`)) return
    applyConfig(deleteNodeFromConfig(config, selectedMeta.kind, selectedMeta.item.id))
    setSelectedId(null)
  }

  const handleConnect = useCallback(
    (connection) => {
      const { source, target } = connection
      if (!source || !target) return
      const targetMeta = findNodeMeta(config, target)
      if (!targetMeta || targetMeta.kind === 'input') return
      applyConfig(connectNodesInConfig(config, source, target))
    },
    [applyConfig, config],
  )

  const handleEdgeClick = useCallback(
    (_, edge) => {
      if (!window.confirm('删除这条连线？')) return
      applyConfig(disconnectEdgeInConfig(config, edge.source, edge.target))
    },
    [applyConfig, config],
  )

  return (
    <div className="logic-visual-editor">
      <aside className="logic-visual-editor__toolbar">
        <h3>添加节点</h3>
        <button type="button" className="users-page__btn" onClick={() => handleAdd('input')}>
          + 输入
        </button>
        <button type="button" className="users-page__btn" onClick={() => handleAdd('gate')}>
          + 逻辑门
        </button>
        <button type="button" className="users-page__btn" onClick={() => handleAdd('timer')}>
          + 时间元件
        </button>
        <button type="button" className="users-page__btn" onClick={() => handleAdd('output')}>
          + 输出
        </button>
        <p className="logic-visual-editor__hint">
          从节点右侧连接点拖拽到目标节点左侧以建立连线；点击连线可删除。预览与学员端框图使用同一 V4 渲染引擎。
        </p>
        <button type="button" className="users-page__btn" onClick={() => setRightTab('settings')}>
          管理定值 →
        </button>
      </aside>

      <div className="logic-visual-editor__canvas">
        {!hasNodes(config) ? (
          <p className="logic-visual-editor__empty">暂无节点，请从左侧添加或切换到 JSON 模式编辑。</p>
        ) : (
          <ZetaGraphView
            config={config}
            showDevInfo={false}
            editable
            selectedNodeId={selectedId}
            onNodeSelect={setSelectedId}
            onConnect={handleConnect}
            onEdgeClick={handleEdgeClick}
          />
        )}
      </div>

      <div className="logic-visual-editor__right">
        <div className="logic-visual-editor__right-tabs">
          <button
            type="button"
            className={`logic-visual-editor__right-tab${rightTab === 'node' ? ' logic-visual-editor__right-tab--active' : ''}`}
            onClick={() => setRightTab('node')}
          >
            节点
          </button>
          <button
            type="button"
            className={`logic-visual-editor__right-tab${rightTab === 'settings' ? ' logic-visual-editor__right-tab--active' : ''}`}
            onClick={() => setRightTab('settings')}
          >
            定值
          </button>
        </div>
        {rightTab === 'node' ? (
          <NodePropertyPanel
            meta={selectedMeta}
            config={config}
            onChange={handleUpdate}
            onDelete={handleDelete}
          />
        ) : (
          <SettingsPanel config={config} onConfigChange={applyConfig} />
        )}
      </div>
    </div>
  )
}
