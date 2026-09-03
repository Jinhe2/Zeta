/** 固定序列位；清空前项时同步清空其后各项。 */
export function updateWholeSelection(selection, index, value) {
  const next = [...selection]
  next[index] = value
  if (!value) next.fill('', index + 1)
  return next
}

export function wholeCandidates(logics, sequence) {
  return logics.filter((logic) => Number(logic.wholeExperimentSequence ?? 1) === sequence)
}

export function validWholeSelection(selection, logics) {
  return Boolean(selection[0] && selection[1])
    && selection.every((id, index) => !id || wholeCandidates(logics, index + 1)
      .some((logic) => String(logic.id) === String(id)))
    && new Set(selection.filter(Boolean)).size === selection.filter(Boolean).length
}

/** 复用组合实验页面，只替换整组实验的数据来源。 */
export function wholeExperimentApi(api, id) {
  return {
    ...api,
    getKnowledgeLogicGroup: () => api.getWholeExperiment(id),
    listLogicGroupSnapshots: () => api.listWholeExperimentRuns(id),
    listLogicGroupSnapshotMembers: (taskUuid) => api.listWholeExperimentRunMembers(taskUuid),
    listKnowledgeExperimentGuides: () => api.getWholeExperimentGuide(id),
    checkExperimentPreconditionsForGroup: () => api.checkWholeExperiment(id),
    startLogicGroupMonitor: () => api.wholeExperimentMonitor(id, 'start'),
    sendLogicGroupMonitorHeartbeat: (taskUuid) => api.wholeExperimentMonitor(id, 'heartbeat', taskUuid),
    endLogicGroupMonitor: (taskUuid) => api.wholeExperimentMonitor(id, 'end', taskUuid),
    abortLogicGroupMonitor: (taskUuid) => api.wholeExperimentMonitor(id, 'abort', taskUuid),
    getMonitorTaskResult: (taskUuid) => api.getWholeExperimentRun(taskUuid),
  }
}
