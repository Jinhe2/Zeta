import { useEffect, useState } from 'react'
import { api, imageUrl } from '../../../api/client'
import { resolveStudentCabinetId, useStudentCabinetId } from '../studentCabinet'

export default function StructureCognitionContent({ navigationTarget, onPageChange }) {
  const selectedCabinetId = useStudentCabinetId()
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
        const cabinetId = resolveStudentCabinetId(tree, selectedCabinetId)
        if (!cabinetId) {
          throw new Error('未找到屏柜学习数据')
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
  }, [selectedCabinetId])

  useEffect(() => {
    if (navigationTarget?.sectionId !== 'structure') return
    if (navigationTarget.cabinetItemId == null) return

    let cancelled = false
    queueMicrotask(() => {
      if (!cancelled) {
        setSelectedId(navigationTarget.cabinetItemId)
      }
    })
    return () => {
      cancelled = true
    }
  }, [navigationTarget])

  const selectedItem = items.find((item) => item.id === selectedId) ?? items[0] ?? null

  const handleItemSelect = (itemId) => {
    setSelectedId(itemId)
    onPageChange?.({
      sectionId: 'structure',
      cabinetItemId: itemId,
    })
  }

  return (
    <div className="cabinet-section cabinet-section--structure">
      <div className="cabinet-section__media">
        {loading && <p className="cabinet-section__paragraph">加载中…</p>}
        {error && <p className="cabinet-section__paragraph cabinet-section__paragraph--error">{error}</p>}
        {!loading && !error && selectedItem && (
          <img
            className="cabinet-section__image"
            src={imageUrl('cabinet-display', selectedItem.id)}
            alt={selectedItem.title}
          />
        )}
        {!loading && !error && !selectedItem && (
          <p className="cabinet-section__paragraph">暂无屏柜学习条目</p>
        )}
      </div>
      <div className="cabinet-section__text">
        <h2 className="cabinet-section__title">结构认知</h2>
        {!loading && !error && items.length > 1 && (
          <div className="cabinet-section__item-tabs" role="tablist" aria-label="屏柜学习条目">
            {items.map((item) => (
              <button
                key={item.id}
                type="button"
                role="tab"
                aria-selected={item.id === selectedItem?.id}
                className={`cabinet-section__item-tab${
                  item.id === selectedItem?.id ? ' cabinet-section__item-tab--active' : ''
                }`}
                onClick={() => handleItemSelect(item.id)}
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
