import { useEffect, useRef, useState } from 'react'
import { api } from '../api/client'
import {
  formatPressboardChangeState,
  formatPressboardChangeTime,
  newestPressboardChangeId,
} from '../utils/pressboardChange'
import './PressboardChangeStatus.css'

const MODE_PRIORITY = { baseline: 0, detect: 1, view: 2 }
const POLL_INTERVAL = 2000

function higherPriority(current, next) {
  if (!current) return next
  return MODE_PRIORITY[next] > MODE_PRIORITY[current] ? next : current
}

export default function PressboardChangeStatus({ cabinetId }) {
  const [open, setOpen] = useState(false)
  const [unread, setUnread] = useState(false)
  const [records, setRecords] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const openRef = useRef(false)
  const refreshRef = useRef(() => {})

  useEffect(() => {
    openRef.current = open
  }, [open])

  useEffect(() => {
    let active = true
    let initialized = false
    let latestKnownId = 0
    let notificationPending = false
    let running = false
    let queuedMode = null

    openRef.current = false
    queueMicrotask(() => {
      if (!active) return
      setOpen(false)
      setUnread(false)
      setRecords([])
      setError('')
      setLoading(false)
    })

    if (!cabinetId) {
      refreshRef.current = () => {}
      return undefined
    }

    const execute = async (mode) => {
      if (!active) return
      if (running) {
        queuedMode = higherPriority(queuedMode, mode)
        return
      }
      running = true
      if (mode !== 'detect' || openRef.current) setLoading(true)
      try {
        const data = await api.listPressboardChanges(cabinetId)
        if (!active) return
        const nextRecords = Array.isArray(data) ? data : []
        const nextLatestId = newestPressboardChangeId(nextRecords)
        setRecords(nextRecords)
        setError('')

        if (mode === 'baseline' && !initialized) {
          initialized = true
          latestKnownId = nextLatestId
          setUnread(notificationPending)
        } else if (mode === 'view') {
          initialized = true
          latestKnownId = nextLatestId
          notificationPending = false
          setUnread(false)
        } else {
          if (!initialized) {
            initialized = true
            latestKnownId = nextLatestId
            setUnread(notificationPending)
          } else if (nextLatestId > latestKnownId) {
            latestKnownId = nextLatestId
            notificationPending = true
            setUnread(true)
          }
        }
      } catch (loadError) {
        if (active && (openRef.current || mode === 'view')) {
          setError(loadError?.message || '压板变位记录加载失败')
        }
      } finally {
        if (active) setLoading(false)
        running = false
        const nextMode = queuedMode
        queuedMode = null
        if (active && nextMode) execute(nextMode)
      }
    }

    refreshRef.current = execute
    execute('baseline')

    const pollTimer = window.setInterval(() => {
      if (document.hidden) return
      execute(openRef.current ? 'view' : 'detect')
    }, POLL_INTERVAL)

    return () => {
      active = false
      refreshRef.current = () => {}
      window.clearInterval(pollTimer)
    }
  }, [cabinetId])

  if (!cabinetId) return null

  const showDialog = () => {
    openRef.current = true
    setOpen(true)
    refreshRef.current('view')
  }

  const hideDialog = () => {
    openRef.current = false
    setOpen(false)
  }

  return (
    <>
      <button
        type="button"
        className="pressboard-change-toggle"
        onClick={showDialog}
        title={unread ? '有新的压板变位记录' : '暂无未查看的压板变位'}
      >
        <span className={`pressboard-change-toggle__light${unread ? ' pressboard-change-toggle__light--new' : ''}`} />
        遥信变位
      </button>

      {open && (
        <div className="pressboard-change-dialog" role="dialog" aria-modal="true" aria-labelledby="pressboard-change-dialog-title">
          <button type="button" className="pressboard-change-dialog__mask" aria-label="关闭遥信变位" onClick={hideDialog} />
          <section className="pressboard-change-dialog__panel">
            <header className="pressboard-change-dialog__header">
              <div>
                <h2 id="pressboard-change-dialog-title">遥信变位</h2>
                <p>当前屏柜压板变位记录</p>
              </div>
              <div className="pressboard-change-dialog__actions">
                <button type="button" onClick={() => refreshRef.current('view')} disabled={loading}>刷新</button>
                <button type="button" aria-label="关闭" onClick={hideDialog}>×</button>
              </div>
            </header>

            {error ? (
              <div className="pressboard-change-dialog__message pressboard-change-dialog__message--error">
                <p>{error}</p>
                <button type="button" onClick={() => refreshRef.current('view')}>重新加载</button>
              </div>
            ) : loading && records.length === 0 ? (
              <p className="pressboard-change-dialog__message">正在加载变位记录…</p>
            ) : records.length === 0 ? (
              <p className="pressboard-change-dialog__message">当前屏柜暂无压板变位记录。</p>
            ) : (
              <div className="pressboard-change-dialog__table-wrap" aria-busy={loading}>
                <table className="pressboard-change-dialog__table">
                  <thead>
                    <tr><th>序号</th><th>压板名称</th><th>状态</th><th>时间</th></tr>
                  </thead>
                  <tbody>
                    {records.map((record, index) => (
                      <tr key={record.id}>
                        <td>{index + 1}</td>
                        <td>{record.pressboardName || '—'}</td>
                        <td><span className={`pressboard-change-dialog__state pressboard-change-dialog__state--${String(record.state).toLowerCase()}`}>{formatPressboardChangeState(record.state)}</span></td>
                        <td>{formatPressboardChangeTime(record.changeTimeMs)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {loading && <div className="pressboard-change-dialog__refreshing">正在刷新…</div>}
              </div>
            )}
          </section>
        </div>
      )}
    </>
  )
}
