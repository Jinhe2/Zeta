import { useEffect, useRef, useState } from 'react'
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

function parseTimeMetric(section, index, firstTimestampMs) {
  if (typeof section.time === 'number' && Number.isFinite(section.time)) {
    return section.time * 1000
  }
  if (section.timestamp) {
    const parsed = Date.parse(section.timestamp.replace(/\//g, '-'))
    if (Number.isFinite(parsed) && Number.isFinite(firstTimestampMs)) {
      return parsed - firstTimestampMs
    }
  }
  return index * 1000
}

function buildTimelineLayout(sections, containerWidth) {
  const sidePadding = 32
  const minGap = 42
  const availableWidth = Math.max(120, containerWidth - sidePadding * 2)

  if (sections.length === 1) {
    return {
      width: '100%',
      lineLeft: containerWidth / 2,
      lineWidth: 0,
      points: [{ left: containerWidth / 2 }],
    }
  }

  const firstTimestampMs = sections[0]?.timestamp
    ? Date.parse(sections[0].timestamp.replace(/\//g, '-'))
    : NaN
  const times = sections.map((section, index) => parseTimeMetric(section, index, firstTimestampMs))
  const gaps = times.slice(1).map((time, index) => Math.max(0, time - times[index]))
  const positiveGaps = gaps.filter((gap) => gap > 0)
  const minPositiveGap = positiveGaps.length ? Math.min(...positiveGaps) : 1
  const maxPositiveGap = positiveGaps.length ? Math.max(...positiveGaps) : 1
  const shouldCompress = maxPositiveGap / minPositiveGap > 12

  const weights = gaps.map((gap) => {
    if (gap <= 0) return minGap
    const linearWeight = gap / minPositiveGap
    return shouldCompress ? Math.sqrt(linearWeight) : linearWeight
  })
  const totalWeight = weights.reduce((sum, weight) => sum + weight, 0) || 1
  const idealGaps = weights.map((weight) => (weight / totalWeight) * availableWidth)
  const totalMinGap = minGap * gaps.length
  const gapWidths = totalMinGap >= availableWidth
    ? gaps.map(() => availableWidth / gaps.length)
    : idealGaps.map((gap) => Math.max(minGap, gap))

  const totalGapWidth = gapWidths.reduce((sum, gap) => sum + gap, 0)
  const scale = totalGapWidth > availableWidth ? availableWidth / totalGapWidth : 1
  const fittedGaps = gapWidths.map((gap) => gap * scale)

  const points = [{ left: sidePadding }]
  for (const gapWidth of fittedGaps) {
    points.push({ left: points[points.length - 1].left + gapWidth })
  }

  const lastLeft = points[points.length - 1].left
  return {
    width: '100%',
    lineLeft: points[0].left,
    lineWidth: Math.max(0, lastLeft - points[0].left),
    points,
  }
}

export default function SectionSelector({ sections, selectedId, onSelect }) {
  if (!sections?.length) return null
  const timelineRef = useRef(null)
  const [timelineWidth, setTimelineWidth] = useState(520)

  const currentIndex = sections.findIndex((s) => s.id === selectedId)
  const hasPrev = currentIndex > 0
  const hasNext = currentIndex >= 0 && currentIndex < sections.length - 1
  const cur = currentIndex >= 0 ? sections[currentIndex] : null
  const { ok, total } = satisfiedCount(cur?.states)
  const timeline = buildTimelineLayout(sections, timelineWidth)

  useEffect(() => {
    const node = timelineRef.current
    if (!node) return undefined

    const updateWidth = () => setTimelineWidth(node.clientWidth || 520)
    updateWidth()

    if (typeof ResizeObserver === 'undefined') {
      window.addEventListener('resize', updateWidth)
      return () => window.removeEventListener('resize', updateWidth)
    }

    const observer = new ResizeObserver(updateWidth)
    observer.observe(node)
    return () => observer.disconnect()
  }, [])

  const handlePrev = () => { if (hasPrev) onSelect(sections[currentIndex - 1].id) }
  const handleNext = () => { if (hasNext) onSelect(sections[currentIndex + 1].id) }

  return (
    <footer className="section-selector">
      {/* ── Left: title ── */}
      <div className="section-selector__head">
        <span className="section-selector__title">断面选择</span>
      </div>

      <span className="section-selector__divider" />

      {/* ── Center: timeline + nav ── */}
      <div className="section-selector__controls">
        <button
          type="button"
          className="section-selector__nav-btn"
          disabled={!hasPrev}
          onClick={handlePrev}
          aria-label="上一个断面"
        >
          ‹ 上一页
        </button>

        <div ref={timelineRef} className="section-selector__timeline" aria-label="断面时间轴">
          <div className="section-selector__timeline-inner" role="list">
            <div
              className="section-selector__timeline-line"
              aria-hidden="true"
              style={{ left: `${timeline.lineLeft}px`, width: `${timeline.lineWidth}px` }}
            />
            {sections.map((section, i) => {
              const { ok: sOk, total: sTotal } = satisfiedCount(section.states)
              const isActive = section.id === selectedId
              const isOk = sTotal > 0 && sOk === sTotal
              const ts = formatTimestamp(section.timestamp)
              const title = `#${String(i + 1).padStart(2, '0')} ${section.label} ${ts} 满足 ${sOk}/${sTotal}`
              return (
                <button
                  key={section.id}
                  type="button"
                  role="listitem"
                  className={`section-selector__timeline-point${isActive ? ' section-selector__timeline-point--active' : ''}${isOk ? ' section-selector__timeline-point--ok' : ' section-selector__timeline-point--fail'}`}
                  style={{ left: `${timeline.points[i].left}px` }}
                  title={title}
                  aria-current={isActive ? 'step' : undefined}
                  onClick={() => onSelect(section.id)}
                >
                  <span className="section-selector__timeline-dot" />
                  <span className="section-selector__timeline-index">
                    {String(i + 1).padStart(2, '0')}
                  </span>
                  <span className="section-selector__timeline-time">
                    {formatTime(section.time)}
                  </span>
                </button>
              )
            })}
          </div>
        </div>

        <button
          type="button"
          className="section-selector__nav-btn"
          disabled={!hasNext}
          onClick={handleNext}
          aria-label="下一个断面"
        >
          下一页 ›
        </button>

        <span className="section-selector__page-pill" aria-label="当前断面页码">
          <span className="section-selector__page-label">当前断面</span>
          <span className="section-selector__page-current">
            {currentIndex >= 0 ? currentIndex + 1 : '-'}
          </span>
          <span className="section-selector__page-sep">/</span>
          <span className="section-selector__page-label">断面总数</span>
          <span className="section-selector__page-total">{sections.length}</span>
        </span>
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
        </div>
      )}

      <span className="section-selector__divider" />

      {/* ── Legend ── */}
      <div className="section-selector__meta">
        <div className="section-selector__satisfy-summary">
          <span className="section-selector__satisfy-label">满足节点数</span>
          <span className="section-selector__satisfy-ok">{ok}</span>
          <span className="section-selector__satisfy-sep">/</span>
          <span className="section-selector__satisfy-label">总节点数</span>
          <span className="section-selector__satisfy-total">{total}</span>
        </div>
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
