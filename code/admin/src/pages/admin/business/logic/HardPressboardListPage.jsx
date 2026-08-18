/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../../../../api/client'
import '../UsersPage.css'
import './SettingListPage.css'

export default function HardPressboardListPage({ scopeType }) {
  const params = useParams()
  const rawId = scopeType === 'IED_DEVICE'
    ? params.deviceId
    : scopeType === 'LOGIC_GROUP' ? params.groupId : params.logicDiagramId
  const scopeId = Number(rawId)
  const [data, setData] = useState(null)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState('')
  const [dirty, setDirty] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const fileRef = useRef(null)

  const applyData = useCallback((next) => {
    setData(next)
    setItems((next.configuredItems || []).map((item, index) => ({ ...item, sortOrder: index })))
    setDirty(false)
  }, [])

  const load = useCallback(async () => {
    if (!scopeId) return
    setLoading(true)
    setError('')
    try {
      applyData(await api.getHardPressboardList(scopeType, scopeId))
    } catch (err) {
      setError(err.message || '加载硬压板基准清单失败')
    } finally {
      setLoading(false)
    }
  }, [applyData, scopeId, scopeType])

  useEffect(() => { load() }, [load])

  useEffect(() => {
    const beforeUnload = (event) => {
      if (!dirty) return
      event.preventDefault()
      event.returnValue = ''
    }
    const captureLinks = (event) => {
      const link = event.target.closest?.('a[href]')
      if (dirty && link && !window.confirm('当前硬压板基准清单存在未保存修改，确定离开吗？')) {
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

  const updateItem = (index, values) => {
    setItems((current) => current.map((item, i) => i === index ? { ...item, ...values } : item))
    setDirty(true)
    setMessage('')
  }

  const moveItem = (index, direction) => {
    const target = index + direction
    if (target < 0 || target >= items.length) return
    setItems((current) => {
      const next = [...current]
      ;[next[index], next[target]] = [next[target], next[index]]
      return next
    })
    setDirty(true)
    setMessage('')
  }

  const save = async () => {
    setWorking('save'); setError(''); setMessage('')
    try {
      const result = await api.saveHardPressboardList(scopeType, scopeId, items.map((item, index) => ({
        pressboardRef: item.pressboardRef,
        baselineValue: Boolean(item.baselineValue),
        compareEnabled: item.compareEnabled !== false,
        sortOrder: index,
      })))
      applyData(result)
      setMessage('硬压板基准清单已保存')
    } catch (err) {
      setError(err.message || '保存硬压板基准清单失败')
    } finally { setWorking('') }
  }

  const summon = async () => {
    if (dirty && !window.confirm('召唤结果将替换当前未保存的编辑内容，确定继续吗？')) return
    setWorking('summon'); setError(''); setMessage('')
    try {
      const result = await api.summonHardPressboardList(scopeType, scopeId)
      setItems(result.items || [])
      setDirty(true)
      setMessage(`已召唤 ${result.summonCount} 项，匹配硬压板目录 ${result.matchedCount}/${result.catalogCount} 项；确认无误后请保存。`)
    } catch (err) {
      setError(err.message || '召唤装置硬压板失败')
    } finally { setWorking('') }
  }

  const clear = async () => {
    if (!window.confirm('确定清空当前层级的整套硬压板基准清单吗？')) return
    setWorking('clear'); setError('')
    try {
      applyData(await api.clearHardPressboardList(scopeType, scopeId))
      setMessage(scopeType !== 'IED_DEVICE' ? '独立清单已清空，当前自动使用装置级清单。' : '装置级清单已清空。')
    } catch (err) {
      setError(err.message || '清空硬压板基准清单失败')
    } finally { setWorking('') }
  }

  const importExcel = async (event) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file || !window.confirm('导入成功后将整套替换当前层级清单，确定继续吗？')) return
    setWorking('import'); setError('')
    try {
      applyData(await api.importHardPressboardList(scopeType, scopeId, file))
      setMessage('Excel 已导入并替换当前硬压板基准清单')
    } catch (err) {
      setError(err.message || '导入 Excel 失败')
    } finally { setWorking('') }
  }

  const exportExcel = async () => {
    setWorking('export'); setError('')
    try {
      const blob = await api.downloadHardPressboardList(scopeType, scopeId)
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `${data?.scopeName || '硬压板'}-硬压板基准清单.xlsx`
      anchor.click()
      URL.revokeObjectURL(url)
    } catch (err) {
      setError(err.message || '导出 Excel 失败')
    } finally { setWorking('') }
  }

  if (!rawId || Number.isNaN(scopeId)) return <Navigate to="/admin/logic-learning" replace />
  const disabled = Boolean(working)
  const isFallback = (scopeType === 'LOGIC_DIAGRAM' || scopeType === 'LOGIC_GROUP') && data?.fallbackToDevice

  return (
    <div className="users-page setting-list-page">
      <div className="users-page__header">
        <div>
          <p className="users-page__breadcrumb"><Link to="/admin/logic-learning">逻辑学习</Link><span> / </span><span>硬压板基准清单</span></p>
          <h2 className="users-page__title">{data ? `${data.scopeName} — ${scopeType === 'IED_DEVICE' ? '装置硬压板基准清单' : '独立硬压板基准清单'}` : '硬压板基准清单'}</h2>
          <p className="users-page__desc">硬压板项目由屏柜召唤或 Excel 导入生成，召唤后确认并保存才会生效。</p>
        </div>
        <div className="setting-list-page__toolbar">
          <button type="button" onClick={summon} disabled={disabled}>{working === 'summon' ? '召唤中…' : '召唤当前硬压板'}</button>
          <button type="button" onClick={exportExcel} disabled={disabled}>{working === 'export' ? '导出中…' : '导出 Excel'}</button>
          <button type="button" onClick={() => fileRef.current?.click()} disabled={disabled}>{working === 'import' ? '导入中…' : '导入 Excel'}</button>
          <input ref={fileRef} type="file" accept=".xlsx" hidden onChange={importExcel} />
        </div>
      </div>
      {error && <div className="users-page__error">{error}</div>}
      {message && <div className="users-page__message">{message}</div>}
      {isFallback && <div className="setting-list-page__fallback">当前未配置独立清单，实验时将使用装置级清单（{data.effectiveItems.length} 项）。召唤或导入并保存后将启用独立清单。</div>}
      {!isFallback && scopeType !== 'IED_DEVICE' && data?.configuredItems.length > 0 && <div className="setting-list-page__active">当前使用独立清单。</div>}
      {loading ? <p className="users-page__loading">加载中…</p> : (
        <div className="users-page__table-wrap">
          <table className="users-page__table setting-list-page__table">
            <thead><tr><th>序号</th><th>硬压板名称</th><th>硬压板编号</th><th>参与比对</th><th>基准状态</th><th>排序</th></tr></thead>
            <tbody>{items.length === 0 ? <tr><td colSpan={6} className="users-page__empty-cell">当前层级暂无硬压板项目，请召唤屏柜硬压板或导入 Excel。</td></tr> : items.map((item, index) => (
              <tr key={item.pressboardRef}>
                <td>{index + 1}</td><td>{item.pressboardName}</td><td className="setting-list-page__ref">{item.pressboardRef}</td>
                <td className="setting-list-page__compare"><input type="checkbox" checked={item.compareEnabled !== false} onChange={(event) => updateItem(index, { compareEnabled: event.target.checked })} aria-label={`${item.pressboardName}参与比对`} /></td>
                <td><select value={item.baselineValue ? 'true' : 'false'} onChange={(event) => updateItem(index, { baselineValue: event.target.value === 'true' })} aria-label={`${item.pressboardName}基准状态`}><option value="true">投入</option><option value="false">退出</option></select></td>
                <td className="setting-list-page__sort"><button type="button" onClick={() => moveItem(index, -1)} disabled={index === 0} aria-label={`${item.pressboardName}上移`}>↑</button><button type="button" onClick={() => moveItem(index, 1)} disabled={index === items.length - 1} aria-label={`${item.pressboardName}下移`}>↓</button></td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      )}
      <div className="setting-list-page__actions">
        <button type="button" className="setting-list-page__save" onClick={save} disabled={disabled || !dirty}>{working === 'save' ? '保存中…' : '保存清单'}</button>
        <button type="button" className="setting-list-page__clear" onClick={clear} disabled={disabled || !data?.configuredItems.length}>清空当前层级</button>
      </div>
    </div>
  )
}
