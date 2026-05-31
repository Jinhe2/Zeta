import { useEffect, useMemo } from 'react'
import GraphView from '../v4/components/GraphView'
import ConfigInfoPanel from '../v4/components/ConfigInfoPanel'
import { parseGraphJson } from '../v4/demo/parseGraphJson'
import EngineNav from '../components/EngineNav'
import VersionBanner from '../components/VersionBanner'
import { useDiagramConfig } from '../hooks/useDiagramConfig'
import { getEngine } from '../engines/registry'
import '../App.css'

/**
 * V4 引擎页面：Graph JSON → ELK Layout → ReactFlow
 */
export default function DiagramEnginePageV4() {
  const engine = getEngine('v4')
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
      return parseGraphJson(config)
    } catch (err) {
      console.error('[v4]', err)
      return { error: err instanceof Error ? err.message : String(err) }
    }
  }, [config, configRevision])

  const diagramKey = `v4-${configRevision}-${selectedSample || 'upload'}`

  return (
    <div className="app app--engine-v4">
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
        <span className="meta-tag meta-tag-v4">当前引擎：V4 · ELK + ReactFlow</span>
        {!loading && configRevision > 0 && (
          <span className="meta-revision">渲染 #{configRevision}</span>
        )}
      </div>

      <div className="v4-workspace">
        <main className="main main--v4 v4-workspace__diagram">
          {loading ? (
            <div className="diagram-empty">正在加载配置…</div>
          ) : !graphData ? (
            <div className="diagram-empty">请选择或上传 JSON 配置文件</div>
          ) : 'error' in graphData ? (
            <div className="diagram-error">[V4] 渲染失败：{graphData.error}</div>
          ) : (
            <GraphView key={diagramKey} data={graphData} />
          )}
        </main>

        <ConfigInfoPanel
          config={config}
          configName={configName}
          filename={selectedSample || undefined}
          loading={loading}
        />
      </div>
    </div>
  )
}
