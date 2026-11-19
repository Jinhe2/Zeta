import { useEffect, useMemo, useState } from 'react'
import { api } from '../../../api/client'
import './LearningPathConfigPage.css'

export default function LearningPathConfigPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const load = async () => {
    setLoading(true); setError('')
    try { setRows(await api.getLearningPathConfig()) }
    catch (err) { setError(err.message || '学习路径配置加载失败') }
    finally { setLoading(false) }
  }
  useEffect(() => {
    const timer = window.setTimeout(load, 0)
    return () => window.clearTimeout(timer)
  }, [])

  const ordered = useMemo(() => [...rows].sort((a, b) => Number(a.sortOrder) - Number(b.sortOrder)), [rows])
  const update = (code, change) => setRows((items) => items.map((item) => item.code === code ? { ...item, ...change } : item))
  const toggleDependency = (code, dependency) => setRows((items) => items.map((item) => {
    if (item.code !== code) return item
    const current = item.prerequisites || []
    return { ...item, prerequisites: current.includes(dependency) ? current.filter((value) => value !== dependency) : [...current, dependency] }
  }))
  const save = async () => {
    setSaving(true); setError(''); setMessage('')
    try {
      const data = await api.saveLearningPathConfig(rows.map(({ code, sortOrder, enabled, prerequisites }) => ({ code, sortOrder: Number(sortOrder), enabled, prerequisites })))
      setRows(data); setMessage('学习路径已保存，新顺序和解锁关系已生效。')
    } catch (err) { setError(err.message || '保存失败') }
    finally { setSaving(false) }
  }

  return <div className="path-config">
    <div className="path-config__header"><div><h2>教练模式学习路径</h2><p>配置五个学习入口的展示顺序、启用状态和前置完成条件。已产生的学员完成记录会保留。</p></div><button type="button" onClick={save} disabled={loading || saving}>{saving ? '保存中…' : '保存配置'}</button></div>
    {message && <div className="path-config__message">{message}</div>}
    {error && <div className="path-config__error">{error}</div>}
    {loading ? <p className="path-config__empty">正在加载…</p> : <>
      <div className="path-config__flow">{ordered.filter((item) => item.enabled).map((item, index) => <span key={item.code}>{index > 0 && <i>→</i>}<b>{item.name}</b></span>)}</div>
      <div className="path-config__list">{ordered.map((item) => <section key={item.code} className={`path-config__card${item.enabled ? '' : ' path-config__card--disabled'}`}>
        <div className="path-config__identity"><span>{item.code}</span><strong>{item.name}</strong><small>{item.description}</small></div>
        <label>顺序<input type="number" min="1" value={item.sortOrder} onChange={(event) => update(item.code, { sortOrder: event.target.value })} /></label>
        <label className="path-config__switch"><input type="checkbox" checked={item.enabled} onChange={(event) => update(item.code, { enabled: event.target.checked })} /><span>启用入口</span></label>
        <div className="path-config__deps"><span>完成以下阶段后解锁</span><div>{rows.filter((candidate) => candidate.code !== item.code).map((candidate) => <label key={candidate.code}><input type="checkbox" checked={(item.prerequisites || []).includes(candidate.code)} onChange={() => toggleDependency(item.code, candidate.code)} />{candidate.name}</label>)}</div><small>不勾选表示学员初始即可进入；可设置多个前置条件。</small></div>
      </section>)}</div>
    </>}
  </div>
}
