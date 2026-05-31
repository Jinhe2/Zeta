import { useEffect, useMemo } from 'react'
import GraphView from '../v2/components/GraphView'
import { adaptZetaConfig } from '../v2/demo/configAdapter'
import EngineNav from '../components/EngineNav'
import VersionBanner from '../components/VersionBanner'
import { useDiagramConfig } from '../hooks/useDiagramConfig'
import { getEngine } from '../engines/registry'
import '../App.css'

/**
 * V2 引擎页面：ReactFlow + TypeScript DAG 自动布局（v2.md 规范）
 */
export default function DiagramEnginePageV2() {
  const engine = getEngine('v2')
  const {
    samples,
    config,
    selectedSample,
    configName,
    configRevision,
    error,
    loading,
    fileInputRef,
    loadSampleByName,
    handleUpload,
  } = useDiagramConfig()

  useEffect(() => {
    document.title = engine.title
    document.body.dataset.engine = engine.id
    return () => {
      delete document.body.dataset.engine
    }
  }, [engine.id, engine.title])

  const graphData = useMemo(() => {
    if (!config) return null
    try {
      return adaptZetaConfig(config)
    } catch (err) {
      console.error('[v2]', err)
      return { error: err instanceof Error ? err.message : String(err) }
    }
  }, [config, configRevision])

  const diagramKey = `v2-${configRevision}-${selectedSample || 'upload'}`

  return (
    <div className="app app--engine-v2">
      <header className="toolbar">
        <h1 className="title">{engine.title}</h1>
        <EngineNav active={engine.id} />
        <div className="controls">
          <label className="control-group">
            <span>样本配置</span>
            <select
              value={selectedSample}
              disabled={loading || samples.length === 0}
              onChange={(e) => loadSampleByName(e.target.value)}
            >
              {samples.length === 0 ? (
                <option value="">无可用样本</option>
              ) : (
                samples.map((s) => (
                  <option key={s.filename} value={s.filename}>
                    {s.name} ({s.filename})
                  </option>
                ))
              )}
            </select>
          </label>
          <button
            type="button"
            className="btn-upload"
            disabled={loading}
            onClick={() => fileInputRef.current?.click()}
          >
            上传 JSON
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".json,application/json"
            hidden
            onChange={handleUpload}
          />
        </div>
      </header>

      <VersionBanner engine={engine} />

      {error && <div className="banner-error">{error}</div>}

      <div className="meta">
        {loading && <span className="meta-loading">加载中…</span>}
        {!loading && configName && <span className="meta-name">{configName}</span>}
        {!loading && config?.description && (
          <span className="meta-desc">{config.description}</span>
        )}
        <span className="meta-tag meta-tag-v2">当前引擎：V2 · ReactFlow</span>
        {!loading && configRevision > 0 && (
          <span className="meta-revision">渲染 #{configRevision}</span>
        )}
      </div>

      <main className="main main--v2">
        {loading ? (
          <div className="diagram-empty">正在加载配置…</div>
        ) : !graphData ? (
          <div className="diagram-empty">请选择或上传 JSON 配置文件</div>
        ) : 'error' in graphData ? (
          <div className="diagram-error">[V2] 渲染失败：{graphData.error}</div>
        ) : (
          <GraphView key={diagramKey} data={graphData} />
        )}
      </main>
    </div>
  )
}
