import { useCallback, useEffect, useRef, useState } from 'react'
import { api, imageUrl } from '../../../api/client'
import { ImageRegionViewer } from '../../../components/ImageRegionEditor'
import { normalizeRegion } from '../../../utils/imageRegionUtils'
import useFilteredCabinetCognition from './useFilteredCabinetCognition'

const POLL_INTERVAL = 5000

const PRESSBOARD_TYPE_SUFFIX = {
  FUNCTION: 'y',
  EXPORT: 'r',
  SPARE: 't',
}

const PRESSBOARD_STATE_NAME = {
  ON: 'close',
  CONNECTED: 'close',
  CLOSE: 'close',
  CLOSED: 'close',
  OFF: 'open',
  DISCONNECTED: 'open',
  OPEN: 'open',
  OPENED: 'open',
}

/** 根据压板类型和状态选择 SVG */
function pressboardSvg(type, state) {
  const typeSuffix = PRESSBOARD_TYPE_SUFFIX[type] ?? PRESSBOARD_TYPE_SUFFIX.FUNCTION
  const stateName = PRESSBOARD_STATE_NAME[normalizePressboardState(state)] ?? 'idel'
  return `/images/pressboard/${stateName}_${typeSuffix}.svg`
}

/** 压板类型颜色标识 */
function pressboardColor(type) {
  switch (type) {
    case 'FUNCTION': return '#fdd835' // 黄色
    case 'EXPORT': return '#e53935'   // 红色
    case 'SPARE': return '#bcaaa4'    // 驼色
    default: return '#90a4ae'
  }
}

/** 按首个中文字符拆分压板名称 */
function splitPressboardName(name) {
  const text = String(name ?? '')
  const chineseIndex = text.search(/[\u3400-\u9fff]/)
  if (chineseIndex <= 0) {
    return { prefix: text, suffix: '' }
  }
  return {
    prefix: text.slice(0, chineseIndex),
    suffix: text.slice(chineseIndex),
  }
}

/** 兼容 monitord 返回的不同压板状态表达 */
function normalizePressboardState(state) {
  if (state === true || state === 1) return 'ON'
  if (state === false || state === 0) return 'OFF'
  const text = String(state ?? '').trim().toUpperCase()
  if (!text) return ''
  if (PRESSBOARD_STATE_NAME[text]) return text
  if (['合闸', '闭合', '合位', '投入', '投'].includes(text)) return 'ON'
  if (['分闸', '断开', '分位', '退出', '退'].includes(text)) return 'OFF'
  if (['1', 'TRUE', 'YES', 'Y'].includes(text)) return 'ON'
  if (['0', 'FALSE', 'NO', 'N'].includes(text)) return 'OFF'
  return text
}

function readPressboardStatusId(pressboardStatus) {
  return pressboardStatus.pressboard_id
    ?? pressboardStatus.pressboardId
    ?? pressboardStatus.id
}

function readPressboardStatusValue(pressboardStatus) {
  return pressboardStatus.state
    ?? pressboardStatus.status
    ?? pressboardStatus.value
    ?? pressboardStatus.position
    ?? pressboardStatus.switch_state
    ?? pressboardStatus.switchState
}

function pressboardStateKey(prefix, value) {
  if (value == null) return null
  const text = String(value).trim()
  return text ? `${prefix}:${text}` : null
}

function readPressboardState(pressboard, states) {
  const idKey = pressboardStateKey('id', pressboard.id)
  const nameKey = pressboardStateKey('name', pressboard.name)
  return states[idKey] ?? states[nameKey] ?? ''
}

export default function PlateCognitionContent({ navigationTarget, onPageChange }) {
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
  } = useFilteredCabinetCognition('PLATE_GROUP')
  const [pressboards, setPressboards] = useState([])
  const [pressboardStates, setPressboardStates] = useState({})
  const [statusLoading, setStatusLoading] = useState(false)
  const [statusError, setStatusError] = useState(null)
  const pollRef = useRef(null)

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
    if (navigationTarget?.sectionId !== 'plate') return

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

  useEffect(() => {
    if (!cabinetId) return undefined
    let cancelled = false
    api.listHardPressboards(cabinetId)
      .then((data) => {
        if (!cancelled) {
          setPressboards(data)
          setStatusError(null)
        }
      })
      .catch((err) => {
        if (!cancelled) setStatusError('压板数据加载失败: ' + err.message)
      })
    return () => {
      cancelled = true
    }
  }, [cabinetId])

  const fetchStatus = useCallback(async () => {
    if (!cabinetId) return
    setStatusLoading(true)
    setStatusError(null)
    try {
      const data = await api.triggerPressboardStatus(cabinetId)
      if (data?.pressboards) {
        const states = {}
        for (const pb of data.pressboards) {
          const id = readPressboardStatusId(pb)
          const state = normalizePressboardState(readPressboardStatusValue(pb))
          if (id != null) {
            states[pressboardStateKey('id', id)] = state
          }
          const nameKey = pressboardStateKey('name', pb.name)
          if (nameKey) {
            states[nameKey] = state
          }
        }
        setPressboardStates(states)
      }
    } catch (err) {
      setStatusError('压板状态读取失败: ' + err.message)
      console.warn('压板状态读取失败:', err.message)
    } finally {
      setStatusLoading(false)
    }
  }, [cabinetId])

  useEffect(() => {
    if (!cabinetId) return undefined
    const firstTimer = setTimeout(fetchStatus, 0)
    pollRef.current = setInterval(fetchStatus, POLL_INTERVAL)
    return () => {
      clearTimeout(firstTimer)
      clearInterval(pollRef.current)
    }
  }, [cabinetId, fetchStatus])

  const displayItems = displayItemsState.deviceId === activeDeviceId ? displayItemsState.items : []
  const hasStatusSlide = Boolean(selectedCabinetItem)
  const totalSlides = displayItems.length + (hasStatusSlide ? 1 : 0)
  const activeSlide = totalSlides > 0 ? Math.min(currentSlide, totalSlides - 1) : 0
  const statusSlideIndex = displayItems.length
  const isStatusSlide = hasStatusSlide && activeSlide === statusSlideIndex
  const currentDisplayItem = isStatusSlide ? null : displayItems[activeSlide] ?? displayItems[0] ?? null

  const maxRow = Math.max(0, ...pressboards.map((pb) => pb.rowNo ?? 0))
  const maxCol = Math.max(0, ...pressboards.map((pb) => pb.colNo ?? 0))
  const pressboardGrid = []
  for (let r = 1; r <= maxRow; r++) {
    const row = []
    for (let c = 1; c <= maxCol; c++) {
      const pb = pressboards.find((p) => p.rowNo === r && p.colNo === c)
      row.push(pb ?? null)
    }
    pressboardGrid.push(row)
  }

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
      sectionId: 'plate',
      cabinetItemId: itemId,
    })
  }

  const handleDeviceSelect = (deviceId) => {
    setSelectedDeviceId(deviceId)
    setDisplayItemsState({ deviceId: null, items: [] })
    setCurrentSlide(0)
    onPageChange?.({
      sectionId: 'plate',
      cabinetItemId: selectedCabinetItem?.id,
      deviceId,
    })
  }

  const handleSlideSelect = (slideIndex) => {
    setCurrentSlide(slideIndex)
    onPageChange?.({
      sectionId: 'plate',
      cabinetItemId: selectedCabinetItem?.id,
      deviceId: activeDeviceId,
      slideIndex,
      kind: slideIndex === statusSlideIndex ? 'status' : 'display',
    })
  }

  return (
    <div className="cabinet-section cabinet-section--device">
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
          <p className="cabinet-section__paragraph">暂无压板认知条目</p>
        )}
      </div>

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
            <img
              key={activeSlide}
              className="cabinet-section__image cabinet-section__image--device"
              src={imageUrl('device-display', currentDisplayItem.id)}
              alt={currentDisplayItem.title}
            />
          </>
        )}
        {!loading && !error && isStatusSlide && (
          <div className="pressboard-grid pressboard-grid--full pressboard-grid--slide">
            <div className="pressboard-grid__header">
              <span>屏柜压板状态</span>
              <span className="pressboard-grid__status">
                {statusLoading ? '读取中…' : `已更新 ${Object.keys(pressboardStates).filter((key) => key.startsWith('id:')).length}/${pressboards.length}`}
              </span>
            </div>
            {statusError && (
              <p className="pressboard-grid__error">{statusError}</p>
            )}
            {pressboards.length === 0 ? (
              <p className="cabinet-section__paragraph">暂无可渲染的压板状态数据</p>
            ) : (
              <div className="pressboard-grid__board">
                {pressboardGrid.map((row, ri) => (
                  <div key={ri} className="pressboard-grid__row">
                    {row.map((pb, ci) => {
                      const nameParts = pb ? splitPressboardName(pb.name) : null
                      const state = pb ? readPressboardState(pb, pressboardStates) : ''
                      return (
                        <div
                          key={ci}
                          className={`pressboard-grid__cell${pb ? '' : ' pressboard-grid__cell--empty'}`}
                          title={pb ? `${pb.name} (${pb.pressboardType})\n前端压板ID: ${pb.id}\n匹配状态: ${state || '未匹配'}` : ''}
                          data-pressboard-id={pb?.id ?? undefined}
                          data-pressboard-name={pb?.name ?? undefined}
                          data-pressboard-type={pb?.pressboardType ?? undefined}
                          data-pressboard-state={state || undefined}
                        >
                          {pb && (
                            <>
                              <img
                                src={pressboardSvg(pb.pressboardType, state)}
                                alt={pb.name}
                                className="pressboard-grid__svg"
                              />
                              <span
                                className="pressboard-grid__label"
                                style={{ borderBottomColor: pressboardColor(pb.pressboardType) }}
                              >
                                <span className="pressboard-grid__label-line">{nameParts.prefix}</span>
                                {nameParts.suffix && (
                                  <span className="pressboard-grid__label-line">{nameParts.suffix}</span>
                                )}
                              </span>
                            </>
                          )}
                        </div>
                      )
                    })}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
        {!loading && !error && selectedCabinetItem && totalSlides > 1 && (
          <div className="cabinet-section__slide-dots">
            {Array.from({ length: totalSlides }, (_, i) => (
              <button
                key={i}
                type="button"
                className={`cabinet-section__slide-dot${i === activeSlide ? ' cabinet-section__slide-dot--active' : ''}`}
                onClick={() => handleSlideSelect(i)}
                aria-label={i === statusSlideIndex ? '压板状态' : `第 ${i + 1} 张`}
              />
            ))}
          </div>
        )}
        {!loading && !error && selectedCabinetItem && totalSlides === 0 && (
          <p className="cabinet-section__paragraph">暂无压板认知条目</p>
        )}
      </div>

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
            <h3 className="cabinet-section__cognition-title">屏柜压板状态</h3>
            <p className="cabinet-section__paragraph">
              自动读取屏柜压板状态并按行列渲染，作为压板认知设备组所有数据库认知条目之后的最后一项。
            </p>
          </div>
        )}
      </div>
    </div>
  )
}
