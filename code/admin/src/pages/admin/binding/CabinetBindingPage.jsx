import { useCallback, useEffect, useState } from 'react'
import { api } from '../../../api/client'
import { getDeviceBindId, clearDeviceBindId } from '../../../api/deviceBinding'
import './CabinetBindingPage.css'

export default function CabinetBindingPage() {
  const [cabinets, setCabinets] = useState([])
  const [bindingStatus, setBindingStatus] = useState(null) // { status, cabinetName, cabinetId, bindLabel, bindId }
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [flash, setFlash] = useState(null)

  // 绑定弹窗
  const [bindModal, setBindModal] = useState(null)
  const [bindLabel, setBindLabel] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const currentDeviceId = getDeviceBindId()

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    Promise.all([
      api.listBindingCabinets(),
      api.checkBinding(currentDeviceId),
    ])
      .then(([cabinetList, checkResult]) => {
        setCabinets(cabinetList)
        setBindingStatus(checkResult)
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [currentDeviceId])

  useEffect(() => { load() }, [load])

  const showFlash = (msg, ok = true) => {
    setFlash({ msg, ok })
    setTimeout(() => setFlash(null), 3000)
  }

  const handleBind = async () => {
    if (!bindLabel.trim()) return
    setSubmitting(true)
    try {
      await api.bindCabinet(bindModal.cabinetId, currentDeviceId, bindLabel.trim())
      showFlash('绑定成功')
      setBindModal(null)
      setBindLabel('')
      load()
    } catch (e) {
      showFlash(e.message || '绑定失败', false)
    } finally {
      setSubmitting(false)
    }
  }

  const handleForceUnbind = async (cabinetId, cabinetName) => {
    if (!window.confirm(`确定强制解除「${cabinetName}」的绑定？\n\n注意：此操作仅清除服务端绑定记录，对方平板本地的绑定信息无法清除，但对方下次请求时会得到未绑定结果。`)) return
    try {
      await api.forceUnbindCabinet(cabinetId)
      showFlash('强制解绑成功（仅服务端）')
      load()
    } catch (e) {
      showFlash(e.message || '解绑失败', false)
    }
  }

  const handleUnbind = async (cabinetId, cabinetName, isCurrentDevice) => {
    if (!window.confirm(`确定解除「${cabinetName}」的绑定？`)) return
    try {
      await api.unbindCabinet(cabinetId)
      // 如果是当前设备的绑定，同时清除本地存储
      if (isCurrentDevice) {
        clearDeviceBindId()
      }
      showFlash('解绑成功')
      load()
    } catch (e) {
      showFlash(e.message || '解绑失败', false)
    }
  }

  const formatDate = (ts) => {
    if (!ts) return '-'
    const d = new Date(ts)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }

  const isBound = bindingStatus?.status === 'BOUND'

  return (
    <div className="binding-page">
      {/* ── 当前设备绑定状态卡片 ── */}
      <div className={`binding-card ${isBound ? 'binding-card--bound' : 'binding-card--unbound'}`}>
        <div className="binding-card__status-row">
          <span className={`binding-card__dot ${isBound ? 'binding-card__dot--ok' : 'binding-card__dot--warn'}`} />
          <span className="binding-card__status-text">
            {loading ? '检查中…' : isBound ? '已绑定屏柜' : '未绑定屏柜'}
          </span>
        </div>

        {isBound ? (
          <div className="binding-card__body">
            <div className="binding-card__main">
              <h3 className="binding-card__cabinet-name">{bindingStatus.cabinetName}</h3>
              <div className="binding-card__details">
                <div className="binding-card__detail">
                  <span className="binding-card__detail-label">绑定标签</span>
                  <span className="binding-card__detail-value">{bindingStatus.bindLabel}</span>
                </div>
                <div className="binding-card__detail">
                  <span className="binding-card__detail-label">设备 ID</span>
                  <code className="binding-card__detail-value binding-card__detail-value--mono">
                    {bindingStatus.bindId}
                  </code>
                </div>
                <div className="binding-card__detail">
                  <span className="binding-card__detail-label">屏柜 ID</span>
                  <span className="binding-card__detail-value">#{bindingStatus.cabinetId}</span>
                </div>
              </div>
            </div>
            <button
              type="button"
              className="binding-card__unbind-btn"
              onClick={() => handleUnbind(bindingStatus.cabinetId, bindingStatus.cabinetName, true)}
            >
              解除绑定
            </button>
          </div>
        ) : (
          !loading && (
            <div className="binding-card__body">
              <div className="binding-card__main">
                <p className="binding-card__hint">当前设备尚未绑定任何屏柜</p>
                <div className="binding-card__details">
                  <div className="binding-card__detail">
                    <span className="binding-card__detail-label">设备 ID</span>
                    <code className="binding-card__detail-value binding-card__detail-value--mono">
                      {currentDeviceId}
                    </code>
                  </div>
                </div>
              </div>
            </div>
          )
        )}
      </div>

      {/* ── 屏柜清单 ── */}
      <div className="binding-page__section-header">
        <h3 className="binding-page__section-title">屏柜清单与绑定情况</h3>
        <button type="button" className="binding-page__refresh" onClick={load} disabled={loading}>
          刷新
        </button>
      </div>

      {flash && (
        <div className={`binding-page__flash ${flash.ok ? 'binding-page__flash--ok' : 'binding-page__flash--err'}`}>
          {flash.msg}
        </div>
      )}

      {error && <div className="binding-page__error">{error}</div>}

      {loading ? (
        <p className="binding-page__loading">加载中…</p>
      ) : (
        <table className="binding-page__table">
          <thead>
            <tr>
              <th>屏柜名称</th>
              <th>位置</th>
              <th>绑定 ID</th>
              <th>绑定标签</th>
              <th>绑定时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {cabinets.map((c) => {
              const isCurrentDevice = c.bindId === currentDeviceId
              return (
              <tr key={c.cabinetId} className={`${!c.bindId ? 'binding-page__row--unbound' : ''} ${isCurrentDevice ? 'binding-page__row--current' : ''}`}>
                <td className="binding-page__name">
                  {c.cabinetName}
                  {isCurrentDevice && <span className="binding-page__current-tag">本机</span>}
                </td>
                <td className="binding-page__location">{c.cabinetLocation || '-'}</td>
                <td>
                  {c.bindId ? (
                    <code className="binding-page__bind-id">{c.bindId.substring(0, 8)}…</code>
                  ) : (
                    <span className="binding-page__unbound-tag">未绑定</span>
                  )}
                </td>
                <td>{c.bindLabel || '-'}</td>
                <td className="binding-page__date">{formatDate(c.boundAt)}</td>
                <td className="binding-page__actions">
                  {!c.bindId ? (
                    <button
                      type="button"
                      className="binding-page__btn binding-page__btn--bind"
                      onClick={() => {
                        setBindModal({ cabinetId: c.cabinetId, cabinetName: c.cabinetName })
                        setBindLabel(`${c.cabinetName} 平板`)
                      }}
                    >
                      绑定
                    </button>
                  ) : (
                    <>
                      <button
                        type="button"
                        className="binding-page__btn binding-page__btn--unbind"
                        onClick={() => handleUnbind(c.cabinetId, c.cabinetName, isCurrentDevice)}
                      >
                        解绑
                      </button>
                      {!isCurrentDevice && (
                        <button
                          type="button"
                          className="binding-page__btn binding-page__btn--force"
                          onClick={() => handleForceUnbind(c.cabinetId, c.cabinetName)}
                          title="强制清除绑定（仅服务端，对方平板本地信息无法清除）"
                        >
                          强制解绑
                        </button>
                      )}
                    </>
                  )}
                </td>
              </tr>
              )
            })}
          </tbody>
        </table>
      )}

      {/* 绑定弹窗 */}
      {bindModal && (
        <div className="binding-modal">
          <div className="binding-modal__panel">
            <h3 className="binding-modal__title">绑定屏柜：{bindModal.cabinetName}</h3>

            <div className="binding-modal__info">
              <span className="binding-modal__info-label">设备 ID（当前平板）</span>
              <code className="binding-modal__info-value">{currentDeviceId}</code>
            </div>

            <label className="binding-modal__field">
              <span>绑定标签</span>
              <input
                type="text"
                value={bindLabel}
                onChange={(e) => setBindLabel(e.target.value)}
                placeholder="如：1号平板"
                autoFocus
              />
            </label>

            <div className="binding-modal__footer">
              <button
                type="button"
                className="binding-modal__btn binding-modal__btn--cancel"
                disabled={submitting}
                onClick={() => setBindModal(null)}
              >
                取消
              </button>
              <button
                type="button"
                className="binding-modal__btn binding-modal__btn--submit"
                disabled={submitting || !bindLabel.trim()}
                onClick={handleBind}
              >
                {submitting ? '绑定中…' : '确认绑定'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
