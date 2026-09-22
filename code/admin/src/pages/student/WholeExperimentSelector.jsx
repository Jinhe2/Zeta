import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import { updateWholeSelection, validWholeSelection, wholeCandidates } from '../../utils/wholeExperiment'
import './WholeExperimentSelector.css'

export default function WholeExperimentSelector({ deviceId, logics, navigationState }) {
  const navigate = useNavigate()
  const [selection, setSelection] = useState(['', '', ''])
  const [recent, setRecent] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const loadRecent = useCallback(() => {
    let cancelled = false
    setLoading(true)
    setError('')
    api.listRecentWholeExperiments(deviceId)
      .then((items) => { if (!cancelled) setRecent(items) })
      .catch((err) => { if (!cancelled) setError(err.message) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [deviceId])

  useEffect(() => loadRecent(), [loadRecent])

  const open = (id) => navigate(`/student/modes/panorama/whole-experiments/${id}`, {
    state: { ...navigationState, mode: 'whole' },
  })

  const selectedLogic = (index) => logics.find((logic) => String(logic.id) === String(selection[index]))

  const chooseCandidate = (logic) => {
    if (loading || saving) return
    const index = Number(logic.wholeExperimentSequence ?? 1) - 1
    if (index < 0 || index > 2) return
    if (selection[index] && String(selection[index]) === String(logic.id)) {
      setSelection(updateWholeSelection(selection, index, ''))
      setError('')
      return
    }
    if (index > 0 && !selection[index - 1]) {
      setError(`请先选择序列 ${index}`)
      return
    }
    setSelection(updateWholeSelection(selection, index, String(logic.id)))
    setError('')
  }

  const removeSelection = (index) => {
    if (loading || saving) return
    setSelection(updateWholeSelection(selection, index, ''))
    setError('')
  }

  const existingExperiment = useMemo(() => {
    const selectedIds = selection.filter(Boolean).map(Number)
    if (selectedIds.length < 2) return null
    return recent.find((item) => {
      const memberIds = (item.members ?? []).map((member) => Number(member.logicDiagramId))
      return memberIds.length === selectedIds.length
        && memberIds.every((id, index) => id === selectedIds[index])
    }) ?? null
  }, [recent, selection])

  const confirm = async () => {
    if (loading || saving || !validWholeSelection(selection, logics)) return
    if (existingExperiment?.valid) {
      open(existingExperiment.id)
      return
    }
    setSaving(true)
    setError('')
    try {
      const detail = await api.createWholeExperiment(deviceId, selection.filter(Boolean).map(Number))
      open(detail.id)
    } catch (err) {
      setError(err.message || '保存整组实验失败')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="whole-experiment-selector">
      <h2>选择整组实验</h2>
      <p>依次选择序列 1、2，可再选择序列 3。确认后查看实验引导并开始实验。</p>
      <div className="whole-experiment-selector__layout">
        <div className="whole-experiment-selector__candidates">
          <h3>基础逻辑备选</h3>
          <p className="whole-experiment-selector__hint">点击基础逻辑，将它加入对应序列；再次点击已选项可移除。</p>
          {[1, 2, 3].map((sequence) => {
            const candidates = wholeCandidates(logics, sequence)
            const blocked = sequence > 1 && !selection[sequence - 2]
            return (
              <div key={sequence} className={`whole-experiment-selector__candidate-group${blocked ? ' is-blocked' : ''}`}>
                <div className="whole-experiment-selector__group-title">
                  <span>序列 {sequence}</span>
                  <small>{sequence === 3 ? '可选' : '必选'}</small>
                </div>
                {candidates.length === 0 ? (
                  <p className="whole-experiment-selector__empty">暂无可用逻辑，请联系管理员配置。</p>
                ) : (
                  <div className="whole-experiment-selector__candidate-list">
                    {candidates.map((logic) => {
                      const selected = String(selection[sequence - 1]) === String(logic.id)
                      return (
                        <button
                          key={logic.id}
                          type="button"
                          className={`whole-experiment-selector__candidate${selected ? ' is-selected' : ''}`}
                          disabled={loading || saving || blocked}
                          aria-pressed={selected}
                          onClick={() => chooseCandidate(logic)}
                        >
                          <strong>{logic.title}</strong>
                          <span>{logic.code || logic.description || '基础逻辑'}</span>
                        </button>
                      )
                    })}
                  </div>
                )}
              </div>
            )
          })}
        </div>

        <div className="whole-experiment-selector__assembly">
          <div className="whole-experiment-selector__assembly-header">
            <div>
              <h3>整组实验组装</h3>
              <p className="whole-experiment-selector__hint">序列 1、2 必须完成，序列 3 可选。</p>
              {existingExperiment && (
                <p className={`whole-experiment-selector__existing${existingExperiment.valid ? '' : ' is-invalid'}`}>
                  {existingExperiment.valid
                    ? '该组合已存在，确认后将直接进入原组合。'
                    : `该组合已存在但当前不可用：${existingExperiment.invalidReason || '组合配置已变化'}`}
                </p>
              )}
            </div>
            <button type="button" className="whole-experiment-selector__confirm" onClick={confirm}
              disabled={loading || saving || !validWholeSelection(selection, logics)}>
              {saving ? '正在保存…' : existingExperiment?.valid ? '进入已有组合' : '确认组合'}
            </button>
          </div>
          <div className="whole-experiment-selector__slots">
            {[1, 2, 3].map((sequence, index) => {
              const logic = selectedLogic(index)
              return (
                <div key={sequence} className={`whole-experiment-selector__slot${logic ? ' is-filled' : ''}`}>
                  <span className="whole-experiment-selector__slot-index">{sequence}</span>
                  <div className="whole-experiment-selector__slot-content">
                    <strong>序列 {sequence}{sequence === 3 ? '（可选）' : ''}</strong>
                    <span>{logic?.title || '等待选择基础逻辑'}</span>
                    {logic && <small>{logic.code || logic.description || '基础逻辑'}</small>}
                  </div>
                  {logic && (
                    <button type="button" className="whole-experiment-selector__remove" disabled={loading || saving}
                      onClick={() => removeSelection(index)}>
                      移除
                    </button>
                  )}
                </div>
              )
            })}
          </div>
          {error && <p role="alert" className="whole-experiment-selector__error">{error}</p>}

          <div className="whole-experiment-selector__recent">
            <div className="whole-experiment-selector__recent-title">
              <h3>最近使用的组合{!loading && recent.length > 0 ? `（${recent.length}）` : ''}</h3>
              <button type="button" className="whole-experiment-selector__refresh" onClick={loadRecent} disabled={loading || saving}>
                {loading ? '加载中…' : '刷新'}
              </button>
            </div>
            {loading ? <p>加载组合列表中，暂不能进行组装…</p> : recent.length === 0 ? <p>暂无已保存的组合。</p> : (
              <div className="whole-experiment-selector__history">
                {recent.map((item) => (
                  <button type="button" key={item.id} className="panorama-list__card" onClick={() => open(item.id)}>
                    <strong>{item.name}</strong>
                    <span>{item.lastStartedAt ? new Date(item.lastStartedAt).toLocaleString() : '尚未启动'}</span>
                    <span>{item.valid ? '打开实验' : item.invalidReason || '组合已失效，可查看旧记录'}</span>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </section>
  )
}
