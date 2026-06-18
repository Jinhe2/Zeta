import './SectionSelector.css'

function formatTime(sec) {
  if (sec == null) return '-'
  if (sec < 1) return `${Math.round(sec * 1000)} ms`
  if (sec < 60) return `${sec.toFixed(3)} s`
  const m = Math.floor(sec / 60)
  const s = (sec % 60).toFixed(1)
  return `${m} min ${s} s`
}

function formatTimestamp(ts) {
  if (!ts) return '-'
  // "2026/06/01 11:00:00.300" → "11:00:00.300"
  const spaceIdx = ts.indexOf(' ')
  return spaceIdx >= 0 ? ts.slice(spaceIdx + 1) : ts
}

function satisfiedCount(states) {
  if (!states) return { ok: 0, total: 0 }
  const vals = Object.values(states)
  return { ok: vals.filter(Boolean).length, total: vals.length }
}

export default function SectionSelector({ sections, selectedId, onSelect }) {
  if (!sections?.length) return null

  const currentIndex = sections.findIndex((s) => s.id === selectedId)
  const hasPrev = currentIndex > 0
  const hasNext = currentIndex >= 0 && currentIndex < sections.length - 1
  const cur = currentIndex >= 0 ? sections[currentIndex] : null
  const { ok, total } = satisfiedCount(cur?.states)

  const handlePrev = () => { if (hasPrev) onSelect(sections[currentIndex - 1].id) }
  const handleNext = () => { if (hasNext) onSelect(sections[currentIndex + 1].id) }
  const handleChange = (e) => { onSelect(e.target.value) }

  return (
    <footer className="section-selector">
      {/* ── Left: title ── */}
      <div className="section-selector__head">
        <span className="section-selector__title">断面选择</span>
      </div>

      <span className="section-selector__divider" />

      {/* ── Center: dropdown + nav ── */}
      <div className="section-selector__controls">
        <select
          className="section-selector__select"
          value={selectedId || ''}
          onChange={handleChange}
        >
          {sections.map((section, i) => {
            const { ok: sOk, total: sTotal } = satisfiedCount(section.states)
            const ts = formatTimestamp(section.timestamp)
            return (
              <option key={section.id} value={section.id}>
                {`#${String(i + 1).padStart(2, '0')}  ${section.label}  ${ts}  满足 ${sOk}/${sTotal}`}
              </option>
            )
          })}
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

      <span className="section-selector__divider" />

      {/* ── Right: info panel ── */}
      {cur && (
        <div className="section-selector__info">
          <div className="section-selector__info-row">
            <span className="section-selector__info-label">时刻</span>
            <span className="section-selector__info-value section-selector__info-value--mono">
              {formatTimestamp(cur.timestamp)}
            </span>
          </div>
          <div className="section-selector__info-row">
            <span className="section-selector__info-label">经过</span>
            <span className="section-selector__info-value section-selector__info-value--mono">
              {formatTime(cur.time)}
            </span>
          </div>
          <div className="section-selector__info-row">
            <span className="section-selector__info-label">满足</span>
            <span className="section-selector__info-value">
              <span className="section-selector__info-ok">{ok}</span>
              <span className="section-selector__info-sep">/</span>
              <span>{total}</span>
            </span>
          </div>
        </div>
      )}

      <span className="section-selector__divider" />

      {/* ── Counter + legend ── */}
      <div className="section-selector__meta">
        <span className="section-selector__count">
          {currentIndex >= 0 ? `${currentIndex + 1}` : '-'}
          <span className="section-selector__count-total"> / {sections.length}</span>
        </span>
        <div className="section-selector__legend">
          <span className="section-selector__legend-dot section-selector__legend-dot--ok" />
          <span>满足</span>
          <span className="section-selector__legend-dot section-selector__legend-dot--fail" />
          <span>不满足</span>
        </div>
      </div>
    </footer>
  )
}
