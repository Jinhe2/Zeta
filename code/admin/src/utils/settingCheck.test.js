import test from 'node:test'
import assert from 'node:assert/strict'
import { buildSettingMismatchDialog, canStartAfterSettingCheck } from './settingCheck.js'

test('清单一致或明确跳过时允许启动实验', () => {
  assert.equal(canStartAfterSettingCheck({ status: 'MATCHED' }), true)
  assert.equal(canStartAfterSettingCheck({ status: 'SKIPPED' }), true)
  assert.equal(canStartAfterSettingCheck({ status: 'MISMATCH' }), false)
})

test('不一致弹窗只展示失败项目并标记未召唤项', () => {
  const dialog = buildSettingMismatchDialog({
    items: [
      { settingRef: 'a', settingName: '正确项', equal: true, matched: true, baselineValue: '1', actualValue: '1' },
      { settingRef: 'b', settingName: '缺失项', equal: false, matched: false, baselineValue: '2' },
    ],
  })
  assert.equal(dialog.baselineItems.length, 1)
  assert.equal(dialog.baselineItems[0].actualValue, '未召唤到')
})
