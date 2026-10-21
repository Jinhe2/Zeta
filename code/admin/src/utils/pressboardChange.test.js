import assert from 'node:assert/strict'
import test from 'node:test'
import {
  formatPressboardChangeState,
  newestPressboardChangeId,
} from './pressboardChange.js'

test('压板状态映射为分合文案', () => {
  assert.equal(formatPressboardChangeState('ON'), '合')
  assert.equal(formatPressboardChangeState('off'), '分')
  assert.equal(formatPressboardChangeState('UNKNOWN'), '—')
})

test('取记录中的最大 ID 作为最新基线', () => {
  assert.equal(newestPressboardChangeId([{ id: 7 }, { id: 12 }, { id: 9 }]), 12)
  assert.equal(newestPressboardChangeId([]), 0)
})
