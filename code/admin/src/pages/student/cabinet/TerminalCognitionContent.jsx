import { useCallback, useEffect, useState } from 'react'
import { api, imageUrl } from '../../../api/client'
import { ImageRegionViewer } from '../../../components/ImageRegionEditor'
import { normalizeRegion } from '../../../utils/imageRegionUtils'
import useFilteredCabinetCognition from './useFilteredCabinetCognition'

function wiringStatusColor(status) {
  switch (status) {
    case 'CORRECT': return '#66bb6a'
    case 'INCORRECT': return '#ef5350'
    case 'ABNORMAL': return '#ffa726'
    default: return '#78909c'
  }
}

function wiringStatusLabel(status) {
  switch (status) {
    case 'CORRECT': return '正确'
    case 'INCORRECT': return '错误'
    case 'ABNORMAL': return '异常'
    default: return '未知'
  }
}

export default function TerminalCognitionContent({ navigationTarget, onPageChange }) {
  const [displayItemsState, setDisplayItemsState] = useState({ deviceId: null, items: [] })
  const [selectedDeviceId, setSelectedDeviceId] = useState(null)
  const [currentSlide, setCurrentSlide] = useState(0)
  const {
    cabinetId,
    cabinetItems,
    cognitionDevices,
    selectedCabinetItem,
    selectedCabinetItemId,
    setSelectedCabinetItemId,
    loading,
    error,
    setError,
  } = useFilteredCabinetCognition('TERMINAL_GROUP')

  // 端子数据
  const [terminalStrips, setTerminalStrips] = useState([])
  const [terminals, setTerminals] = useState([])
  const [terminalStates, setTerminalStates] = useState({})
  const [statusLoading, setStatusLoading] = useState(false)
  const [statusError, setStatusError] = useState(null)

  // 加载端子排和端子
  useEffect(() => {
    if (!cabinetId) return
    let cancelled = false
    Promise.all([
      api.listTerminalStrips(cabinetId),
      api.listTerminals(cabinetId),
    ])
      .then(([strips, terms]) => {
        if (!cancelled) {
          setTerminalStrips(strips)
          setTerminals(terms)
        }
      })
      .catch((err) => {
        if (!cancelled) setError('端子数据加载失败: ' + err.message)
      })
    return () => { cancelled = true }
  }, [cabinetId, setError])

  // 读取端子状态
  const fetchTerminalStatus = useCallback(async () => {
    if (!cabinetId) return
    setStatusLoading(true)
    setStatusError(null)
    try {
      const data = await api.triggerTerminalStatus(cabinetId)
      if (data?.terminals) {
        const states = {}
        for (const t of data.terminals) {
          states[t.terminal_id] = t
        }
        setTerminalStates(states)
      }
    } catch (err) {
      setStatusError(err.message)
      console.warn('端子状态读取失败:', err.message)
    } finally {
      setStatusLoading(false)
    }
  }, [cabinetId])

  const selectedDevice =
    cognitionDevices.find((d) => d.id === selectedDeviceId) ?? cognitionDevices[0] ?? null
  const activeDeviceId = selectedDevice?.id ?? null

  // 加载认知条目
  useEffect(() => {
    if (!activeDeviceId) {
      return undefined
    }
    let cancelled = false
    async function loadItems() {
      try {
        const items = await api.listKnowledgeCognitionDeviceDisplayItems(activeDeviceId)
        if (!cancelled) {
          setDisplayItemsState({ deviceId: activeDeviceId, items })
          setCurrentSlide(0)
        }
      } catch (err) {
        if (!cancelled) setError(err.message)
      }
    }
    loadItems()
    return () => { cancelled = true }
  }, [activeDeviceId, setError])

  useEffect(() => {
    if (navigationTarget?.sectionId !== 'terminal') return

    let cancelled = false
    queueMicrotask(() => {
      if (cancelled) return

      if (
        navigationTarget.cabinetItemId != null
        && navigationTarget.cabinetItemId !== selectedCabinetItemId
      ) {
        setSelectedDeviceId(navigationTarget.deviceId ?? null)
        setDisplayItemsState({ deviceId: null, items: [] })
        setCurrentSlide(navigationTarget.slideIndex ?? 0)
        setSelectedCabinetItemId(navigationTarget.cabinetItemId)
        return
      }

      if (navigationTarget.deviceId != null) {
        setSelectedDeviceId(navigationTarget.deviceId)
      }
      setCurrentSlide(navigationTarget.slideIndex ?? 0)
    })
    return () => {
      cancelled = true
    }
  }, [
    navigationTarget?.sectionId,
    navigationTarget?.cabinetItemId,
    navigationTarget?.deviceId,
    navigationTarget?.slideIndex,
    navigationTarget?.key,
    selectedCabinetItemId,
    setSelectedCabinetItemId,
    displayItemsState.deviceId,
  ])

  const displayItems = displayItemsState.deviceId === activeDeviceId ? displayItemsState.items : []
  const currentDisplayItem = displayItems[currentSlide] ?? displayItems[0] ?? null

  const highlightRegion =
    selectedCabinetItem && selectedDevice && cognitionDevices.length > 0
      ? normalizeRegion(selectedDevice)
      : null

  const handleCabinetItemSelect = (itemId) => {
    setSelectedDeviceId(null)
    setDisplayItemsState({ deviceId: null, items: [] })
    setCurrentSlide(0)
    setSelectedCabinetItemId(itemId)
    onPageChange?.({
      sectionId: 'terminal',
      cabinetItemId: itemId,
    })
  }

  const handleDeviceSelect = (deviceId) => {
    setSelectedDeviceId(deviceId)
    setDisplayItemsState({ deviceId: null, items: [] })
    setCurrentSlide(0)
    onPageChange?.({
      sectionId: 'terminal',
      cabinetItemId: selectedCabinetItem?.id,
      deviceId,
    })
  }

  const handleSlideSelect = (slideIndex) => {
    setCurrentSlide(slideIndex)
    onPageChange?.({
      sectionId: 'terminal',
      cabinetItemId: selectedCabinetItem?.id,
      deviceId: activeDeviceId,
      slideIndex,
    })
  }

  return (
    <div className="cabinet-section cabinet-section--device">
      {/* 左侧：屏柜图 + 区域高亮 */}
      <div className="cabinet-section__media cabinet-section__media--cabinet">
        {loading && <p className="cabinet-section__paragraph">加载中…</p>}
        {error && <p className="cabinet-section__paragraph cabinet-section__paragraph--error">{error}</p>}
        {!loading && !error && selectedCabinetItem && (
          <>
            {cabinetItems.length > 1 && (
              <div className="cabinet-section__item-tabs cabinet-section__item-tabs--compact" role="tablist">
                {cabinetItems.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    role="tab"
                    aria-selected={item.id === selectedCabinetItem?.id}
                    className={`cabinet-section__item-tab${
                      item.id === selectedCabinetItem?.id ? ' cabinet-section__item-tab--active' : ''
                    }`}
                    onClick={() => handleCabinetItemSelect(item.id)}
                  >
                    {item.title}
                  </button>
                ))}
              </div>
            )}
            <ImageRegionViewer
              key={selectedCabinetItemId}
              imageUrl={imageUrl('cabinet-display', selectedCabinetItem.id)}
              region={highlightRegion}
              alt={selectedCabinetItem.title}
            />
          </>
        )}
        {!loading && !error && !selectedCabinetItem && (
          <p className="cabinet-section__paragraph">暂无端子排认知条目</p>
        )}
      </div>

      {/* 右侧：端子状态 + 认知条目 */}
      <div className="cabinet-section__media cabinet-section__media--device">
        {/* 端子状态区域 */}
        {selectedCabinetItem && terminals.length > 0 && (
          <div className="terminal-status">
            <div className="terminal-status__header">
              <span>端子状态</span>
              <button
                type="button"
                className="terminal-status__btn"
                disabled={statusLoading}
                onClick={fetchTerminalStatus}
              >
                {statusLoading ? '读取中…' : '读取端子状态'}
              </button>
            </div>
            {statusError && (
              <p className="terminal-status__error">读取失败: {statusError}</p>
            )}
            {/* 按端子排分组 */}
            {terminalStrips.map((strip) => {
              const stripTerminals = terminals.filter((t) => t.terminalStripId === strip.id)
              if (stripTerminals.length === 0) return null
              return (
                <div key={strip.id} className="terminal-status__group">
                  <div className="terminal-status__group-label">
                    {strip.name}
                    {strip.functionDesc && <span className="terminal-status__group-desc">{strip.functionDesc}</span>}
                  </div>
                  <div className="terminal-status__list">
                    {stripTerminals.map((t) => {
                      const state = terminalStates[t.id]
                      return (
                        <div key={t.id} className="terminal-status__item">
                          <span className="terminal-status__label">{t.terminalLabel}</span>
                          <span className={`terminal-status__type terminal-status__type--${(t.signalType || '').toLowerCase()}`}>
                            {t.signalType}
                          </span>
                          {state && (
                            <>
                              <span
                                className="terminal-status__wiring"
                                style={{ color: wiringStatusColor(state.wiring_status) }}
                              >
                                {wiringStatusLabel(state.wiring_status)}
                              </span>
                              {state.realtime && (
                                <span className="terminal-status__realtime">
                                  {state.realtime.type === 'DIGITAL'
                                    ? state.realtime.state
                                    : `${state.realtime.magnitude ?? '-'}∠${state.realtime.angle ?? '-'}°`}
                                </span>
                              )}
                              {state.wiring_message && (
                                <span className="terminal-status__message">{state.wiring_message}</span>
                              )}
                            </>
                          )}
                        </div>
                      )
                    })}
                  </div>
                </div>
              )
            })}
          </div>
        )}

        {/* 认知条目 */}
        {!loading && !error && cognitionDevices.length > 1 && (
          <div className="cabinet-section__item-tabs cabinet-section__item-tabs--compact" role="tablist">
            {cognitionDevices.map((device) => (
              <button
                key={device.id}
                type="button"
                role="tab"
                aria-selected={device.id === selectedDevice?.id}
                className={`cabinet-section__item-tab${
                  device.id === selectedDevice?.id ? ' cabinet-section__item-tab--active' : ''
                }`}
                onClick={() => handleDeviceSelect(device.id)}
              >
                {device.title}
              </button>
            ))}
          </div>
        )}
        {!loading && !error && currentDisplayItem && (
          <>
            <img
              key={currentSlide}
              className="cabinet-section__image cabinet-section__image--device"
              src={imageUrl('device-display', currentDisplayItem.id)}
              alt={currentDisplayItem.title}
            />
            {displayItems.length > 1 && (
              <div className="cabinet-section__slide-dots">
                {displayItems.map((_, i) => (
                  <button
                    key={i}
                    type="button"
                    className={`cabinet-section__slide-dot${i === currentSlide ? ' cabinet-section__slide-dot--active' : ''}`}
                    onClick={() => handleSlideSelect(i)}
                    aria-label={`第 ${i + 1} 张`}
                  />
                ))}
              </div>
            )}
          </>
        )}
        {!loading && !error && selectedCabinetItem && !currentDisplayItem && terminals.length === 0 && (
          <p className="cabinet-section__paragraph">暂无端子排认知条目</p>
        )}
      </div>

      {/* 文字说明 */}
      <div className="cabinet-section__text cabinet-section__text--device">
        {!loading && !error && currentDisplayItem && (
          <div className="cabinet-section__cognition-item">
            {currentDisplayItem.title && (
              <h3 className="cabinet-section__cognition-title">{currentDisplayItem.title}</h3>
            )}
            <p className="cabinet-section__paragraph">{currentDisplayItem.content}</p>
          </div>
        )}
      </div>
    </div>
  )
}
