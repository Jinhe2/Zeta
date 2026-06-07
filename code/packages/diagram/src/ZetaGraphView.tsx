import { useMemo } from 'react'
import { adaptZetaConfig, type ZetaConfig } from './adaptZetaConfig'
import GraphView, { type GraphViewProps } from './components/GraphView'

export interface ZetaGraphViewProps
  extends Omit<GraphViewProps, 'data'> {
  config: ZetaConfig | Record<string, unknown>
}

export default function ZetaGraphView({ config, className, ...rest }: ZetaGraphViewProps) {
  const data = useMemo(() => adaptZetaConfig(config as ZetaConfig), [config])

  if (data.nodes.length === 0) {
    return null
  }

  return <GraphView data={data} className={className} {...rest} />
}
