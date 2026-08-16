/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../../../api/client'
import ConfigCopyDialog from '../../../../components/ConfigCopyDialog'
import '../UsersPage.css'

/** 屏柜学习 — 屏柜列表（第一级） */
export default function DisplayCabinetListPage() {
  const [cabinets, setCabinets] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [copySource, setCopySource] = useState(null)
  const [message, setMessage] = useState('')

  const loadCabinets = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await api.listKnowledgeCabinets()
      setCabinets(data)
    } catch (err) {
      setError(err.message || '加载屏柜列表失败')
      setCabinets([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadCabinets()
  }, [loadCabinets])

  return (
    <div className="users-page">
      <h2 className="users-page__title">屏柜学习</h2>
      <p className="users-page__desc">
        屏柜与设备基础数据来自屏柜系统（ct-screen，只读）。选择屏柜后即可维护该屏柜的认知条目。
      </p>

      {error && <div className="users-page__error">{error}</div>}
      {message && <div className="users-page__message">{message}</div>}
      {loading ? (
        <p className="users-page__loading">加载中…</p>
      ) : cabinets.length === 0 ? (
        <p className="users-page__empty">暂无屏柜数据，请确认屏柜系统可读。</p>
      ) : (
        <div className="users-page__table-wrap">
          <table className="users-page__table">
            <thead>
              <tr>
                <th>名称</th>
                <th>编码</th>
                <th>描述</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {cabinets.map((cabinet) => (
                <tr key={cabinet.id}>
                  <td>{cabinet.name}</td>
                  <td>{cabinet.code}</td>
                  <td>{cabinet.description || '—'}</td>
                  <td className="users-page__actions">
                    <Link className="users-page__link" to={`/admin/display/cabinets/${cabinet.id}`}>
                      进入
                    </Link>
                    <button type="button" className="users-page__link" onClick={() => setCopySource(cabinet)}>
                      复制配置
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {copySource && <ConfigCopyDialog
        scope="CABINET"
        sourceId={copySource.id}
        sourceName={`${copySource.name}（${copySource.code}）`}
        onClose={() => setCopySource(null)}
        onSuccess={() => setMessage(`已完成“${copySource.name}”的配置复制`)}
      />}
    </div>
  )
}
