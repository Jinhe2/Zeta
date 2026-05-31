import type { ReactNode } from 'react'
import './ConfigInfoPanel.css'

interface ZetaConfigLike {
  version?: string
  name?: string
  description?: string
  inputs?: Array<Record<string, unknown>>
  gates?: Array<Record<string, unknown>>
  timers?: Array<Record<string, unknown>>
  outputs?: Array<Record<string, unknown>>
  settings?: Array<Record<string, unknown>>
  displayState?: Record<string, string>
  nodes?: unknown[]
  edges?: unknown[]
}

export interface ConfigInfoPanelProps {
  config: unknown
  configName?: string
  filename?: string
  loading?: boolean
}

function isGraphJson(config: ZetaConfigLike): boolean {
  return Array.isArray(config.nodes) && Array.isArray(config.edges)
}

function formatValue(value: unknown): string {
  if (value == null || value === '') return '—'
  return String(value)
}

function Section({
  title,
  count,
  children,
}: {
  title: string
  count?: number
  children: ReactNode
}) {
  return (
    <section className="v4-config-section">
      <h3 className="v4-config-section__title">
        {title}
        {count != null && <span className="v4-config-section__count">{count}</span>}
      </h3>
      {children}
    </section>
  )
}

function ConfigList({ items }: { items: Array<{ id: string; lines: string[] }> }) {
  if (items.length === 0) {
    return <p className="v4-config-empty">无</p>
  }
  return (
    <ul className="v4-config-list">
      {items.map((item) => (
        <li key={item.id} className="v4-config-list__item">
          <div className="v4-config-list__id">{item.id}</div>
          {item.lines.map((line) => (
            <div key={line} className="v4-config-list__line">
              {line}
            </div>
          ))}
        </li>
      ))}
    </ul>
  )
}

function ZetaConfigPanel({ config, configName, filename }: Required<Pick<ConfigInfoPanelProps, 'config'>> & ConfigInfoPanelProps) {
  const c = config as ZetaConfigLike
  const inputs = c.inputs ?? []
  const gates = c.gates ?? []
  const timers = c.timers ?? []
  const outputs = c.outputs ?? []
  const settings = c.settings ?? []
  const displayState = c.displayState ?? {}

  return (
    <>
      <Section title="基本信息">
        <dl className="v4-config-dl">
          <div>
            <dt>名称</dt>
            <dd>{formatValue(c.name ?? configName)}</dd>
          </div>
          <div>
            <dt>文件</dt>
            <dd>{formatValue(filename)}</dd>
          </div>
          <div>
            <dt>版本</dt>
            <dd>{formatValue(c.version)}</dd>
          </div>
          {c.description && (
            <div>
              <dt>说明</dt>
              <dd>{c.description}</dd>
            </div>
          )}
        </dl>
      </Section>

      <Section title="统计">
        <dl className="v4-config-stats">
          <div><dt>输入</dt><dd>{inputs.length}</dd></div>
          <div><dt>逻辑门</dt><dd>{gates.length}</dd></div>
          <div><dt>时间元件</dt><dd>{timers.length}</dd></div>
          <div><dt>输出</dt><dd>{outputs.length}</dd></div>
          <div><dt>定值</dt><dd>{settings.length}</dd></div>
        </dl>
      </Section>

      {settings.length > 0 && (
        <Section title="定值" count={settings.length}>
          <ConfigList
            items={settings.map((s) => ({
              id: String(s.id ?? ''),
              lines: [
                s.name ? String(s.name) : '',
                [
                  s.defaultValue != null ? `默认 ${s.defaultValue}` : '',
                  s.unit ? String(s.unit) : '',
                ]
                  .filter(Boolean)
                  .join(' · '),
              ].filter(Boolean),
            }))}
          />
        </Section>
      )}

      <Section title="输入" count={inputs.length}>
        <ConfigList
          items={inputs.map((inp) => {
            const id = String(inp.id ?? '')
            const state = displayState[id]
            return {
              id,
              lines: [
                inp.name ? String(inp.name) : '',
                [
                  inp.channelRef ? String(inp.channelRef) : '',
                  inp.channelType ? String(inp.channelType) : '',
                ]
                  .filter(Boolean)
                  .join(' · '),
                [
                  inp.comparison ? String(inp.comparison) : '',
                  inp.thresholdValue != null ? `阈值 ${inp.thresholdValue}` : '',
                  inp.thresholdRef ? `定值 ${inp.thresholdRef}` : '',
                ]
                  .filter(Boolean)
                  .join(' · '),
                state != null ? `显示 ${state}` : '',
              ].filter(Boolean),
            }
          })}
        />
      </Section>

      <Section title="逻辑门" count={gates.length}>
        <ConfigList
          items={gates.map((gate) => {
            const gateInputs = Array.isArray(gate.inputs) ? gate.inputs : []
            const inputDesc = gateInputs
              .map((inp) => {
                const node = typeof inp === 'object' && inp ? String((inp as { node?: string }).node ?? '') : String(inp)
                const inv = typeof inp === 'object' && inp && (inp as { inverted?: boolean }).inverted
                return inv ? `¬${node}` : node
              })
              .join(', ')
            return {
              id: String(gate.id ?? ''),
              lines: [
                gate.type ? String(gate.type) : '',
                inputDesc ? `输入 ${inputDesc}` : '',
                gate.inverted ? '输出取反' : '',
              ].filter(Boolean),
            }
          })}
        />
      </Section>

      {timers.length > 0 && (
        <Section title="时间元件" count={timers.length}>
          <ConfigList
            items={timers.map((t) => ({
              id: String(t.id ?? ''),
              lines: [
                t.name ? String(t.name) : '',
                t.input ? `输入 ${String(t.input)}` : '',
                t.delayRef ? `延时 ${String(t.delayRef)}` : '',
                t.delayValue != null ? `${t.delayValue}s` : '',
              ].filter(Boolean),
            }))}
          />
        </Section>
      )}

      <Section title="输出" count={outputs.length}>
        <ConfigList
          items={outputs.map((out) => ({
            id: String(out.id ?? ''),
            lines: [
              out.name ? String(out.name) : '',
              out.input ? `输入 ${String(out.input)}` : '',
              out.channelRef ? String(out.channelRef) : '',
            ].filter(Boolean),
          }))}
        />
      </Section>
    </>
  )
}

function GraphConfigPanel({ config }: { config: ZetaConfigLike }) {
  const nodes = config.nodes ?? []
  const edges = config.edges ?? []
  const byType = nodes.reduce<Record<string, number>>((acc, n) => {
    const node = n as { type?: string }
    const t = node.type ?? 'unknown'
    acc[t] = (acc[t] ?? 0) + 1
    return acc
  }, {})

  return (
    <>
      <Section title="Graph JSON">
        <dl className="v4-config-stats">
          <div><dt>节点</dt><dd>{nodes.length}</dd></div>
          <div><dt>连线</dt><dd>{edges.length}</dd></div>
          {Object.entries(byType).map(([type, count]) => (
            <div key={type}>
              <dt>{type}</dt>
              <dd>{count}</dd>
            </div>
          ))}
        </dl>
      </Section>
      <Section title="节点列表" count={nodes.length}>
        <ConfigList
          items={nodes.map((n) => {
            const node = n as { id?: string; name?: string; type?: string }
            return {
              id: String(node.id ?? ''),
              lines: [node.name ? String(node.name) : '', node.type ? String(node.type) : ''].filter(Boolean),
            }
          })}
        />
      </Section>
    </>
  )
}

export default function ConfigInfoPanel({
  config,
  configName,
  filename,
  loading,
}: ConfigInfoPanelProps) {
  return (
    <aside className="v4-config-panel">
      <header className="v4-config-panel__header">
        <h2 className="v4-config-panel__title">配置信息</h2>
      </header>

      <div className="v4-config-panel__body">
        {loading && <p className="v4-config-panel__placeholder">加载中…</p>}
        {!loading && !config && (
          <p className="v4-config-panel__placeholder">请选择或上传 JSON 配置文件</p>
        )}
        {!loading && config && (
          isGraphJson(config as ZetaConfigLike) ? (
            <GraphConfigPanel config={config as ZetaConfigLike} />
          ) : (
            <ZetaConfigPanel config={config} configName={configName} filename={filename} />
          )
        )}
      </div>
    </aside>
  )
}
