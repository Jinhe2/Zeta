export interface V4NodeData {
  label: string
  nodeId: string
  displayValue?: string
  threshold?: string
  gateType?: string
  inverted?: boolean
  delayValue?: string | number
  channelRef?: string
  /** 断面状态：true 满足，false 不满足，null/undefined 未选择断面 */
  sectionSatisfied?: boolean | null
  [key: string]: unknown
}
