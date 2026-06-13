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

export default function StructureCognitionContent() {
  const [items, setItems] = useState([])
  const [selectedId, setSelectedId] = useState(null)
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
        if (!cabinetId) {
          throw new Error('未找到屏柜认知数据')
        }
        const data = await api.listKnowledgeCabinetDisplayItems(cabinetId)
        if (!cancelled) {
          setItems(data)
          setSelectedId(data[0]?.id ?? null)
        }
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

  const selectedItem = items.find((item) => item.id === selectedId) ?? items[0] ?? null

  return (
    <div className="cabinet-section cabinet-section--structure">
      <div className="cabinet-section__media">
        {loading && <p className="cabinet-section__paragraph">加载中…</p>}
        {error && <p className="cabinet-section__paragraph cabinet-section__paragraph--error">{error}</p>}
        {!loading && !error && selectedItem && (
          <img
            className="cabinet-section__image"
            src={selectedItem.imageUrl}
            alt={selectedItem.title}
          />
        )}
        {!loading && !error && !selectedItem && (
          <p className="cabinet-section__paragraph">暂无屏柜认知条目</p>
        )}
      </div>
      <div className="cabinet-section__text">
        <h2 className="cabinet-section__title">结构认知</h2>
        {!loading && !error && items.length > 1 && (
          <div className="cabinet-section__item-tabs" role="tablist" aria-label="屏柜认知条目">
            {items.map((item) => (
              <button
                key={item.id}
                type="button"
                role="tab"
                aria-selected={item.id === selectedItem?.id}
                className={`cabinet-section__item-tab${
                  item.id === selectedItem?.id ? ' cabinet-section__item-tab--active' : ''
                }`}
                onClick={() => setSelectedId(item.id)}
              >
                {item.title}
              </button>
            ))}
          </div>
        )}
        {!loading && !error && selectedItem && (
          <div className="cabinet-section__cognition-item">
            {items.length <= 1 && selectedItem.title && (
              <h3 className="cabinet-section__cognition-title">{selectedItem.title}</h3>
            )}
            <p className="cabinet-section__paragraph">{selectedItem.content}</p>
          </div>
        )}
      </div>
    </div>
  )
}
