import './SectionSelector.css'

export default function SectionSelector({ sections, selectedId, onSelect }) {
  if (!sections?.length) return null

  return (
    <footer className="section-selector">
      <div className="section-selector__head">
        <span className="section-selector__title">断面选择</span>
        <span className="section-selector__desc">选择实验时刻，查看各节点满足状态</span>
      </div>
      <div className="section-selector__list" role="tablist">
        {sections.map((section) => (
          <button
            key={section.id}
            type="button"
            role="tab"
            aria-selected={selectedId === section.id}
            className={`section-selector__item${selectedId === section.id ? ' section-selector__item--active' : ''}`}
            onClick={() => onSelect(section.id)}
          >
            {section.label}
          </button>
        ))}
      </div>
      <div className="section-selector__legend">
        <span className="section-selector__legend-item section-selector__legend-item--ok">满足</span>
        <span className="section-selector__legend-item section-selector__legend-item--fail">不满足</span>
      </div>
    </footer>
  )
}
