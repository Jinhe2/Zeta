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
    <div className="cabinet-section cabinet-section--structure">
      <div className="cabinet-section__media">
        <img
          className="cabinet-section__image"
          src="/images/cabinet-structure.svg"
          alt="继电保护屏柜结构示意图"
        />
      </div>
      <div className="cabinet-section__text">
        <h2 className="cabinet-section__title">结构认知</h2>
        {loading && <p className="cabinet-section__paragraph">加载中…</p>}
        {error && <p className="cabinet-section__paragraph">{error}</p>}
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
