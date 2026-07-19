import { useCallback, useEffect, useRef, useState } from 'react'
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

function initialPracticeState() {
  return {
    sequence: null,
    phase: 'idle',
    startRefreshVersion: 0,
  }
}

export default function TerminalCognitionContent({ navigationTarget, navigationEvent, onPageChange }) {
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
  const [statusRefreshVersion, setStatusRefreshVersion] = useState(0)
  const [statusError, setStatusError] = useState(null)
  const [practiceDialog, setPracticeDialog] = useState(null)
  const practiceRef = useRef(initialPracticeState())

  const selectedDevice =
    cognitionDevices.find((d) => d.id === selectedDeviceId) ?? cognitionDevices[0] ?? null
  const activeDeviceId = selectedDevice?.id ?? null
  const displayItems = displayItemsState.deviceId === activeDeviceId ? displayItemsState.items : []
  const currentDisplayItem = displayItems[Math.min(currentSlide, Math.max(displayItems.length - 1, 0))] ?? null
  const terminalOperation = currentDisplayItem?.mediaType === 'TERMINAL_OPERATION' ? currentDisplayItem.terminalOperation : null
  const terminalStripId = terminalOperation?.terminalStripId ?? null
  const isPracticeStatusTarget = Boolean(terminalOperation) && navigationTarget?.sectionId === 'terminal'
  const visiblePracticeDialog =
    isPracticeStatusTarget
    && practiceDialog?.pageKey === navigationTarget?.key
    && practiceDialog?.sequence === navigationEvent?.sequence

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
      setStatusRefreshVersion((version) => version + 1)
      return states
    } catch (err) {
      setStatusError('端子状态读取失败: ' + err.message)
      console.warn('端子状态读取失败:', err.message)
      return null
    }
  }, [cabinetId])

  useEffect(() => {
    if (!isPracticeStatusTarget || !cabinetId) return undefined

    const initialTimer = setTimeout(fetchTerminalStatus, 0)
    const timer = setInterval(fetchTerminalStatus, POLL_INTERVAL)
    return () => {
      clearTimeout(initialTimer)
      clearInterval(timer)
    }
  }, [cabinetId, fetchTerminalStatus, isPracticeStatusTarget])

  useEffect(() => {
    if (isPracticeStatusTarget) return
    practiceRef.current = initialPracticeState()
  }, [isPracticeStatusTarget, navigationTarget?.key])

  useEffect(() => {
    if (
      navigationEvent?.source !== 'next'
      || navigationEvent.pageKey !== navigationTarget?.key
      || !isPracticeStatusTarget
      || practiceRef.current.sequence === navigationEvent.sequence
    ) {
      return
    }

    practiceRef.current = {
      sequence: navigationEvent.sequence,
      phase: 'waiting-refresh',
      startRefreshVersion: statusRefreshVersion,
    }
    setPracticeDialog(null)
    fetchTerminalStatus()
  }, [
    fetchTerminalStatus,
    isPracticeStatusTarget,
    navigationEvent?.pageKey,
    navigationEvent?.sequence,
    navigationEvent?.source,
    navigationTarget?.key,
    statusRefreshVersion,
  ])

  useEffect(() => {
    const practice = practiceRef.current
    if (
      practice.phase !== 'waiting-refresh'
      || statusRefreshVersion <= practice.startRefreshVersion
      || terminals.length === 0
    ) {
      return
    }

    const practiceTerminals = terminalOperation?.terminals ?? []
    const disconnectedTerminals = practiceTerminals.filter(
      (terminal) => terminalStates[terminalStatusKey(terminal.terminalId)]?.wiring_status !== 'CORRECT',
    )
    if (disconnectedTerminals.length === 0) {
      practiceRef.current = { ...practice, phase: 'completed' }
      return
    }

    practiceRef.current = { ...practice, phase: 'watching' }
    setPracticeDialog({
      pageKey: navigationTarget?.key,
      sequence: practice.sequence,
      message: disconnectedTerminals.map((terminal) => `请将测试仪上的 ${terminal.meaning} 接入 ${terminal.terminalLabel} 端子`).join('\n'),
    })
  }, [navigationTarget?.key, statusRefreshVersion, terminalStates, terminalOperation])

  useEffect(() => {
    const practice = practiceRef.current
    if (practice.phase !== 'watching') return

    const practiceTerminals = terminalOperation?.terminals ?? []
    const allConnected = practiceTerminals.length > 0 && practiceTerminals.every(
      (terminal) => terminalStates[terminalStatusKey(terminal.terminalId)]?.wiring_status === 'CORRECT',
    )
    if (!allConnected) return

    practiceRef.current = { ...practice, phase: 'completed' }
    setPracticeDialog({
      pageKey: navigationTarget?.key,
      sequence: practice.sequence,
      message: '恭喜你，已正确接入端子连线，请继续学习。',
    })
  }, [navigationTarget?.key, statusRefreshVersion, terminalStates, terminalOperation])

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

  const totalSlides = displayItems.length
  const activeSlide = totalSlides > 0 ? Math.min(currentSlide, totalSlides - 1) : 0
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
      kind: 'display',
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
            {terminalOperation ? <div className="terminal-wiring-status">
              <div className="terminal-wiring-status__header">
                <img className="terminal-wiring-status__tag" src={publicUrl('images/terminal/terminal_tag.svg')} alt={`端子排 ${terminalOperation.terminalStripLabelPrefix || terminalOperation.terminalStripName || ''}`} />
                <span className="terminal-wiring-status__tag-label">{String(terminalOperation.terminalStripLabelPrefix || terminalOperation.terminalStripName || '').replace(/-+$/, '')}</span>
              </div>
              {statusError && <p className="terminal-wiring-status__error">{statusError}</p>}
              {terminals.length === 0 ? <p className="cabinet-section__paragraph">暂无可渲染的端子数据</p> : <div className="terminal-wiring-status__list">
                {terminals.map((terminal) => {
                  const connected = terminalStates[terminalStatusKey(terminal.id)]?.wiring_status === 'CORRECT'
                  return <div key={terminal.id} className="terminal-wiring-status__item">
                    {connected && <img className="terminal-wiring-status__line" src={publicUrl('images/terminal/line.svg')} alt="已接线" />}
                    <span className="terminal-wiring-status__terminal-clip"><img className="terminal-wiring-status__terminal" src={publicUrl('images/terminal/terminal_ang.svg')} alt={`端子 ${terminal.terminalLabel}`} /></span>
                    <span className="terminal-wiring-status__label">{terminalNumber(terminal.terminalLabel)}</span>
                  </div>
                })}
              </div>}
            </div> : <CognitionMediaViewer
              key={currentDisplayItem.id}
              item={currentDisplayItem}
              imageType="device-display"
              region={itemHighlightRegion}
              alt={currentDisplayItem.title}
            />}
            {totalSlides > 1 && (
              <div className="cabinet-section__slide-dots">
                {Array.from({ length: totalSlides }, (_, i) => (
                  <button
                    key={i}
                    type="button"
                    className={`cabinet-section__slide-dot${i === activeSlide ? ' cabinet-section__slide-dot--active' : ''}`}
                    onClick={() => handleSlideSelect(i)}
                    aria-label={`第 ${i + 1} 张`}
                  />
                ))}
              </div>
            )}
          </>
        )}
        {!loading && !error && selectedCabinetItem && totalSlides > 1 && !currentDisplayItem && (
          <div className="cabinet-section__slide-dots">
            {Array.from({ length: totalSlides }, (_, i) => (
              <button
                key={i}
                type="button"
                className={`cabinet-section__slide-dot${i === activeSlide ? ' cabinet-section__slide-dot--active' : ''}`}
                onClick={() => handleSlideSelect(i)}
                aria-label={`第 ${i + 1} 张`}
              />
            ))}
          </div>
        )}
        {!loading && !error && selectedCabinetItem && !currentDisplayItem && (
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

      {visiblePracticeDialog && (
        <div className="pressboard-practice-dialog" role="dialog" aria-modal="true" aria-labelledby="terminal-practice-dialog-message">
          <div className="pressboard-practice-dialog__mask" />
          <div className="pressboard-practice-dialog__panel">
            <p id="terminal-practice-dialog-message" className="pressboard-practice-dialog__message" style={{ whiteSpace: 'pre-line' }}>
              {practiceDialog.message}
            </p>
            <button
              type="button"
              className="pressboard-practice-dialog__btn"
              onClick={() => setPracticeDialog(null)}
            >
              确定
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
