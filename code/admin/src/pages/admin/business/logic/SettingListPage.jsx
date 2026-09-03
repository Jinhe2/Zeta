/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../../../../api/client'
import '../UsersPage.css'
import './SettingListPage.css'

function displayValueType(valueType) {
  if (valueType === 'FLOAT') return '浮点型'
  if (valueType === 'INTEGER') return '整数型'
  return valueType || '—'
}

export default function SettingListPage({ scopeType, basePath = '/admin/logic-learning', apiNamespace = 'admin' }) {
  const isDevice = scopeType === 'IED_DEVICE'
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
    setItems(((isDevice ? next.configuredItems : next.effectiveItems) || []).map((item, index) => ({ ...item, sortOrder: index })))
    setDirty(false)
  }, [isDevice])

  const load = useCallback(async () => {
    if (!scopeId) return
    setLoading(true)
    setError('')
    try {
      applyData(await api.getSettingList(scopeType, scopeId, apiNamespace))
    } catch (err) {
      setError(err.message || '加载定值清单失败')
    } finally {
      setLoading(false)
    }
  }, [apiNamespace, applyData, scopeId, scopeType])

  useEffect(() => { load() }, [load])

  useEffect(() => {
    const beforeUnload = (event) => {
      if (!dirty) return
      event.preventDefault()
      event.returnValue = ''
    }
    const captureLinks = (event) => {
      const link = event.target.closest?.('a[href]')
      if (dirty && link && !window.confirm('当前定值清单存在未保存修改，确定离开吗？')) {
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

  const updateValue = (index, value) => {
    setItems((current) => current.map((item, i) => i === index ? { ...item, baselineValue: value } : item))
    setDirty(true)
    setMessage('')
  }

  const updateCompareEnabled = (index, enabled) => {
    setItems((current) => current.map((item, i) => i === index ? { ...item, compareEnabled: enabled } : item))
    setDirty(true)
    setMessage('')
  }

  const updateCompareSelection = (mode) => {
    setItems((current) => current.map((item) => ({
      ...item,
      compareEnabled: mode === 'all'
        ? true
        : mode === 'none' ? false : item.compareEnabled === false,
    })))
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
    setWorking('save')
    setError('')
    setMessage('')
    try {
      const result = isDevice ? await api.saveSettingList(scopeType, scopeId, items.map((item, index) => ({
        settingRef: item.settingRef,
        baselineValue: item.baselineValue,
        compareEnabled: item.compareEnabled !== false,
        sortOrder: index,
      })), apiNamespace) : await api.saveSettingSelection(scopeType, scopeId,
        items.filter((item) => item.compareEnabled).map((item) => item.settingRef), apiNamespace)
      applyData(result)
      setMessage(isDevice ? '定值清单已保存' : '校验项目已保存')
    } catch (err) {
      setError(err.message || '保存定值清单失败')
    } finally {
      setWorking('')
    }
  }

  const summon = async () => {
    if (dirty && !window.confirm('召唤结果将替换当前未保存的编辑内容，确定继续吗？')) return
    setWorking('summon')
    setError('')
    setMessage('')
    try {
      const result = await api.summonSettingList(scopeType, scopeId, apiNamespace)
      setItems(result.items || [])
      setDirty(true)
      setMessage(`已召唤 ${result.summonCount} 项，匹配装置目录 ${result.matchedCount}/${result.catalogCount} 项；确认无误后请保存。`)
    } catch (err) {
      setError(err.message || '召唤装置定值失败')
    } finally {
      setWorking('')
    }
  }

  const clear = async () => {
    if (!window.confirm('确定清空当前层级的整套定值清单吗？')) return
    setWorking('clear')
    setError('')
    try {
      applyData(await api.clearSettingList(scopeType, scopeId, apiNamespace))
      setMessage('装置级清单已清空。')
    } catch (err) {
      setError(err.message || '清空定值清单失败')
    } finally {
      setWorking('')
    }
  }

  const importExcel = async (event) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (!window.confirm('导入成功后将整套替换当前层级清单，确定继续吗？')) return
    setWorking('import')
    setError('')
    try {
      applyData(await api.importSettingList(scopeType, scopeId, file, apiNamespace))
      setMessage('Excel 已导入并替换当前定值清单')
    } catch (err) {
      setError(err.message || '导入 Excel 失败')
    } finally {
      setWorking('')
    }
  }

  const exportExcel = async () => {
    setWorking('export')
    setError('')
    try {
      const blob = await api.downloadSettingList(scopeType, scopeId, apiNamespace)
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `${data?.scopeName || '定值清单'}-定值清单.xlsx`
      anchor.click()
      URL.revokeObjectURL(url)
    } catch (err) {
      setError(err.message || '导出 Excel 失败')
    } finally {
      setWorking('')
    }
  }

  if (!rawId || Number.isNaN(scopeId)) return <Navigate to={basePath} replace />
  const disabled = Boolean(working)
  const comparedCount = items.filter((item) => item.compareEnabled !== false).length

  return (
    <div className="users-page setting-list-page">
      <div className="users-page__header">
        <div>
          <p className="users-page__breadcrumb"><Link to={basePath}>基准管理</Link><span> / </span><span>定值清单</span></p>
          <h2 className="users-page__title">{data ? `${data.scopeName} — ${scopeType === 'IED_DEVICE' ? '装置定值清单' : '定值校验项目'}` : '定值清单'}</h2>
          <p className="users-page__desc">{isDevice ? '定值项目集合由装置召唤或 Excel 导入生成，界面可直接修改定值。' : '定值统一来自装置清单，仅勾选本逻辑需要校验的项目；定值修改请前往装置层。'}</p>
        </div>
        {isDevice && <div className="setting-list-page__toolbar">
          <button type="button" onClick={summon} disabled={disabled}>{working === 'summon' ? '召唤中…' : '召唤当前定值'}</button>
          <button type="button" onClick={exportExcel} disabled={disabled}>{working === 'export' ? '导出中…' : '导出 Excel'}</button>
          <button type="button" onClick={() => fileRef.current?.click()} disabled={disabled}>{working === 'import' ? '导入中…' : '导入 Excel'}</button>
          <input ref={fileRef} type="file" accept=".xlsx" hidden onChange={importExcel} />
        </div>}
      </div>

      {error && <div className="users-page__error">{error}</div>}
      {message && <div className="users-page__message">{message}</div>}

      {loading ? <p className="users-page__loading">加载中…</p> : (
        <>
          <div className="setting-list-page__selection-toolbar">
            <span>已选择 {comparedCount} / {items.length} 项参与比对</span>
            <div>
              <button type="button" onClick={() => updateCompareSelection('all')} disabled={disabled || items.length === 0 || comparedCount === items.length}>全选</button>
              <button type="button" onClick={() => updateCompareSelection('none')} disabled={disabled || items.length === 0 || comparedCount === 0}>全不选</button>
              <button type="button" onClick={() => updateCompareSelection('invert')} disabled={disabled || items.length === 0}>反选</button>
            </div>
          </div>
          <div className="users-page__table-wrap">
            <table className="users-page__table setting-list-page__table">
              <thead><tr><th>序号</th><th>定值名称</th><th>定值引用</th><th>类型</th><th>参与比对</th><th>定值</th>{isDevice && <th>排序</th>}</tr></thead>
              <tbody>
                {items.length === 0 ? <tr><td colSpan={isDevice ? 7 : 6} className="users-page__empty-cell">{isDevice ? '当前装置暂无定值项目，请召唤装置定值或导入 Excel。' : '装置尚未配置定值清单，请先在装置层维护。'}</td></tr> : items.map((item, index) => (
                  <tr key={item.settingRef}>
                    <td>{index + 1}</td><td>{item.settingName}</td><td className="setting-list-page__ref">{item.settingRef}</td><td>{displayValueType(item.valueType)}</td>
                    <td className="setting-list-page__compare"><input type="checkbox" disabled={disabled} checked={item.compareEnabled !== false} onChange={(event) => updateCompareEnabled(index, event.target.checked)} aria-label={`${item.settingName}参与比对`} /></td>
                    <td>{isDevice ? <input type="text" value={item.baselineValue ?? ''} onChange={(event) => updateValue(index, event.target.value)} aria-label={`${item.settingName}定值`} /> : item.baselineValue}</td>
                    {isDevice && <td className="setting-list-page__sort">
                      <button type="button" onClick={() => moveItem(index, -1)} disabled={index === 0} aria-label={`${item.settingName}上移`}>↑</button>
                      <button type="button" onClick={() => moveItem(index, 1)} disabled={index === items.length - 1} aria-label={`${item.settingName}下移`}>↓</button>
                    </td>}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      <div className="setting-list-page__actions">
        <button type="button" className="setting-list-page__save" onClick={save} disabled={disabled || !dirty}>{working === 'save' ? '保存中…' : isDevice ? '保存清单' : '保存校验项目'}</button>
        {isDevice && <button type="button" className="setting-list-page__clear" onClick={clear} disabled={disabled || !data?.configuredItems.length}>清空装置清单</button>}
      </div>
    </div>
  )
}
