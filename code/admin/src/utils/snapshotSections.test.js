import test from 'node:test'
import assert from 'node:assert/strict'
import {
  formatEffectiveValue,
  formatNodeEffectiveValues,
  mergeSnapshotEffectiveValues,
  parseSnapshotSections,
} from './snapshotSections.js'

test('即时快照转换保留有效值数值、null 和单位', () => {
  const sections = parseSnapshotSections({
    nodes: [{ id: 'analog' }, { id: 'digital' }],
    timestamps: ['2026/10/20 10:00:00.000', '2026/10/20 10:00:00.100'],
    channels: [
      {
        values: [0, 1],
        effectiveValues: [null, 12.3412],
        effectiveValueType: 'fundamental_rms',
        unit: 'A',
      },
      { values: [0, 1] },
    ],
  })

  assert.equal(sections[0].effectiveValues.analog.value, null)
  assert.equal(sections[1].effectiveValues.analog.value, 12.3412)
  assert.equal(sections[1].effectiveValues.analog.unit, 'A')
  assert.deepEqual(sections[0].effectiveValues.digital, undefined)
})

test('有效值最多保留三位小数并正确处理空值、负零和空单位', () => {
  assert.equal(formatEffectiveValue({
    value: 12.3416,
    effectiveValueType: 'fundamental_rms',
    unit: 'A',
  }), '12.342 A')
  assert.equal(formatEffectiveValue({
    value: 12.34,
    effectiveValueType: 'fundamental_rms',
    unit: '',
  }), '12.34')
  assert.equal(formatEffectiveValue({
    value: -0,
    effectiveValueType: 'fundamental_rms',
    unit: '',
  }), '0')
  assert.equal(formatEffectiveValue({
    value: null,
    effectiveValueType: 'fundamental_rms',
    unit: 'V',
  }), '--')
})

test('旧快照不生成有效值展示文本', () => {
  const sections = parseSnapshotSections({
    nodes: [{ id: 'input' }],
    timestamps: ['2026/10/20 10:00:00.000'],
    channels: [{ values: [1] }],
  })

  assert.deepEqual(sections[0].effectiveValues, {})
  assert.equal(formatNodeEffectiveValues(sections[0].effectiveValues), null)
})

test('历史接口缺少有效值时从原始快照合并有效值', () => {
  const apiSections = [{
    id: 'section-0',
    label: 'T = 0.000 s',
    states: { analog: 1 },
  }]
  const snapshotJson = {
    nodes: [{ id: 'analog' }],
    timestamps: ['2026/10/20 10:00:00.000'],
    channels: [{
      values: [1],
      effectiveValues: [12.3456],
      effectiveValueType: 'fundamental_rms',
      unit: 'A',
    }],
  }

  const merged = mergeSnapshotEffectiveValues(apiSections, snapshotJson)

  assert.deepEqual(merged[0].states, { analog: 1 })
  assert.equal(merged[0].effectiveValues.analog.value, 12.3456)
  assert.equal(formatNodeEffectiveValues(merged[0].effectiveValues).analog, '12.346 A')
})
