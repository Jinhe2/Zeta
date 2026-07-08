import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
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

const HEARTBEAT_INTERVAL = 5000
const POLL_INTERVAL = 3000
const EXPERIMENT_SUCCESS_MESSAGE = '恭喜成功完成实验'
const EXPERIMENT_FAILED_MESSAGE = '实验失败了，请结合中间文件分析结果进一步确认原因'
const EXPERIMENT_DIAGNOSIS_MESSAGE = '实验失败了，请重新学习逻辑框图和相关操作'

/** 将 v2.3 snapshot JSON 解析为 sections 数组 */
function parseSnapshotSections(snapshotJson) {
  let data
  if (typeof snapshotJson === 'string') {
    try { data = JSON.parse(snapshotJson) } catch { return [] }
  } else {
    data = snapshotJson
  }

  const nodes = data.nodes ?? []
  const timestamps = data.timestamps ?? []
  const channels = data.channels ?? []

  if (!timestamps.length || !nodes.length) return []

  const baseTime = parseTimestampMs(timestamps[0])

  return timestamps.map((ts, k) => {
    const states = {}
    for (let i = 0; i < nodes.length && i < channels.length; i++) {
      const nodeId = nodes[i].id
      const values = channels[i]?.values
      states[nodeId] = values && k < values.length ? values[k] !== 0 : false
    }
    const elapsedSec = (parseTimestampMs(ts) - baseTime) / 1000
    return {
      id: `section-${k}`,
      label: `T = ${elapsedSec.toFixed(3)} s`,
      time: elapsedSec,
      timestamp: ts,
      states,
    }
  })
}

function parseTimestampMs(ts) {
  const spaceIdx = ts.indexOf(' ')
  const timePart = spaceIdx >= 0 ? ts.substring(spaceIdx + 1) : ts
  const parts = timePart.split(/[:.]/)
  const h = parseInt(parts[0]) || 0
  const m = parseInt(parts[1]) || 0
  const s = parseInt(parts[2]) || 0
  const ms = parseInt(parts[3]) || 0
  return ((h * 60 + m) * 60 + s) * 1000 + ms
}

function parseSnapshotMeta(snapshotJson) {
  if (!snapshotJson) return {}
  if (typeof snapshotJson === 'object') return snapshotJson
  try {
    return JSON.parse(snapshotJson)
  } catch {
    return {}
  }
}

function normalizeBoolean(value) {
  if (typeof value === 'boolean') return value
  if (typeof value === 'string') {
    const normalized = value.trim().toLowerCase()
    if (normalized === 'true') return true
    if (normalized === 'false') return false
  }
  return undefined
}

function getExperimentDialogMessage(result, task) {
  const snapshotMeta = parseSnapshotMeta(task?.snapshotJson)
  const resultType = result?.result_type
    ?? result?.resultType
    ?? task?.resultType
    ?? snapshotMeta.resultType

  if (resultType === 'diagnosis' || resultType === 'diagnosis_v2') {
    return EXPERIMENT_DIAGNOSIS_MESSAGE
  }

  if (resultType === 'snapshot') {
    const experimentPassed = normalizeBoolean(
      result?.experiment_passed
        ?? result?.experimentPassed
        ?? task?.experimentPassed
        ?? snapshotMeta.experimentPassed
    )

    if (experimentPassed === true) return EXPERIMENT_SUCCESS_MESSAGE
    if (experimentPassed === false) return EXPERIMENT_FAILED_MESSAGE
  }

  return ''
}

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
  const [error, setError] = useState(null)
  const [jsonViewer, setJsonViewer] = useState({ open: false, title: '', json: '' })
  const [importOpen, setImportOpen] = useState(false)
  const [experimentDialog, setExperimentDialog] = useState({ open: false, message: '' })

  // 实验监视状态
  const [monitoring, setMonitoring] = useState(false)
  const [monitorStatus, setMonitorStatus] = useState('') // '' | 'starting' | 'watching' | 'completed' | 'failed'
  const taskUuidRef = useRef(null)
  const heartbeatRef = useRef(null)
  const pollRef = useRef(null)

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
      })
      .catch((err) => {
        if (!cancelled) setError(err.message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => { cancelled = true }
  }, [id])

  // 清理：组件卸载时停止心跳和轮询
  const clearMonitorTimers = useCallback(() => {
    if (heartbeatRef.current) {
      clearInterval(heartbeatRef.current)
      heartbeatRef.current = null
    }
    if (pollRef.current) {
      clearInterval(pollRef.current)
      pollRef.current = null
    }
  }, [])

  useEffect(() => {
    return () => {
      clearMonitorTimers()
      if (taskUuidRef.current) {
        api.endLogicMonitor(taskUuidRef.current).catch(() => {})
      }
    }
  }, [clearMonitorTimers])

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
      .then((detailData) => {
        setJsonViewer({ open: true, title: label || `断面 #${snapshotId}`, json: detailData.snapshotJson })
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
      setSnapshots((prev) => prev.some((s) => s.id === result.id) ? prev : [newSnap, ...prev])
      setSelectedSnapshotId(result.id)
      return api.getSnapshotSections(result.id).then((secs) => {
        setSections(secs)
        setSelectedSectionId(secs[0]?.id ?? null)
      })
    })
  }, [id, detail])

  // 加载 monitor task 结果并渲染断面
  const loadMonitorTaskResult = useCallback(async (snapshotPath) => {
    try {
      const task = await api.getMonitorTask(snapshotPath)
      if (task.snapshotJson) {
        const secs = parseSnapshotSections(task.snapshotJson)
        setSections(secs)
        setSelectedSectionId(secs[0]?.id ?? null)

        // 添加到快照列表（去重）
        setSnapshots((prev) => {
          if (prev.some((s) => s.id === task.id)) return prev
          return [{
            id: task.id,
            status: task.state === 'COMPLETED' ? 'COMPLETED' : task.state,
            source: 'MONITOR',
            totalTransitions: task.totalTransitions ?? 0,
            createdAt: task.createdAt,
            logicId: Number(id),
            logicCode: detail?.code,
            logicName: detail?.title,
          }, ...prev]
        })
        setSelectedSnapshotId(task.id)
      }
      return task
    } catch (err) {
      setError('加载实验结果失败: ' + err.message)
      return null
    }
  }, [id, detail])

  // 轮询任务结果
  const startResultPolling = useCallback((taskUuid) => {
    if (pollRef.current) clearInterval(pollRef.current)

    pollRef.current = setInterval(async () => {
      try {
        const result = await api.getMonitorTaskResult(taskUuid)
        // 有结果了
        clearInterval(pollRef.current)
        pollRef.current = null
        if (heartbeatRef.current) {
          clearInterval(heartbeatRef.current)
          heartbeatRef.current = null
        }

        const snapshotPath = result.snapshot_path ?? result.snapshotPath

        if (result.result === 'success') {
          setMonitorStatus('completed')
          setMonitoring(false)
          if (taskUuidRef.current === taskUuid) taskUuidRef.current = null
          const task = snapshotPath ? await loadMonitorTaskResult(snapshotPath) : null
          const dialogMessage = getExperimentDialogMessage(result, task)
          if (dialogMessage) {
            setExperimentDialog({ open: true, message: dialogMessage })
          }
        } else if (result.result === 'failed') {
          setMonitorStatus('failed')
          setMonitoring(false)
          if (taskUuidRef.current === taskUuid) taskUuidRef.current = null
          setError('实验失败: ' + (result.error_message || '未知错误'))
        } else {
          setMonitorStatus('completed')
          setMonitoring(false)
          if (taskUuidRef.current === taskUuid) taskUuidRef.current = null
        }
      } catch {
        // 404 = 结果尚未返回，继续轮询
      }
    }, POLL_INTERVAL)
  }, [loadMonitorTaskResult])

  // 开始实验
  const handleStartExperiment = useCallback(async () => {
    if (!detail?.iedName || !detail?.code) {
      setError('缺少装置信息（iedName / logicId）')
      return
    }

    const previousTaskUuid = taskUuidRef.current
    clearMonitorTimers()
    taskUuidRef.current = null

    setMonitoring(true)
    setMonitorStatus('starting')
    setError(null)
    setExperimentDialog({ open: false, message: '' })
    setSections([])
    setSelectedSectionId(null)
    setSelectedSnapshotId(null)

    try {
      if (previousTaskUuid) {
        await api.endLogicMonitor(previousTaskUuid).catch(() => {})
      }

      const response = await api.startLogicMonitor(detail.iedName, detail.code)
      // req_id 就是 taskUuid
      const taskUuid = response.req_id || response.reqId
      if (!taskUuid) {
        throw new Error('未返回 taskUuid')
      }

      taskUuidRef.current = taskUuid
      setMonitorStatus('watching')

      // 启动心跳（5s）
      heartbeatRef.current = setInterval(() => {
        api.sendLogicMonitorHeartbeat(taskUuid).catch(() => {})
      }, HEARTBEAT_INTERVAL)

      // 启动结果轮询（3s）
      startResultPolling(taskUuid)
    } catch (err) {
      setMonitoring(false)
      setMonitorStatus('')
      setError('启动实验失败: ' + err.message)
    }
  }, [clearMonitorTimers, detail, startResultPolling])

  // 停止实验
  const handleStopExperiment = useCallback(async () => {
    const taskUuid = taskUuidRef.current
    if (!taskUuid) return

    clearMonitorTimers()

    try {
      await api.endLogicMonitor(taskUuid)
      setMonitorStatus('stopping')
      // 继续轮询等待最终结果
      startResultPolling(taskUuid)
    } catch (err) {
      setError('停止实验失败: ' + err.message)
      setMonitoring(false)
      setMonitorStatus('')
    }

    taskUuidRef.current = null
  }, [clearMonitorTimers, startResultPolling])

  // Reload data
  const handleReload = useCallback(() => {
    setLoading(true)
    setError(null)
    setExperimentDialog({ open: false, message: '' })
    setSections([])
    setSelectedSectionId(null)
    setSelectedSnapshotId(null)
    Promise.all([api.getProtectionLogic(id), api.listSnapshotsByLogic(id)])
      .then(([detailData, snapshotData]) => {
        setDetail(detailData)
        setSnapshots(snapshotData)
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => {
    if (detail?.title) document.title = detail.title
  }, [detail])

  const nodeStates = useMemo(() => {
    const section = sections.find((s) => s.id === selectedSectionId)
    return section?.states ?? null
  }, [sections, selectedSectionId])

  const formatSnapTime = (ts) => {
    if (!ts) return ''
    const d = new Date(ts)
    return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }

  const statusLabel = {
    starting: '正在启动…',
    watching: '实验监视中',
    stopping: '正在停止…',
    completed: '实验完成',
    failed: '实验失败',
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
            ) : !detail?.config ? (
              <div className="diagram-canvas__placeholder">
                <p>配置文件有误，请检查配置</p>
                <button
                  type="button"
                  className="diagram-canvas__trigger-btn"
                  onClick={handleReload}
                >
                  ↻ 刷新
                </button>
              </div>
            ) : (
              <>
                <div className="diagram-canvas__header">
                  <span>逻辑框图</span>
                  {monitoring ? (
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span style={{ color: '#ffd54f', fontSize: 12 }}>● {statusLabel[monitorStatus] || '监视中'}</span>
                      <button
                        type="button"
                        className="diagram-canvas__trigger-btn diagram-canvas__trigger-btn--inline diagram-canvas__trigger-btn--stop"
                        onClick={handleStopExperiment}
                      >
                        ■ 停止实验
                      </button>
                    </div>
                  ) : nodeStates ? (
                    <button
                      type="button"
                      className="diagram-canvas__trigger-btn diagram-canvas__trigger-btn--inline diagram-canvas__trigger-btn--restart"
                      onClick={handleStartExperiment}
                    >
                      ↻ 重新开始实验
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="diagram-canvas__trigger-btn diagram-canvas__trigger-btn--inline"
                      onClick={handleStartExperiment}
                    >
                      ▶ 开始实验
                    </button>
                  )}
                </div>
                <div className="diagram-canvas__area">
                  <ZetaGraphView
                    config={detail.config}
                    showDevInfo={false}
                    nodeStates={nodeStates}
                    className="diagram-canvas__preview"
                  />
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
                  disabled={monitoring}
                  onClick={handleStartExperiment}
                >
                  {monitoring ? '…' : '+ 新实验'}
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
                    {snap.source === 'MONITOR' && (
                      <span className="diagram-page__history-source diagram-page__history-source--monitor" title="实验监视">
                        E
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

      {experimentDialog.open && (
        <div className="experiment-result-dialog" role="dialog" aria-modal="true" aria-labelledby="experiment-result-dialog-message">
          <button
            type="button"
            className="experiment-result-dialog__mask"
            aria-label="关闭实验结果提示"
            onClick={() => setExperimentDialog({ open: false, message: '' })}
          />
          <div className="experiment-result-dialog__panel">
            <p id="experiment-result-dialog-message" className="experiment-result-dialog__message">
              {experimentDialog.message}
            </p>
            <button
              type="button"
              className="experiment-result-dialog__btn"
              onClick={() => setExperimentDialog({ open: false, message: '' })}
            >
              确定
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
