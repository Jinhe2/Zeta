/**
 * 计算 object-fit: contain 下图片在容器内的实际显示区域。
 */
export const DEFAULT_REGION = {
  leftPercent: 30,
  topPercent: 25,
  widthPercent: 20,
  heightPercent: 15,
}

export function getContainedImageRect(containerWidth, containerHeight, naturalWidth, naturalHeight) {
  if (!containerWidth || !containerHeight || !naturalWidth || !naturalHeight) {
    return { left: 0, top: 0, width: containerWidth, height: containerHeight }
  }

  const containerAspect = containerWidth / containerHeight
  const imageAspect = naturalWidth / naturalHeight

  if (imageAspect > containerAspect) {
    const width = containerWidth
    const height = containerWidth / imageAspect
    return { left: 0, top: (containerHeight - height) / 2, width, height }
  }

  const height = containerHeight
  const width = containerHeight * imageAspect
  return { left: (containerWidth - width) / 2, top: 0, width, height }
}

function safeNumber(value, fallback = 0) {
  const num = Number(value)
  return Number.isFinite(num) ? num : fallback
}

/** 像素区域 → 相对图片的百分比（0–100） */
export function pixelRectToPercent(rect, imageRect) {
  if (!imageRect.width || !imageRect.height) {
    return { ...DEFAULT_REGION }
  }
  return {
    leftPercent: ((rect.left - imageRect.left) / imageRect.width) * 100,
    topPercent: ((rect.top - imageRect.top) / imageRect.height) * 100,
    widthPercent: (rect.width / imageRect.width) * 100,
    heightPercent: (rect.height / imageRect.height) * 100,
  }
}

/** 百分比 → 容器内像素区域（相对容器左上角） */
export function percentToPixelRect(region, imageRect) {
  return {
    left: imageRect.left + (region.leftPercent / 100) * imageRect.width,
    top: imageRect.top + (region.topPercent / 100) * imageRect.height,
    width: (region.widthPercent / 100) * imageRect.width,
    height: (region.heightPercent / 100) * imageRect.height,
  }
}

export function clampRegion(region) {
  const x = Math.max(0, Math.min(100, safeNumber(region.leftPercent, DEFAULT_REGION.leftPercent)))
  const y = Math.max(0, Math.min(100, safeNumber(region.topPercent, DEFAULT_REGION.topPercent)))
  const maxW = 100 - x
  const maxH = 100 - y
  const width = Math.max(1, Math.min(maxW, safeNumber(region.widthPercent, DEFAULT_REGION.widthPercent)))
  const height = Math.max(1, Math.min(maxH, safeNumber(region.heightPercent, DEFAULT_REGION.heightPercent)))
  return { leftPercent: x, topPercent: y, widthPercent: width, heightPercent: height }
}

export function buildRegionPayload(region, cabinetDisplayItemId) {
  const normalized = clampRegion(region ?? DEFAULT_REGION)
  const left = roundPercent(normalized.leftPercent)
  const top = roundPercent(normalized.topPercent)
  const width = roundPercent(normalized.widthPercent)
  const height = roundPercent(normalized.heightPercent)
  return {
    cabinetDisplayItemId: Number(cabinetDisplayItemId),
    leftPercent: left,
    topPercent: top,
    widthPercent: width,
    heightPercent: height,
    // 兼容旧版后端字段名
    xPercent: left,
    yPercent: top,
  }
}

function roundPercent(value) {
  return Math.round(safeNumber(value, 0) * 1000) / 1000
}

/** 兼容旧字段名 xPercent / yPercent */
export function normalizeRegion(region) {
  if (!region) return { ...DEFAULT_REGION }
  return clampRegion({
    leftPercent: region.leftPercent ?? region.xPercent,
    topPercent: region.topPercent ?? region.yPercent,
    widthPercent: region.widthPercent,
    heightPercent: region.heightPercent,
  })
}
