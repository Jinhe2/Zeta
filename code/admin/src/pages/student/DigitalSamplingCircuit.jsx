import { useMemo } from 'react'
import { Background, BaseEdge, Controls, Handle, MarkerType, Position, ReactFlow, ReactFlowProvider, getStraightPath } from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { DIGITAL_LINK_STYLES } from '../../utils/digitalSampling'

function SamplingDeviceNode({ data }) {
  return <div className={`digital-sampling-node${data.center ? ' digital-sampling-node--center' : ''}`}>
    {!data.center && <Handle type="source" position={Position.Right} />}
    <strong>{data.displayName}</strong><span>{data.iedName}</span>
    {data.center && <Handle type="target" position={Position.Left} />}
  </div>
}

function SamplingStatusEdge({ id, sourceX, sourceY, targetX, targetY, markerEnd, data }) {
  const offset = data.offset || 0
  const [path] = getStraightPath({ sourceX, sourceY: sourceY + offset, targetX, targetY: targetY + offset })
  return <BaseEdge id={id} path={path} markerEnd={markerEnd} label={data.label} labelStyle={{ fill: '#dceafa', fontSize: 11 }} style={{ stroke: DIGITAL_LINK_STYLES[data.state].color, strokeWidth: 3 }} />
}

const nodeTypes = { samplingDevice: SamplingDeviceNode }
const edgeTypes = { samplingStatus: SamplingStatusEdge }

export default function DigitalSamplingCircuit({ topology, associationStates }) {
  const graph = useMemo(() => {
    if (!topology) return { nodes: [], edges: [] }
    const peers = topology.peers || []
    const centerY = Math.max(40, (peers.length - 1) * 80)
    const nodes = [
      ...peers.map((peer, index) => ({
        id: `device:${peer.iedName}`, type: 'samplingDevice', position: { x: 20, y: index * 160 },
        draggable: false, selectable: false, data: peer,
      })),
      {
        id: `device:${topology.center.iedName}`, type: 'samplingDevice', position: { x: 520, y: centerY },
        draggable: false, selectable: false, data: { ...topology.center, center: true },
      },
    ]
    const edgesBySource = new Map()
    for (const edge of topology.edges || []) {
      const list = edgesBySource.get(edge.sourceIedName) || []
      list.push(edge)
      edgesBySource.set(edge.sourceIedName, list)
    }
    const edges = (topology.edges || []).map((edge) => {
      const state = edge.valid ? (associationStates.get(Number(edge.associationId)) || 'gray') : 'gray'
      const siblings = edgesBySource.get(edge.sourceIedName) || [edge]
      const offset = (siblings.indexOf(edge) - (siblings.length - 1) / 2) * 18
      return {
        id: edge.id, source: `device:${edge.sourceIedName}`, target: `device:${edge.targetIedName}`,
        type: 'samplingStatus', markerEnd: { type: MarkerType.ArrowClosed, color: DIGITAL_LINK_STYLES[state].color },
        data: { state, label: edge.controlName || 'SV', offset },
      }
    })
    return { nodes, edges }
  }, [associationStates, topology])

  return <div className="digital-sampling-circuit">
    <div className="digital-sampling-circuit__legend">{Object.entries(DIGITAL_LINK_STYLES).map(([key, style]) => <span key={key}><i style={{ background: style.color }} />{style.label}</span>)}</div>
    <ReactFlowProvider><ReactFlow nodes={graph.nodes} edges={graph.edges} nodeTypes={nodeTypes} edgeTypes={edgeTypes} fitView fitViewOptions={{ padding: 0.25 }} minZoom={0.3} maxZoom={1.6} nodesConnectable={false} elementsSelectable={false}><Background gap={22} size={1} color="#294159" /><Controls showInteractive={false} /></ReactFlow></ReactFlowProvider>
  </div>
}
