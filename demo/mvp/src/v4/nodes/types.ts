export interface V4NodeData {
  label: string
  nodeId: string
  displayValue?: string
  threshold?: string
  gateType?: string
  inverted?: boolean
  delayValue?: string | number
  channelRef?: string
  [key: string]: unknown
}
