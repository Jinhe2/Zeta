const createImage = (url) =>
  new Promise((resolve, reject) => {
    const image = new Image()
    image.addEventListener('load', () => resolve(image))
    image.addEventListener('error', (error) => reject(error))
    image.setAttribute('crossOrigin', 'anonymous')
    image.src = url
  })

// 保留认知图片中的细线和小字。仅对超过浏览器安全画布尺寸的极端图片降采样。
export const DEFAULT_IMAGE_OUTPUT_MAX_SIZE = 16384
export const DEFAULT_IMAGE_OUTPUT_QUALITY = 0.98

const LOSSLESS_IMAGE_TYPES = new Set(['image/png'])
const EDITABLE_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])

function getRadianAngle(degreeValue) {
  return (degreeValue * Math.PI) / 180
}

function rotateSize(width, height, rotation) {
  const rotRad = getRadianAngle(rotation)
  return {
    width: Math.abs(Math.cos(rotRad) * width) + Math.abs(Math.sin(rotRad) * height),
    height: Math.abs(Math.sin(rotRad) * width) + Math.abs(Math.cos(rotRad) * height),
  }
}

/**
 * 将裁切区域导出为 Blob（支持旋转、水平/垂直翻转）。
 * @param {string} imageSrc data URL 或同源 URL
 * @param {{ x: number, y: number, width: number, height: number }} pixelCrop
 * @param {number} rotation 角度
 * @param {{ horizontal?: boolean, vertical?: boolean }} flip
 * @param {{ outputType?: string, maxOutputSize?: number, quality?: number }} output 输出配置
 */
export async function getCroppedImageBlob(
  imageSrc,
  pixelCrop,
  rotation = 0,
  flip = { horizontal: false, vertical: false },
  output = {},
) {
  const {
    outputType = 'image/jpeg',
    maxOutputSize = DEFAULT_IMAGE_OUTPUT_MAX_SIZE,
    quality = DEFAULT_IMAGE_OUTPUT_QUALITY,
  } = output
  const image = await createImage(imageSrc)
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    throw new Error('无法创建画布')
  }

  const rotRad = getRadianAngle(rotation)
  const { width: bBoxWidth, height: bBoxHeight } = rotateSize(image.width, image.height, rotation)

  canvas.width = bBoxWidth
  canvas.height = bBoxHeight

  ctx.translate(bBoxWidth / 2, bBoxHeight / 2)
  ctx.rotate(rotRad)
  ctx.scale(flip.horizontal ? -1 : 1, flip.vertical ? -1 : 1)
  ctx.translate(-image.width / 2, -image.height / 2)
  ctx.drawImage(image, 0, 0)

  const croppedCanvas = document.createElement('canvas')
  const croppedCtx = croppedCanvas.getContext('2d')
  if (!croppedCtx) {
    throw new Error('无法创建裁切画布')
  }

  const { width: outW, height: outH } = resolveImageOutputSize(
    pixelCrop.width,
    pixelCrop.height,
    maxOutputSize,
  )

  croppedCanvas.width = outW
  croppedCanvas.height = outH
  croppedCtx.imageSmoothingEnabled = true
  croppedCtx.imageSmoothingQuality = 'high'

  croppedCtx.drawImage(
    canvas,
    pixelCrop.x,
    pixelCrop.y,
    pixelCrop.width,
    pixelCrop.height,
    0,
    0,
    outW,
    outH,
  )

  return new Promise((resolve, reject) => {
    croppedCanvas.toBlob(
      (blob) => {
        if (blob) {
          resolve(blob)
        } else {
          reject(new Error('导出图片失败'))
        }
      },
      outputType,
      LOSSLESS_IMAGE_TYPES.has(outputType) ? undefined : quality,
    )
  })
}

export async function loadImageSourceFromUrl(url) {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error('无法加载图片')
  }
  const blob = await response.blob()
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = () => reject(new Error('读取图片失败'))
    reader.readAsDataURL(blob)
  })
}

export function isSvgFile(file) {
  return file.type === 'image/svg+xml' || /\.svg$/i.test(file.name)
}

export function isGifFile(file) {
  return file.type === 'image/gif' || /\.gif$/i.test(file.name)
}

export function isRasterImageFile(file) {
  return file.type.startsWith('image/') && !isSvgFile(file)
}

export function resolveImageOutputType(type = '') {
  const normalized = String(type).toLowerCase()
  return EDITABLE_IMAGE_TYPES.has(normalized) ? normalized : 'image/png'
}

export function extensionForImageType(type) {
  if (type === 'image/png') return 'png'
  if (type === 'image/webp') return 'webp'
  return 'jpg'
}

export function resolveImageOutputSize(width, height, maxOutputSize = DEFAULT_IMAGE_OUTPUT_MAX_SIZE) {
  const longest = Math.max(width, height)
  if (!Number.isFinite(maxOutputSize) || maxOutputSize <= 0 || longest <= maxOutputSize) {
    return { width: Math.round(width), height: Math.round(height) }
  }
  const scale = maxOutputSize / longest
  return {
    width: Math.round(width * scale),
    height: Math.round(height * scale),
  }
}
