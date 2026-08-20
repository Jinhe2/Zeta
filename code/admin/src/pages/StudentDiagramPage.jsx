/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { ZetaGraphView } from '@zeta/diagram'
import { api } from '../api/client'
import SectionSelector from '../components/SectionSelector'
import ConfigPanel from '../components/ConfigPanel'
import JsonViewerModal from '../components/JsonViewerModal'
import SnapshotImportModal from '../components/SnapshotImportModal'
import CognitionMediaViewer from '../components/CognitionMediaViewer'
import ExperimentGuideDialog from '../components/ExperimentGuideDialog'
import { useAuth } from '../auth/AuthContext'
import {
  buildExperimentPrecheckMismatchDialog,
  buildExperimentStartConfirmDialog,
  canStartAfterExperimentPrecheck,
} from '../utils/settingCheck'
import './student/TabletShell.css'
import './StudentPages.css'

const HEARTBEAT_INTERVAL = 5000
const POLL_INTERVAL = 3000
const EXPERIMENT_REMINDER_INTERVAL = 10 * 60 * 1000
const EXPERIMENT_AUTO_STOP_GRACE = 60 * 1000
const APP_TITLE = '继电保护智慧实操教学系统'
const EXPERIMENT_SUCCESS_MESSAGE = '恭喜成功完成实验'
const EXPERIMENT_FAILED_MESSAGE = '实验失败了，请结合中间文件分析结果进一步确认原因'
const EXPERIMENT_DIAGNOSIS_MESSAGE = '实验失败了，请重新学习逻辑框图和相关操作'
const EXPERIMENT_NO_WAVEFORM_MESSAGE = '未获取到录波，请结合定值和功能压板状态排查'

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

function pickDefaultSectionId(sections, outputNodeIds) {
  if (!sections.length) return null
  if (!outputNodeIds?.length) return sections[0].id

  let lastRisingActionIndex = -1
  let lastActionIndex = -1

  for (let i = 0; i < sections.length; i += 1) {
    const states = sections[i]?.states ?? {}
    const previousStates = i > 0 ? sections[i - 1]?.states ?? {} : {}

    for (const nodeId of outputNodeIds) {
      const isAction = states[nodeId] === true
      if (isAction) lastActionIndex = i
      if (isAction && previousStates[nodeId] !== true) {
        lastRisingActionIndex = i
      }
    }
  }

  const actionIndex = lastRisingActionIndex >= 0 ? lastRisingActionIndex : lastActionIndex
  return sections[actionIndex >= 0 ? actionIndex : 0]?.id ?? null
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

function readFirstValue(source, keys) {
  for (const key of keys) {
    const value = source?.[key]
    if (value != null && value !== '') return value
  }
  return ''
}

function hasBinaryData(value) {
  if (value == null) return false
  if (typeof value === 'string') return value.trim().length > 0
  if (Array.isArray(value)) return value.length > 0
  return true
}

function isNoWaveformResult(result, task) {
  const errorText = `${result?.error_code ?? result?.errorCode ?? ''} ${result?.error_message ?? result?.errorMessage ?? task?.errorMessage ?? ''}`.toLowerCase()
  const noWaveformHint = /无录波|未录波|没有录波|no.?wave|no.?record|record.*not|comtrade/.test(errorText)
  const hasSnapshot = Boolean(task?.snapshotJson)
  const hasComtrade = ['comtradeCfg', 'comtradeDat', 'comtradeMid', 'comtradeDes', 'comtradeHdr']
    .some((key) => hasBinaryData(task?.[key]))
  const transitionCount = Number(task?.totalTransitions ?? result?.total_transitions ?? result?.totalTransitions ?? 0)
  return noWaveformHint || (!hasSnapshot && !hasComtrade && transitionCount === 0)
}

function normalizePressboardStateValue(state) {
  if (state === true || state === 1) return '投入'
  if (state === false || state === 0) return '退出'
  const text = String(state ?? '').trim()
  const normalized = text.toUpperCase()
  if (!text) return ''
  if (['ON', 'CONNECTED', 'CLOSE', 'CLOSED', '1', 'TRUE', 'YES', 'Y', '合闸', '闭合', '合位', '投入', '投'].includes(normalized) || ['合闸', '闭合', '合位', '投入', '投'].includes(text)) return '投入'
  if (['OFF', 'DISCONNECTED', 'OPEN', 'OPENED', '0', 'FALSE', 'NO', 'N', '分闸', '断开', '分位', '退出', '退'].includes(normalized) || ['分闸', '断开', '分位', '退出', '退'].includes(text)) return '退出'
  return text
}

function readPressboardStatusId(pressboardStatus) {
  return pressboardStatus?.pressboard_id
    ?? pressboardStatus?.pressboardId
    ?? pressboardStatus?.id
}

function readPressboardStatusValue(pressboardStatus) {
  return pressboardStatus?.state
    ?? pressboardStatus?.status
    ?? pressboardStatus?.value
    ?? pressboardStatus?.position
    ?? pressboardStatus?.switch_state
    ?? pressboardStatus?.switchState
}

function normalizeBaselineItems(result) {
  const items = result?.items ?? result?.settings ?? result?.results ?? []
  if (!Array.isArray(items)) return []
  return items.map((item, index) => ({
    key: `${readFirstValue(item, ['setting_ref', 'settingRef', 'description', 'name'])}-${index}`,
    name: readFirstValue(item, ['description', 'name', 'setting_name', 'settingName', 'setting_ref', 'settingRef']) || `定值 ${index + 1}`,
    baselineValue: readFirstValue(item, ['baselineValue', 'baseline_value', 'expectedValue', 'expected_value', 'referenceValue', 'reference_value']),
    actualValue: readFirstValue(item, ['actualValue', 'actual_value', 'currentValue', 'current_value', 'realValue', 'real_value']),
    matched: item.equal ?? item.matched ?? item.pass ?? item.passed,
  })).filter((item) => item.baselineValue !== '' || item.actualValue !== '')
}

function buildPressboardRows(pressboards, statusResponse) {
  const functionPressboards = (Array.isArray(pressboards) ? pressboards : [])
    .filter((pressboard) => pressboard.pressboardType === 'FUNCTION')
  const statusItems = Array.isArray(statusResponse?.pressboards) ? statusResponse.pressboards : []
  const statesById = new Map()
  const statesByName = new Map()
  for (const item of statusItems) {
    const state = normalizePressboardStateValue(readPressboardStatusValue(item))
    const id = readPressboardStatusId(item)
    if (id != null) statesById.set(String(id), state)
    if (item?.name) statesByName.set(String(item.name), state)
  }
  return functionPressboards.map((pressboard) => ({
    id: pressboard.id,
    name: pressboard.name || `功能压板 ${pressboard.id}`,
    actualValue: statesById.get(String(pressboard.id)) ?? statesByName.get(String(pressboard.name)) ?? '',
  })).filter((item) => item.actualValue)
}

function buildConfigurableNodeMap(config) {
  const map = new Map()
  const addNode = (node, type) => {
    if (!node?.id || map.has(node.id)) return
    map.set(node.id, {
      id: node.id,
      name: node.name || node.id,
      type,
    })
  }
  for (const input of config?.inputs ?? []) addNode(input, 'INPUT')
  for (const timer of config?.timers ?? []) addNode(timer, 'TIMER')
  for (const output of config?.outputs ?? []) addNode(output, 'OUTPUT')
  return map
}

function normalizeRegion(item) {
  if (
    item.leftPercent == null
    || item.topPercent == null
    || item.widthPercent == null
    || item.heightPercent == null
  ) {
    return null
  }
  return {
    leftPercent: item.leftPercent,
    topPercent: item.topPercent,
    widthPercent: item.widthPercent,
    heightPercent: item.heightPercent,
  }
}

export default function StudentDiagramPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
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
  const [experimentDialog, setExperimentDialog] = useState({ open: false, title: '', message: '' })
  const [timeoutDialog, setTimeoutDialog] = useState({ open: false, autoStopAt: null })
  const [selectedLogicNodeId, setSelectedLogicNodeId] = useState(null)
  const [nodeCognitionItems, setNodeCognitionItems] = useState([])
  const [nodeCognitionIndex, setNodeCognitionIndex] = useState(0)
  const [nodeCognitionLoading, setNodeCognitionLoading] = useState(false)
  const [nodeCognitionError, setNodeCognitionError] = useState('')
  const [guideOpen, setGuideOpen] = useState(false)
  const [guideItems, setGuideItems] = useState([])
  const [guideLoading, setGuideLoading] = useState(false)

  // 实验监视状态
  const [monitoring, setMonitoring] = useState(false)
  const [monitorStatus, setMonitorStatus] = useState('') // '' | 'starting' | 'watching' | 'completed' | 'failed'
  const taskUuidRef = useRef(null)
  const heartbeatRef = useRef(null)
  const pollRef = useRef(null)
  const reminderRef = useRef(null)
  const autoStopRef = useRef(null)
  const pendingPreviousTaskUuidRef = useRef(null)
  const startConfirmationInFlightRef = useRef(false)

  const outputNodeIds = useMemo(
    () => (detail?.config?.outputs ?? []).map((output) => output.id).filter(Boolean),
    [detail?.config?.outputs],
  )

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
    if (reminderRef.current) {
      clearTimeout(reminderRef.current)
      reminderRef.current = null
    }
    if (autoStopRef.current) {
      clearTimeout(autoStopRef.current)
      autoStopRef.current = null
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
        setSelectedSectionId(pickDefaultSectionId(secs, outputNodeIds))
      })
      .catch((err) => setError(err.message))
  }, [outputNodeIds])

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
        setSelectedSectionId(pickDefaultSectionId(secs, outputNodeIds))
      })
    })
  }, [id, detail, outputNodeIds])

  // 加载 monitor task 结果并渲染断面
  const loadMonitorTaskResult = useCallback(async (snapshotPath) => {
    try {
      const task = await api.getMonitorTask(snapshotPath)
      if (task.snapshotJson) {
        const secs = parseSnapshotSections(task.snapshotJson)
        setSections(secs)
        setSelectedSectionId(pickDefaultSectionId(secs, outputNodeIds))

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
  }, [id, detail, outputNodeIds])

  const cognitionDeviceId = detail?.cognitionDeviceId
  const screenCabinetId = detail?.screenCabinetId

  const loadExperimentDiagnostics = useCallback(async () => {
    const [baselineResult, pressboardResult] = await Promise.allSettled([
      cognitionDeviceId ? api.compareCognitionDeviceBaselineSettings(cognitionDeviceId) : Promise.resolve(null),
      screenCabinetId
        ? Promise.all([
          api.listHardPressboards(screenCabinetId),
          api.triggerPressboardStatus(screenCabinetId),
        ])
        : Promise.resolve(null),
    ])

    const baselineItems = baselineResult.status === 'fulfilled'
      ? normalizeBaselineItems(baselineResult.value)
      : []
    const pressboardItems = pressboardResult.status === 'fulfilled' && pressboardResult.value
      ? buildPressboardRows(pressboardResult.value[0], pressboardResult.value[1])
      : []

    return {
      baselineItems,
      pressboardItems,
      errors: [
        baselineResult.status === 'rejected' ? `定值读取失败：${baselineResult.reason?.message || '未知错误'}` : '',
        pressboardResult.status === 'rejected' ? `功能压板读取失败：${pressboardResult.reason?.message || '未知错误'}` : '',
      ].filter(Boolean),
    }
  }, [cognitionDeviceId, screenCabinetId])

  const showExperimentResultDialog = useCallback(async (result, task, fallbackMessage) => {
    const noWaveform = isNoWaveformResult(result, task)
    const diagnostics = await loadExperimentDiagnostics()
    const snapshotMeta = parseSnapshotMeta(task?.snapshotJson)
    const message = noWaveform
      ? EXPERIMENT_NO_WAVEFORM_MESSAGE
      : fallbackMessage || getExperimentDialogMessage(result, task) || (result?.result === 'failed' ? EXPERIMENT_FAILED_MESSAGE : '实验已结束')

    setExperimentDialog({
      open: true,
      title: noWaveform ? '无录波辅助排查' : '实验结果',
      message,
      noWaveform,
      summary: {
        result: result?.result,
        resultType: result?.result_type ?? result?.resultType ?? task?.resultType ?? snapshotMeta.resultType,
        totalTransitions: task?.totalTransitions ?? result?.total_transitions ?? result?.totalTransitions,
        sectionCount: task?.snapshotJson ? parseSnapshotSections(task.snapshotJson).length : 0,
      },
      baselineItems: diagnostics.baselineItems,
      pressboardItems: diagnostics.pressboardItems,
      diagnosticErrors: diagnostics.errors,
    })
  }, [loadExperimentDiagnostics])

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
          await showExperimentResultDialog(result, task)
        } else if (result.result === 'failed') {
          setMonitorStatus('failed')
          setMonitoring(false)
          if (taskUuidRef.current === taskUuid) taskUuidRef.current = null
          await showExperimentResultDialog(result, null, '实验失败: ' + (result.error_message || '未知错误'))
        } else {
          setMonitorStatus('completed')
          setMonitoring(false)
          if (taskUuidRef.current === taskUuid) taskUuidRef.current = null
          await showExperimentResultDialog(result, null)
        }
      } catch {
        // 404 = 结果尚未返回，继续轮询
      }
    }, POLL_INTERVAL)
  }, [loadMonitorTaskResult, showExperimentResultDialog])

  const scheduleExperimentReminder = useCallback((taskUuid) => {
    if (reminderRef.current) clearTimeout(reminderRef.current)
    if (autoStopRef.current) clearTimeout(autoStopRef.current)
    reminderRef.current = setTimeout(() => {
      if (taskUuidRef.current !== taskUuid) return
      setTimeoutDialog({ open: true, autoStopAt: Date.now() + EXPERIMENT_AUTO_STOP_GRACE })
      autoStopRef.current = setTimeout(async () => {
        if (taskUuidRef.current !== taskUuid) return
        clearMonitorTimers()
        setTimeoutDialog({ open: false, autoStopAt: null })
        try {
          await api.endLogicMonitor(taskUuid)
          setMonitorStatus('stopping')
          startResultPolling(taskUuid)
        } catch (err) {
          setError('自动停止实验失败: ' + err.message)
          setMonitoring(false)
          setMonitorStatus('')
        }
        taskUuidRef.current = null
      }, EXPERIMENT_AUTO_STOP_GRACE)
    }, EXPERIMENT_REMINDER_INTERVAL)
  }, [clearMonitorTimers, startResultPolling])

  const handleContinueExperiment = useCallback(() => {
    const taskUuid = taskUuidRef.current
    setTimeoutDialog({ open: false, autoStopAt: null })
    if (autoStopRef.current) {
      clearTimeout(autoStopRef.current)
      autoStopRef.current = null
    }
    if (taskUuid) scheduleExperimentReminder(taskUuid)
  }, [scheduleExperimentReminder])

  const startExperimentAfterConfirmation = useCallback(async () => {
    if (startConfirmationInFlightRef.current) return
    startConfirmationInFlightRef.current = true
    const previousTaskUuid = pendingPreviousTaskUuidRef.current
    pendingPreviousTaskUuidRef.current = null
    taskUuidRef.current = null
    setExperimentDialog({ open: false, title: '', message: '' })
    setMonitoring(true)
    setMonitorStatus('starting')
    setError(null)
    setTimeoutDialog({ open: false, autoStopAt: null })
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
      scheduleExperimentReminder(taskUuid)
    } catch (err) {
      startConfirmationInFlightRef.current = false
      setMonitoring(false)
      setMonitorStatus('')
      setError('启动实验失败: ' + err.message)
    }
  }, [detail, scheduleExperimentReminder, startResultPolling])

  // 开始实验前先联合校验定值和软压板，校验通过后等待人工确认。
  const handleStartExperiment = useCallback(async () => {
    if (!detail?.iedName || !detail?.code) {
      setError('缺少装置信息（iedName / logicId）')
      return
    }
    pendingPreviousTaskUuidRef.current = taskUuidRef.current
    startConfirmationInFlightRef.current = false
    clearMonitorTimers()
    setMonitoring(true)
    setMonitorStatus('checking')
    setError(null)
    setExperimentDialog({ open: false, title: '', message: '' })
    setTimeoutDialog({ open: false, autoStopAt: null })
    try {
      const precheck = await api.checkExperimentPreconditions(Number(id))
      setMonitoring(false)
      setMonitorStatus('')
      if (!canStartAfterExperimentPrecheck(precheck)) {
        pendingPreviousTaskUuidRef.current = null
        setExperimentDialog(buildExperimentPrecheckMismatchDialog(precheck))
        return
      }
      setExperimentDialog(buildExperimentStartConfirmDialog())
    } catch (err) {
      pendingPreviousTaskUuidRef.current = null
      setMonitoring(false)
      setMonitorStatus('')
      setError('实验前基准校核失败: ' + err.message)
    }
  }, [clearMonitorTimers, detail, id])

  // 停止实验
  const handleStopExperiment = useCallback(async () => {
    const taskUuid = taskUuidRef.current
    if (!taskUuid) return

    clearMonitorTimers()
    setTimeoutDialog({ open: false, autoStopAt: null })

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
    setExperimentDialog({ open: false, title: '', message: '' })
    setTimeoutDialog({ open: false, autoStopAt: null })
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
    return () => {
      document.title = APP_TITLE
    }
  }, [detail])

  const nodeStates = useMemo(() => {
    const section = sections.find((s) => s.id === selectedSectionId)
    return section?.states ?? null
  }, [sections, selectedSectionId])

  const inputNodeIds = useMemo(
    () => (detail?.config?.inputs ?? []).map((input) => input.id).filter(Boolean),
    [detail?.config?.inputs],
  )

  const configurableNodeMap = useMemo(
    () => buildConfigurableNodeMap(detail?.config),
    [detail?.config],
  )

  const selectedLogicNode = selectedLogicNodeId ? configurableNodeMap.get(selectedLogicNodeId) : null

  const handleLogicNodeSelect = useCallback(
    (nodeId) => {
      if (!nodeId || !configurableNodeMap.has(nodeId)) {
        setSelectedLogicNodeId(null)
        return
      }
      setSelectedLogicNodeId((current) => (current === nodeId ? null : nodeId))
    },
    [configurableNodeMap],
  )

  useEffect(() => {
    if (!selectedLogicNodeId || !detail?.id) {
      setNodeCognitionItems([])
      setNodeCognitionError('')
      setNodeCognitionLoading(false)
      return undefined
    }
    let cancelled = false
    setNodeCognitionLoading(true)
    setNodeCognitionError('')
    api.listKnowledgeLogicNodeCognitionItems(detail.id, selectedLogicNodeId)
      .then((items) => {
        if (!cancelled) {
          setNodeCognitionItems(items)
          setNodeCognitionIndex(0)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setNodeCognitionError(err.message || '加载节点认知失败')
          setNodeCognitionItems([])
        }
      })
      .finally(() => {
        if (!cancelled) setNodeCognitionLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [detail?.id, selectedLogicNodeId])

  const currentNodeCognitionItem = nodeCognitionItems[nodeCognitionIndex] ?? null

  const closeNodeCognition = useCallback(() => {
    setSelectedLogicNodeId(null)
    setNodeCognitionItems([])
    setNodeCognitionIndex(0)
    setNodeCognitionError('')
  }, [])

  const handleOpenGuide = useCallback(async () => {
    setGuideLoading(true)
    setError(null)
    try {
      const items = await api.listKnowledgeExperimentGuides('LOGIC_DIAGRAM', Number(id))
      setGuideItems(items)
      setGuideOpen(true)
    } catch (err) {
      setError(err.message || '加载实验引导失败')
    } finally {
      setGuideLoading(false)
    }
  }, [id])

  const closeGuide = useCallback(() => {
    setGuideOpen(false)
    setGuideItems([])
  }, [])

  const formatSnapTime = (ts) => {
    if (!ts) return ''
    const d = new Date(ts)
    return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }

  const statusLabel = {
    checking: '定值校核中…',
    starting: '正在启动…',
    watching: '实验监视中',
    stopping: '正在停止…',
    completed: '实验完成',
    failed: '实验失败',
  }
  const fromCoach = location.state?.from === 'coach'
  const listState = {
    ...(fromCoach ? { from: 'coach', section: 'logic' } : {}),
    ...(location.state?.deviceId ? { deviceId: location.state.deviceId } : {}),
  }

  return (
    <div className="tablet-shell diagram-page">
      <header className="tablet-shell__header diagram-page__toolbar">
        <div className="tablet-shell__header-left">
          <button
            type="button"
            className="tablet-shell__back"
            onClick={() => {
              const groupId = location.state?.groupId
              if (groupId != null) {
                navigate(`/student/modes/panorama/groups/${groupId}`, { state: listState })
              } else {
                navigate('/student/modes/panorama', { state: listState })
              }
            }}
          >
            ← 返回上级
          </button>
          <button type="button" className="tablet-shell__home" onClick={() => navigate('/student')}>
            返回首页
          </button>
        </div>
        <h1>{detail?.title || '逻辑框图'}</h1>
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
                      {monitorStatus !== 'checking' && (
                        <button
                          type="button"
                          className="diagram-canvas__trigger-btn diagram-canvas__trigger-btn--inline diagram-canvas__trigger-btn--stop"
                          onClick={handleStopExperiment}
                        >
                          ■ 停止实验
                        </button>
                      )}
                    </div>
                  ) : nodeStates ? (
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <button
                        type="button"
                        className="diagram-canvas__trigger-btn diagram-canvas__trigger-btn--inline diagram-canvas__trigger-btn--restart"
                        onClick={handleStartExperiment}
                      >
                        ↻ 重新开始实验
                      </button>
                      <button
                        type="button"
                        className="diagram-canvas__trigger-btn diagram-canvas__trigger-btn--inline"
                        onClick={handleOpenGuide}
                        disabled={guideLoading}
                      >
                        {guideLoading ? '加载中…' : '试验引导'}
                      </button>
                    </div>
                  ) : (
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <button
                        type="button"
                        className="diagram-canvas__trigger-btn diagram-canvas__trigger-btn--inline"
                        onClick={handleStartExperiment}
                      >
                        ▶ 开始实验
                      </button>
                      <button
                        type="button"
                        className="diagram-canvas__trigger-btn diagram-canvas__trigger-btn--inline"
                        onClick={handleOpenGuide}
                        disabled={guideLoading}
                      >
                        {guideLoading ? '加载中…' : '试验引导'}
                      </button>
                    </div>
                  )}
                </div>
                <div className="diagram-canvas__area">
                  <ZetaGraphView
                    config={detail.config}
                    showDevInfo={false}
                    nodeStates={nodeStates}
                    selectedNodeId={selectedLogicNodeId}
                    onNodeSelect={handleLogicNodeSelect}
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
              inputNodeIds={inputNodeIds}
              outputNodeIds={outputNodeIds}
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
            onClick={() => { pendingPreviousTaskUuidRef.current = null; startConfirmationInFlightRef.current = false; setExperimentDialog({ open: false, title: '', message: '' }) }}
          />
          <div className="experiment-result-dialog__panel">
            {experimentDialog.title && (
              <h2 className="experiment-result-dialog__title">{experimentDialog.title}</h2>
            )}
            <p id="experiment-result-dialog-message" className="experiment-result-dialog__message">
              {experimentDialog.message}
            </p>
            {experimentDialog.summary && (
              <dl className="experiment-result-dialog__summary">
                {experimentDialog.summary.totalTransitions != null && (
                  <div><dt>变位次数</dt><dd>{experimentDialog.summary.totalTransitions}</dd></div>
                )}
                {experimentDialog.summary.sectionCount > 0 && (
                  <div><dt>断面数量</dt><dd>{experimentDialog.summary.sectionCount}</dd></div>
                )}
              </dl>
            )}
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
            {experimentDialog.pressboardItems?.length > 0 && (
              <section className="experiment-result-dialog__section">
                <h3>功能压板状态</h3>
                <table className="experiment-result-dialog__table">
                  <thead><tr><th>名称</th><th>实际状态</th></tr></thead>
                  <tbody>
                    {experimentDialog.pressboardItems.map((item) => (
                      <tr key={item.id}>
                        <td>{item.name}</td>
                        <td>{item.actualValue}</td>
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
            {experimentDialog.diagnosticErrors?.length > 0 && (
              <div className="experiment-result-dialog__errors">
                {experimentDialog.diagnosticErrors.map((item) => <p key={item}>{item}</p>)}
              </div>
            )}
            {experimentDialog.kind === 'start-confirm' ? (
              <div className="experiment-result-dialog__actions">
                <button type="button" className="experiment-result-dialog__btn experiment-result-dialog__btn--secondary" onClick={() => { pendingPreviousTaskUuidRef.current = null; startConfirmationInFlightRef.current = false; setExperimentDialog({ open: false, title: '', message: '' }) }}>取消</button>
                <button type="button" className="experiment-result-dialog__btn" onClick={startExperimentAfterConfirmation}>确认并开始实验</button>
              </div>
            ) : (
              <button type="button" className="experiment-result-dialog__btn" onClick={() => setExperimentDialog({ open: false, title: '', message: '' })}>确定</button>
            )}
          </div>
        </div>
      )}

      {timeoutDialog.open && (
        <div className="experiment-result-dialog" role="dialog" aria-modal="true" aria-labelledby="experiment-timeout-dialog-message">
          <div className="experiment-result-dialog__mask" />
          <div className="experiment-result-dialog__panel experiment-result-dialog__panel--timeout">
            <h2 className="experiment-result-dialog__title">试验超时提醒</h2>
            <p id="experiment-timeout-dialog-message" className="experiment-result-dialog__message">
              试验已运行 10 分钟，是否继续？
            </p>
            <p className="experiment-result-dialog__hint">若 60 秒内未选择，系统将自动停止试验。</p>
            <div className="experiment-result-dialog__actions">
              <button
                type="button"
                className="experiment-result-dialog__btn experiment-result-dialog__btn--secondary"
                onClick={handleStopExperiment}
              >
                停止试验
              </button>
              <button
                type="button"
                className="experiment-result-dialog__btn"
                onClick={handleContinueExperiment}
              >
                继续试验
              </button>
            </div>
          </div>
        </div>
      )}

      {selectedLogicNode && (
        <div className="logic-node-cognition-dialog" role="dialog" aria-modal="false" aria-labelledby="logic-node-cognition-title">
          <div
            className={`logic-node-cognition-dialog__panel${
              currentNodeCognitionItem && currentNodeCognitionItem.mediaType === 'TEXT' ? ' logic-node-cognition-dialog__panel--text-only' : ''
            }`}
          >
            <div className="logic-node-cognition-dialog__header">
              <div>
                <span className="logic-node-cognition-dialog__eyebrow">节点认知</span>
                <h2 id="logic-node-cognition-title">{selectedLogicNode.name}</h2>
              </div>
              {nodeCognitionItems.length > 0 && (
                <span className="logic-node-cognition-dialog__count">
                  {nodeCognitionIndex + 1} / {nodeCognitionItems.length}
                </span>
              )}
            </div>

            {(!currentNodeCognitionItem || currentNodeCognitionItem.mediaType !== 'TEXT' || nodeCognitionLoading || nodeCognitionError) && (
              <div className="logic-node-cognition-dialog__image">
                {nodeCognitionLoading ? (
                  <p>正在加载…</p>
                ) : nodeCognitionError ? (
                  <p className="logic-node-cognition-dialog__error">{nodeCognitionError}</p>
                ) : currentNodeCognitionItem && currentNodeCognitionItem.mediaType !== 'TEXT' ? (
                  <CognitionMediaViewer
                    key={currentNodeCognitionItem.id}
                    item={currentNodeCognitionItem}
                    imageType="logic-node-cognition"
                    region={normalizeRegion(currentNodeCognitionItem)}
                    alt={currentNodeCognitionItem.title}
                  />
                ) : (
                  <p>该节点暂无认知条目</p>
                )}
              </div>
            )}

            <div className="logic-node-cognition-dialog__text">
              {currentNodeCognitionItem ? (
                <>
                  <h3>{currentNodeCognitionItem.title}</h3>
                  {currentNodeCognitionItem.content && <p>{currentNodeCognitionItem.content}</p>}
                </>
              ) : (
                <p>当前节点还没有配置认知内容。</p>
              )}
            </div>

            <div className="logic-node-cognition-dialog__actions">
              <button
                type="button"
                onClick={() => setNodeCognitionIndex((current) => Math.max(0, current - 1))}
                disabled={nodeCognitionIndex <= 0}
              >
                上一条
              </button>
              <button
                type="button"
                onClick={() => setNodeCognitionIndex((current) => Math.min(nodeCognitionItems.length - 1, current + 1))}
                disabled={nodeCognitionIndex >= nodeCognitionItems.length - 1}
              >
                下一条
              </button>
              <button type="button" onClick={closeNodeCognition}>
                关闭
              </button>
            </div>
          </div>
        </div>
      )}

      {guideOpen && (
        <ExperimentGuideDialog
          items={guideItems}
          title={detail?.title}
          onClose={closeGuide}
        />
      )}
    </div>
  )
}
