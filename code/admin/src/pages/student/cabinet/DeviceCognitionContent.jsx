import { useEffect, useState } from 'react'
import { api, imageUrl } from '../../../api/client'
import { ImageRegionViewer } from '../../../components/ImageRegionEditor'
import { normalizeRegion } from '../../../utils/imageRegionUtils'

const DEFAULT_CABINET_CODE = 'cabinet-line-220'

const DEVICE_TYPE_LABELS = {
  IED: 'IED 设备',
  TERMINAL_GROUP: '端子组',
  PLATE_GROUP: '压板组',
}

function findCabinetId(tree, cabinetCode) {
  for (const cabinet of tree?.cabinets ?? []) {
    if (cabinet.code === cabinetCode) {
      return cabinet.id
    }
  }
  return tree?.cabinets?.[0]?.id ?? null
}

export default function DeviceCognitionContent() {
  const [cabinetItems, setCabinetItems] = useState([])
  const [cognitionDevices, setCognitionDevices] = useState([])
  const [displayItems, setDisplayItems] = useState([])
  const [selectedCabinetItemId, setSelectedCabinetItemId] = useState(null)
  const [selectedCognitionDeviceId, setSelectedCognitionDeviceId] = useState(null)
  const [currentSlide, setCurrentSlide] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false

    async function loadCabinetItems() {
      setLoading(true)
      setError(null)
      try {
        const tree = await api.getKnowledgeTree()
        const cabinetId = findCabinetId(tree, DEFAULT_CABINET_CODE)
        if (!cabinetId) {
          throw new Error('未找到屏柜学习数据')
        }
        const items = await api.listKnowledgeCabinetDisplayItems(cabinetId)
        if (!cancelled) {
          setCabinetItems(items)
          setSelectedCabinetItemId(items[0]?.id ?? null)
        }
      } catch (err) {
        if (!cancelled) setError(err.message)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    loadCabinetItems()
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (!selectedCabinetItemId) {
      setCognitionDevices([])
      setSelectedCognitionDeviceId(null)
      return undefined
    }

    let cancelled = false
    async function loadDevices() {
      try {
        const allDevices = await api.listKnowledgeCognitionDevices(selectedCabinetItemId)
        // 设备认知页签只显示 IED 类型的保护装置
        const devices = allDevices.filter((d) => d.deviceType === 'IED')
        if (!cancelled) {
          setCognitionDevices(devices)
          setSelectedCognitionDeviceId(devices[0]?.id ?? null)
        }
      } catch (err) {
        if (!cancelled) setError(err.message)
      }
    }

    loadDevices()
    return () => {
      cancelled = true
    }
  }, [selectedCabinetItemId])

  useEffect(() => {
    if (!selectedCognitionDeviceId) {
      setDisplayItems([])
      return undefined
    }

    let cancelled = false
    async function loadDisplayItems() {
      try {
        const items = await api.listKnowledgeCognitionDeviceDisplayItems(selectedCognitionDeviceId)
        if (!cancelled) {
          setDisplayItems(items)
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
  }, [selectedCognitionDeviceId])

  const selectedCabinetItem =
    cabinetItems.find((item) => item.id === selectedCabinetItemId) ?? cabinetItems[0] ?? null
  const selectedCognitionDevice =
    cognitionDevices.find((d) => d.id === selectedCognitionDeviceId) ?? cognitionDevices[0] ?? null
  const currentDisplayItem = displayItems[currentSlide] ?? displayItems[0] ?? null

  // 只有屏柜条目和设备都已选定，且设备属于当前屏柜条目时，才显示高亮区域
  const highlightRegion =
    selectedCabinetItem && selectedCognitionDevice && cognitionDevices.length > 0
      ? normalizeRegion(selectedCognitionDevice)
      : null

  const handleCabinetItemSelect = (itemId) => {
    // 立即清除旧设备和区域，避免切换瞬间错位
    setCognitionDevices([])
    setSelectedCognitionDeviceId(null)
    setDisplayItems([])
    setCurrentSlide(0)
    setSelectedCabinetItemId(itemId)
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
                onClick={() => setSelectedCognitionDeviceId(device.id)}
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
        {!loading && !error && !currentDisplayItem && (
          <p className="cabinet-section__paragraph">暂无设备认知条目</p>
        )}
      </div>

      <div className="cabinet-section__text cabinet-section__text--device">
        <h2 className="cabinet-section__title">设备认知</h2>
        {selectedCognitionDevice && (
          <p className="cabinet-section__paragraph cabinet-section__meta">
            {DEVICE_TYPE_LABELS[selectedCognitionDevice.deviceType] ?? selectedCognitionDevice.deviceType}
            {' · '}
            {selectedCognitionDevice.title}
          </p>
        )}
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
