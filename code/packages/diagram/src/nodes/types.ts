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
  /** 当前断面的有效值文本；仅输入节点使用。 */
  effectiveValueText?: string | null
  [key: string]: unknown
}
