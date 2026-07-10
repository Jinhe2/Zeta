import { useEffect, useState } from 'react'
import { api, imageUrl } from '../../../api/client'
import { ImageRegionViewer } from '../../../components/ImageRegionEditor'
import CognitionMediaViewer from '../../../components/CognitionMediaViewer'
import { hasRegion, normalizeRegion } from '../../../utils/imageRegionUtils'
import useFilteredCabinetCognition from './useFilteredCabinetCognition'

export default function TerminalCognitionContent({ navigationTarget, onPageChange }) {
  const [displayItemsState, setDisplayItemsState] = useState({ deviceId: null, items: [] })
  const [selectedDeviceId, setSelectedDeviceId] = useState(null)
  const [currentSlide, setCurrentSlide] = useState(0)
  const {
    cabinetItems,
    cognitionDevices,
    selectedCabinetItem,
    selectedCabinetItemId,
    setSelectedCabinetItemId,
    loading,
    error,
    setError,
  } = useFilteredCabinetCognition('TERMINAL_GROUP')

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
    </div>
  )
}
