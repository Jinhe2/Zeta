const createImage = (url) =>
  new Promise((resolve, reject) => {
    const image = new Image()
    image.addEventListener('load', () => resolve(image))
    image.addEventListener('error', (error) => reject(error))
    image.setAttribute('crossOrigin', 'anonymous')
    image.src = url
  })

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
 * @param {number} maxOutputSize 最长边上限（像素）
 */
export async function getCroppedImageBlob(
  imageSrc,
  pixelCrop,
  rotation = 0,
  flip = { horizontal: false, vertical: false },
  maxOutputSize = 1920,
) {
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
        if (blob) {
          resolve(blob)
        } else {
          reject(new Error('导出图片失败'))
        }
      },
      'image/jpeg',
      0.92,
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

export function isRasterImageFile(file) {
  return file.type.startsWith('image/') && !isSvgFile(file)
}
