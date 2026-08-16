import test from 'node:test'
import assert from 'node:assert/strict'
import { angleDifference, evaluateSamplingChannel, statesByTerminalId } from './samplingValidation.js'

const ua = { outputCode: 'Ua', baselineMagnitude: 220, baselineAngle: 0 }

test('evaluates connection_status and actual_outputs from completed response', () => {
  const states = statesByTerminalId({ terminals: [{
    terminal_id: 102,
    connection_status: 'CONNECTED',
    actual_outputs: [{ output_code: 'Ua', phase: 'A' }],
    realtime: { type: 'ANALOG', magnitude: '220.5', angle: '1.0' },
    read_success: true,
  }] })
  assert.equal(evaluateSamplingChannel(ua, states.get('102')).passed, true)
})

test('uses circular angle distance across zero degrees', () => {
  assert.equal(angleDifference(359, 1), 2)
  assert.equal(evaluateSamplingChannel(
    { ...ua, baselineAngle: 1 },
    { connection_status: 'CONNECTED', actual_outputs: [{ output_code: 'Ua' }], realtime: { type: 'ANALOG', magnitude: '220', angle: '359' }, read_success: true },
  ).passed, true)
})

test('Un only evaluates wiring', () => {
  assert.equal(evaluateSamplingChannel(
    { outputCode: 'Un', baselineMagnitude: null, baselineAngle: null },
    { connection_status: 'CONNECTED', actual_outputs: [{ output_code: 'Un' }], read_success: true },
  ).passed, true)
})

test('In also only evaluates common-end wiring', () => {
  assert.equal(evaluateSamplingChannel(
    { outputCode: 'In', baselineMagnitude: null, baselineAngle: null },
    { connection_status: 'CONNECTED', actual_outputs: [{ output_code: 'In' }], read_success: true },
  ).passed, true)
})

test('reports wrong and multiple outputs', () => {
  assert.match(evaluateSamplingChannel(ua, {
    connection_status: 'CONNECTED', actual_outputs: [{ output_code: 'Ub' }], read_success: true,
  }).message, /当前实际接入 Ub/)
  assert.match(evaluateSamplingChannel(ua, {
    connection_status: 'MULTIPLE', actual_outputs: [{ output_code: 'Ua' }, { output_code: 'Ub' }], read_success: true,
  }).message, /多路接入/)
})

test('evaluates zero-current threshold after wiring matches', () => {
  const current = { outputCode: 'Ia', baselineMagnitude: 0, baselineAngle: 120 }
  assert.equal(evaluateSamplingChannel(current, {
    connection_status: 'CONNECTED', actual_outputs: [{ output_code: 'Ia' }], realtime: { type: 'ANALOG', magnitude: '0.05', angle: '999' }, read_success: true,
  }).passed, true)
  assert.equal(evaluateSamplingChannel(current, {
    connection_status: 'CONNECTED', actual_outputs: [{ output_code: 'Ia' }], realtime: { type: 'ANALOG', magnitude: '0.051', angle: '0' }, read_success: true,
  }).passed, false)
})
