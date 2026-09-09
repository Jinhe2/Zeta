import { angleDifference, finiteNumber } from './samplingValidation.js'

export const DIGITAL_LINK_STYLES = {
  red: { color: '#dc2626', label: '断链' },
  orange: { color: '#f59e0b', label: '异常' },
  gray: { color: '#94a3b8', label: '未知' },
  green: { color: '#22c55e', label: '正常' },
}

function signals(block, camel, snake) {
  return block?.[camel] ?? block?.[snake] ?? []
}

export function digitalLinkState(configValid, controlBlock) {
  if (!configValid || !controlBlock || controlBlock.status === 'UNMATCHED') return 'gray'
  const disconnect = signals(controlBlock, 'disconnectSignals', 'disconnect_signals')
  const abnormal = signals(controlBlock, 'linkAbnormalSignals', 'link_abnormal_signals')
  if (disconnect.length === 0 || disconnect.some((signal) => signal.read_success !== true || !['ON', 'OFF'].includes(signal.state))) return 'gray'
  if (disconnect.some((signal) => signal.state === 'ON')) return 'red'
  if (abnormal.some((signal) => signal.read_success === true && signal.state === 'ON')) return 'orange'
  return 'green'
}

export function indexDigitalSamplingResponse(data) {
  const associations = new Map()
  const channels = new Map()
  for (const association of data?.associations || []) {
    const associationId = Number(association.association_id ?? association.associationId)
    associations.set(associationId, association)
    for (const channel of association.channels || []) {
      channels.set(Number(channel.channel_id ?? channel.channelId), channel)
    }
  }
  return { associations, channels }
}

export function evaluateDigitalSamplingChannel(channel, liveChannel, linkState) {
  if (!channel?.valid) return { passed: false, message: channel?.invalidReason || '配置失效' }
  if (linkState !== 'green') return { passed: false, message: `链路${DIGITAL_LINK_STYLES[linkState]?.label || '未知'}` }
  if (!liveChannel || liveChannel.read_success !== true) return { passed: false, message: '实时量值读取失败' }
  const magnitude = finiteNumber(liveChannel.realtime?.magnitude)
  const angle = finiteNumber(liveChannel.realtime?.angle)
  if (magnitude === null || angle === null) return { passed: false, message: '实时幅值或相角无效' }
  const baselineMagnitude = Number(channel.baselineMagnitude)
  const baselineAngle = Number(channel.baselineAngle)
  const zeroTolerance = channel.category === 'VOLTAGE' ? 0.5 : 0.05
  const magnitudePassed = baselineMagnitude === 0
    ? Math.abs(magnitude) <= zeroTolerance
    : Math.abs(magnitude - baselineMagnitude) <= Math.abs(baselineMagnitude) * 0.05
  if (!magnitudePassed) return { passed: false, magnitude, angle, message: '幅值超出允许范围' }
  if (baselineMagnitude !== 0 && angleDifference(angle, baselineAngle) > 5) {
    return { passed: false, magnitude, angle, message: '相角超出 ±5°' }
  }
  return { passed: true, magnitude, angle, message: '' }
}
