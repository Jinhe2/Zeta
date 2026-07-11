import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from '../api/client'
import { getDeviceBindId } from '../api/deviceBinding'
import { useAuth } from '../auth/AuthContext'
import './IedCommunicationStatus.css'

const POLL_INTERVAL = 5000

function isOnline(device) {
  return String(device?.status || '').toLowerCase() === 'online'
}

function getSummaryState(devices, failed) {
  if (failed || devices.length === 0) return 'error'
  const onlineCount = devices.filter(isOnline).length
  if (onlineCount === devices.length) return 'ok'
  if (onlineCount === 0) return 'error'
  return 'warning'
}

export default function IedCommunicationStatus() {
  const { session } = useAuth()
  const isAdmin = session?.role === 'ADMIN'
  const [devices, setDevices] = useState([])
  const [error, setError] = useState('')
  const [open, setOpen] = useState(false)
  const requestInFlight = useRef(false)

  const refresh = useCallback(async () => {
    if (requestInFlight.current) return
    requestInFlight.current = true
    try {
      let cabinetId
      if (isAdmin) {
        const cabinets = await api.listBindingCabinets()
        cabinetId = (Array.isArray(cabinets) ? cabinets : [])
          .map((cabinet) => cabinet?.cabinetId)
          .filter((id) => id !== null && id !== undefined && Number.isFinite(Number(id)))
          .map(Number)
          .sort((a, b) => a - b)[0]
        if (!cabinetId) throw new Error('暂无可展示的屏柜')
      } else {
        const binding = await api.checkBinding(getDeviceBindId())
        if (binding?.status !== 'BOUND' || !binding?.cabinetId) {
          throw new Error('当前设备未绑定屏柜')
        }
        cabinetId = binding.cabinetId
      }
      const data = await api.triggerIedCommStatus(cabinetId)
      setDevices(Array.isArray(data?.devices) ? data.devices : [])
      setError('')
    } catch (err) {
      setDevices([])
      setError(err?.message || '通讯状态读取失败')
    } finally {
      requestInFlight.current = false
    }
  }, [isAdmin])

  useEffect(() => {
    const initialRefresh = window.setTimeout(refresh, 0)
    const timer = window.setInterval(refresh, POLL_INTERVAL)
    return () => {
      window.clearTimeout(initialRefresh)
      window.clearInterval(timer)
    }
  }, [refresh])

  const summaryState = getSummaryState(devices, Boolean(error))
  const summaryText = summaryState === 'ok' ? '通讯正常' : summaryState === 'warning' ? '部分中断' : '通讯中断'

  return (
    <>
      <button
        type="button"
        className="ied-status-toggle"
        onClick={() => setOpen(true)}
        title={summaryText}
      >
        <span className={`ied-status-toggle__light ied-status-toggle__light--${summaryState}`} />
        设备状态
      </button>

      {open && (
        <div className="ied-status-dialog" role="dialog" aria-modal="true" aria-labelledby="ied-status-dialog-title">
          <button type="button" className="ied-status-dialog__mask" aria-label="关闭设备状态" onClick={() => setOpen(false)} />
          <section className="ied-status-dialog__panel">
            <header className="ied-status-dialog__header">
              <div>
                <h2 id="ied-status-dialog-title">设备状态</h2>
                <p>当前屏柜 · {summaryText}</p>
              </div>
              <div className="ied-status-dialog__actions">
                <button type="button" onClick={refresh}>刷新</button>
                <button type="button" aria-label="关闭" onClick={() => setOpen(false)}>×</button>
              </div>
            </header>

            {error ? (
              <p className="ied-status-dialog__message ied-status-dialog__message--error">{error}</p>
            ) : devices.length === 0 ? (
              <p className="ied-status-dialog__message">未读取到屏柜内设备通讯状态。</p>
            ) : (
              <div className="ied-status-dialog__grid">
                {devices.map((device, index) => {
                  const online = isOnline(device)
                  return (
                    <article className="ied-status-device" key={device.ied_device_id ?? `${device.ied_name}-${index}`}>
                      <div className="ied-status-device__name">{device.ied_desc || '未命名装置'}</div>
                      <dl>
                        <div><dt>IED 名称</dt><dd>{device.ied_name || '—'}</dd></div>
                        <div><dt>IP</dt><dd>{device.ip || '—'}</dd></div>
                        <div><dt>状态</dt><dd className={online ? 'ied-status-device__state--ok' : 'ied-status-device__state--error'}><span />{online ? '正常' : '中断'}</dd></div>
                      </dl>
                    </article>
                  )
                })}
              </div>
            )}
          </section>
        </div>
      )}
    </>
  )
}
