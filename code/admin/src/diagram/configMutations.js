function cloneConfig(config) {
  return JSON.parse(JSON.stringify(config))
}

function nextId(prefix, items) {
  let i = items.length + 1
  let id = `${prefix}_${i}`
  const ids = new Set(items.map((x) => x.id))
  while (ids.has(id)) {
    i += 1
    id = `${prefix}_${i}`
  }
  return id
}

export function addNodeToConfig(config, kind) {
  const next = cloneConfig(config)
  next.inputs = next.inputs ?? []
  next.gates = next.gates ?? []
  next.timers = next.timers ?? []
  next.outputs = next.outputs ?? []
  next.settings = next.settings ?? []

  let item
  switch (kind) {
    case 'input':
      item = { id: nextId('IN', next.inputs), name: '新输入', channelType: 'DIGITAL', unit: '无' }
      next.inputs.push(item)
      break
    case 'gate':
      item = { id: nextId('GATE', next.gates), type: 'AND', inputs: [], inverted: false }
      next.gates.push(item)
      break
    case 'timer':
      item = { id: nextId('TIMER', next.timers), name: '新时间元件', input: '' }
      next.timers.push(item)
      break
    case 'output':
      item = { id: nextId('OUT', next.outputs), name: '新输出', input: '' }
      next.outputs.push(item)
      break
    default:
      return next
  }
  return next
}

export function deleteNodeFromConfig(config, kind, id) {
  const next = cloneConfig(config)
  const removeRefs = (nodeId) => {
    next.gates = (next.gates ?? []).map((gate) => ({
      ...gate,
      inputs: (gate.inputs ?? []).filter((inp) => {
        const ref = typeof inp === 'object' ? inp.node : inp
        return ref !== nodeId
      }),
    }))
    next.timers = (next.timers ?? []).map((t) => (t.input === nodeId ? { ...t, input: '' } : t))
    next.outputs = (next.outputs ?? []).map((o) => (o.input === nodeId ? { ...o, input: '' } : o))
  }

  removeRefs(id)

  switch (kind) {
    case 'input':
      next.inputs = (next.inputs ?? []).filter((x) => x.id !== id)
      break
    case 'gate':
      next.gates = (next.gates ?? []).filter((x) => x.id !== id)
      break
    case 'timer':
      next.timers = (next.timers ?? []).filter((x) => x.id !== id)
      break
    case 'output':
      next.outputs = (next.outputs ?? []).filter((x) => x.id !== id)
      break
    default:
      break
  }
  return next
}

export function updateNodeInConfig(config, kind, id, patch) {
  const next = cloneConfig(config)
  const listKey = kind === 'input' ? 'inputs' : kind === 'gate' ? 'gates' : kind === 'timer' ? 'timers' : 'outputs'
  next[listKey] = (next[listKey] ?? []).map((item) => (item.id === id ? { ...item, ...patch } : item))
  return next
}

export function findNodeMeta(config, id) {
  for (const kind of ['input', 'gate', 'timer', 'output']) {
    const listKey = kind === 'input' ? 'inputs' : kind === 'gate' ? 'gates' : kind === 'timer' ? 'timers' : 'outputs'
    const hit = (config[listKey] ?? []).find((x) => x.id === id)
    if (hit) return { kind, item: hit }
  }
  return null
}

export function listAllNodeIds(config) {
  return [
    ...(config.inputs ?? []).map((x) => x.id),
    ...(config.gates ?? []).map((x) => x.id),
    ...(config.timers ?? []).map((x) => x.id),
    ...(config.outputs ?? []).map((x) => x.id),
  ]
}

export function connectNodesInConfig(config, sourceId, targetId) {
  if (!sourceId || !targetId || sourceId === targetId) {
    return config
  }
  const targetMeta = findNodeMeta(config, targetId)
  if (!targetMeta) return config

  const next = cloneConfig(config)
  switch (targetMeta.kind) {
    case 'gate': {
      next.gates = (next.gates ?? []).map((gate) => {
        if (gate.id !== targetId) return gate
        const inputs = [...(gate.inputs ?? [])]
        const exists = inputs.some((inp) => (typeof inp === 'object' ? inp.node : inp) === sourceId)
        if (exists) return gate
        return { ...gate, inputs: [...inputs, { node: sourceId, inverted: false }] }
      })
      break
    }
    case 'timer':
      next.timers = (next.timers ?? []).map((t) => (t.id === targetId ? { ...t, input: sourceId } : t))
      break
    case 'output':
      next.outputs = (next.outputs ?? []).map((o) => (o.id === targetId ? { ...o, input: sourceId } : o))
      break
    default:
      return config
  }
  return next
}

export function disconnectEdgeInConfig(config, sourceId, targetId) {
  const targetMeta = findNodeMeta(config, targetId)
  if (!targetMeta) return config

  const next = cloneConfig(config)
  switch (targetMeta.kind) {
    case 'gate':
      next.gates = (next.gates ?? []).map((gate) => {
        if (gate.id !== targetId) return gate
        return {
          ...gate,
          inputs: (gate.inputs ?? []).filter((inp) => {
            const ref = typeof inp === 'object' ? inp.node : inp
            return ref !== sourceId
          }),
        }
      })
      break
    case 'timer':
      next.timers = (next.timers ?? []).map((t) =>
        t.id === targetId && t.input === sourceId ? { ...t, input: '' } : t,
      )
      break
    case 'output':
      next.outputs = (next.outputs ?? []).map((o) =>
        o.id === targetId && o.input === sourceId ? { ...o, input: '' } : o,
      )
      break
    default:
      break
  }
  return next
}

function nextSettingId(settings) {
  return nextId('SET', settings)
}

export function addSettingToConfig(config) {
  const next = cloneConfig(config)
  next.settings = next.settings ?? []
  next.settings.push({
    id: nextSettingId(next.settings),
    name: '新定值',
    dataType: 'FLOAT',
    defaultValue: 0,
  })
  return next
}

export function updateSettingInConfig(config, id, patch) {
  const next = cloneConfig(config)
  next.settings = (next.settings ?? []).map((s) => (s.id === id ? { ...s, ...patch } : s))
  return next
}

export function deleteSettingFromConfig(config, id) {
  const next = cloneConfig(config)
  next.settings = (next.settings ?? []).filter((s) => s.id !== id)
  return next
}
