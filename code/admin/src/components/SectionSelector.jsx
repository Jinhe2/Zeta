import { useEffect, useRef, useState } from 'react'
import './SectionSelector.css'

const TIMELINE_WINDOW_THRESHOLD = 20
const TIMELINE_WINDOW_RADIUS = 5

function formatTime(sec) {
  if (sec == null) return '-'
  if (sec < 1) return `${Math.round(sec * 1000)} ms`
  if (sec < 60) return `${sec.toFixed(3)} s`
  const m = Math.floor(sec / 60)
  const s = (sec % 60).toFixed(1)
  return `${m} min ${s} s`
}

function formatTimelineTime(sec) {
  if (sec == null) return '-'
  if (sec < 1) return `${Math.round(sec * 1000)}ms`
  if (sec < 10) return `${sec.toFixed(3)}s`
  if (sec < 60) return `${sec.toFixed(2)}s`
  const m = Math.floor(sec / 60)
  const s = (sec % 60).toFixed(1).padStart(4, '0')
  return `${m}m${s}s`
}

function formatTimestamp(ts) {
  if (!ts) return '-'
  // "2026/06/01 11:00:00.300" → "11:00:00.300"
  const spaceIdx = ts.indexOf(' ')
  return spaceIdx >= 0 ? ts.slice(spaceIdx + 1) : ts
}

function satisfiedCount(states, nodeIds) {
  if (!states) return { ok: 0, total: 0, invalid: 0 }
  if (nodeIds?.length) {
    const validNodeIds = nodeIds.filter((nodeId) => states[nodeId] !== -1)
    return {
      ok: validNodeIds.filter((nodeId) => isNodeOn(states, nodeId)).length,
      total: validNodeIds.length,
      invalid: nodeIds.length - validNodeIds.length,
    }
  }
  const vals = Object.values(states)
  const validVals = vals.filter((value) => value !== -1)
  return {
    ok: validVals.filter((value) => value === true || value === 1).length,
    total: validVals.length,
    invalid: vals.length - validVals.length,
  }
}

function isNodeOn(states, nodeId) {
  return states?.[nodeId] === true || states?.[nodeId] === 1
}

function isActionSection(sections, index, outputNodeIds) {
  if (!outputNodeIds?.length || index <= 0) return false
  const previousStates = sections[index - 1]?.states
  const currentStates = sections[index]?.states
  return outputNodeIds.some((nodeId) => !isNodeOn(previousStates, nodeId) && isNodeOn(currentStates, nodeId))
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

export default function SectionSelector({ sections, selectedId, onSelect, inputNodeIds = [], outputNodeIds = [] }) {
  const timelineRef = useRef(null)
  const [timelineWidth, setTimelineWidth] = useState(520)
  const [windowFocusIndex, setWindowFocusIndex] = useState(null)
  const safeSections = sections ?? []

  const currentIndex = safeSections.findIndex((s) => s.id === selectedId)
  const hasPrev = currentIndex > 0
  const hasNext = currentIndex >= 0 && currentIndex < safeSections.length - 1
  const cur = currentIndex >= 0 ? safeSections[currentIndex] : null
  const { ok, total, invalid } = satisfiedCount(cur?.states, inputNodeIds)
  const shouldWindowTimeline = safeSections.length > TIMELINE_WINDOW_THRESHOLD && currentIndex >= 0
  const windowSize = TIMELINE_WINDOW_RADIUS * 2 + 1
  const clampIndex = (index) => Math.min(safeSections.length - 1, Math.max(0, index))
  const windowAnchorIndex = safeSections.length && currentIndex >= 0
    ? clampIndex(windowFocusIndex ?? currentIndex)
    : currentIndex
  let visibleStart = 0
  let visibleEnd = safeSections.length - 1
  if (shouldWindowTimeline) {
    visibleStart = Math.max(0, windowAnchorIndex - TIMELINE_WINDOW_RADIUS)
    visibleEnd = Math.min(safeSections.length - 1, windowAnchorIndex + TIMELINE_WINDOW_RADIUS)
    if (visibleEnd - visibleStart + 1 < windowSize) {
      if (visibleStart === 0) {
        visibleEnd = Math.min(safeSections.length - 1, visibleStart + windowSize - 1)
      } else if (visibleEnd === safeSections.length - 1) {
        visibleStart = Math.max(0, visibleEnd - windowSize + 1)
      }
    }
  }
  const visibleSections = safeSections.slice(visibleStart, visibleEnd + 1).map((section, offset) => ({
    section,
    index: visibleStart + offset,
  }))
  const timeline = visibleSections.length
    ? buildTimelineLayout(visibleSections.map((item) => item.section), timelineWidth)
    : null
  const overviewWindowLeft = safeSections.length > 1 ? (visibleStart / (safeSections.length - 1)) * 100 : 0
  const overviewWindowRight = safeSections.length > 1 ? (visibleEnd / (safeSections.length - 1)) * 100 : 100
  const overviewWindowWidth = Math.max(2, overviewWindowRight - overviewWindowLeft)

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

  useEffect(() => {
    if (windowFocusIndex == null && currentIndex >= 0) {
      setWindowFocusIndex(currentIndex)
    }
  }, [currentIndex, windowFocusIndex])

  if (!safeSections.length) return null

  const selectByIndex = (index) => {
    const clampedIndex = clampIndex(index)
    onSelect(safeSections[clampedIndex].id)
  }
  const slideWindowToIndex = (index) => {
    setWindowFocusIndex(clampIndex(index))
  }
  const handlePrev = () => { if (hasPrev) selectByIndex(currentIndex - 1) }
  const handleNext = () => { if (hasNext) selectByIndex(currentIndex + 1) }
  const slideOverviewPosition = (clientX, target) => {
    const track = target.closest?.('.section-selector__overview-track')
    if (!track || safeSections.length <= 1) return
    const rect = track.getBoundingClientRect()
    const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width))
    const targetIndex = Math.round(ratio * (safeSections.length - 1))
    slideWindowToIndex(targetIndex)
  }
  const handleOverviewPointerDown = (event) => {
    if (!shouldWindowTimeline) return
    event.preventDefault()
    event.currentTarget.setPointerCapture?.(event.pointerId)
    slideOverviewPosition(event.clientX, event.currentTarget)
  }
  const handleOverviewPointerMove = (event) => {
    if (!shouldWindowTimeline || !(event.buttons & 1)) return
    slideOverviewPosition(event.clientX, event.currentTarget)
  }

  return (
    <footer className="section-selector">
      {/* ── Center: timeline + nav ── */}
      <div className="section-selector__controls">
        <button
          type="button"
          className="section-selector__nav-btn"
          disabled={!hasPrev}
          onClick={handlePrev}
          aria-label="上一个断面"
        >
          ‹ 上一点
        </button>

        <div ref={timelineRef} className="section-selector__timeline" aria-label="断面时间轴">
          <div className="section-selector__timeline-inner" role="list">
            {visibleStart > 0 && <span className="section-selector__timeline-ellipsis section-selector__timeline-ellipsis--left">…</span>}
            {visibleEnd < safeSections.length - 1 && <span className="section-selector__timeline-ellipsis section-selector__timeline-ellipsis--right">…</span>}
            <div
              className="section-selector__timeline-line"
              aria-hidden="true"
              style={{ left: `${timeline.lineLeft}px`, width: `${timeline.lineWidth}px` }}
            />
            {visibleSections.map(({ section, index: i }, localIndex) => {
              const { ok: sOk, total: sTotal } = satisfiedCount(section.states, inputNodeIds)
              const isActive = section.id === selectedId
              const isOk = sTotal > 0 && sOk === sTotal
              const isAction = isActionSection(safeSections, i, outputNodeIds)
              const ts = formatTimestamp(section.timestamp)
              const title = `#${String(i + 1).padStart(2, '0')} ${section.label} ${ts} 满足 ${sOk}/${sTotal}${isAction ? ' 动作' : ''}`
              return (
                <button
                  key={section.id}
                  type="button"
                  role="listitem"
                  className={`section-selector__timeline-point${isActive ? ' section-selector__timeline-point--active' : ''}${isOk ? ' section-selector__timeline-point--ok' : ' section-selector__timeline-point--fail'}`}
                  style={{ left: `${timeline.points[localIndex].left}px` }}
                  title={title}
                  aria-current={isActive ? 'step' : undefined}
                  onClick={() => onSelect(section.id)}
                >
                  {isAction && <span className="section-selector__action-marker" aria-label="动作">动作</span>}
                  <span className="section-selector__timeline-dot" />
                  <span className="section-selector__timeline-index">
                    {String(i + 1).padStart(2, '0')}
                  </span>
                  <span className="section-selector__timeline-time">
                    {formatTimelineTime(section.time)}
                  </span>
                </button>
              )
            })}
            {shouldWindowTimeline && (
              <div className="section-selector__overview" aria-label="断面全局概览">
                <div
                  className="section-selector__overview-track"
                  onPointerDown={handleOverviewPointerDown}
                  onPointerMove={handleOverviewPointerMove}
                >
                  <span
                    className="section-selector__overview-window"
                    style={{ left: `${overviewWindowLeft}%`, width: `${overviewWindowWidth}%` }}
                  />
                  {safeSections.map((section, i) => {
                    const { ok: sOk, total: sTotal, invalid: sInvalid } = satisfiedCount(section.states, inputNodeIds)
                    const isActive = i === currentIndex
                    const isAction = isActionSection(safeSections, i, outputNodeIds)
                    const isInvalid = sInvalid > 0 || sTotal === 0
                    const isOk = sTotal > 0 && sOk === sTotal
                    const left = safeSections.length > 1 ? (i / (safeSections.length - 1)) * 100 : 50
                    return (
                      <button
                        key={`overview-${section.id}`}
                        type="button"
                        className={`section-selector__overview-point${isActive ? ' section-selector__overview-point--active' : ''}${isAction ? ' section-selector__overview-point--action' : ''}${isInvalid ? ' section-selector__overview-point--invalid' : isOk ? ' section-selector__overview-point--ok' : ' section-selector__overview-point--fail'}`}
                        style={{ left: `${left}%` }}
                        title={`选择断面 ${i + 1}${isAction ? ' · 动作' : ''}`}
                        aria-label={`选择断面 ${i + 1}`}
                        onPointerDown={(event) => event.stopPropagation()}
                        onClick={() => selectByIndex(i)}
                      />
                    )
                  })}
                </div>
              </div>
            )}
          </div>
        </div>

        <button
          type="button"
          className="section-selector__nav-btn"
          disabled={!hasNext}
          onClick={handleNext}
          aria-label="下一个断面"
        >
          下一点 ›
        </button>

        {cur && (
          <div className="section-selector__info">
            <div className="section-selector__info-row">
              <span className="section-selector__info-label">时刻</span>
              <span
                className="section-selector__info-value section-selector__info-value--mono"
                title={formatTimestamp(cur.timestamp)}
              >
                {formatTimestamp(cur.timestamp)}
              </span>
            </div>
            <div className="section-selector__info-row">
              <span className="section-selector__info-label">经过</span>
              <span
                className="section-selector__info-value section-selector__info-value--mono"
                title={formatTime(cur.time)}
              >
                {formatTime(cur.time)}
              </span>
            </div>
          </div>
        )}

        <span className="section-selector__page-pill" aria-label="当前断面页码">
          <span className="section-selector__page-row">
            <span className="section-selector__page-label">当前断面</span>
            <span className="section-selector__page-current">
              {currentIndex >= 0 ? currentIndex + 1 : '-'}
            </span>
          </span>
          <span className="section-selector__page-row">
            <span className="section-selector__page-label">断面总数</span>
            <span className="section-selector__page-total">{safeSections.length}</span>
          </span>
        </span>

      </div>

      <span className="section-selector__divider" />

      {/* ── Summary ── */}
      <div className="section-selector__meta">
        <div className="section-selector__satisfy-summary">
          <span className="section-selector__satisfy-row">
            <span className="section-selector__satisfy-label">满足节点数</span>
            <span className="section-selector__satisfy-ok">{ok}</span>
          </span>
          <span className="section-selector__satisfy-row">
            <span className="section-selector__satisfy-label">有效输入节点数</span>
            <span className="section-selector__satisfy-total">{total}</span>
          </span>
          {invalid > 0 && <span className="section-selector__invalid-count">无效节点 {invalid}</span>}
        </div>
      </div>

    </footer>
  )
}
