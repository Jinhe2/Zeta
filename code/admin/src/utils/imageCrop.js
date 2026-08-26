const TO_RADIANS = Math.PI / 180
const DEFAULT_OUTPUT_MAX_SIZE = 1600
const DEFAULT_JPEG_QUALITY = 0.86

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.addEventListener('load', () => resolve(image))
    image.addEventListener('error', (err) => reject(err))
    image.setAttribute('crossOrigin', 'anonymous')
    image.src = src
  })
}

function rotateSize(width, height, rotation) {
  const rotRad = rotation * TO_RADIANS
  return {
    width: Math.abs(Math.cos(rotRad) * width) + Math.abs(Math.sin(rotRad) * height),
    height: Math.abs(Math.sin(rotRad) * width) + Math.abs(Math.cos(rotRad) * height),
  }
}

/**
 * 根据 react-easy-crop 的 crop 区域导出 Blob。
 */
export async function getCroppedImageBlob(
  imageSrc,
  pixelCrop,
  rotation = 0,
  outputType = 'image/jpeg',
  quality = DEFAULT_JPEG_QUALITY,
  maxOutputSize = DEFAULT_OUTPUT_MAX_SIZE,
) {
  const image = await loadImage(imageSrc)
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    throw new Error('无法创建画布')
  }

  const rotRad = rotation * TO_RADIANS
  const { width: bBoxWidth, height: bBoxHeight } = rotateSize(image.width, image.height, rotation)

  canvas.width = bBoxWidth
  canvas.height = bBoxHeight

  ctx.translate(bBoxWidth / 2, bBoxHeight / 2)
  ctx.rotate(rotRad)
  ctx.translate(-image.width / 2, -image.height / 2)
  ctx.drawImage(image, 0, 0)

  const croppedCanvas = document.createElement('canvas')
  const croppedCtx = croppedCanvas.getContext('2d')
  if (!croppedCtx) {
    throw new Error('无法创建画布')
  }

  let outW = pixelCrop.width
  let outH = pixelCrop.height
  const longest = Math.max(outW, outH)
  if (longest > maxOutputSize) {
    const scale = maxOutputSize / longest
    outW = Math.round(outW * scale)
    outH = Math.round(outH * scale)
  }

  croppedCanvas.width = outW
  croppedCanvas.height = outH

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
        if (!blob) {
          reject(new Error('图片导出失败'))
          return
        }
        resolve(blob)
      },
      outputType,
      quality,
    )
  })
}

export function isSvgFile(file) {
  if (!file) return false
  const type = file.type?.toLowerCase() || ''
  const name = file.name?.toLowerCase() || ''
  return type.includes('svg') || name.endsWith('.svg')
}

export function pickOutputType(file) {
  if (file?.type === 'image/png') {
    return { mime: 'image/png', ext: 'png', quality: undefined }
  }
  if (file?.type === 'image/webp') {
    return { mime: 'image/webp', ext: 'webp', quality: DEFAULT_JPEG_QUALITY }
  }
  return { mime: 'image/jpeg', ext: 'jpg', quality: DEFAULT_JPEG_QUALITY }
}

export function blobToFile(blob, baseName, ext) {
  const safeBase = baseName.replace(/\.[^.]+$/, '') || 'cabinet-image'
  return new File([blob], `${safeBase}-cropped.${ext}`, { type: blob.type })
}
