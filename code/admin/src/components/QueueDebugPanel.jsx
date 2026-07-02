import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from '../api/client'
import './QueueDebugPanel.css'

const POLL_INTERVAL = 2000

function formatTime(ts) {
  if (!ts) return '—'
  const d = new Date(ts)
  return d.toLocaleTimeString('zh-CN', { hour12: false }) + '.' + String(d.getMilliseconds()).padStart(3, '0')
}

function timeAgo(ts) {
  if (!ts) return '—'
  const diff = Date.now() - ts
  if (diff < 1000) return '刚刚'
  if (diff < 60000) return `${Math.floor(diff / 1000)}s 前`
  return `${Math.floor(diff / 60000)}m 前`
}

function shortReqId(id) {
  if (!id) return '—'
  return id.length > 8 ? id.substring(0, 8) + '…' : id
}

export default function QueueDebugPanel() {
  const [open, setOpen] = useState(false)
  const [status, setStatus] = useState(null)
  const [tab, setTab] = useState('overview')
  const timerRef = useRef(null)

  const fetchStatus = useCallback(async () => {
    try {
      const data = await api.getQueueStatus()
      setStatus(data)
    } catch {
      setStatus((prev) => prev ? { ...prev, redisConnected: false } : null)
    }
  }, [])

  useEffect(() => {
    if (!open) {
      if (timerRef.current) clearInterval(timerRef.current)
      return
    }
    fetchStatus()
    timerRef.current = setInterval(fetchStatus, POLL_INTERVAL)
    return () => clearInterval(timerRef.current)
  }, [open, fetchStatus])

  if (!open) {
    return (
      <button type="button" className="qdp-toggle" onClick={() => setOpen(true)} title="队列监控">
        <span className="qdp-toggle__dot" /> MQ
      </button>
    )
  }

  const connected = status?.redisConnected ?? false
  const enabled = status?.enabled ?? false
  const messages = status?.recentMessages ?? []
  const sentByCommand = status?.sentByCommand ?? {}
  const outMsgs = messages.filter((m) => m.direction === 'OUT')
  const inMsgs = messages.filter((m) => m.direction === 'IN')

  return (
    <div className="qdp">
      {/* Header */}
      <div className="qdp__header">
        <span className="qdp__title">MQ 队列监控</span>
        <div className="qdp__header-right">
          <span className={`qdp__badge ${connected ? 'qdp__badge--ok' : 'qdp__badge--err'}`}>
            {connected ? 'Redis ✓' : 'Redis ✗'}
          </span>
          <button type="button" className="qdp__btn-icon" onClick={fetchStatus} title="刷新">↻</button>
          <button type="button" className="qdp__btn-icon" onClick={() => setOpen(false)}>✕</button>
        </div>
      </div>

      {/* Overview metrics */}
      <div className="qdp__metrics">
        <div className="qdp__metric">
          <div className="qdp__metric-value">{status?.totalSent ?? 0}</div>
          <div className="qdp__metric-label">已发送</div>
        </div>
        <div className="qdp__metric">
          <div className="qdp__metric-value">{status?.totalReceived ?? 0}</div>
          <div className="qdp__metric-label">已接收</div>
        </div>
        <div className="qdp__metric">
          <div className={`qdp__metric-value ${(status?.outboundPending ?? 0) > 0 ? 'qdp__metric-value--warn' : ''}`}>
            {status?.outboundPending ?? '—'}
          </div>
          <div className="qdp__metric-label">发送队列</div>
        </div>
        <div className="qdp__metric">
          <div className={`qdp__metric-value ${(status?.inboundPending ?? 0) > 0 ? 'qdp__metric-value--warn' : ''}`}>
            {status?.inboundPending ?? '—'}
          </div>
          <div className="qdp__metric-label">接收队列</div>
        </div>
        <div className="qdp__metric">
          <div className={`qdp__metric-value ${(status?.pendingRequests ?? 0) > 0 ? 'qdp__metric-value--active' : ''}`}>
            {status?.pendingRequests ?? 0}
          </div>
          <div className="qdp__metric-label">等待响应</div>
        </div>
      </div>

      {/* Activity */}
      <div className="qdp__activity">
        <span>最后发送: {timeAgo(status?.lastSentAt)}</span>
        <span>最后接收: {timeAgo(status?.lastReceivedAt)}</span>
      </div>

      {/* Tabs */}
      <div className="qdp__tabs">
        <button className={`qdp__tab ${tab === 'overview' ? 'qdp__tab--active' : ''}`} onClick={() => setTab('overview')}>命令统计</button>
        <button className={`qdp__tab ${tab === 'out' ? 'qdp__tab--active' : ''}`} onClick={() => setTab('out')}>发送 ({outMsgs.length})</button>
        <button className={`qdp__tab ${tab === 'in' ? 'qdp__tab--active' : ''}`} onClick={() => setTab('in')}>接收 ({inMsgs.length})</button>
        <button className={`qdp__tab ${tab === 'all' ? 'qdp__tab--active' : ''}`} onClick={() => setTab('all')}>全部 ({messages.length})</button>
      </div>

      {/* Tab content */}
      <div className="qdp__content">
        {tab === 'overview' && (
          <div className="qdp__cmd-stats">
            {Object.keys(sentByCommand).length === 0 ? (
              <p className="qdp__empty">暂无命令记录</p>
            ) : (
              Object.entries(sentByCommand).map(([cmd, count]) => (
                <div key={cmd} className="qdp__cmd-row">
                  <span className="qdp__cmd-name">{cmd}</span>
                  <span className="qdp__cmd-count">{count} 次</span>
                </div>
              ))
            )}
            <div className="qdp__keys">
              <div>OUT: <code>{status?.outboundKey ?? '—'}</code></div>
              <div>IN: <code>{status?.inboundKey ?? '—'}</code></div>
            </div>
          </div>
        )}

        {(tab === 'out' || tab === 'in' || tab === 'all') && (
          <div className="qdp__msg-list">
            {(tab === 'out' ? outMsgs : tab === 'in' ? inMsgs : messages).length === 0 ? (
              <p className="qdp__empty">暂无消息</p>
            ) : (
              (tab === 'out' ? outMsgs : tab === 'in' ? inMsgs : messages).map((msg, i) => (
                <div key={i} className={`qdp__msg qdp__msg--${msg.direction?.toLowerCase()}`}>
                  <div className="qdp__msg-header">
                    <span className={`qdp__msg-dir qdp__msg-dir--${msg.direction?.toLowerCase()}`}>
                      {msg.direction === 'OUT' ? '→ OUT' : '← IN'}
                    </span>
                    <span className="qdp__msg-cmd">{msg.command}</span>
                    <span className="qdp__msg-time">{formatTime(msg.time)}</span>
                  </div>
                  <div className="qdp__msg-detail">
                    <span className="qdp__msg-reqid">req: {shortReqId(msg.reqId)}</span>
                    {msg.data && (
                      <pre className="qdp__msg-data">{JSON.stringify(msg.data, null, 0).substring(0, 200)}</pre>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </div>
    </div>
  )
}
