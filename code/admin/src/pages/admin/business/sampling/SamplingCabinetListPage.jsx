import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../../../api/client'
import '../UsersPage.css'

export default function SamplingCabinetListPage() {
  const [cabinets, setCabinets] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.listKnowledgeCabinets().then(setCabinets).catch((err) => setError(err.message)).finally(() => setLoading(false))
  }, [])

  return <div className="users-page">
    <h2 className="users-page__title">采样测试</h2>
    <p className="users-page__desc">按屏柜维护采样测试的图片、视频和八路采样配置。</p>
    {error && <div className="users-page__error">{error}</div>}
    {loading ? <p className="users-page__loading">加载中…</p> : cabinets.length === 0 ? <p className="users-page__empty">暂无屏柜数据。</p> : <div className="users-page__table-wrap">
      <table className="users-page__table"><thead><tr><th>名称</th><th>编码</th><th>描述</th><th>操作</th></tr></thead><tbody>
        {cabinets.map((cabinet) => <tr key={cabinet.id}><td>{cabinet.name}</td><td>{cabinet.code}</td><td>{cabinet.description || '—'}</td><td><Link className="users-page__link" to={`/admin/sampling-tests/cabinets/${cabinet.id}`}>配置</Link></td></tr>)}
      </tbody></table>
    </div>}
  </div>
}
