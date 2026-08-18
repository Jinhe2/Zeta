/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../../../../api/client'
import '../UsersPage.css'
import './SettingListPage.css'
import './WiringRequirementPage.css'

const CATEGORY_LABELS = { VOLTAGE: '电压', CURRENT: '电流' }
const POSITIONS = [
  { key: 'a', label: 'A相' },
  { key: 'b', label: 'B相' },
  { key: 'c', label: 'C相' },
  { key: 'n', label: 'N相' },
]

function emptyGroup() {
  return { a: '', b: '', c: '', n: '' }
}

function emptyCategory(category) {
  return { category, required: false, phaseMode: 'THREE_PHASE', groups: [emptyGroup()] }
}

function categoryFromData(categoryData) {
  return {
    category: categoryData.category,
    required: categoryData.required,
    phaseMode: categoryData.phaseMode || 'THREE_PHASE',
    groups: (categoryData.groups || []).map((group) => ({
      a: group.a?.terminalId ?? '',
      b: group.b?.terminalId ?? '',
      c: group.c?.terminalId ?? '',
      n: group.n?.terminalId ?? '',
    })),
  }
}

function toId(value) {
  return value === '' || value == null ? null : Number(value)
}

export default function WiringRequirementPage() {
  const { logicDiagramId } = useParams()
  const scopeId = Number(logicDiagramId)
  const [data, setData] = useState(null)
  const [categories, setCategories] = useState([])
  const [strips, setStrips] = useState([])
  const [terminals, setTerminals] = useState([])
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState(false)
  const [dirty, setDirty] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const applyData = useCallback((next) => {
    setData(next)
    const nextCategories = (next.categories || [])
    setCategories(['VOLTAGE', 'CURRENT'].map((category) => {
      const found = nextCategories.find((item) => item.category === category)
      return found ? categoryFromData(found) : emptyCategory(category)
    }))
    setDirty(false)
  }, [])

  const load = useCallback(async () => {
    if (!scopeId) return
    setLoading(true)
    setError('')
    try {
      const result = await api.getWiringRequirement(scopeId)
      applyData(result)
      if (result.cabinetId) {
        const [stripData, terminalData] = await Promise.all([
          api.listTerminalStrips(result.cabinetId),
          api.listTerminals(result.cabinetId),
        ])
        setStrips(stripData)
        setTerminals(terminalData)
      }
    } catch (err) {
      setError(err.message || '加载试验仪接线要求失败')
    } finally {
      setLoading(false)
    }
  }, [applyData, scopeId])

  useEffect(() => { load() }, [load])

  useEffect(() => {
    const beforeUnload = (event) => {
      if (!dirty) return
      event.preventDefault()
      event.returnValue = ''
    }
    const captureLinks = (event) => {
      const link = event.target.closest?.('a[href]')
      if (dirty && link && !window.confirm('当前试验仪接线要求存在未保存修改，确定离开吗？')) {
        event.preventDefault()
        event.stopPropagation()
      }
    }
    window.addEventListener('beforeunload', beforeUnload)
    document.addEventListener('click', captureLinks, true)
    return () => {
      window.removeEventListener('beforeunload', beforeUnload)
      document.removeEventListener('click', captureLinks, true)
    }
  }, [dirty])

  const stripGroups = useMemo(() => {
    const groups = strips.map((strip) => ({
      strip,
      terminals: terminals.filter((terminal) => Number(terminal.terminalStripId) === Number(strip.id)),
    }))
    const stripIds = new Set(strips.map((strip) => Number(strip.id)))
    const orphanTerminals = terminals.filter((terminal) =>
      terminal.terminalStripId == null || !stripIds.has(Number(terminal.terminalStripId)))
    if (orphanTerminals.length > 0) {
      groups.push({ strip: { id: '__orphan', name: '其他端子', labelPrefix: '' }, terminals: orphanTerminals })
    }
    return groups
  }, [strips, terminals])

  const markDirty = () => {
    setDirty(true)
    setMessage('')
  }

  const updateCategory = (category, patch) => {
    setCategories((current) => current.map((item) => item.category === category ? { ...item, ...patch } : item))
    markDirty()
  }

  const updateGroup = (category, groupIndex, position, value) => {
    setCategories((current) => current.map((item) => item.category === category
      ? { ...item, groups: item.groups.map((group, index) => index === groupIndex ? { ...group, [position]: value } : group) }
      : item))
    markDirty()
  }

  const addGroup = (category) => {
    setCategories((current) => current.map((item) => item.category === category
      ? { ...item, groups: [...item.groups, emptyGroup()] }
      : item))
    markDirty()
  }

  const removeGroup = (category, groupIndex) => {
    setCategories((current) => current.map((item) => item.category === category
      ? { ...item, groups: item.groups.filter((_, index) => index !== groupIndex) }
      : item))
    markDirty()
  }

  const toggleRequired = (category, checked) => {
    setCategories((current) => current.map((item) => {
      if (item.category !== category) return item
      const groups = checked && item.groups.length === 0 ? [emptyGroup()] : item.groups
      return { ...item, required: checked, groups }
    }))
    markDirty()
  }

  const save = async () => {
    setWorking(true); setError(''); setMessage('')
    try {
      const payload = categories.map((category) => ({
        category: category.category,
        required: category.required,
        phaseMode: category.phaseMode,
        groups: category.required ? category.groups.map((group) => ({
          terminalAId: toId(group.a),
          terminalBId: toId(group.b),
          terminalCId: toId(group.c),
          terminalNId: toId(group.n),
        })) : [],
      }))
      const result = await api.saveWiringRequirement(scopeId, payload)
      applyData(result)
      setMessage('试验仪接线要求已保存')
    } catch (err) {
      setError(err.message || '保存试验仪接线要求失败')
    } finally { setWorking(false) }
  }

  if (!logicDiagramId || Number.isNaN(scopeId)) return <Navigate to="/admin/logic-learning" replace />

  const renderTerminalSelect = (value, onChange, ariaLabel, disabled) => (
    <select value={value} onChange={(event) => onChange(event.target.value)} aria-label={ariaLabel} disabled={disabled}>
      <option value="">请选择</option>
      {stripGroups.map(({ strip, terminals: stripTerminals }) => (
        <optgroup key={strip.id} label={strip.labelPrefix ? `${strip.name}（${strip.labelPrefix}）` : strip.name}>
          {stripTerminals.map((terminal) => (
            <option key={terminal.id} value={terminal.id}>{terminal.terminalLabel}</option>
          ))}
        </optgroup>
      ))}
    </select>
  )

  return (
    <div className="users-page setting-list-page">
      <div className="users-page__header">
        <div>
          <p className="users-page__breadcrumb"><Link to="/admin/logic-learning">逻辑学习</Link><span> / </span><span>试验仪接线要求</span></p>
          <h2 className="users-page__title">{data ? `${data.scopeName} — 试验仪接线要求` : '试验仪接线要求'}</h2>
          <p className="users-page__desc">配置试验仪电压/电流是否需要接入、三相或单相，以及 A/B/C/N 端子分组；实验前将据此校验接线。</p>
        </div>
      </div>
      {error && <div className="users-page__error">{error}</div>}
      {message && <div className="users-page__message">{message}</div>}
      {loading ? <p className="users-page__loading">加载中…</p> : (
        <div className="wiring-requirement">
          {categories.map((category) => {
            const disabled = !category.required || Boolean(working)
            return (
              <section key={category.category} className="wiring-requirement__section">
                <div className="wiring-requirement__header">
                  <h3 className="wiring-requirement__title">{CATEGORY_LABELS[category.category]}</h3>
                  <label className="wiring-requirement__required">
                    <input
                      type="checkbox"
                      checked={category.required}
                      onChange={(event) => toggleRequired(category.category, event.target.checked)}
                    />
                    需要接入
                  </label>
                  <label className="wiring-requirement__mode">
                    接线方式
                    <select
                      value={category.phaseMode}
                      disabled={disabled}
                      onChange={(event) => updateCategory(category.category, { phaseMode: event.target.value })}
                    >
                      <option value="THREE_PHASE">三相</option>
                      <option value="SINGLE_PHASE">单相</option>
                    </select>
                  </label>
                </div>
                {!category.required ? (
                  <p className="wiring-requirement__hint">未启用接入，实验前将跳过{ CATEGORY_LABELS[category.category] }接线校验。</p>
                ) : (
                  <div className="users-page__table-wrap">
                    <table className="users-page__table wiring-requirement__table">
                      <thead>
                        <tr><th>组</th>{POSITIONS.map((position) => <th key={position.key}>{position.label}</th>)}<th>操作</th></tr>
                      </thead>
                      <tbody>
                        {category.groups.map((group, index) => (
                          <tr key={index}>
                            <td>{index + 1}</td>
                            {POSITIONS.map((position) => (
                              <td key={position.key}>
                                {renderTerminalSelect(
                                  group[position.key],
                                  (value) => updateGroup(category.category, index, position.key, value),
                                  `${CATEGORY_LABELS[category.category]}第${index + 1}组${position.label}端子`,
                                  Boolean(working),
                                )}
                              </td>
                            ))}
                            <td>
                              <button type="button" className="users-page__link users-page__link--danger" disabled={Boolean(working)} onClick={() => removeGroup(category.category, index)}>删除</button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                    <button type="button" className="wiring-requirement__add" disabled={Boolean(working)} onClick={() => addGroup(category.category)}>+ 添加一组</button>
                  </div>
                )}
              </section>
            )
          })}
          <div className="setting-list-page__actions">
            <button type="button" className="setting-list-page__save" onClick={save} disabled={working || !dirty}>
              {working ? '保存中…' : '保存接线要求'}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
