import { useEffect, useState } from 'react'
import { api } from '../../../api/client'

const DEFAULT_CABINET_CODE = 'cabinet-line-220'

function findCabinetId(tree, cabinetCode) {
  for (const cabinet of tree?.cabinets ?? []) {
    if (cabinet.code === cabinetCode) {
      return cabinet.id
    }
  }
  return tree?.cabinets?.[0]?.id ?? null
}

export default function useFilteredCabinetCognition(deviceType) {
  const [cabinetId, setCabinetId] = useState(null)
  const [cabinetItems, setCabinetItems] = useState([])
  const [devicesByItemId, setDevicesByItemId] = useState({})
  const [selectedCabinetItemId, setSelectedCabinetItemId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false

    async function loadCabinetItems() {
      setLoading(true)
      setError(null)
      try {
        const tree = await api.getKnowledgeTree()
        const nextCabinetId = findCabinetId(tree, DEFAULT_CABINET_CODE)
        if (!nextCabinetId) {
          throw new Error('未找到屏柜学习数据')
        }

        const allItems = await api.listKnowledgeCabinetDisplayItems(nextCabinetId)
        const itemDevicePairs = await Promise.all(
          allItems.map(async (item) => {
            const devices = await api.listKnowledgeCognitionDevices(item.id)
            return {
              item,
              devices: devices.filter((device) => device.deviceType === deviceType),
            }
          }),
        )
        const visiblePairs = itemDevicePairs.filter((pair) => pair.devices.length > 0)
        const nextItems = visiblePairs.map((pair) => pair.item)
        const nextDevicesByItemId = Object.fromEntries(
          visiblePairs.map((pair) => [pair.item.id, pair.devices]),
        )

        if (!cancelled) {
          setCabinetId(nextCabinetId)
          setCabinetItems(nextItems)
          setDevicesByItemId(nextDevicesByItemId)
          setSelectedCabinetItemId(nextItems[0]?.id ?? null)
        }
      } catch (err) {
        if (!cancelled) {
          setCabinetId(null)
          setCabinetItems([])
          setDevicesByItemId({})
          setSelectedCabinetItemId(null)
          setError(err.message)
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    loadCabinetItems()
    return () => {
      cancelled = true
    }
  }, [deviceType])

  const selectedCabinetItem =
    cabinetItems.find((item) => item.id === selectedCabinetItemId) ?? cabinetItems[0] ?? null
  const cognitionDevices = selectedCabinetItemId ? devicesByItemId[selectedCabinetItemId] ?? [] : []

  return {
    cabinetId,
    cabinetItems,
    cognitionDevices,
    selectedCabinetItem,
    selectedCabinetItemId,
    setSelectedCabinetItemId,
    loading,
    error,
    setError,
  }
}
