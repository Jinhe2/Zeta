import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../api/client'
import './CabinetActivityMonitorPage.css'

const STATUS_LABELS = {
  LOGOUT: '主动退出',
  EXPIRED: '超时离线',
  REPLACED: '其他学员登录',
  CABINET_CHANGED: '屏柜切换',
  ACTIVE: '当前在线',
}

function localDateValue(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function dateBoundary(value, nextDay = false) {
  const date = new Date(`${value}T00:00:00`)
  if (nextDay) date.setDate(date.getDate() + 1)
  return date.toISOString()
}

function formatDuration(value) {
  const seconds = Math.max(0, Number(value) || 0)
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const rest = seconds % 60
  if (hours) return `${hours}小时${minutes}分钟`
  if (minutes) return `${minutes}分钟${rest}秒`
  return `${rest}秒`
}

function formatTime(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function relativeActivity(value) {
  if (!value) return '—'
  const seconds = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 1000))
  if (seconds < 60) return `${seconds}秒前`
  return `${Math.floor(seconds / 60)}分钟前`
}

export default function CabinetActivityMonitorPage() {
  const today = useMemo(() => localDateValue(new Date()), [])
  const monthStart = useMemo(() => {
    const date = new Date()
    date.setDate(1)
    return localDateValue(date)
  }, [])
  const [cabinets, setCabinets] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [keyword, setKeyword] = useState('')
  const [sortKey, setSortKey] = useState('todayDurationSeconds')
  const [sortDirection, setSortDirection] = useState('desc')
  const [selected, setSelected] = useState(null)
  const [fromDate, setFromDate] = useState(monthStart)
  const [toDate, setToDate] = useState(today)
  const [history, setHistory] = useState(null)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [historyPage, setHistoryPage] = useState(0)
  const [historySort, setHistorySort] = useState('desc')

  const loadOnline = useCallback(async (silent = false) => {
    if (!silent) setLoading(true)
    setError('')
    try {
      setCabinets(await api.getCabinetOnlineStatus())
    } catch (err) {
      setError(err.message || '屏柜状态加载失败')
    } finally {
      if (!silent) setLoading(false)
    }
  }, [])

  useEffect(() => {
    const initial = window.setTimeout(() => loadOnline(), 0)
    const timer = window.setInterval(() => loadOnline(true), 30000)
    return () => { window.clearTimeout(initial); window.clearInterval(timer) }
  }, [loadOnline])

  const loadHistory = useCallback(async () => {
    if (!selected || !fromDate || !toDate || fromDate > toDate) return
    setHistoryLoading(true)
    try {
      setHistory(await api.getCabinetActivitySessions(selected.cabinetId, {
        from: dateBoundary(fromDate),
        to: dateBoundary(toDate, true),
        page: historyPage,
        size: 20,
        durationSort: historySort,
      }))
    } catch (err) {
      setError(err.message || '屏柜历史加载失败')
      setHistory(null)
    } finally {
      setHistoryLoading(false)
    }
  }, [selected, fromDate, toDate, historyPage, historySort])

  useEffect(() => {
    if (!selected) return undefined
    const timer = window.setTimeout(loadHistory, 0)
    return () => window.clearTimeout(timer)
  }, [selected, loadHistory])

  const visibleCabinets = useMemo(() => cabinets.filter((cabinet) => {
    if (statusFilter === 'online' && !cabinet.online) return false
    if (statusFilter === 'idle' && cabinet.online) return false
    const text = `${cabinet.cabinetName || ''} ${cabinet.bindLabel || ''}`.toLowerCase()
    return text.includes(keyword.trim().toLowerCase())
  }).sort((left, right) => {
    const difference = (Number(left[sortKey]) || 0) - (Number(right[sortKey]) || 0)
    return sortDirection === 'asc' ? difference : -difference
  }), [cabinets, statusFilter, keyword, sortKey, sortDirection])

  const onlineCount = cabinets.filter((cabinet) => cabinet.online).length
  const todaySeconds = cabinets.reduce((sum, cabinet) => sum + (Number(cabinet.todayDurationSeconds) || 0), 0)

  const openHistory = (cabinet) => {
    setSelected(cabinet)
    setHistoryPage(0)
    setHistory(null)
  }

  return (
    <div className="cabinet-monitor">
      <div className="activity-monitor__header"><div><h2>屏柜监控</h2><p>查看每个屏柜当前在线学员和历史使用记录，在线状态每 30 秒自动刷新。</p></div><button type="button" className="activity-monitor__refresh" onClick={() => loadOnline()} disabled={loading}>{loading ? '加载中…' : '立即刷新'}</button></div>
      <div className="activity-monitor__stats"><div><span>屏柜总数</span><strong>{cabinets.length}</strong></div><div><span>当前使用中</span><strong>{onlineCount}</strong></div><div><span>当前空闲</span><strong>{Math.max(0, cabinets.length - onlineCount)}</strong></div><div><span>今日累计使用</span><strong>{formatDuration(todaySeconds)}</strong></div></div>
      <div className="cabinet-monitor__filters"><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="搜索屏柜或绑定设备" /><select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}><option value="all">全部状态</option><option value="online">使用中</option><option value="idle">空闲</option></select><select value={sortKey} onChange={(event) => setSortKey(event.target.value)}><option value="todayDurationSeconds">今日使用时长</option><option value="currentOnlineSeconds">当前在线时长</option></select><select value={sortDirection} onChange={(event) => setSortDirection(event.target.value)}><option value="desc">从高到低</option><option value="asc">从低到高</option></select></div>
      {error && <div className="activity-monitor__error">{error}</div>}
      {loading ? <p className="activity-monitor__empty">正在加载…</p> : <div className="users-page__table-wrap"><table className="users-page__table cabinet-monitor__table"><thead><tr><th>屏柜</th><th>绑定设备</th><th>状态</th><th>当前学员</th><th>本次已在线</th><th>最后活动</th><th>今日使用</th><th>操作</th></tr></thead><tbody>{visibleCabinets.map((cabinet) => <tr key={cabinet.cabinetId}><td>{cabinet.cabinetName}</td><td>{cabinet.bindLabel || <span className="cabinet-monitor__muted">未绑定</span>}</td><td><span className={`cabinet-monitor__status cabinet-monitor__status--${cabinet.online ? 'online' : 'idle'}`}>{cabinet.online ? '使用中' : '空闲'}</span></td><td>{cabinet.online ? <><strong>{cabinet.studentName || '未知学员'}</strong><small>{cabinet.studentNo || '—'}</small></> : '—'}</td><td>{cabinet.online ? formatDuration(cabinet.currentOnlineSeconds) : '—'}</td><td title={formatTime(cabinet.lastActivityAt)}>{cabinet.online ? relativeActivity(cabinet.lastActivityAt) : '—'}</td><td>{formatDuration(cabinet.todayDurationSeconds)}</td><td><button type="button" className="users-page__link" onClick={() => openHistory(cabinet)}>历史记录</button></td></tr>)}</tbody></table></div>}

      {selected && <div className="cabinet-monitor__overlay" role="dialog" aria-modal="true"><button type="button" className="cabinet-monitor__mask" aria-label="关闭" onClick={() => setSelected(null)} /><section className="cabinet-monitor__dialog"><div className="cabinet-monitor__dialog-header"><div><h3>{selected.cabinetName}使用历史</h3><p>{selected.bindLabel || '未绑定设备'}</p></div><button type="button" onClick={() => setSelected(null)}>关闭</button></div><div className="cabinet-monitor__history-filters"><label>开始日期<input type="date" value={fromDate} max={toDate} onChange={(event) => { setFromDate(event.target.value); setHistoryPage(0) }} /></label><label>结束日期<input type="date" value={toDate} min={fromDate} onChange={(event) => { setToDate(event.target.value); setHistoryPage(0) }} /></label><label>时长排序<select value={historySort} onChange={(event) => { setHistorySort(event.target.value); setHistoryPage(0) }}><option value="desc">从高到低</option><option value="asc">从低到高</option></select></label></div>{historyLoading ? <p className="activity-monitor__empty">正在加载…</p> : !history?.content?.length ? <p className="activity-monitor__empty">所选时间范围内暂无使用记录。</p> : <div className="users-page__table-wrap"><table className="users-page__table"><thead><tr><th>开始时间</th><th>结束时间</th><th>学员</th><th>使用时长</th><th>绑定设备</th><th>结束原因</th></tr></thead><tbody>{history.content.map((item) => <tr key={item.id}><td>{formatTime(item.startedAt)}</td><td>{item.status === 'ACTIVE' ? '当前在线' : formatTime(item.endedAt)}</td><td>{item.studentName || '未知学员'}<small>{item.studentNo || '—'}</small></td><td>{formatDuration(item.durationSeconds)}</td><td>{item.bindLabel || '—'}</td><td>{STATUS_LABELS[item.status] || item.status}</td></tr>)}</tbody></table></div>}<div className="cabinet-monitor__pagination"><span>共 {history?.totalElements || 0} 条</span><button type="button" disabled={historyPage <= 0 || historyLoading} onClick={() => setHistoryPage((page) => page - 1)}>上一页</button><span>{(history?.page || 0) + 1} / {Math.max(history?.totalPages || 1, 1)}</span><button type="button" disabled={!history || historyPage + 1 >= history.totalPages || historyLoading} onClick={() => setHistoryPage((page) => page + 1)}>下一页</button></div></section></div>}
    </div>
  )
}
