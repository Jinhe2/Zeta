export interface V4NodeData {
  label: string
  nodeId: string
  displayValue?: string
  threshold?: string
  gateType?: string
  inverted?: boolean
  delayValue?: string | number
  channelRef?: string
  /** 断面状态：true/1 满足，false/0 不满足，-1 无实际数据，null/undefined 未选择断面 */
  sectionSatisfied?: boolean | number | null
  [key: string]: unknown
}
