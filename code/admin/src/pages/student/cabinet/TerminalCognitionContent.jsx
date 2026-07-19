import { useCallback, useEffect, useState } from 'react'
import { api, imageUrl, publicUrl } from '../../../api/client'
import { ImageRegionViewer } from '../../../components/ImageRegionEditor'
import CognitionMediaViewer from '../../../components/CognitionMediaViewer'
import { hasRegion, normalizeRegion } from '../../../utils/imageRegionUtils'
import useFilteredCabinetCognition from './useFilteredCabinetCognition'

const POLL_INTERVAL = 5000

function terminalStatusKey(terminalId) {
  return terminalId == null ? null : String(terminalId)
}

function terminalNumber(terminalLabel) {
  const matches = String(terminalLabel ?? '').match(/\d+/g)
  return matches?.[matches.length - 1] ?? String(terminalLabel ?? '')
}

function terminalStripTitle(labelPrefix) {
  return String(labelPrefix ?? '').replace(/-+$/, '')
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
  const [terminals, setTerminals] = useState([])
  const [terminalStates, setTerminalStates] = useState({})
  const [statusError, setStatusError] = useState(null)

  const selectedDevice =
    cognitionDevices.find((d) => d.id === selectedDeviceId) ?? cognitionDevices[0] ?? null
  const activeDeviceId = selectedDevice?.id ?? null
  const terminalStripId = navigationTarget?.terminalStripId ?? null
  const terminalStripTitleText = terminalStripTitle(navigationTarget?.terminalStripLabelPrefix)
  const isStatusSlide = navigationTarget?.kind === 'status'

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
    if (!cabinetId || !terminalStripId) {
      return undefined
    }

    let cancelled = false
    api.listTerminals(cabinetId)
      .then((data) => {
        if (!cancelled) {
          setTerminals(data.filter((terminal) => Number(terminal.terminalStripId) === Number(terminalStripId)))
        }
      })
      .catch((err) => {
        if (!cancelled) setStatusError('端子数据加载失败: ' + err.message)
      })
    return () => {
      cancelled = true
    }
  }, [cabinetId, terminalStripId])

  const fetchTerminalStatus = useCallback(async () => {
    if (!cabinetId) return

    setStatusError(null)
    try {
      const data = await api.triggerTerminalStatus(cabinetId)
      if (!Array.isArray(data?.terminals)) return

      const states = {}
      data.terminals.forEach((terminal) => {
        const key = terminalStatusKey(terminal.terminal_id)
        if (key) states[key] = terminal
      })
      setTerminalStates(states)
    } catch (err) {
      setStatusError('端子状态读取失败: ' + err.message)
      console.warn('端子状态读取失败:', err.message)
    }
  }, [cabinetId])

  useEffect(() => {
    if (!isStatusSlide || !cabinetId) return undefined

    const initialTimer = setTimeout(fetchTerminalStatus, 0)
    const timer = setInterval(fetchTerminalStatus, POLL_INTERVAL)
    return () => {
      clearTimeout(initialTimer)
      clearInterval(timer)
    }
  }, [cabinetId, fetchTerminalStatus, isStatusSlide])

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
  const hasStatusSlide = Boolean(terminalStripId)
  const statusSlideIndex = displayItems.length
  const totalSlides = displayItems.length + (hasStatusSlide ? 1 : 0)
  const activeSlide = isStatusSlide
    ? statusSlideIndex
    : totalSlides > 0 ? Math.min(currentSlide, totalSlides - 1) : 0
  const currentDisplayItem = isStatusSlide ? null : displayItems[activeSlide] ?? displayItems[0] ?? null
  const itemHighlightRegion = hasRegion(currentDisplayItem) ? normalizeRegion(currentDisplayItem) : null

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
      kind: slideIndex === statusSlideIndex ? 'status' : 'display',
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

      {/* 右侧：认知条目 */}
      <div className="cabinet-section__media cabinet-section__media--device">
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
            <CognitionMediaViewer
              key={currentDisplayItem.id}
              item={currentDisplayItem}
              imageType="device-display"
              region={itemHighlightRegion}
              alt={currentDisplayItem.title}
            />
            {totalSlides > 1 && (
              <div className="cabinet-section__slide-dots">
                {Array.from({ length: totalSlides }, (_, i) => (
                  <button
                    key={i}
                    type="button"
                    className={`cabinet-section__slide-dot${i === activeSlide ? ' cabinet-section__slide-dot--active' : ''}`}
                    onClick={() => handleSlideSelect(i)}
                    aria-label={i === statusSlideIndex ? '端子连线状态' : `第 ${i + 1} 张`}
                  />
                ))}
              </div>
            )}
          </>
        )}
        {!loading && !error && isStatusSlide && (
          <div className="terminal-wiring-status">
            <div className="terminal-wiring-status__header">
              <img
                className="terminal-wiring-status__tag"
                src={publicUrl('images/terminal/terminal_tag.svg')}
                alt={`端子排 ${terminalStripTitleText}`}
              />
              <span className="terminal-wiring-status__tag-label">{terminalStripTitleText}</span>
            </div>
            {statusError && <p className="terminal-wiring-status__error">{statusError}</p>}
            {terminals.length === 0 ? (
              <p className="cabinet-section__paragraph">暂无可渲染的端子数据</p>
            ) : (
              <div className="terminal-wiring-status__list">
                {terminals.map((terminal) => {
                  const state = terminalStates[terminalStatusKey(terminal.id)]
                  const connected = state?.wiring_status === 'CORRECT'
                  return (
                    <div key={terminal.id} className="terminal-wiring-status__item">
                      {connected && (
                        <img
                          className="terminal-wiring-status__line"
                          src={publicUrl('images/terminal/line.svg')}
                          alt="已接线"
                        />
                      )}
                      <span className="terminal-wiring-status__terminal-clip">
                        <img
                          className="terminal-wiring-status__terminal"
                          src={publicUrl('images/terminal/terminal_ang.svg')}
                          alt={`端子 ${terminal.terminalLabel}`}
                        />
                      </span>
                      <span className="terminal-wiring-status__label">{terminalNumber(terminal.terminalLabel)}</span>
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        )}
        {!loading && !error && selectedCabinetItem && totalSlides > 1 && !currentDisplayItem && (
          <div className="cabinet-section__slide-dots">
            {Array.from({ length: totalSlides }, (_, i) => (
              <button
                key={i}
                type="button"
                className={`cabinet-section__slide-dot${i === activeSlide ? ' cabinet-section__slide-dot--active' : ''}`}
                onClick={() => handleSlideSelect(i)}
                aria-label={i === statusSlideIndex ? '端子连线状态' : `第 ${i + 1} 张`}
              />
            ))}
          </div>
        )}
        {!loading && !error && selectedCabinetItem && !currentDisplayItem && !isStatusSlide && (
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
        {!loading && !error && isStatusSlide && (
          <div className="cabinet-section__cognition-item">
            <h3 className="cabinet-section__cognition-title">端子排连线状态</h3>
            <p className="cabinet-section__paragraph">
              自动读取屏柜端子连接状态；仅连线正确的端子显示接线图元。
            </p>
          </div>
        )}
      </div>
    </div>
  )
}
