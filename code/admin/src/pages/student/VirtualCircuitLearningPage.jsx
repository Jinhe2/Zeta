import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Background,
  BaseEdge,
  Controls,
  Handle,
  MarkerType,
  Position,
  ReactFlow,
  ReactFlowProvider,
  getStraightPath,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { api } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import { resolveStudentCabinetId, useStudentCabinetId } from './studentCabinet'
import './TabletShell.css'
import './VirtualCircuitLearningPage.css'

const POLL_INTERVAL = 5000
const STATE_STYLE = {
  red: { color: '#dc2626', label: '断链' },
  orange: { color: '#f59e0b', label: '异常' },
  gray: { color: '#94a3b8', label: '未知' },
  green: { color: '#22c55e', label: '正常' },
}

function readSignals(block, camel, snake) {
  return block?.[camel] ?? block?.[snake] ?? []
}

function controlBlockState(configBlock, liveBlock) {
  const disconnect = readSignals(liveBlock, 'disconnectSignals', 'disconnect_signals')
  const abnormal = readSignals(liveBlock, 'linkAbnormalSignals', 'link_abnormal_signals')
  const configuredDisconnectCount = configBlock?.disconnectSignals?.length ?? 0
  const disconnectReadable = disconnect.length >= configuredDisconnectCount
    && configuredDisconnectCount > 0
    && disconnect.every((item) => item.read_success === true && ['ON', 'OFF'].includes(item.state))
  if (!disconnectReadable) return 'gray'
  if (disconnect.some((item) => item.state === 'ON')) return 'red'
  if (abnormal.some((item) => item.read_success === true && item.state === 'ON')) return 'orange'
  return 'green'
}

function edgeState(edge, blockById, liveById) {
  const states = edge.controlBlockIds.map((id) => controlBlockState(blockById.get(id), liveById[id]))
  if (states.includes('red')) return 'red'
  if (states.includes('orange')) return 'orange'
  if (states.includes('gray')) return 'gray'
  return 'green'
}

function DeviceNode({ data }) {
  return (
    <div className={`virtual-node${data.center ? ' virtual-node--center' : ''}`} style={{ height: data.height }}>
      <div className="virtual-node__name">{data.displayName}</div>
      <div className="virtual-node__code">{data.iedName}</div>
      {data.handles.map((handle) => (
        <Handle
          key={handle.id}
          id={handle.id}
          type={handle.type}
          position={handle.side === 'left' ? Position.Left : Position.Right}
          className="virtual-node__handle"
          style={{ top: handle.offsetY }}
          onClick={(event) => {
            event.stopPropagation()
            data.onOpen(handle.peerName)
          }}
        >
          <span>{handle.label}</span>
        </Handle>
      ))}
    </div>
  )
}

function StatusEdge({ id, sourceX, sourceY, targetX, targetY, markerEnd, data }) {
  const offset = data.reverseOffset ?? 0
  const [path] = getStraightPath({
    sourceX,
    sourceY: sourceY + offset,
    targetX,
    targetY: targetY + offset,
  })
  return <BaseEdge
    id={id}
    path={path}
    markerEnd={markerEnd}
    style={{ stroke: STATE_STYLE[data.state].color, strokeWidth: 3 }}
  />
}

const nodeTypes = { device: DeviceNode }
const edgeTypes = { status: StatusEdge }

function createGraph(topology, liveById, onOpen) {
  if (!topology) return { nodes: [], edges: [] }
  const centerName = topology.center.iedName
  const blockById = new Map(topology.controlBlocks.map((block) => [Number(block.id), block]))
  const connectionCounts = new Map(topology.peers.map((peer) => [peer.iedName, 0]))
  topology.edges.forEach((edge) => {
    const peerName = edge.sourceIedName === centerName ? edge.targetIedName : edge.sourceIedName
    connectionCounts.set(peerName, (connectionCounts.get(peerName) || 0) + 1)
  })
  const weightedPeers = topology.peers
    .map((peer, index) => ({ peer, index, weight: connectionCounts.get(peer.iedName) || 1 }))
    .sort((a, b) => b.weight - a.weight || a.index - b.index)
  const leftPeers = []
  const rightPeers = []
  let leftWeight = 0
  let rightWeight = 0
  weightedPeers.forEach(({ peer, weight }) => {
    if (leftWeight < rightWeight || (leftWeight === rightWeight && leftPeers.length <= rightPeers.length)) {
      leftPeers.push(peer)
      leftWeight += weight
    } else {
      rightPeers.push(peer)
      rightWeight += weight
    }
  })
  const allDevices = [topology.center, ...topology.peers]
  const handles = new Map(allDevices.map((device) => [device.iedName, []]))
  const peerSides = new Map()
  leftPeers.forEach((peer) => peerSides.set(peer.iedName, 'left'))
  rightPeers.forEach((peer) => peerSides.set(peer.iedName, 'right'))
  const pairDirections = new Set(topology.edges.map((edge) => `${edge.sourceIedName}\u0000${edge.targetIedName}`))
  const edges = topology.edges.map((edge, edgeOrder) => {
    const sourceHandle = `${edge.id}-source`
    const targetHandle = `${edge.id}-target`
    const peerName = edge.sourceIedName === centerName ? edge.targetIedName : edge.sourceIedName
    const peerOnLeft = peerSides.get(peerName) === 'left'
    const sourceIsCenter = edge.sourceIedName === centerName
    const sourceSide = sourceIsCenter
      ? (peerOnLeft ? 'left' : 'right')
      : (peerOnLeft ? 'right' : 'left')
    const targetSide = sourceIsCenter
      ? (peerOnLeft ? 'right' : 'left')
      : (peerOnLeft ? 'left' : 'right')
    handles.get(edge.sourceIedName).push({ id: sourceHandle, type: 'source', side: sourceSide, label: edge.sourcePort, peerName, edgeOrder })
    handles.get(edge.targetIedName).push({ id: targetHandle, type: 'target', side: targetSide, label: edge.targetPort, peerName, edgeOrder })
    const reverse = pairDirections.has(`${edge.targetIedName}\u0000${edge.sourceIedName}`)
    return {
      id: edge.id,
      source: `device:${edge.sourceIedName}`,
      target: `device:${edge.targetIedName}`,
      sourceHandle,
      targetHandle,
      type: 'status',
      markerEnd: { type: MarkerType.ArrowClosed, color: STATE_STYLE[edgeState(edge, blockById, liveById)].color },
      data: {
        state: edgeState(edge, blockById, liveById),
        peerName,
        reverseOffset: reverse ? (edge.sourceIedName < edge.targetIedName ? -8 : 8) : 0,
      },
    }
  })

  const peerRanks = new Map()
  leftPeers.forEach((peer, index) => peerRanks.set(peer.iedName, index))
  rightPeers.forEach((peer, index) => peerRanks.set(peer.iedName, index))
  handles.forEach((deviceHandles, deviceName) => {
    deviceHandles.sort((a, b) => {
      if (deviceName === centerName) {
        const peerDifference = (peerRanks.get(a.peerName) || 0) - (peerRanks.get(b.peerName) || 0)
        if (peerDifference !== 0) return peerDifference
      }
      return a.edgeOrder - b.edgeOrder
    })
  })

  const centerY = 50
  const laneTop = 54
  const laneGap = 40
  // 不同外围装置的端口轨道之间额外留白，避免动态高度的装置外框相互拥挤。
  const deviceGap = 140
  const edgeLaneY = new Map()
  let requiredCenterHeight = 300
  for (const side of ['left', 'right']) {
    const sideHandles = handles.get(centerName).filter((handle) => handle.side === side)
    let offsetY = laneTop
    let previousPeer = null
    sideHandles.forEach((handle) => {
      if (previousPeer !== null && previousPeer !== handle.peerName) offsetY += deviceGap
      handle.offsetY = offsetY
      edgeLaneY.set(handle.edgeOrder, centerY + offsetY)
      previousPeer = handle.peerName
      offsetY += laneGap
    })
    requiredCenterHeight = Math.max(requiredCenterHeight, offsetY + laneTop - laneGap)
  }

  const nodeHeights = new Map([[centerName, requiredCenterHeight]])
  const positions = new Map([[centerName, { x: 520, y: centerY }]])
  const placePeers = (peers, x) => {
    peers.forEach((peer) => {
      const peerHandles = handles.get(peer.iedName)
      const laneValues = peerHandles.map((handle) => edgeLaneY.get(handle.edgeOrder)).filter(Number.isFinite)
      const firstLane = Math.min(...laneValues)
      const lastLane = Math.max(...laneValues)
      const height = Math.max(116, lastLane - firstLane + 70)
      const y = (firstLane + lastLane) / 2 - height / 2
      nodeHeights.set(peer.iedName, height)
      positions.set(peer.iedName, { x, y })
      peerHandles.forEach((handle) => { handle.offsetY = edgeLaneY.get(handle.edgeOrder) - y })
    })
  }
  placePeers(leftPeers, 40)
  placePeers(rightPeers, 1000)

  const nodes = allDevices.map((device) => ({
    id: `device:${device.iedName}`,
    type: 'device',
    position: positions.get(device.iedName),
    draggable: false,
    selectable: false,
    data: {
      ...device,
      center: device.iedName === centerName,
      handles: handles.get(device.iedName),
      height: nodeHeights.get(device.iedName),
      onOpen,
    },
  }))
  return { nodes, edges }
}

function DetailDialog({ topology, peerName, liveById, onClose }) {
  if (!peerName) return null
  const centerName = topology.center.iedName
  const blocks = topology.controlBlocks.filter((block) => (
    (block.sourceIedName === centerName && block.receiverIedName === peerName)
    || (block.sourceIedName === peerName && block.receiverIedName === centerName)
  ))
  const directions = [
    { source: centerName, target: peerName },
    { source: peerName, target: centerName },
  ]
  return <div className="virtual-modal" role="presentation" onMouseDown={onClose}>
    <section className="virtual-modal__panel" role="dialog" aria-modal="true" onMouseDown={(event) => event.stopPropagation()}>
      <header><div><h2>装置间虚端子详情</h2><p>{centerName} ⇄ {peerName}</p></div><button type="button" onClick={onClose}>×</button></header>
      <div className="virtual-detail">
        {directions.map((direction) => {
          const directionBlocks = blocks.filter((block) => block.sourceIedName === direction.source)
          if (directionBlocks.length === 0) return null
          return <section key={`${direction.source}-${direction.target}`}>
            <h3>{direction.source} → {direction.target}</h3>
            {['GOOSE', 'SV'].map((serviceType) => {
              const typed = directionBlocks.filter((block) => block.serviceType === serviceType)
              if (typed.length === 0) return null
              return <div key={serviceType} className="virtual-detail__protocol">
                <h4>{serviceType === 'GOOSE' ? 'GOCB / GOOSE' : 'SMVCB / SV'}</h4>
                {typed.map((block) => {
                  const live = liveById[block.id]
                  const state = controlBlockState(block, live)
                  return <article key={block.id} className="virtual-detail__block">
                    <div className="virtual-detail__meta">
                      <strong>{block.controlDescription || block.controlName || `控制块 ${block.id}`}</strong>
                      <span>{block.sourcePorts?.[0] || '未配置端口'} → {block.targetPorts?.[0] || '未配置端口'}</span>
                      <span>控制块：{block.controlReference || '—'}</span><span>数据集：{block.datasetReference || block.datasetName || '—'}</span>
                      <span className={`virtual-detail__state virtual-detail__state--${state}`}>状态：{STATE_STYLE[state].label}</span>
                    </div>
                    {block.errorMessage && <p className="virtual-detail__error">{block.errorMessage}</p>}
                    <table><thead><tr><th>发送信号</th><th>接收位置</th><th>intAddr</th><th>FC</th></tr></thead>
                      <tbody>{block.connections.map((connection) => <tr key={connection.id}>
                        <td><span>{connection.sourceDescription || '—'}</span><code>{connection.sourceReference}</code></td>
                        <td><span>{connection.targetDescription || '—'}</span><code>{connection.targetReference || '—'}</code></td>
                        <td>{connection.intAddr || '—'}</td><td>{connection.fc || '—'}</td>
                      </tr>)}</tbody></table>
                  </article>
                })}
              </div>
            })}
          </section>
        })}
      </div>
    </section>
  </div>
}

function VirtualCircuitCanvas({ topology, liveById, onOpen }) {
  const graph = useMemo(() => createGraph(topology, liveById, onOpen), [topology, liveById, onOpen])
  return <ReactFlowProvider><ReactFlow
    nodes={graph.nodes} edges={graph.edges} nodeTypes={nodeTypes} edgeTypes={edgeTypes}
    onEdgeClick={(event, edge) => { event.stopPropagation(); onOpen(edge.data.peerName) }}
    fitView fitViewOptions={{ padding: 0.18 }} minZoom={0.35} maxZoom={1.8}
    nodesConnectable={false} elementsSelectable={false}
  ><Background gap={24} size={1} color="#294159" /><Controls showInteractive={false} /></ReactFlow></ReactFlowProvider>
}

export default function VirtualCircuitLearningPage() {
  const navigate = useNavigate()
  const { logout } = useAuth()
  const selectedCabinetId = useStudentCabinetId()
  const { iedDeviceId } = useParams()
  const [cabinetId, setCabinetId] = useState(null)
  const [topology, setTopology] = useState(null)
  const [liveById, setLiveById] = useState({})
  const [selectedPeer, setSelectedPeer] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [lastRead, setLastRead] = useState(null)
  const [refreshing, setRefreshing] = useState(false)
  const inFlight = useRef(false)
  const selectedDeviceId = Number(iedDeviceId)

  useEffect(() => {
    let cancelled = false
    api.getKnowledgeTree().then(async (tree) => {
      const resolved = resolveStudentCabinetId(tree, selectedCabinetId)
      if (!resolved) throw new Error('未找到当前绑定屏柜')
      const eligible = await api.listVirtualCircuitDevices(resolved)
      if (cancelled) return
      const valid = eligible.some((device) => Number(device.id) === selectedDeviceId)
      if (!valid) throw new Error('所选装置不属于当前屏柜，或没有虚回路信息')
      setCabinetId(resolved)
    }).catch((err) => { if (!cancelled) setError(err.message) })
    return () => { cancelled = true }
  }, [selectedCabinetId, selectedDeviceId])

  useEffect(() => {
    if (!cabinetId || !selectedDeviceId) return undefined
    let cancelled = false
    // 切换中心装置时清空上一装置的拓扑与实时状态。
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true); setError(null); setLiveById({})
    api.getVirtualCircuitTopology(cabinetId, selectedDeviceId)
      .then((data) => { if (!cancelled) setTopology(data) })
      .catch((err) => { if (!cancelled) setError(err.message) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [cabinetId, selectedDeviceId])

  const refreshStatus = useCallback(async () => {
    if (!topology || inFlight.current) return
    inFlight.current = true
    setRefreshing(true)
    const targets = topology.statusTargets.filter((target) => target.available)
    try {
      const responses = await Promise.allSettled(targets.map((target) => (
        api.triggerVirtualCircuitStatus(target.cabinetId, target.iedDeviceId)
      )))
      const next = {}; let latest = 0
      responses.forEach((response) => {
        if (response.status !== 'fulfilled') return
        latest = Math.max(latest, Number(response.value?.read_time || 0))
        for (const block of response.value?.control_blocks || []) next[block.control_block_id] = block
      })
      setLiveById(next); setLastRead(latest || Date.now())
    } finally {
      inFlight.current = false
      setRefreshing(false)
    }
  }, [topology])

  useEffect(() => {
    if (!topology) return undefined
    refreshStatus()
    const timer = window.setInterval(refreshStatus, POLL_INTERVAL)
    return () => window.clearInterval(timer)
  }, [topology, refreshStatus])

  const openDetails = useCallback((peerName) => setSelectedPeer(peerName), [])

  return <div className="tablet-shell virtual-page">
    <header className="tablet-shell__header"><div className="tablet-shell__header-left">
      <button type="button" className="tablet-shell__back" onClick={() => navigate('/student/modes/coach/virtual-circuit')}>← 返回上级</button>
      <button type="button" className="tablet-shell__home" onClick={() => navigate('/student')}>返回首页</button>
    </div><h1>虚回路学习</h1><div className="tablet-shell__header-actions">
      <button type="button" className="tablet-shell__logout" onClick={async () => { await logout(); navigate('/login', { replace: true }) }}>退出登录</button>
    </div></header>
    <main className="virtual-page__main">
      <div className="virtual-page__toolbar">
        <strong className="virtual-page__device-name">中心装置：{topology?.center?.displayName || '加载中…'}</strong>
        <div className="virtual-page__legend">{Object.entries(STATE_STYLE).map(([key, item]) => <span key={key}><i style={{ background: item.color }} />{item.label}</span>)}</div>
        <button type="button" onClick={refreshStatus} disabled={!topology || refreshing}>刷新状态</button>
        <time>{lastRead ? `最后读取：${new Date(lastRead).toLocaleString()}` : '尚未读取状态'}</time>
      </div>
      <section className="virtual-page__canvas">
        {loading && <div className="virtual-page__message">正在构建虚回路拓扑…</div>}
        {error && <div className="virtual-page__message virtual-page__message--error">{error}</div>}
        {!loading && !error && topology && <VirtualCircuitCanvas topology={topology} liveById={liveById} onOpen={openDetails} />}
      </section>
    </main>
    {topology && <DetailDialog topology={topology} peerName={selectedPeer} liveById={liveById} onClose={() => setSelectedPeer(null)} />}
  </div>
}
