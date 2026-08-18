import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildExperimentPrecheckMismatchDialog,
  buildExperimentStartConfirmDialog,
  buildSettingMismatchDialog,
  canStartAfterExperimentPrecheck,
  canStartAfterSettingCheck,
} from './settingCheck.js'

test('清单一致或明确跳过时允许启动实验', () => {
  assert.equal(canStartAfterSettingCheck({ status: 'MATCHED' }), true)
  assert.equal(canStartAfterSettingCheck({ status: 'SKIPPED' }), true)
  assert.equal(canStartAfterSettingCheck({ status: 'MISMATCH' }), false)
})

test('联合预检仅在匹配或全部跳过时允许进入人工确认', () => {
  assert.equal(canStartAfterExperimentPrecheck({ status: 'MATCHED' }), true)
  assert.equal(canStartAfterExperimentPrecheck({ status: 'SKIPPED' }), true)
  assert.equal(canStartAfterExperimentPrecheck({ status: 'MISMATCH' }), false)
})

test('联合预检失败弹窗分别展示定值和软压板失败项', () => {
  const dialog = buildExperimentPrecheckMismatchDialog({
    settingCheck: { items: [
      { settingRef: 's1', settingName: '启动值', equal: false, matched: true, baselineValue: '1', actualValue: '2' },
    ] },
    softPressboardCheck: { items: [
      { pressboardRef: 'p1', pressboardName: '差动保护', equal: true, matched: true, baselineValue: '投入', actualValue: '投入' },
      { pressboardRef: 'p2', pressboardName: '距离保护', equal: false, matched: false, baselineValue: '退出' },
    ] },
  })
  assert.equal(dialog.baselineItems.length, 1)
  assert.equal(dialog.softPressboardItems.length, 1)
  assert.equal(dialog.softPressboardItems[0].actualValue, '未召唤到')
})

test('校验成功后生成加量回路确认提示', () => {
  const dialog = buildExperimentStartConfirmDialog()
  assert.equal(dialog.kind, 'start-confirm')
  assert.match(dialog.message, /加量回路连接正常/)
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

test('联合预检失败弹窗展示硬压板失败项', () => {
  const dialog = buildExperimentPrecheckMismatchDialog({
    settingCheck: { items: [] },
    softPressboardCheck: { items: [] },
    hardPressboardCheck: { items: [
      { pressboardRef: 'h1', pressboardName: '保护跳闸出口', equal: true, matched: true, baselineValue: '投入', actualValue: '投入' },
      { pressboardRef: 'h2', pressboardName: '重合闸功能', equal: false, matched: false, baselineValue: '退出' },
    ] },
  })
  assert.equal(dialog.hardPressboardItems.length, 1)
  assert.equal(dialog.hardPressboardItems[0].name, '重合闸功能')
  assert.equal(dialog.hardPressboardItems[0].actualValue, '未召唤到')
})
