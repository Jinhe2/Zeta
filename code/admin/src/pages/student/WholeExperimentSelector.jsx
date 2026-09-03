import { useEffect, useState } from 'react'
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

  useEffect(() => {
    let cancelled = false
    api.listRecentWholeExperiments(deviceId)
      .then((items) => { if (!cancelled) setRecent(items) })
      .catch((err) => { if (!cancelled) setError(err.message) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [deviceId])

  const open = (id) => navigate(`/student/modes/panorama/whole-experiments/${id}`, {
    state: { ...navigationState, mode: 'whole' },
  })
  const confirm = async () => {
    if (saving || !validWholeSelection(selection, logics)) return
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
      <div className="whole-experiment-selector__slots">
        {[1, 2, 3].map((sequence, index) => {
          const candidates = wholeCandidates(logics, sequence)
          return (
            <label key={sequence}>
              <span>序列 {sequence}（{sequence === 3 ? '可选' : '必选'}）</span>
              <select
                value={selection[index]}
                disabled={saving || (index > 0 && !selection[index - 1])}
                onChange={(event) => setSelection(updateWholeSelection(selection, index, event.target.value))}
              >
                <option value="">请选择基础逻辑</option>
                {candidates.map((logic) => <option key={logic.id} value={logic.id}>{logic.title}</option>)}
              </select>
              {candidates.length === 0 && <small>暂无序列 {sequence} 的基础逻辑，请联系管理员配置。</small>}
            </label>
          )
        })}
      </div>
      {error && <p role="alert" className="panorama-list__status--error">{error}</p>}
      <button type="button" className="whole-experiment-selector__confirm" onClick={confirm}
        disabled={saving || !validWholeSelection(selection, logics)}>
        {saving ? '正在保存…' : '确认组合'}
      </button>
      <h3>最近使用的组合</h3>
      {loading ? <p>加载历史组合中…</p> : recent.length === 0 ? <p>暂无已启动的整组实验。</p> : (
        <div className="whole-experiment-selector__history">
          {recent.map((item) => (
            <button type="button" key={item.id} className="panorama-list__card" onClick={() => open(item.id)}>
              <strong>{item.name}</strong>
              <span>{new Date(item.lastStartedAt).toLocaleString()}</span>
              <span>{item.valid ? '打开实验' : item.invalidReason || '组合已失效，可查看旧记录'}</span>
            </button>
          ))}
        </div>
      )}
    </section>
  )
}
