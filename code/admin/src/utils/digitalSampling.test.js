import test from 'node:test'
import assert from 'node:assert/strict'
import { digitalLinkState, evaluateDigitalSamplingChannel, indexDigitalSamplingResponse } from './digitalSampling.js'

const readable = (state) => ({ state, read_success: true })

test('数字化链路按断链、异常、正常优先级判定', () => {
  assert.equal(digitalLinkState(true, { disconnect_signals: [readable('ON')], link_abnormal_signals: [readable('ON')] }), 'red')
  assert.equal(digitalLinkState(true, { disconnect_signals: [readable('OFF')], link_abnormal_signals: [readable('ON')] }), 'orange')
  assert.equal(digitalLinkState(true, { disconnect_signals: [readable('OFF')], link_abnormal_signals: [] }), 'green')
  assert.equal(digitalLinkState(false, { disconnect_signals: [readable('OFF')] }), 'gray')
  assert.equal(digitalLinkState(true, { disconnect_signals: [] }), 'gray')
})

test('按关联和通道编号索引 monitord 响应', () => {
  const indexed = indexDigitalSamplingResponse({ associations: [{ association_id: 31, channels: [{ channel_id: 41 }] }] })
  assert.equal(indexed.associations.get(31).association_id, 31)
  assert.equal(indexed.channels.get(41).channel_id, 41)
})

test('链路正常且量值满足容差时通过', () => {
  const channel = { valid: true, category: 'VOLTAGE', baselineMagnitude: 57.735, baselineAngle: 0 }
  assert.equal(evaluateDigitalSamplingChannel(channel, {
    read_success: true,
    realtime: { magnitude: '58.0', angle: '359.0' },
  }, 'green').passed, true)
  assert.equal(evaluateDigitalSamplingChannel(channel, {
    read_success: true,
    realtime: { magnitude: '58.0', angle: '0' },
  }, 'red').passed, false)
})

test('零值阈值按电压和电流分类', () => {
  const live = { read_success: true, realtime: { magnitude: '0.1', angle: '30' } }
  assert.equal(evaluateDigitalSamplingChannel({ valid: true, category: 'VOLTAGE', baselineMagnitude: 0, baselineAngle: 0 }, live, 'green').passed, true)
  assert.equal(evaluateDigitalSamplingChannel({ valid: true, category: 'CURRENT', baselineMagnitude: 0, baselineAngle: 0 }, live, 'green').passed, false)
})
