import { useEffect, useMemo, useState } from 'react'
import { api } from '../api/client'
import './ConfigCopyDialog.css'

const MODULES = [
  ['CABINET_LEARNING', '屏柜学习'],
  ['DRAWING_LEARNING', '图纸学习'],
  ['LOGIC_LEARNING', '逻辑学习'],
  ['SAMPLING_TEST', '采样测试'],
  ['LEARNING_RESOURCE', '学习资料'],
  ['BASELINE_CONFIG', '基准配置'],
  ['LOGIC_GROUP', '组合逻辑'],
]

const DEVICE_MODULES = [
  ['LOGIC_LEARNING', '逻辑学习'],
  ['BASELINE_CONFIG', '基准配置'],
  ['LOGIC_GROUP', '组合逻辑'],
]

const STATUS_LABELS = {
  READY: '校验通过',
  NEEDS_MAPPING: '需要装置映射',
  INCOMPATIBLE: '基础数据不一致',
}

export default function ConfigCopyDialog({ scope, sourceId, sourceName, onClose, onSuccess }) {
  const [tree, setTree] = useState({ cabinets: [] })
  const [selectedTargets, setSelectedTargets] = useState([])
  const [modules, setModules] = useState(
    scope === 'DEVICE' ? DEVICE_MODULES.map(([key]) => key) : MODULES.map(([key]) => key),
  )
  const [mappings, setMappings] = useState({})
  const [report, setReport] = useState(null)
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    api.getKnowledgeTree()
      .then(setTree)
      .catch((err) => setError(err.message || '加载目标列表失败'))
      .finally(() => setLoading(false))
  }, [])

  const targets = useMemo(() => {
    if (scope === 'CABINET') {
      return (tree.cabinets || []).filter((cabinet) => cabinet.id !== sourceId)
        .map((cabinet) => ({ id: cabinet.id, label: `${cabinet.name}（${cabinet.code}）` }))
    }
    return (tree.cabinets || []).flatMap((cabinet) => (cabinet.devices || [])
      .filter((device) => device.id !== sourceId)
      .map((device) => ({
        id: device.id,
        label: `${cabinet.name} / ${device.name}（${device.code}）`,
      })))
  }, [scope, sourceId, tree])

  const invalidate = () => {
    setReport(null)
    setResult(null)
    setError('')
  }

  const toggleTarget = (id) => {
    invalidate()
    setSelectedTargets((current) => current.includes(id)
      ? current.filter((value) => value !== id)
      : [...current, id])
  }

  const toggleModule = (module) => {
    invalidate()
    setModules((current) => current.includes(module)
      ? current.filter((value) => value !== module)
      : [...current, module])
  }

  const payload = () => ({
    scope,
    sourceId,
    modules,
    targets: selectedTargets.map((targetId) => ({
      targetId,
      deviceMappings: Object.entries(mappings[targetId] || {})
        .filter(([, targetDeviceId]) => targetDeviceId)
        .map(([sourceDeviceId, targetDeviceId]) => ({
          sourceDeviceId: Number(sourceDeviceId),
          targetDeviceId: Number(targetDeviceId),
        })),
    })),
  })

  const precheck = async () => {
    if (selectedTargets.length === 0 || modules.length === 0) return
    setSubmitting(true)
    setError('')
    setResult(null)
    try {
      setReport(await api.precheckConfigCopy(payload()))
    } catch (err) {
      setError(err.message || '复制预检失败')
    } finally {
      setSubmitting(false)
    }
  }

  const execute = async () => {
    if (!report?.ready || !window.confirm('将整体覆盖所有目标的已选模块配置，确定继续吗？')) return
    setSubmitting(true)
    setError('')
    try {
      const response = await api.executeConfigCopy(payload())
      setResult(response)
      onSuccess?.(response)
    } catch (err) {
      if (err.status === 409 && err.data?.precheck) setReport(err.data.precheck)
      setError(err.status === 409 ? '基础数据或配置已发生变化，请根据最新结果重新确认。' : (err.message || '复制失败'))
    } finally {
      setSubmitting(false)
    }
  }

  const setMapping = (targetId, sourceDeviceId, targetDeviceId) => {
    setMappings((current) => ({
      ...current,
      [targetId]: { ...(current[targetId] || {}), [sourceDeviceId]: targetDeviceId ? Number(targetDeviceId) : null },
    }))
    setResult(null)
  }

  return <div className="users-page__overlay">
    <div className="users-page__dialog config-copy-dialog" role="dialog" aria-modal="true">
      <h3>{scope === 'CABINET' ? '复制屏柜配置' : '复制装置逻辑配置'}</h3>
      <p className="config-copy-dialog__source">源：{sourceName}</p>
      {error && <div className="users-page__error">{error}</div>}
      {loading ? <p className="users-page__loading">加载目标列表中…</p> : <>
        <section>
          <h4>1. 选择目标（可多选）</h4>
          <div className="config-copy-dialog__choices">
            {targets.length === 0 ? <p className="users-page__empty">暂无可选目标</p> : targets.map((target) =>
              <label className="users-page__checkbox" key={target.id}>
                <input type="checkbox" checked={selectedTargets.includes(target.id)} onChange={() => toggleTarget(target.id)} />
                <span>{target.label}</span>
              </label>)}
          </div>
        </section>
        <section>
          <h4>2. 选择复制内容</h4>
          <div className="config-copy-dialog__modules">
            {(scope === 'CABINET' ? MODULES : DEVICE_MODULES).map(([key, label]) => <label className="users-page__checkbox" key={key}>
              <input type="checkbox" checked={modules.includes(key)} onChange={() => toggleModule(key)} />
              <span>{label}</span>
            </label>)}
          </div>
        </section>
        {report && <section>
          <h4>3. 预检结果</h4>
          {report.targets.map((target) => <div className={`config-copy-dialog__target config-copy-dialog__target--${target.status.toLowerCase()}`} key={target.targetId}>
            <div className="config-copy-dialog__target-head">
              <strong>{target.targetName}</strong><span>{STATUS_LABELS[target.status] || target.status}</span>
            </div>
            <p>源配置：{Object.values(target.sourceCounts || {}).reduce((sum, value) => sum + value, 0)} 项；将覆盖：{Object.values(target.overwriteCounts || {}).reduce((sum, value) => sum + value, 0)} 项</p>
            {target.issues?.length > 0 && <ul>{target.issues.map((issue, index) => <li key={`${issue.code}-${index}`}>{issue.message}</li>)}</ul>}
            {target.deviceMappings?.filter((mapping) => !mapping.targetDeviceId).map((mapping) =>
              <label key={mapping.sourceDeviceId}>映射“{mapping.sourceDeviceName}”
                <select value={mappings[target.targetId]?.[mapping.sourceDeviceId] || ''} onChange={(event) => setMapping(target.targetId, mapping.sourceDeviceId, event.target.value)}>
                  <option value="">请选择同类型目标装置</option>
                  {mapping.candidates.map((candidate) => <option value={candidate.id} key={candidate.id}>{candidate.name}（{candidate.code}）</option>)}
                </select>
              </label>)}
          </div>)}
        </section>}
        {result?.success && <div className="users-page__message">
          配置复制完成：{result.targets.map((target) => `${target.targetName} ${Object.values(target.copiedCounts).reduce((sum, value) => sum + value, 0)} 项`).join('；')}
        </div>}
      </>}
      <div className="users-page__dialog-actions">
        <button type="button" className="users-page__btn users-page__btn--ghost" onClick={onClose}>{result ? '关闭' : '取消'}</button>
        {!result && <button type="button" className="users-page__btn" disabled={submitting || selectedTargets.length === 0 || modules.length === 0} onClick={precheck}>
          {submitting ? '处理中…' : report ? '重新预检' : '开始预检'}
        </button>}
        {!result && <button type="button" className="users-page__btn users-page__btn--primary" disabled={submitting || !report?.ready} onClick={execute}>确认覆盖并复制</button>}
      </div>
    </div>
  </div>
}
