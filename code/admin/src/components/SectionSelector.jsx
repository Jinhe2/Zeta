import './SectionSelector.css'

export default function SectionSelector({ sections, selectedId, onSelect }) {
  if (!sections?.length) return null

  const currentIndex = sections.findIndex((s) => s.id === selectedId)
  const hasPrev = currentIndex > 0
  const hasNext = currentIndex >= 0 && currentIndex < sections.length - 1

  const handlePrev = () => {
    if (hasPrev) onSelect(sections[currentIndex - 1].id)
  }

  const handleNext = () => {
    if (hasNext) onSelect(sections[currentIndex + 1].id)
  }

  const handleChange = (e) => {
    onSelect(e.target.value)
  }

  return (
    <footer className="section-selector">
      <div className="section-selector__head">
        <span className="section-selector__title">断面选择</span>
        <span className="section-selector__desc">选择实验时刻，查看各节点满足状态</span>
      </div>

      <div className="section-selector__controls">
        <select
          className="section-selector__select"
          value={selectedId || ''}
          onChange={handleChange}
        >
          {sections.map((section) => (
            <option key={section.id} value={section.id}>
              {section.label}
            </option>
          ))}
        </select>

        <button
          type="button"
          className="section-selector__nav-btn"
          disabled={!hasPrev}
          onClick={handlePrev}
          aria-label="上一个断面"
        >
          ‹ 上一页
        </button>

        <button
          type="button"
          className="section-selector__nav-btn"
          disabled={!hasNext}
          onClick={handleNext}
          aria-label="下一个断面"
        >
          下一页 ›
        </button>
      </div>

      <span className="section-selector__count">
        {currentIndex >= 0 ? `${currentIndex + 1} / ${sections.length}` : sections.length}
      </span>

      <div className="section-selector__legend">
        <span className="section-selector__legend-item section-selector__legend-item--ok">满足</span>
        <span className="section-selector__legend-item section-selector__legend-item--fail">不满足</span>
      </div>
    </footer>
  )
}
