import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../../../api/client'
import '../UsersPage.css'

export default function DrawingCabinetListPage() {
  const [cabinets, setCabinets] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadCabinets = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setCabinets(await api.listKnowledgeCabinets())
    } catch (err) {
      setError(err.message || '加载屏柜列表失败')
      setCabinets([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(loadCabinets, 0)
    return () => window.clearTimeout(timer)
  }, [loadCabinets])

  return (
    <div className="users-page">
      <h2 className="users-page__title">图纸学习</h2>
      <p className="users-page__desc">
        按屏柜维护蓝图、白图下的图纸分组、图纸图片和图纸认知条目。
      </p>

      {error && <div className="users-page__error">{error}</div>}
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
                  <td>
                    <Link className="users-page__link" to={`/admin/drawing-learning/cabinets/${cabinet.id}`}>
                      进入
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
