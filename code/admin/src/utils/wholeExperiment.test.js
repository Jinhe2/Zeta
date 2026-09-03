import test from 'node:test'
import assert from 'node:assert/strict'
import { updateWholeSelection, validWholeSelection, wholeCandidates, wholeExperimentApi } from './wholeExperiment.js'

const logics = [
  { id: 1, wholeExperimentSequence: 1 },
  { id: 2, wholeExperimentSequence: 2 },
  { id: 3, wholeExperimentSequence: 3 },
  { id: 4, wholeExperimentSequence: 1 },
]

test('序列候选只能来自对应位置', () => {
  assert.deepEqual(wholeCandidates(logics, 1).map((item) => item.id), [1, 4])
  assert.deepEqual(wholeCandidates(logics, 2).map((item) => item.id), [2])
})

test('只允许从序列一开始连续选择两个或三个', () => {
  assert.equal(validWholeSelection(['1', '2', ''], logics), true)
  assert.equal(validWholeSelection(['1', '2', '3'], logics), true)
  for (const selection of [['1', '', ''], ['', '2', '3'], ['1', '', '3'], ['2', '1', ''], ['1', '1', '']]) {
    assert.equal(validWholeSelection(selection, logics), false)
  }
})

test('清空前项同时清空后项且不修改原数组', () => {
  const original = ['1', '2', '3']
  assert.deepEqual(updateWholeSelection(original, 1, ''), ['1', '', ''])
  assert.deepEqual(updateWholeSelection(original, 0, ''), ['', '', ''])
  assert.deepEqual(original, ['1', '2', '3'])
})

test('共享页面适配器使用整组接口且保留有序数据', async () => {
  const calls = []
  const client = {
    getWholeExperimentGuide: (id) => calls.push(['引导', id]),
    wholeExperimentMonitor: (...args) => calls.push(args),
    listWholeExperimentRuns: (id) => calls.push(['记录', id]),
  }
  const adapter = wholeExperimentApi(client, 7)
  adapter.listKnowledgeExperimentGuides('LOGIC_GROUP', 999)
  adapter.startLogicGroupMonitor()
  adapter.sendLogicGroupMonitorHeartbeat('任务')
  adapter.endLogicGroupMonitor('任务')
  adapter.listLogicGroupSnapshots()
  assert.deepEqual(calls, [['引导', 7], [7, 'start'], [7, 'heartbeat', '任务'], [7, 'end', '任务'], ['记录', 7]])
})
