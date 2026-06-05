import './ConfigPanel.css'

function count(list) {
  return Array.isArray(list) ? list.length : 0
}

export default function ConfigPanel({ config, title, loading }) {
  return (
    <aside className="config-panel">
      <header className="config-panel__header">
        <h2>配置信息</h2>
      </header>
      <div className="config-panel__body">
        {loading && <p className="config-panel__placeholder">加载中…</p>}
        {!loading && !config && <p className="config-panel__placeholder">暂无配置</p>}
        {!loading && config && (
          <>
            <section>
              <h3>基本信息</h3>
              <p>{title || config.name || '—'}</p>
              {config.description && <p className="config-panel__muted">{config.description}</p>}
            </section>
            <section>
              <h3>统计</h3>
              <ul className="config-panel__stats">
                <li>输入 {count(config.inputs)}</li>
                <li>逻辑门 {count(config.gates)}</li>
                <li>时间元件 {count(config.timers)}</li>
                <li>输出 {count(config.outputs)}</li>
                <li>定值 {count(config.settings)}</li>
              </ul>
            </section>
          </>
        )}
      </div>
    </aside>
  )
}
