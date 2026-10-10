import test from 'node:test'
import assert from 'node:assert/strict'
import {
  DEFAULT_IMAGE_OUTPUT_MAX_SIZE,
  extensionForImageType,
  resolveImageOutputSize,
  resolveImageOutputType,
} from './cropImage.js'

test('普通高清图片保持原始裁切分辨率', () => {
  assert.deepEqual(resolveImageOutputSize(6240, 4160), { width: 6240, height: 4160 })
})

test('仅极端尺寸图片按比例限制到浏览器安全画布范围', () => {
  assert.deepEqual(resolveImageOutputSize(20000, 10000), {
    width: DEFAULT_IMAGE_OUTPUT_MAX_SIZE,
    height: DEFAULT_IMAGE_OUTPUT_MAX_SIZE / 2,
  })
})

test('编辑后保持 PNG 和 WebP 格式，未知光栅格式使用无损 PNG', () => {
  assert.equal(resolveImageOutputType('image/jpeg'), 'image/jpeg')
  assert.equal(resolveImageOutputType('image/png'), 'image/png')
  assert.equal(resolveImageOutputType('image/webp'), 'image/webp')
  assert.equal(resolveImageOutputType('image/bmp'), 'image/png')
  assert.equal(extensionForImageType('image/png'), 'png')
  assert.equal(extensionForImageType('image/webp'), 'webp')
  assert.equal(extensionForImageType('image/jpeg'), 'jpg')
})
