import { listAllNodeIds } from './configMutations'

const GATE_TYPES = ['AND', 'OR', 'NAND', 'NOR', 'XOR']

export default function NodePropertyPanel({ meta, config, onChange, onDelete }) {
  if (!meta) {
    return (
      <div className="logic-visual-editor__panel">
        <h3>节点属性</h3>
        <p className="logic-visual-editor__hint">点击画布中的节点进行编辑，或使用左侧按钮新增节点。</p>
      </div>
    )
  }

  const { kind, item } = meta
  const allIds = listAllNodeIds(config).filter((id) => id !== item.id)

  const patch = (fields) => onChange({ ...item, ...fields })

  return (
    <div className="logic-visual-editor__panel">
      <div className="logic-visual-editor__panel-head">
        <h3>节点属性</h3>
        <button type="button" className="users-page__link users-page__link--danger" onClick={onDelete}>
          删除
        </button>
      </div>
      <p className="logic-visual-editor__node-id">ID：{item.id}</p>

      {kind === 'input' && (
        <>
          <label>
            名称
            <input value={item.name ?? ''} onChange={(e) => patch({ name: e.target.value })} />
          </label>
          <label>
            通道类型
            <input value={item.channelType ?? ''} onChange={(e) => patch({ channelType: e.target.value })} />
          </label>
          <label>
            单位
            <input value={item.unit ?? ''} onChange={(e) => patch({ unit: e.target.value })} />
          </label>
          <label>
            阈值定值引用
            <input
              value={item.thresholdRef ?? ''}
              onChange={(e) => patch({ thresholdRef: e.target.value })}
              placeholder="settings 中的 ID"
            />
          </label>
          <label>
            通道引用
            <input value={item.channelRef ?? ''} onChange={(e) => patch({ channelRef: e.target.value })} />
          </label>
        </>
      )}

      {kind === 'gate' && (
        <>
          <label>
            逻辑门类型
            <select value={item.type ?? 'AND'} onChange={(e) => patch({ type: e.target.value })}>
              {GATE_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </label>
          <label className="users-page__checkbox">
            <input
              type="checkbox"
              checked={!!item.inverted}
              onChange={(e) => patch({ inverted: e.target.checked })}
            />
            输出取反
          </label>
          <label>
            输入节点（每行一个 ID）
            <textarea
              rows={4}
              value={(item.inputs ?? [])
                .map((inp) => (typeof inp === 'object' ? inp.node : inp))
                .join('\n')}
              onChange={(e) => {
                const inputs = e.target.value
                  .split('\n')
                  .map((s) => s.trim())
                  .filter(Boolean)
                  .map((node) => ({ node, inverted: false }))
                patch({ inputs })
              }}
            />
          </label>
        </>
      )}

      {kind === 'timer' && (
        <>
          <label>
            名称
            <input value={item.name ?? ''} onChange={(e) => patch({ name: e.target.value })} />
          </label>
          <label>
            输入节点
            <select value={item.input ?? ''} onChange={(e) => patch({ input: e.target.value })}>
              <option value="">— 未连接 —</option>
              {allIds.map((id) => (
                <option key={id} value={id}>
                  {id}
                </option>
              ))}
            </select>
          </label>
          <label>
            延时定值引用
            <input value={item.delayRef ?? ''} onChange={(e) => patch({ delayRef: e.target.value })} />
          </label>
        </>
      )}

      {kind === 'output' && (
        <>
          <label>
            名称
            <input value={item.name ?? ''} onChange={(e) => patch({ name: e.target.value })} />
          </label>
          <label>
            输入节点
            <select value={item.input ?? ''} onChange={(e) => patch({ input: e.target.value })}>
              <option value="">— 未连接 —</option>
              {allIds.map((id) => (
                <option key={id} value={id}>
                  {id}
                </option>
              ))}
            </select>
          </label>
          <label>
            通道引用
            <input value={item.channelRef ?? ''} onChange={(e) => patch({ channelRef: e.target.value })} />
          </label>
        </>
      )}
    </div>
  )
}
