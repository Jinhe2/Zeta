import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ZetaGraphView } from '@zeta/diagram'
import { api } from '../api/client'
import SectionSelector from '../components/SectionSelector'
import ConfigPanel from '../components/ConfigPanel'
import JsonViewerModal from '../components/JsonViewerModal'
import SnapshotImportModal from '../components/SnapshotImportModal'
import { useAuth } from '../auth/AuthContext'
import './student/TabletShell.css'
import './StudentPages.css'

export default function StudentDiagramPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { logout } = useAuth()
  const [detail, setDetail] = useState(null)
  const [snapshots, setSnapshots] = useState([])
  const [selectedSnapshotId, setSelectedSnapshotId] = useState(null)
  const [sections, setSections] = useState([])
  const [selectedSectionId, setSelectedSectionId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [triggering, setTriggering] = useState(false)
  const [error, setError] = useState(null)
  const [jsonViewer, setJsonViewer] = useState({ open: false, title: '', json: '' })
  const [importOpen, setImportOpen] = useState(false)

  // Load detail + existing snapshots
  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    Promise.all([api.getProtectionLogic(id), api.listSnapshotsByLogic(id)])
      .then(([detailData, snapshotData]) => {
        if (cancelled) return
        setDetail(detailData)
        setSnapshots(snapshotData)
        // Auto-load latest snapshot if exists
        if (snapshotData.length > 0) {
          const latest = snapshotData[0]
          setSelectedSnapshotId(latest.id)
          return api.getSnapshotSections(latest.id).then((secs) => {
            if (cancelled) return
            setSections(secs)
            setSelectedSectionId(secs[0]?.id ?? null)
          })
        }
      })
      .catch((err) => {
        if (!cancelled) setError(err.message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => { cancelled = true }
  }, [id])

  // Load sections when switching snapshots
  const loadSnapshotSections = useCallback((snapshotId) => {
    setSelectedSnapshotId(snapshotId)
    setSections([])
    setSelectedSectionId(null)
    api.getSnapshotSections(snapshotId)
      .then((secs) => {
        setSections(secs)
        setSelectedSectionId(secs[0]?.id ?? null)
      })
      .catch((err) => setError(err.message))
  }, [])

  // View raw JSON
  const handleViewJson = useCallback((snapshotId, label) => {
    api.getSnapshotDetail(snapshotId)
      .then((detail) => {
        setJsonViewer({ open: true, title: label || `断面 #${snapshotId}`, json: detail.snapshotJson })
      })
      .catch((err) => setError(err.message))
  }, [])

  // Import JSON
  const handleImport = useCallback((jsonText) => {
    return api.importSnapshotJson(id, jsonText).then((result) => {
      const newSnap = {
        id: result.id,
        status: result.status,
        source: 'MANUAL',
        totalTransitions: result.totalTransitions,
        createdAt: new Date().toISOString(),
        logicId: Number(id),
        logicCode: detail?.code,
        logicName: detail?.title,
      }
      setSnapshots((prev) => [newSnap, ...prev])
      setSelectedSnapshotId(result.id)
      return api.getSnapshotSections(result.id).then((secs) => {
        setSections(secs)
        setSelectedSectionId(secs[0]?.id ?? null)
      })
    })
  }, [id, detail])

  // Trigger new experiment
  const handleTrigger = useCallback(() => {
    setTriggering(true)
    setError(null)
    api.triggerExperiment(id)
      .then((result) => {
        // Add to snapshot list and load its sections
        const newSnap = {
          id: result.id,
          status: result.status,
          totalTransitions: result.totalTransitions,
          createdAt: new Date().toISOString(),
          logicId: Number(id),
          logicCode: detail?.code,
          logicName: detail?.title,
        }
        setSnapshots((prev) => [newSnap, ...prev])
        setSelectedSnapshotId(result.id)
        return api.getSnapshotSections(result.id).then((secs) => {
          setSections(secs)
          setSelectedSectionId(secs[0]?.id ?? null)
        })
      })
      .catch((err) => setError(err.message))
      .finally(() => setTriggering(false))
  }, [id, detail])

  useEffect(() => {
    if (detail?.title) document.title = detail.title
  }, [detail])

  const nodeStates = useMemo(() => {
    const section = sections.find((s) => s.id === selectedSectionId)
    return section?.states ?? null
  }, [sections, selectedSectionId])

  const satisfiedCount = nodeStates ? Object.values(nodeStates).filter(Boolean).length : 0
  const totalCount = nodeStates ? Object.keys(nodeStates).length : 0

  const formatSnapTime = (ts) => {
    if (!ts) return ''
    const d = new Date(ts)
    return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }

  return (
    <div className="tablet-shell diagram-page">
      <header className="tablet-shell__header diagram-page__toolbar">
        <button type="button" className="tablet-shell__back" onClick={() => navigate('/student/modes/panorama')}>
          ← 返回列表
        </button>
        <h1>{detail?.title || '逻辑框图'}</h1>
        <button
          type="button"
          className="tablet-shell__logout"
          onClick={() => logout().then(() => navigate('/login', { replace: true }))}
        >
          退出登录
        </button>
      </header>

      {error && <div className="diagram-page__error">{error}</div>}

      <div className="diagram-page__body">
        <div className="diagram-page__workspace">
          <div className="diagram-canvas">
            {loading ? (
              <p className="diagram-canvas__placeholder">正在加载…</p>
            ) : sections.length === 0 ? (
              <div className="diagram-canvas__placeholder">
                {snapshots.length === 0 ? (
                  <>
                    <p>暂无实验数据</p>
                    <button
                      type="button"
                      className="diagram-canvas__trigger-btn"
                      disabled={triggering}
                      onClick={handleTrigger}
                    >
                      {triggering ? '实验进行中…' : '▶ 开始实验'}
                    </button>
                  </>
                ) : (
                  <p>请从右侧选择一次实验记录查看断面</p>
                )}
              </div>
            ) : (
              <>
                <div className="diagram-canvas__header">
                  <span>逻辑框图</span>
                  {nodeStates && (
                    <span>当前断面：满足 {satisfiedCount} / {totalCount}</span>
                  )}
                </div>
                <div className="diagram-canvas__area">
                  {detail?.config ? (
                    <ZetaGraphView
                      config={detail.config}
                      showDevInfo={false}
                      nodeStates={nodeStates}
                      className="diagram-canvas__preview"
                    />
                  ) : (
                    <p className="diagram-canvas__placeholder">暂无框图配置</p>
                  )}
                </div>
              </>
            )}
          </div>

          {!loading && sections.length > 0 && (
            <SectionSelector
              sections={sections}
              selectedId={selectedSectionId}
              onSelect={setSelectedSectionId}
            />
          )}
        </div>

        {/* Right sidebar: config + snapshot history */}
        <div className="diagram-page__sidebar">
          <ConfigPanel config={detail?.config} title={detail?.title} loading={loading} />

          <div className="diagram-page__history">
            <div className="diagram-page__history-header">
              <span>实验记录</span>
              <div className="diagram-page__history-actions">
                <button
                  type="button"
                  className="diagram-page__history-import-btn"
                  onClick={() => setImportOpen(true)}
                  title="导入断面 JSON"
                >
                  导入
                </button>
                <button
                  type="button"
                  className="diagram-page__history-trigger"
                  disabled={triggering}
                  onClick={handleTrigger}
                >
                  {triggering ? '…' : '+ 新实验'}
                </button>
              </div>
            </div>
            {snapshots.length === 0 ? (
              <p className="diagram-page__history-empty">暂无记录</p>
            ) : (
              <ul className="diagram-page__history-list">
                {snapshots.map((snap) => (
                  <li
                    key={snap.id}
                    className={`diagram-page__history-item${snap.id === selectedSnapshotId ? ' diagram-page__history-item--active' : ''}`}
                    onClick={() => loadSnapshotSections(snap.id)}
                  >
                    <span className="diagram-page__history-time">{formatSnapTime(snap.createdAt)}</span>
                    <span className="diagram-page__history-transitions">{snap.totalTransitions} 次变位</span>
                    <button
                      type="button"
                      className="diagram-page__history-json-btn"
                      title="查看原始 JSON"
                      onClick={(e) => {
                        e.stopPropagation()
                        handleViewJson(snap.id, `断面 #${snap.id} · ${formatSnapTime(snap.createdAt)}`)
                      }}
                    >
                      {'{ }'}
                    </button>
                    {snap.source === 'MANUAL' && (
                      <span className="diagram-page__history-source diagram-page__history-source--manual" title="手动导入">
                        M
                      </span>
                    )}
                    <span className={`diagram-page__history-status diagram-page__history-status--${snap.status?.toLowerCase()}`}>
                      {snap.status}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>

      <JsonViewerModal
        open={jsonViewer.open}
        title={jsonViewer.title}
        jsonString={jsonViewer.json}
        onClose={() => setJsonViewer({ open: false, title: '', json: '' })}
      />

      <SnapshotImportModal
        open={importOpen}
        onClose={() => setImportOpen(false)}
        onImport={handleImport}
      />
    </div>
  )
}
