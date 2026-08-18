/* eslint-disable react-hooks/set-state-in-effect, react-hooks/exhaustive-deps */
import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import { getDeviceBindId } from '../../api/deviceBinding'
import { useAuth } from '../../auth/AuthContext'
import { useStudentCabinetId } from './studentCabinet'
import './TabletShell.css'
import './PanoramaListPage.css'

export default function PanoramaListPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { logout, session } = useAuth()
  const selectedCabinetId = useStudentCabinetId()
  const [cabinet, setCabinet] = useState(null)
  const [devices, setDevices] = useState([])
  const [selectedDeviceId, setSelectedDeviceId] = useState(location.state?.deviceId ?? null)
  const [groups, setGroups] = useState([])
  const [mode, setMode] = useState(null) // null | 'basic' | 'group'
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const fromCoach = location.state?.from === 'coach'
  const backTo = fromCoach ? '/student/modes/coach' : '/student'
  const pageTitle = fromCoach ? '逻辑原理 · 保护逻辑' : '全景模式 · 保护逻辑'
  const hasDeviceSelection = devices.length > 1
  const activeDevice = devices.find((device) => device.id === selectedDeviceId)
    ?? (devices.length === 1 ? devices[0] : null)
  const list = activeDevice?.protectionLogics ?? []
  const hasGroups = groups.length > 0
  const introText = hasDeviceSelection && !activeDevice
    ? `当前屏柜${cabinet?.name ? `「${cabinet.name}」` : ''}有多个装置配置了逻辑图，请先选择装置。`
    : mode === null && hasGroups
      ? '请选择学习方式：组合逻辑学习（多个基础逻辑拼接）或基础逻辑学习（单个逻辑框图）。'
      : mode === 'group'
        ? '选择组合逻辑，学习多个基础逻辑的拼接与联动。'
        : fromCoach
          ? '选择保护逻辑，学习逻辑框图与动作原理。'
          : '选择保护逻辑，进入逻辑框图全景浏览。'
  const diagramState = {
    ...(fromCoach ? { from: 'coach', section: 'logic' } : {}),
    ...(activeDevice ? { deviceId: activeDevice.id } : {}),
  }

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const tree = await api.getKnowledgeTree()
        const cabinetId = session?.role === 'ADMIN'
          ? selectedCabinetId
          : (await api.checkBinding(getDeviceBindId())).cabinetId
        if (!cabinetId) throw new Error('未找到当前评估屏柜')
        const currentCabinet = tree?.cabinets?.find((item) => item.id === cabinetId)
        if (!currentCabinet) throw new Error('未找到当前评估屏柜')

        const summaries = await api.listProtectionLogics(cabinetId)
        const summaryById = new Map(summaries.map((logic) => [logic.id, logic]))
        const availableDevices = (currentCabinet.devices ?? [])
          .map((device) => ({
            ...device,
            protectionLogics: (device.protectionLogics ?? [])
              .map((logic) => ({ ...logic, ...summaryById.get(logic.id) }))
              .filter((logic) => summaryById.has(logic.id)),
          }))
          .filter((device) => device.protectionLogics.length > 0)

        if (!cancelled) {
          setCabinet(currentCabinet)
          setDevices(availableDevices)
          setSelectedDeviceId((current) => (
            availableDevices.some((device) => device.id === current) ? current : null
          ))
        }
      } catch (err) {
        if (!cancelled) setError(err.message)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [selectedCabinetId, session?.role])

  // 选中装置后加载该装置下的组合逻辑
  useEffect(() => {
    if (!activeDevice) {
      setGroups([])
      setMode(null)
      return undefined
    }
    let cancelled = false
    setMode(null)
    api.listKnowledgeLogicGroups(activeDevice.id)
      .then((g) => {
        if (!cancelled) setGroups(Array.isArray(g) ? g : [])
      })
      .catch(() => {
        if (!cancelled) setGroups([])
      })
    return () => {
      cancelled = true
    }
  }, [activeDevice?.id])

  const goBack = () => {
    if (mode !== null) {
      setMode(null)
      return
    }
    if (hasDeviceSelection && activeDevice) {
      setSelectedDeviceId(null)
      return
    }
    navigate(backTo)
  }

  return (
    <div className="tablet-shell panorama-list-shell">
      <header className="tablet-shell__header">
        <div className="tablet-shell__header-left">
          <button type="button" className="tablet-shell__back" onClick={goBack}>
            ← 返回上级
          </button>
          {fromCoach && (
            <button type="button" className="tablet-shell__home" onClick={() => navigate('/student')}>
              返回首页
            </button>
          )}
        </div>
        <h1>{pageTitle}</h1>
        <div className="tablet-shell__header-actions">
          <button
            type="button"
            className="tablet-shell__logout"
            onClick={async () => {
              await logout()
              navigate('/login', { replace: true })
            }}
          >
            退出登录
          </button>
        </div>
      </header>

      <main className="tablet-shell__main panorama-list">
        <p className="panorama-list__intro">{introText}</p>
        {loading && <p className="panorama-list__status">加载中…</p>}
        {error && <p className="panorama-list__status panorama-list__status--error">{error}</p>}
        {!loading && !error && (
          <div className="panorama-list__grid">
            {hasDeviceSelection && !activeDevice ? (
              devices.map((device) => (
                <button
                  key={device.id}
                  type="button"
                  className="panorama-list__card panorama-list__device-card"
                  onClick={() => setSelectedDeviceId(device.id)}
                >
                  <div className="panorama-list__device-icon" aria-hidden="true">▣</div>
                  <div>
                    <h3>{device.name}</h3>
                    <p>{device.description || device.code || '保护装置'}</p>
                    <span className="panorama-list__device-count">
                      {device.protectionLogics.length} 张逻辑图
                    </span>
                  </div>
                </button>
              ))
            ) : mode === null && hasGroups ? (
              <>
                <button
                  type="button"
                  className="panorama-list__card panorama-list__mode-card"
                  onClick={() => setMode('group')}
                >
                  <div className="panorama-list__mode-icon" aria-hidden="true">⧉</div>
                  <div>
                    <h3>组合逻辑学习</h3>
                    <p>多个基础逻辑拼接，学习联动与组合实验</p>
                    <span className="panorama-list__device-count">{groups.length} 个组合</span>
                  </div>
                </button>
                <button
                  type="button"
                  className="panorama-list__card panorama-list__mode-card"
                  onClick={() => setMode('basic')}
                >
                  <div className="panorama-list__mode-icon" aria-hidden="true">▦</div>
                  <div>
                    <h3>基础逻辑学习</h3>
                    <p>单个逻辑框图，学习动作原理</p>
                    <span className="panorama-list__device-count">{list.length} 张逻辑图</span>
                  </div>
                </button>
              </>
            ) : mode === 'group' ? (
              groups.map((group) => (
                <Link
                  key={group.id}
                  to={`/student/modes/panorama/groups/${group.id}`}
                  state={diagramState}
                  className="panorama-list__card"
                >
                  <div className="panorama-list__card-header">
                    <h3>{group.name}</h3>
                  </div>
                  <p>{group.memberCount} 个基础逻辑</p>
                  <div className="panorama-list__meta">
                    <span className="panorama-list__meta-item">组合逻辑</span>
                  </div>
                </Link>
              ))
            ) : list.length === 0 ? (
              <p className="panorama-list__empty">当前屏柜暂无保护逻辑配置</p>
            ) : (
              list.map((item) => (
                <Link
                  key={item.id}
                  to={`/student/modes/panorama/${item.id}`}
                  state={diagramState}
                  className="panorama-list__card"
                >
                  <div className="panorama-list__card-header">
                    <h3>{item.title}</h3>
                    {item.category && (
                      <span className="panorama-list__category">{item.category}</span>
                    )}
                  </div>
                  <p>{item.description || '暂无描述'}</p>
                  <div className="panorama-list__meta">
                    <span className="panorama-list__meta-item">输入 <span>{item.inputCount}</span></span>
                    <span className="panorama-list__meta-item">逻辑门 <span>{item.gateCount}</span></span>
                    <span className="panorama-list__meta-item">输出 <span>{item.outputCount}</span></span>
                  </div>
                </Link>
              ))
            )}
          </div>
        )}
      </main>
    </div>
  )
}
