import { useEffect } from 'react'
import DiagramCanvas from '../components/DiagramCanvas'
import EngineNav from '../components/EngineNav'
import VersionBanner from '../components/VersionBanner'
import { useDiagramConfig } from '../hooks/useDiagramConfig'
import { getEngine } from '../engines/registry'
import '../App.css'

/** V3 专用页面 */
export default function DiagramEnginePageV3() {
  const engine = getEngine('v3')
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

  const diagramKey = `v3-${configRevision}-${selectedSample || 'upload'}`

  return (
    <div className={`app app--engine-${engine.id}`}>
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
        <span className={`meta-tag meta-tag-${engine.id}`}>
          当前引擎：{engine.label}
        </span>
        {!loading && configRevision > 0 && (
          <span className="meta-revision">渲染 #{configRevision}</span>
        )}
      </div>

      <main className="main">
        {loading ? (
          <div className="diagram-empty">正在加载配置…</div>
        ) : (
          <DiagramCanvas
            key={diagramKey}
            engineId={engine.id}
            engineLabel={engine.label}
            buildDiagram={engine.buildDiagram}
            config={config}
          />
        )}
      </main>
    </div>
  )
}
