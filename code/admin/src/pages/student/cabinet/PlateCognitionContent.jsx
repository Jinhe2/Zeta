import { useCallback, useEffect, useRef, useState } from 'react'
import { api, imageUrl } from '../../../api/client'
import { ImageRegionViewer } from '../../../components/ImageRegionEditor'
import { normalizeRegion } from '../../../utils/imageRegionUtils'
import useFilteredCabinetCognition from './useFilteredCabinetCognition'

const POLL_INTERVAL = 5000

/** 根据压板类型和状态选择 SVG */
function pressboardSvg(type, state) {
  if (type === 'SPARE') return '/images/pressboard/idel_y.svg'
  if (state === 'ON' || state === 'CONNECTED') return '/images/pressboard/close_y.svg'
  if (state === 'OFF' || state === 'DISCONNECTED') return '/images/pressboard/open_y.svg'
  return '/images/pressboard/idel_y.svg'
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

export default function PlateCognitionContent() {
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

  // 压板状态
  const [pressboards, setPressboards] = useState([])
  const [pressboardStates, setPressboardStates] = useState({})
  const [statusLoading, setStatusLoading] = useState(false)
  const pollRef = useRef(null)

  // 加载压板列表
  useEffect(() => {
    if (!cabinetId) return
    let cancelled = false
    api.listHardPressboards(cabinetId)
      .then((data) => { if (!cancelled) setPressboards(data) })
      .catch((err) => { if (!cancelled) setError('压板数据加载失败: ' + err.message) })
    return () => { cancelled = true }
  }, [cabinetId, setError])

  // 轮询压板状态
  const fetchStatus = useCallback(async () => {
    if (!cabinetId) return
    setStatusLoading(true)
    try {
      const data = await api.triggerPressboardStatus(cabinetId)
      if (data?.pressboards) {
        const states = {}
        for (const pb of data.pressboards) {
          states[pb.pressboard_id] = pb.state
        }
        setPressboardStates(states)
      }
    } catch (err) {
      // 静默处理轮询错误（monitord 未启动时预期会失败）
      console.warn('压板状态读取失败:', err.message)
    } finally {
      setStatusLoading(false)
    }
  }, [cabinetId])

  useEffect(() => {
    if (!cabinetId) return
    const firstTimer = setTimeout(fetchStatus, 0)
    pollRef.current = setInterval(fetchStatus, POLL_INTERVAL)
    return () => {
      clearTimeout(firstTimer)
      clearInterval(pollRef.current)
    }
  }, [cabinetId, fetchStatus])

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
  }

  // 构建压板网格（按行列排列）
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
          <p className="cabinet-section__paragraph">暂无压板认知条目</p>
        )}
      </div>

      {/* 右侧：压板状态网格 + 认知条目 */}
      <div className="cabinet-section__media cabinet-section__media--device">
        {/* 压板状态网格 */}
        {selectedCabinetItem && pressboards.length > 0 && (
          <div className="pressboard-grid">
            <div className="pressboard-grid__header">
              <span>压板状态</span>
              <span className="pressboard-grid__status">
                {statusLoading ? '读取中…' : `已更新 ${Object.keys(pressboardStates).length}/${pressboards.length}`}
              </span>
            </div>
            <div className="pressboard-grid__board">
              {pressboardGrid.map((row, ri) => (
                <div key={ri} className="pressboard-grid__row">
                  {row.map((pb, ci) => (
                    <div
                      key={ci}
                      className={`pressboard-grid__cell${pb ? '' : ' pressboard-grid__cell--empty'}`}
                      title={pb ? `${pb.name} (${pb.pressboardType})` : ''}
                    >
                      {pb && (
                        <>
                          <img
                            src={pressboardSvg(pb.pressboardType, pressboardStates[pb.id])}
                            alt={pb.name}
                            className="pressboard-grid__svg"
                          />
                          <span
                            className="pressboard-grid__label"
                            style={{ borderBottomColor: pressboardColor(pb.pressboardType) }}
                          >
                            {pb.name}
                          </span>
                        </>
                      )}
                    </div>
                  ))}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* 认知条目幻灯 */}
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
                onClick={() => setSelectedDeviceId(device.id)}
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
                    onClick={() => setCurrentSlide(i)}
                    aria-label={`第 ${i + 1} 张`}
                  />
                ))}
              </div>
            )}
          </>
        )}
        {!loading && !error && selectedCabinetItem && !currentDisplayItem && pressboards.length === 0 && (
          <p className="cabinet-section__paragraph">暂无压板认知条目</p>
        )}
      </div>

      {/* 右侧下方：文字说明 */}
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
