export function finiteNumber(value) {
  if (value === null || value === undefined || value === '') return null
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

export function angleDifference(actual, expected) {
  return Math.abs(((actual - expected + 180) % 360 + 360) % 360 - 180)
}

export function actualOutputs(rawState) {
  return Array.isArray(rawState?.actual_outputs) ? rawState.actual_outputs : []
}

function sameOutputCode(actual, expected) {
  return String(actual ?? '').trim().toLowerCase() === String(expected ?? '').trim().toLowerCase()
}

export function evaluateSamplingWiring(channel, rawState) {
  if (!rawState) return { passed: false, wiringText: '等待读取', message: `${channel.outputCode}：未返回端子状态` }
  if (rawState.read_success === false || rawState.connection_status === 'ERROR') {
    return { passed: false, wiringText: '读取失败', message: `${channel.outputCode}：端子状态读取失败` }
  }

  const outputs = actualOutputs(rawState)
  if (rawState.connection_status === 'MULTIPLE' || outputs.length > 1) {
    const codes = outputs.map((output) => output?.output_code).filter(Boolean).join('、')
    return { passed: false, wiringText: codes ? `多路接入（${codes}）` : '多路接入', message: `${channel.outputCode}：端子存在多路接入${codes ? `，当前为 ${codes}` : ''}` }
  }
  if (rawState.connection_status !== 'CONNECTED') {
    return { passed: false, wiringText: '未接线', message: `${channel.outputCode}：端子未接线` }
  }
  if (outputs.length !== 1 || !outputs[0]?.output_code) {
    return { passed: false, wiringText: '已接线，输出未知', message: `${channel.outputCode}：已检测到接线，但未返回输出信息` }
  }

  const actualOutput = outputs[0]
  if (!sameOutputCode(actualOutput.output_code, channel.outputCode)) {
    return { passed: false, wiringText: `接线错误（${actualOutput.output_code}）`, actualOutput, message: `${channel.outputCode}：接线不正确，当前实际接入 ${actualOutput.output_code}` }
  }
  return { passed: true, wiringText: '接线正确', actualOutput, message: '' }
}

export function evaluateSamplingChannel(channel, rawState) {
  const wiring = evaluateSamplingWiring(channel, rawState)
  if (!wiring.passed) return wiring
  if (channel.outputCode === 'Un' || channel.outputCode === 'In') return wiring

  const realtime = rawState.realtime
  if (realtime?.type !== 'ANALOG') return { ...wiring, passed: false, message: `${channel.outputCode}：未读取到模拟量实时数据` }
  const magnitude = finiteNumber(realtime.magnitude)
  const angle = finiteNumber(realtime.angle)
  if (magnitude === null || angle === null) return { ...wiring, passed: false, message: `${channel.outputCode}：实时幅值或角度无效` }

  const baselineMagnitude = Number(channel.baselineMagnitude)
  const baselineAngle = Number(channel.baselineAngle)
  const isVoltage = channel.outputCode.startsWith('U')
  const magnitudePassed = baselineMagnitude === 0
    ? Math.abs(magnitude) <= (isVoltage ? 0.5 : 0.05)
    : Math.abs(magnitude - baselineMagnitude) <= Math.abs(baselineMagnitude) * 0.05
  if (!magnitudePassed) {
    return { ...wiring, passed: false, magnitude, angle, message: `${channel.outputCode}：幅值 ${realtime.magnitude}，目标 ${channel.baselineMagnitude}，超出允许范围` }
  }
  if (baselineMagnitude !== 0 && angleDifference(angle, baselineAngle) > 5) {
    return { ...wiring, passed: false, magnitude, angle, message: `${channel.outputCode}：角度 ${realtime.angle}°，目标 ${channel.baselineAngle}°，超出 ±5°` }
  }
  return { ...wiring, passed: true, message: '', magnitude, angle }
}

export function statesByTerminalId(data) {
  return new Map((Array.isArray(data?.terminals) ? data.terminals : []).map((terminal) => [String(terminal.terminal_id), terminal]))
}
