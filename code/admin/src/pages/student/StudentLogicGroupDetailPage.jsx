/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { LogicGroupView } from '@zeta/diagram'
import { api } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import {
  buildExperimentPrecheckMismatchDialog,
  buildExperimentStartConfirmDialog,
  canStartAfterExperimentPrecheck,
} from '../../utils/settingCheck'
import ExperimentGuideDialog from '../../components/ExperimentGuideDialog'
import './TabletShell.css'
import '../StudentPages.css'

const HEARTBEAT_INTERVAL = 5000
const POLL_INTERVAL = 3000

const statusLabel = {
  checking: '基准校核中…',
  starting: '正在启动…',
  watching: '实验监视中',
  stopping: '正在停止…',
  completed: '实验完成',
  failed: '实验失败',
}

function mapLogicResults(logicResults) {
  const map = {}
  for (const r of (logicResults || [])) {
    const key = r.logic_diagram_id ?? r.logicDiagramId
    if (key == null) continue
    map[String(key)] = {
      success: r.success,
      experimentPassed: r.experiment_passed ?? r.experimentPassed,
      totalTransitions: r.total_transitions ?? r.totalTransitions,
      error: r.error ?? r.errorMessage ?? r.error_message,
    }
  }
  return map
}

function readPassed(value) {
  if (typeof value === 'boolean') return value
  if (value === 1 || value === '1' || value === 'true') return true
  if (value === 0 || value === '0' || value === 'false') return false
  return null
}

export default function StudentLogicGroupDetailPage() {
  const { groupId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { logout } = useAuth()

  const [detail, setDetail] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [memberResults, setMemberResults] = useState({})
  const [overallPassed, setOverallPassed] = useState(null)

  const [monitoring, setMonitoring] = useState(false)
  const [monitorStatus, setMonitorStatus] = useState('')
  const taskUuidRef = useRef(null)
  const heartbeatRef = useRef(null)
  const pollRef = useRef(null)
  const startConfirmationInFlightRef = useRef(false)

  const [experimentDialog, setExperimentDialog] = useState({ open: false, title: '', message: '' })
  const [snapshots, setSnapshots] = useState([])

  const [guideOpen, setGuideOpen] = useState(false)
  const [guideItems, setGuideItems] = useState([])
  const [guideLoading, setGuideLoading] = useState(false)

  const fromCoach = location.state?.from === 'coach'
  const listState = {
    ...(fromCoach ? { from: 'coach', section: 'logic' } : {}),
    ...(location.state?.deviceId ? { deviceId: location.state.deviceId } : {}),
  }

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [detailData, snapshotData] = await Promise.all([
        api.getKnowledgeLogicGroup(groupId),
        api.listLogicGroupSnapshots(groupId).catch(() => []),
      ])
      setDetail(detailData)
      setSnapshots(snapshotData)
    } catch (err) {
      setError(err.message || '加载组合逻辑失败')
    } finally {
      setLoading(false)
    }
  }, [groupId])

  const loadSnapshots = useCallback(async () => {
    try {
      const snapshotData = await api.listLogicGroupSnapshots(groupId).catch(() => [])
      setSnapshots(snapshotData)
    } catch {
      // 忽略刷新失败
    }
  }, [groupId])

  useEffect(() => {
    load()
  }, [load])

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
        api.endLogicGroupMonitor(taskUuidRef.current).catch(() => {})
      }
    }
  }, [clearMonitorTimers])

  const startResultPolling = useCallback((taskUuid) => {
    if (pollRef.current) clearInterval(pollRef.current)
    pollRef.current = setInterval(async () => {
      try {
        const result = await api.getMonitorTaskResult(taskUuid)
        clearInterval(pollRef.current)
        pollRef.current = null
        if (heartbeatRef.current) {
          clearInterval(heartbeatRef.current)
          heartbeatRef.current = null
        }

        const logicResults = result?.logic_results ?? result?.logicResults ?? []
        setMemberResults(mapLogicResults(logicResults))
        setOverallPassed(readPassed(result?.experiment_passed ?? result?.experimentPassed))

        if (result?.result === 'success') {
          setMonitorStatus('completed')
          setMonitoring(false)
          setExperimentDialog({ open: true, title: '实验结果', message: readPassed(result?.experiment_passed ?? result?.experimentPassed) === true ? '恭喜成功完成组合实验' : '组合实验未全部通过，请结合各基础逻辑结果分析原因' })
        } else if (result?.result === 'failed') {
          setMonitorStatus('failed')
          setMonitoring(false)
          setExperimentDialog({ open: true, title: '实验结果', message: '组合实验失败: ' + (result.error_message || '未知错误') })
        } else {
          setMonitorStatus('completed')
          setMonitoring(false)
          setExperimentDialog({ open: true, title: '实验结果', message: '组合实验已结束' })
        }
        if (taskUuidRef.current === taskUuid) taskUuidRef.current = null
        loadSnapshots()
      } catch {
        // 404 = 结果尚未返回，继续轮询
      }
    }, POLL_INTERVAL)
  }, [loadSnapshots])

  const startExperimentAfterConfirmation = useCallback(async () => {
    if (startConfirmationInFlightRef.current) return
    startConfirmationInFlightRef.current = true
    taskUuidRef.current = null
    setExperimentDialog({ open: false, title: '', message: '' })
    setMonitoring(true)
    setMonitorStatus('starting')
    setError(null)
    setMemberResults({})
    setOverallPassed(null)
    try {
      const response = await api.startLogicGroupMonitor(Number(groupId))
      const taskUuid = response.req_id || response.reqId
      if (!taskUuid) {
        throw new Error('未返回 taskUuid')
      }
      taskUuidRef.current = taskUuid
      setMonitorStatus('watching')
      heartbeatRef.current = setInterval(() => {
        api.sendLogicGroupMonitorHeartbeat(taskUuid).catch(() => {})
      }, HEARTBEAT_INTERVAL)
      startResultPolling(taskUuid)
    } catch (err) {
      startConfirmationInFlightRef.current = false
      setMonitoring(false)
      setMonitorStatus('')
      setError('启动组合实验失败: ' + err.message)
    }
  }, [groupId, startResultPolling])

  const handleStartExperiment = useCallback(async () => {
    taskUuidRef.current = null
    clearMonitorTimers()
    setMonitoring(true)
    setMonitorStatus('checking')
    setError(null)
    setExperimentDialog({ open: false, title: '', message: '' })
    try {
      const precheck = await api.checkExperimentPreconditionsForGroup(Number(groupId))
      setMonitoring(false)
      setMonitorStatus('')
      if (!canStartAfterExperimentPrecheck(precheck)) {
        setExperimentDialog(buildExperimentPrecheckMismatchDialog(precheck))
        return
      }
      setExperimentDialog(buildExperimentStartConfirmDialog())
    } catch (err) {
      setMonitoring(false)
      setMonitorStatus('')
      setError('组合实验前基准校核失败: ' + err.message)
    }
  }, [clearMonitorTimers, groupId])

  const handleStopExperiment = useCallback(async () => {
    const taskUuid = taskUuidRef.current
    if (!taskUuid) return
    clearMonitorTimers()
    try {
      await api.endLogicGroupMonitor(taskUuid)
      setMonitorStatus('stopping')
      startResultPolling(taskUuid)
    } catch (err) {
      setError('停止组合实验失败: ' + err.message)
      setMonitoring(false)
      setMonitorStatus('')
    }
    taskUuidRef.current = null
  }, [clearMonitorTimers, startResultPolling])

  const handleOpenGuide = useCallback(async () => {
    setGuideLoading(true)
    setError(null)
    try {
      const items = await api.listKnowledgeExperimentGuides('LOGIC_GROUP', Number(groupId))
      setGuideItems(items)
      setGuideOpen(true)
    } catch (err) {
      setError(err.message || '加载实验引导失败')
    } finally {
      setGuideLoading(false)
    }
  }, [groupId])

  const closeGuide = useCallback(() => {
    setGuideOpen(false)
    setGuideItems([])
  }, [])

  const viewSnapshot = (snapshot) => {
    const meta = typeof snapshot.snapshotJson === 'string'
      ? (() => { try { return JSON.parse(snapshot.snapshotJson) } catch { return {} } })()
      : snapshot.snapshotJson ?? {}
    const logics = meta.logics ?? []
    const results = logics.map((l) => ({
      logic_diagram_id: l.logicDiagramId,
      experiment_passed: l.experimentResult?.passed ?? l.experimentPassed,
      success: l.status === 'success',
      total_transitions: l.totalTransitions,
    }))
    setMemberResults(mapLogicResults(results))
    setOverallPassed(readPassed(meta.experimentPassed))
  }

  const items = useMemo(
    () => (detail?.members ?? []).map((m) => ({ id: String(m.logicDiagramId), name: m.title })),
    [detail?.members],
  )

  const nodeStates = useMemo(() => {
    const states = {}
    for (const [id, result] of Object.entries(memberResults)) {
      const passed = readPassed(result.experimentPassed)
      if (result.success === false) {
        states[id] = false
      } else if (passed === true) {
        states[id] = true
      } else if (passed === false) {
        states[id] = false
      }
    }
    return states
  }, [memberResults])

  const openMemberById = (logicDiagramId) => {
    navigate(`/student/modes/panorama/${logicDiagramId}`, {
      state: { from: 'coach', section: 'logic', deviceId: detail?.iedDeviceId, groupId: Number(groupId) },
    })
  }

  return (
    <div className="tablet-shell diagram-page">
      <header className="tablet-shell__header diagram-page__toolbar">
        <div className="tablet-shell__header-left">
          <button
            type="button"
            className="tablet-shell__back"
            onClick={() => navigate('/student/modes/panorama', { state: listState })}
          >
            ← 返回上级
          </button>
          <button type="button" className="tablet-shell__home" onClick={() => navigate('/student')}>
            返回首页
          </button>
        </div>
        <h1>{detail?.name || '组合逻辑'}</h1>
        <div className="tablet-shell__header-actions">
          <button
            type="button"
            className="tablet-shell__logout"
            onClick={() => logout().then(() => navigate('/login', { replace: true }))}
          >
            退出登录
          </button>
        </div>
      </header>

      {error && <div className="diagram-page__error">{error}</div>}

      <main className="tablet-shell__main logic-group">
        {loading ? (
          <p className="panorama-list__status">加载中…</p>
        ) : !detail ? (
          <p className="panorama-list__empty">组合逻辑不存在</p>
        ) : (
          <>
            <div className="logic-group__toolbar">
              <div>
                <p className="logic-group__hint">组合逻辑由以下基础逻辑按序拼接，点击名称进入对应逻辑框图。</p>
                {monitoring && (
                  <span className="logic-group__status">● {statusLabel[monitorStatus] || '监视中'}</span>
                )}
                {overallPassed != null && (
                  <span className={`logic-group__overall${overallPassed ? ' logic-group__overall--ok' : ' logic-group__overall--error'}`}>
                    整体{overallPassed ? '通过' : '未通过'}
                  </span>
                )}
              </div>
              <div className="logic-group__actions">
                {monitoring ? (
                  monitorStatus !== 'checking' && (
                    <button type="button" className="diagram-canvas__trigger-btn diagram-canvas__trigger-btn--stop" onClick={handleStopExperiment}>
                      ■ 停止实验
                    </button>
                  )
                ) : (
                  <>
                    <button type="button" className="diagram-canvas__trigger-btn" onClick={handleStartExperiment}>
                      ▶ 开始实验
                    </button>
                    <button type="button" className="diagram-canvas__trigger-btn" onClick={handleOpenGuide} disabled={guideLoading}>
                      {guideLoading ? '加载中…' : '试验引导'}
                    </button>
                  </>
                )}
              </div>
            </div>

            <div className="logic-group__canvas">
              <LogicGroupView
                items={items}
                nodeStates={nodeStates}
                onNodeClick={openMemberById}
                className="logic-group__graph"
              />
            </div>

            {snapshots.length > 0 && (
              <section className="logic-group__history">
                <h3>实验记录</h3>
                <ul>
                  {snapshots.map((snap) => (
                    <li key={snap.id}>
                      <button type="button" className="users-page__link" onClick={() => viewSnapshot(snap)}>
                        {snap.createdAt ? new Date(snap.createdAt).toLocaleString() : `#${snap.id}`}
                        {snap.experimentPassed != null && (
                          <span>{snap.experimentPassed ? ' · 通过' : ' · 未通过'}</span>
                        )}
                      </button>
                    </li>
                  ))}
                </ul>
              </section>
            )}
          </>
        )}
      </main>

      {experimentDialog.open && (
        <div className="experiment-result-dialog" role="dialog" aria-modal="true" aria-labelledby="logic-group-result-message">
          <button
            type="button"
            className="experiment-result-dialog__mask"
            aria-label="关闭实验结果提示"
            onClick={() => { startConfirmationInFlightRef.current = false; setExperimentDialog({ open: false, title: '', message: '' }) }}
          />
          <div className="experiment-result-dialog__panel">
            {experimentDialog.title && (
              <h2 className="experiment-result-dialog__title">{experimentDialog.title}</h2>
            )}
            <p id="logic-group-result-message" className="experiment-result-dialog__message">
              {experimentDialog.message}
            </p>
            {experimentDialog.kind === 'start-confirm' ? (
              <div className="experiment-result-dialog__actions">
                <button type="button" className="experiment-result-dialog__btn experiment-result-dialog__btn--secondary" onClick={() => { startConfirmationInFlightRef.current = false; setExperimentDialog({ open: false, title: '', message: '' }) }}>取消</button>
                <button type="button" className="experiment-result-dialog__btn" onClick={startExperimentAfterConfirmation}>确认并开始实验</button>
              </div>
            ) : (
              <button type="button" className="experiment-result-dialog__btn" onClick={() => setExperimentDialog({ open: false, title: '', message: '' })}>确定</button>
            )}
          </div>
        </div>
      )}

      {guideOpen && (
        <ExperimentGuideDialog
          items={guideItems}
          title={detail?.name}
          onClose={closeGuide}
        />
      )}
    </div>
  )
}
