import { useEffect, useState } from 'react'
import { api } from '../../../api/client'

const DEFAULT_DEVICE_CODE = 'device-line-a'

function findDeviceId(tree, deviceCode) {
  for (const cabinet of tree?.cabinets ?? []) {
    for (const device of cabinet.devices ?? []) {
      if (device.code === deviceCode) {
        return device.id
      }
    }
  }
  const firstCabinet = tree?.cabinets?.[0]
  return firstCabinet?.devices?.[0]?.id ?? null
}

export default function DeviceCognitionContent() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false

    async function load() {
      setLoading(true)
      setError(null)
      try {
        const tree = await api.getKnowledgeTree()
        const deviceId = findDeviceId(tree, DEFAULT_DEVICE_CODE)
        if (!deviceId) {
          throw new Error('未找到设备认知数据')
        }
        const data = await api.listKnowledgeDeviceCognitionItems(deviceId)
        if (!cancelled) setItems(data)
      } catch (err) {
        if (!cancelled) setError(err.message)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="cabinet-section cabinet-section--device">
      <div className="cabinet-section__media cabinet-section__media--cabinet">
        <img
          className="cabinet-section__image"
          src="/images/cabinet-structure.svg"
          alt="继电保护屏柜示意图"
        />
      </div>

      <div className="cabinet-section__media cabinet-section__media--device">
        <img
          className="cabinet-section__image cabinet-section__image--device"
          src="/images/protection-device.svg"
          alt="保护测控装置面板示意图"
        />
      </div>

      <div className="cabinet-section__text cabinet-section__text--device">
        <h2 className="cabinet-section__title">设备认知</h2>
        {loading && <p className="cabinet-section__paragraph">正在加载…</p>}
        {error && <p className="cabinet-section__paragraph cabinet-section__paragraph--error">{error}</p>}
        {!loading &&
          !error &&
          items.map((item) => (
            <div key={item.id} className="cabinet-section__cognition-item">
              {item.title && <h3 className="cabinet-section__cognition-title">{item.title}</h3>}
              <p className="cabinet-section__paragraph">{item.content}</p>
            </div>
          ))}
      </div>
    </div>
  )
}
