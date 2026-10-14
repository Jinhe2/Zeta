import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../api/client'
import './StudentActivityMonitorPage.css'

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
  if (hours > 0) return `${hours}小时${minutes}分钟`
  if (minutes > 0) return `${minutes}分钟${rest}秒`
  return `${rest}秒`
}

function aggregateStudents(rows) {
  const students = new Map()
  rows.forEach((row) => {
    const key = String(row.userId)
    const existing = students.get(key) || {
      userId: row.userId,
      studentNo: row.studentNo || '—',
      studentName: row.studentName || '未知学员',
      durationSeconds: 0,
      sessionCount: 0,
      cabinets: [],
    }
    const durationSeconds = Number(row.durationSeconds) || 0
    existing.durationSeconds += durationSeconds
    existing.sessionCount += Number(row.sessionCount) || 0
    existing.cabinets.push({
      cabinetId: row.cabinetId,
      cabinetName: row.cabinetName || `屏柜 ${row.cabinetId}`,
      durationSeconds,
    })
    students.set(key, existing)
  })
  return Array.from(students.values())
}

export default function StudentActivityMonitorPage() {
  const today = useMemo(() => localDateValue(new Date()), [])
  const [fromDate, setFromDate] = useState(today)
  const [toDate, setToDate] = useState(today)
  const [cabinetId, setCabinetId] = useState('')
  const [sortDirection, setSortDirection] = useState('desc')
  const [rows, setRows] = useState([])
  const [cabinets, setCabinets] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    if (!fromDate || !toDate) return
    if (fromDate > toDate) {
      setError('开始日期不能晚于结束日期')
      return
    }
    setLoading(true)
    setError('')
    try {
      const params = {
        from: dateBoundary(fromDate),
        to: dateBoundary(toDate, true),
      }
      if (cabinetId) params.cabinetId = cabinetId
      const [summary, cabinetList] = await Promise.all([
        api.getStudentActivitySummary(params),
        cabinets.length ? Promise.resolve(cabinets) : api.listKnowledgeCabinets(),
      ])
      setRows(summary)
      if (!cabinets.length) setCabinets(cabinetList)
    } catch (err) {
      setError(err.message || '学员在线时长加载失败')
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [fromDate, toDate, cabinetId, cabinets])

  useEffect(() => {
    const timer = window.setTimeout(load, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  const students = useMemo(() => aggregateStudents(rows).sort((left, right) => {
    const difference = left.durationSeconds - right.durationSeconds
    return sortDirection === 'asc' ? difference : -difference
  }), [rows, sortDirection])

  const totalDuration = students.reduce((sum, student) => sum + student.durationSeconds, 0)
  const toggleSort = () => setSortDirection((value) => (value === 'desc' ? 'asc' : 'desc'))

  return (
    <div className="activity-monitor">
      <div className="activity-monitor__header">
        <div>
          <h2>学员监控</h2>
          <p>按照学员的有效接口交互统计在线时长，并归集到当前绑定屏柜。</p>
        </div>
        <button type="button" className="activity-monitor__refresh" onClick={load} disabled={loading}>
          {loading ? '加载中…' : '刷新'}
        </button>
      </div>

      <div className="activity-monitor__filters">
        <label>开始日期<input type="date" value={fromDate} max={toDate} onChange={(event) => setFromDate(event.target.value)} /></label>
        <label>结束日期<input type="date" value={toDate} min={fromDate} onChange={(event) => setToDate(event.target.value)} /></label>
        <label>屏柜<select value={cabinetId} onChange={(event) => setCabinetId(event.target.value)}><option value="">全部屏柜</option>{cabinets.map((cabinet) => <option key={cabinet.id} value={cabinet.id}>{cabinet.name}</option>)}</select></label>
        <label>时长排序<select value={sortDirection} onChange={(event) => setSortDirection(event.target.value)}><option value="desc">从高到低</option><option value="asc">从低到高</option></select></label>
      </div>

      <div className="activity-monitor__stats">
        <div><span>活跃学员</span><strong>{students.length}</strong></div>
        <div><span>累计在线时长</span><strong>{formatDuration(totalDuration)}</strong></div>
      </div>

      {error && <div className="activity-monitor__error">{error}</div>}
      {loading ? <p className="activity-monitor__empty">正在加载…</p> : students.length === 0 ? <p className="activity-monitor__empty">所选时间范围内暂无学员活动记录。</p> : (
        <div className="users-page__table-wrap">
          <table className="users-page__table activity-monitor__table">
            <thead><tr><th>学号</th><th>学员姓名</th><th>屏柜时长</th><th>活动会话</th><th><button type="button" className="activity-monitor__sort" onClick={toggleSort}>在线时长 {sortDirection === 'desc' ? '↓' : '↑'}</button></th></tr></thead>
            <tbody>{students.map((student) => <tr key={student.userId}><td>{student.studentNo}</td><td>{student.studentName}</td><td><div className="activity-monitor__cabinet-list">{student.cabinets.sort((a, b) => b.durationSeconds - a.durationSeconds).map((cabinet) => <span key={cabinet.cabinetId}>{cabinet.cabinetName}：{formatDuration(cabinet.durationSeconds)}</span>)}</div></td><td>{student.sessionCount} 次</td><td className="activity-monitor__duration">{formatDuration(student.durationSeconds)}</td></tr>)}</tbody>
          </table>
        </div>
      )}
    </div>
  )
}
