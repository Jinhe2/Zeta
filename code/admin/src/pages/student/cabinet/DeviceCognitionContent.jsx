import { useEffect, useState } from 'react'
import { api, imageUrl } from '../../../api/client'
import { ImageRegionViewer } from '../../../components/ImageRegionEditor'
import CognitionMediaViewer from '../../../components/CognitionMediaViewer'
import { hasRegion, normalizeRegion } from '../../../utils/imageRegionUtils'
import useFilteredCabinetCognition from './useFilteredCabinetCognition'
import IedBaselineSettingContent from './IedBaselineSettingContent'

const DEVICE_COGNITION_TYPES = ['IED', 'OTHER_DEVICE']

export default function DeviceCognitionContent({
  navigationTarget,
  onPageChange,
  sectionId = 'device',
  deviceTypes = DEVICE_COGNITION_TYPES,
  emptyMessage = '暂无设备认知条目',
  navigationEvent,
}) {
  const [displayItemsState, setDisplayItemsState] = useState({ deviceId: null, items: [] })
  const [selectedCognitionDeviceId, setSelectedCognitionDeviceId] = useState(null)
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
  } = useFilteredCabinetCognition(deviceTypes)

  const selectedCognitionDevice =
    cognitionDevices.find((d) => d.id === selectedCognitionDeviceId) ?? cognitionDevices[0] ?? null
  const activeCognitionDeviceId = selectedCognitionDevice?.id ?? null

  useEffect(() => {
    if (!activeCognitionDeviceId) {
      return undefined
    }

    let cancelled = false
    async function loadDisplayItems() {
      try {
        const items = await api.listKnowledgeCognitionDeviceDisplayItems(activeCognitionDeviceId)
        if (!cancelled) {
          setDisplayItemsState({ deviceId: activeCognitionDeviceId, items })
          setCurrentSlide(0)
        }
      } catch (err) {
        if (!cancelled) setError(err.message)
      }
    }

    loadDisplayItems()
    return () => {
      cancelled = true
    }
  }, [activeCognitionDeviceId, setError])

  useEffect(() => {
    if (navigationTarget?.sectionId !== sectionId) return

    let cancelled = false
    queueMicrotask(() => {
      if (cancelled) return

      if (
        navigationTarget.cabinetItemId != null
        && navigationTarget.cabinetItemId !== selectedCabinetItemId
      ) {
        setSelectedCognitionDeviceId(navigationTarget.deviceId ?? null)
        setDisplayItemsState({ deviceId: null, items: [] })
        setCurrentSlide(navigationTarget.slideIndex ?? 0)
        setSelectedCabinetItemId(navigationTarget.cabinetItemId)
        return
      }

      if (navigationTarget.deviceId != null) {
        setSelectedCognitionDeviceId(navigationTarget.deviceId)
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

  const displayItems = displayItemsState.deviceId === activeCognitionDeviceId ? displayItemsState.items : []
  const currentDisplayItem = displayItems[currentSlide] ?? displayItems[0] ?? null
  const itemHighlightRegion = hasRegion(currentDisplayItem) ? normalizeRegion(currentDisplayItem) : null

  const highlightRegion =
    selectedCabinetItem && selectedCognitionDevice && cognitionDevices.length > 0
      ? normalizeRegion(selectedCognitionDevice)
      : null

  const handleCabinetItemSelect = (itemId) => {
    setSelectedCognitionDeviceId(null)
    setDisplayItemsState({ deviceId: null, items: [] })
    setCurrentSlide(0)
    setSelectedCabinetItemId(itemId)
    onPageChange?.({
      sectionId,
      cabinetItemId: itemId,
    })
  }

  const handleDeviceSelect = (deviceId) => {
    setSelectedCognitionDeviceId(deviceId)
    setCurrentSlide(0)
    onPageChange?.({
      sectionId,
      cabinetItemId: selectedCabinetItem?.id,
      deviceId,
    })
  }

  const handleSlideSelect = (slideIndex) => {
    setCurrentSlide(slideIndex)
    onPageChange?.({
      sectionId,
      cabinetItemId: selectedCabinetItem?.id,
      deviceId: activeCognitionDeviceId,
      slideIndex,
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
          <p className="cabinet-section__paragraph">{emptyMessage}</p>
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
                aria-selected={device.id === selectedCognitionDevice?.id}
                className={`cabinet-section__item-tab${
                  device.id === selectedCognitionDevice?.id ? ' cabinet-section__item-tab--active' : ''
                }`}
                onClick={() => handleDeviceSelect(device.id)}
              >
                {device.title}
              </button>
            ))}
          </div>
        )}
        {!loading && !error && currentDisplayItem?.mediaType === 'IED_BASELINE_SETTING' && activeCognitionDeviceId && (
          <IedBaselineSettingContent
            cognitionDeviceId={activeCognitionDeviceId}
            item={currentDisplayItem}
            navigationEvent={navigationEvent}
            showControls={false}
          />
        )}
        {!loading && !error && currentDisplayItem && currentDisplayItem.mediaType !== 'IED_BASELINE_SETTING' && (
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
          <p className="cabinet-section__paragraph">{emptyMessage}</p>
        )}
      </div>

      <div className="cabinet-section__text cabinet-section__text--device">
        {!loading && !error && currentDisplayItem?.mediaType === 'IED_BASELINE_SETTING' && activeCognitionDeviceId && (
          <IedBaselineSettingContent
            cognitionDeviceId={activeCognitionDeviceId}
            item={currentDisplayItem}
            navigationEvent={navigationEvent}
            showTable={false}
          />
        )}
        {!loading && !error && currentDisplayItem && currentDisplayItem.mediaType !== 'IED_BASELINE_SETTING' && (
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
