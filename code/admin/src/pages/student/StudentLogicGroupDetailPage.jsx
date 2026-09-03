/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { LogicGroupView } from '@zeta/diagram'
import { api as baseApi } from '../../api/client'
import { wholeExperimentApi } from '../../utils/wholeExperiment'
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
  response_timeout: '响应超时，设备状态待确认',
  completed: '实验完成',
  failed: '实验失败',
}

function findMemberForLogicResult(result, members) {
  const diagramId = result.logic_diagram_id ?? result.logicDiagramId
  if (diagramId != null) {
    return members.find((member) => String(member.logicDiagramId) === String(diagramId))
  }
  const logicCode = result.logic_id ?? result.logicId ?? result.logic_code ?? result.logicCode ?? result.code
  if (logicCode != null) {
    return members.find((member) => String(member.code) === String(logicCode))
  }
  return null
}

function mapLogicResults(logicResults, members = []) {
  const map = {}
  for (const r of (logicResults || [])) {
    const member = findMemberForLogicResult(r, members)
    const key = member?.logicDiagramId ?? r.logic_diagram_id ?? r.logicDiagramId
    if (key == null) continue
    const status = String(r.status ?? '').toLowerCase()
    const statusSuccess = status === 'success'
      ? true
      : (status === 'failed' || status === 'error' ? false : undefined)
    map[String(key)] = {
      status,
      success: r.success ?? statusSuccess,
      experimentPassed: r.experiment_result?.passed ?? r.experimentResult?.passed ?? r.experiment_passed ?? r.experimentPassed,
      totalTransitions: r.total_transitions ?? r.totalTransitions,
      error: r.error ?? r.errorMessage ?? r.error_message,
      errorCode: r.errorCode ?? r.error_code,
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

export default function StudentLogicGroupDetailPage({ experimentType = 'group' }) {
  const { groupId } = useParams()
  const whole = experimentType === 'whole'
  const label = whole ? '整组' : '组合'
  const api = useMemo(() => whole ? wholeExperimentApi(baseApi, groupId) : baseApi, [whole, groupId])
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
  const [selectedSnapshotId, setSelectedSnapshotId] = useState(null)

  const [guideOpen, setGuideOpen] = useState(false)
  const [guideItems, setGuideItems] = useState([])
  const [guideLoading, setGuideLoading] = useState(false)

  const fromCoach = location.state?.from === 'coach'
  const listState = {
    mode: whole ? 'whole' : 'group',
    ...(fromCoach ? { from: 'coach', section: 'logic' } : {}),
    ...(location.state?.deviceId ? { deviceId: location.state.deviceId } : {}),
  }

  const load = useCallback(async () => {
    setLoading(true)
    setMonitoring(false)
    setMonitorStatus('')
    startConfirmationInFlightRef.current = false
    setError(null)
    setSelectedSnapshotId(null)
    setMemberResults({})
    setOverallPassed(null)
    try {
      const [detailData, snapshotData] = await Promise.all([
        api.getKnowledgeLogicGroup(groupId),
        api.listLogicGroupSnapshots(groupId),
      ])
      setDetail(detailData)
      setSnapshots(snapshotData)
    } catch (err) {
      setError(err.message || '加载组合逻辑失败')
    } finally {
      setLoading(false)
    }
  }, [api, groupId])

  const loadSnapshots = useCallback(async (selectLatest = false) => {
    try {
      const snapshotData = await api.listLogicGroupSnapshots(groupId)
      setSnapshots(snapshotData)
      if (selectLatest && snapshotData.length > 0) {
        const latest = snapshotData[0]
        setSelectedSnapshotId(latest.id)
        setMemberResults({})
        setOverallPassed(null)
        if (latest.resultStatus && latest.resultStatus !== 'SNAPSHOT_READY') return
        const members = await api.listLogicGroupSnapshotMembers(latest.id)
        setMemberResults(mapLogicResults(members, detail?.members ?? []))
        setOverallPassed(readPassed(latest.experimentPassed))
      }
    } catch {
      // 忽略刷新失败
    }
  }, [api, detail, groupId])

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
      const taskUuid = taskUuidRef.current
      taskUuidRef.current = null
      if (taskUuid) {
        api.endLogicGroupMonitor(taskUuid).catch(() => {})
      }
    }
  }, [api, clearMonitorTimers])

  const startResultPolling = useCallback((taskUuid) => {
    if (pollRef.current) clearInterval(pollRef.current)
    pollRef.current = setInterval(async () => {
      try {
        const result = await api.getMonitorTaskResult(taskUuid)
        if (whole && result.result === 'pending') {
          setMonitorStatus(result.status === 'RESPONSE_TIMEOUT' ? 'response_timeout'
            : result.status === 'STOPPING' ? 'stopping' : 'watching')
          setError(result.errorMessage || null)
          return
        }
        if (whole) setError(null)
        clearInterval(pollRef.current)
        pollRef.current = null
        if (heartbeatRef.current) {
          clearInterval(heartbeatRef.current)
          heartbeatRef.current = null
        }

        const logicResults = result?.logic_results ?? result?.logicResults ?? []
        setMemberResults(mapLogicResults(logicResults, detail?.members ?? []))
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
        startConfirmationInFlightRef.current = false
        loadSnapshots(true)
      } catch {
        // 404 = 结果尚未返回，继续轮询
      }
    }, POLL_INTERVAL)
  }, [api, whole, detail, loadSnapshots])

  // 刷新页面或从历史打开时，恢复仍未结束的整组监测，防止重复启动。
  useEffect(() => {
    if (!whole || loading || taskUuidRef.current) return
    const active = snapshots.find((run) => ['STARTING', 'WATCHING', 'STOPPING', 'RESPONSE_TIMEOUT'].includes(run.status))
    if (!active) return
    taskUuidRef.current = active.taskUuid
    setMonitoring(true)
    setMonitorStatus(active.status === 'RESPONSE_TIMEOUT' ? 'response_timeout' : 'watching')
    heartbeatRef.current = setInterval(() => {
      api.sendLogicGroupMonitorHeartbeat(active.taskUuid).catch(() => {})
    }, HEARTBEAT_INTERVAL)
    startResultPolling(active.taskUuid)
  }, [api, whole, loading, snapshots, startResultPolling])

  const startExperimentAfterConfirmation = useCallback(async () => {
    if (startConfirmationInFlightRef.current) return
    startConfirmationInFlightRef.current = true
    taskUuidRef.current = null
    setExperimentDialog({ open: false, title: '', message: '' })
    setMonitoring(true)
    setMonitorStatus('starting')
    setError(null)
    setMemberResults({})
    setSelectedSnapshotId(null)
    setOverallPassed(null)
    try {
      const response = await api.startLogicGroupMonitor(Number(groupId))
      const taskUuid = response.req_id || response.reqId
      if (!taskUuid) {
        throw new Error('未返回 taskUuid')
      }
      taskUuidRef.current = taskUuid
      setMonitorStatus(response.status === 'RESPONSE_TIMEOUT' ? 'response_timeout' : 'watching')
      if (whole && response.errorMessage) setError(response.errorMessage)
      heartbeatRef.current = setInterval(() => {
        api.sendLogicGroupMonitorHeartbeat(taskUuid).catch(() => {})
      }, HEARTBEAT_INTERVAL)
      startResultPolling(taskUuid)
      if (whole) loadSnapshots()
    } catch (err) {
      startConfirmationInFlightRef.current = false
      setMonitoring(false)
      setMonitorStatus('')
      setError('启动组合实验失败: ' + err.message)
      // HTTP 响应丢失不代表设备未启动，读取已持久化任务以恢复监测。
      if (whole) loadSnapshots()
    }
  }, [api, whole, groupId, loadSnapshots, startResultPolling])

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
  }, [api, clearMonitorTimers, groupId])

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
      setMonitoring(whole)
      setMonitorStatus(whole ? 'response_timeout' : '')
      if (whole) startResultPolling(taskUuid)
    }
    if (!whole) taskUuidRef.current = null
  }, [api, whole, clearMonitorTimers, startResultPolling])

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
  }, [api, groupId])

  const closeGuide = useCallback(() => {
    setGuideOpen(false)
    setGuideItems([])
  }, [])

  const viewSnapshot = async (snapshot) => {
    setError(null)
    setSelectedSnapshotId(snapshot.id)
    setMemberResults({})
    setOverallPassed(null)
    if (snapshot.resultStatus && snapshot.resultStatus !== 'SNAPSHOT_READY') return
    try {
      const members = await api.listLogicGroupSnapshotMembers(snapshot.id)
      setMemberResults(mapLogicResults(members, detail?.members ?? []))
      setOverallPassed(readPassed(snapshot.experimentPassed))
    } catch (err) {
      setError(err.message || '加载组合实验结果失败')
    }
  }

  const displayedMembers = snapshots.find((run) => run.id === selectedSnapshotId)?.members ?? detail?.members
  const items = useMemo(
    () => (displayedMembers ?? []).map((m) => ({ id: String(m.logicDiagramId), name: m.title })),
    [displayedMembers],
  )

  const nodeStates = useMemo(() => {
    const states = {}
    for (const [id, result] of Object.entries(memberResults)) {
      const passed = readPassed(result.experimentPassed)
      if (result.status === 'failed' || result.success === false) {
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
    if (selectedSnapshotId == null) {
      setError('请先选择一条组合实验记录')
      return
    }
    const selectedSnapshot = snapshots.find((snapshot) => snapshot.id === selectedSnapshotId)
    if (whole && selectedSnapshot?.resultStatus !== 'SNAPSHOT_READY') {
      setError(selectedSnapshot?.errorMessage || '该实验暂无有效断面，请等待实验结束或检查监测结果')
      return
    }
    if (selectedSnapshot?.resultStatus === 'DEVICE_NOT_STARTED') {
      setError('该实验记录未生成断面数据，保护装置未启动')
      return
    }
    if (selectedSnapshot?.resultStatus === 'INVALID_SNAPSHOT') {
      setError('该实验记录的断面数据异常，无法查看')
      return
    }
    navigate(`/student/modes/panorama/${logicDiagramId}?${whole ? 'wholeRunId' : 'groupSnapshotId'}=${selectedSnapshotId}`, {
      state: {
        from: 'coach',
        section: 'logic',
        deviceId: detail?.iedDeviceId,
        ...(whole ? { wholeExperimentId: Number(groupId), wholeRunId: selectedSnapshotId }
          : { groupId: Number(groupId) }),
      },
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
        <h1>{detail?.name || `${label}实验`}</h1>
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

      {error && <div className="diagram-page__error">{whole ? error.replaceAll('组合', '整组') : error}</div>}
      {whole && detail?.valid === false && <div className="diagram-page__error">
        {detail.invalidReason}；可查看旧实验记录，开始新实验请返回重新选择。
      </div>}

      <div className="diagram-page__body logic-group">
        {loading ? (
          <p className="panorama-list__status">加载中…</p>
        ) : !detail ? (
          <p className="panorama-list__empty">组合逻辑不存在</p>
        ) : (
          <>
            <div className="diagram-page__workspace">
              <div className="diagram-canvas">
                <div className="diagram-canvas__header">
                  <div>
                    <span>{label}逻辑框图</span>
                    <span className="logic-group__hint">点击基础逻辑节点查看当前记录的节点断面</span>
                  </div>
                  <div className="logic-group__actions">
                    {overallPassed != null && !monitoring && (
                      <span className={`logic-group__overall${overallPassed ? ' logic-group__overall--ok' : ' logic-group__overall--error'}`}>
                        整体{overallPassed ? '通过' : '未通过'}
                      </span>
                    )}
                    {monitoring ? (
                      <>
                        <span className="logic-group__status">● {statusLabel[monitorStatus] || '监视中'}</span>
                        {monitorStatus !== 'checking' && (
                          <button type="button" disabled={monitorStatus === 'starting'} className="diagram-canvas__trigger-btn diagram-canvas__trigger-btn--inline diagram-canvas__trigger-btn--stop" onClick={handleStopExperiment}>
                            ■ 停止实验
                          </button>
                        )}
                      </>
                    ) : (
                      <>
                        <button type="button" disabled={whole && detail?.valid === false} className="diagram-canvas__trigger-btn diagram-canvas__trigger-btn--inline" onClick={handleStartExperiment}>
                          ▶ 开始实验
                        </button>
                        <button type="button" className="diagram-canvas__trigger-btn diagram-canvas__trigger-btn--inline" onClick={handleOpenGuide} disabled={guideLoading || (whole && detail?.valid === false)}>
                          {guideLoading ? '加载中…' : '试验引导'}
                        </button>
                      </>
                    )}
                  </div>
                </div>
                <div className="diagram-canvas__area">
                  <LogicGroupView
                    items={items}
                    nodeStates={nodeStates}
                    onNodeClick={openMemberById}
                    className="diagram-canvas__preview logic-group__graph"
                  />
                </div>
              </div>
            </div>

            <aside className="diagram-page__sidebar">
              <section className="diagram-page__history">
                <div className="diagram-page__history-header">
                  <span>{label}实验记录</span>
                  <button
                    type="button"
                    className="diagram-page__history-trigger"
                    disabled={monitoring || (whole && detail?.valid === false)}
                    onClick={handleStartExperiment}
                  >
                    {monitoring ? '…' : '+ 新实验'}
                  </button>
                </div>
                {snapshots.length === 0 ? (
                  <p className="diagram-page__history-empty">暂无记录</p>
                ) : (
                  <ul className="diagram-page__history-list">
                  {snapshots.map((snap) => (
                    <li
                      key={snap.id}
                      className={`diagram-page__history-item${snap.id === selectedSnapshotId ? ' diagram-page__history-item--active' : ''}`}
                      onClick={() => viewSnapshot(snap)}
                    >
                      <span className="diagram-page__history-time">
                        {snap.createdAt ? new Date(snap.createdAt).toLocaleString() : `#${snap.id}`}
                      </span>
                      <span className="diagram-page__history-transitions">{snap.totalTransitions ?? 0} 次变位</span>
                      {whole && snap.resultStatus === 'PENDING' ? (
                        <span>{snap.status === 'RESPONSE_TIMEOUT' ? '响应超时，状态待确认' : '实验进行中'}</span>
                      ) : whole && (snap.status === 'START_FAILED' || snap.status === 'FAILED') ? (
                        <span title={snap.errorMessage} className="diagram-page__history-status--failed">
                          {snap.status === 'START_FAILED' ? '启动失败' : '实验失败'}
                        </span>
                      ) : snap.resultStatus === 'DEVICE_NOT_STARTED' ? (
                        <span className="diagram-page__history-status diagram-page__history-status--failed">
                          装置未启动
                        </span>
                      ) : snap.resultStatus === 'INVALID_SNAPSHOT' ? (
                        <span className="diagram-page__history-status diagram-page__history-status--failed">
                          断面异常
                        </span>
                      ) : snap.experimentPassed != null && (
                        <span className={`diagram-page__history-status diagram-page__history-status--${snap.experimentPassed ? 'completed' : 'failed'}`}>
                          {snap.experimentPassed ? '通过' : '未通过'}
                        </span>
                      )}
                    </li>
                  ))}
                  </ul>
                )}
              </section>
            </aside>
          </>
        )}
      </div>

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
              {whole ? experimentDialog.message?.replaceAll('组合', '整组') : experimentDialog.message}
            </p>
            {experimentDialog.baselineItems?.length > 0 && (
              <section className="experiment-result-dialog__section">
                <h3>定值比对</h3>
                <table className="experiment-result-dialog__table">
                  <thead><tr><th>名称</th><th>基准值</th><th>实际值</th></tr></thead>
                  <tbody>
                    {experimentDialog.baselineItems.map((item) => (
                      <tr key={item.key}>
                        <td>{item.name}</td>
                        <td>{item.baselineValue || '—'}</td>
                        <td>{item.actualValue || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </section>
            )}
            {experimentDialog.softPressboardItems?.length > 0 && (
              <section className="experiment-result-dialog__section">
                <h3>软压板比对</h3>
                <table className="experiment-result-dialog__table">
                  <thead><tr><th>名称</th><th>基准状态</th><th>实际状态</th></tr></thead>
                  <tbody>
                    {experimentDialog.softPressboardItems.map((item) => (
                      <tr key={item.key}><td>{item.name}</td><td>{item.baselineValue}</td><td>{item.actualValue}</td></tr>
                    ))}
                  </tbody>
                </table>
              </section>
            )}
            {experimentDialog.hardPressboardItems?.length > 0 && (
              <section className="experiment-result-dialog__section">
                <h3>硬压板比对</h3>
                <table className="experiment-result-dialog__table">
                  <thead><tr><th>名称</th><th>基准状态</th><th>实际状态</th></tr></thead>
                  <tbody>
                    {experimentDialog.hardPressboardItems.map((item) => (
                      <tr key={item.key}><td>{item.name}</td><td>{item.baselineValue}</td><td>{item.actualValue}</td></tr>
                    ))}
                  </tbody>
                </table>
              </section>
            )}
            {experimentDialog.wiringItems?.length > 0 && (
              <section className="experiment-result-dialog__section">
                <h3>试验仪接线</h3>
                <table className="experiment-result-dialog__table">
                  <thead><tr><th>分组</th><th>结果</th><th>接线明细</th></tr></thead>
                  <tbody>
                    {experimentDialog.wiringItems.map((item) => (
                      <tr key={item.key}><td>{item.name}</td><td>{item.message}</td><td>{item.detail}</td></tr>
                    ))}
                  </tbody>
                </table>
              </section>
            )}
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
