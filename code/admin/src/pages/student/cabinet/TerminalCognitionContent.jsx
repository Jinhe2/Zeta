import { useEffect, useState } from 'react'
import { api, imageUrl } from '../../../api/client'
import { ImageRegionViewer } from '../../../components/ImageRegionEditor'
import { normalizeRegion } from '../../../utils/imageRegionUtils'

const DEFAULT_CABINET_CODE = 'cabinet-line-220'

function findCabinetId(tree, cabinetCode) {
  for (const cabinet of tree?.cabinets ?? []) {
    if (cabinet.code === cabinetCode) {
      return cabinet.id
    }
  }
  return tree?.cabinets?.[0]?.id ?? null
}

export default function TerminalCognitionContent() {
  const [cabinetItems, setCabinetItems] = useState([])
  const [cognitionDevices, setCognitionDevices] = useState([])
  const [displayItems, setDisplayItems] = useState([])
  const [selectedCabinetItemId, setSelectedCabinetItemId] = useState(null)
  const [selectedDeviceId, setSelectedDeviceId] = useState(null)
  const [currentSlide, setCurrentSlide] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const tree = await api.getKnowledgeTree()
        const cabinetId = findCabinetId(tree, DEFAULT_CABINET_CODE)
        if (!cabinetId) throw new Error('未找到屏柜学习数据')
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
    load()
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    if (!selectedCabinetItemId) {
      setCognitionDevices([])
      setSelectedDeviceId(null)
      return undefined
    }
    let cancelled = false
    async function loadDevices() {
      try {
        const all = await api.listKnowledgeCognitionDevices(selectedCabinetItemId)
        const devices = all.filter((d) => d.deviceType === 'TERMINAL_GROUP')
        if (!cancelled) {
          setCognitionDevices(devices)
          setSelectedDeviceId(devices[0]?.id ?? null)
        }
      } catch (err) {
        if (!cancelled) setError(err.message)
      }
    }
    loadDevices()
    return () => { cancelled = true }
  }, [selectedCabinetItemId])

  useEffect(() => {
    if (!selectedDeviceId) {
      setDisplayItems([])
      return undefined
    }
    let cancelled = false
    async function loadItems() {
      try {
        const items = await api.listKnowledgeCognitionDeviceDisplayItems(selectedDeviceId)
        if (!cancelled) {
          setDisplayItems(items)
          setCurrentSlide(0)
        }
      } catch (err) {
        if (!cancelled) setError(err.message)
      }
    }
    loadItems()
    return () => { cancelled = true }
  }, [selectedDeviceId])

  const selectedCabinetItem =
    cabinetItems.find((item) => item.id === selectedCabinetItemId) ?? cabinetItems[0] ?? null
  const selectedDevice =
    cognitionDevices.find((d) => d.id === selectedDeviceId) ?? cognitionDevices[0] ?? null
  const currentDisplayItem = displayItems[currentSlide] ?? displayItems[0] ?? null

  const highlightRegion =
    selectedCabinetItem && selectedDevice && cognitionDevices.length > 0
      ? normalizeRegion(selectedDevice)
      : null

  const handleCabinetItemSelect = (itemId) => {
    setCognitionDevices([])
    setSelectedDeviceId(null)
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
              imageUrl={imageUrl(selectedCabinetItem.imageUrl)}
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
              src={imageUrl(currentDisplayItem.imageUrl)}
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
          <p className="cabinet-section__paragraph">暂无端子排认知条目</p>
        )}
      </div>

      <div className="cabinet-section__text cabinet-section__text--device">
        <h2 className="cabinet-section__title">端子排认知</h2>
        {selectedDevice && (
          <p className="cabinet-section__paragraph cabinet-section__meta">
            端子排 · {selectedDevice.title}
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
