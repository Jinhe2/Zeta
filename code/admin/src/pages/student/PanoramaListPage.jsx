import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import './TabletShell.css'
import './PanoramaListPage.css'

export default function PanoramaListPage() {
  const navigate = useNavigate()
  const { logout } = useAuth()
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    api
      .listProtectionLogics()
      .then((data) => {
        if (!cancelled) setList(data)
      })
      .catch((err) => {
        if (!cancelled) setError(err.message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="tablet-shell">
      <header className="tablet-shell__header">
        <button type="button" className="tablet-shell__back" onClick={() => navigate('/student')}>
          ← 返回首页
        </button>
        <h1>全景模式 · 保护逻辑</h1>
        <button
          type="button"
          className="tablet-shell__logout"
          onClick={async () => {
            await logout()
            navigate('/login', { replace: true })
          }}
        >
          退出登录
        </button>
      </header>

      <main className="tablet-shell__main panorama-list">
        <p className="panorama-list__intro">选择保护逻辑，进入逻辑框图全景浏览。</p>
        {loading && <p className="panorama-list__status">加载中…</p>}
        {error && <p className="panorama-list__status panorama-list__status--error">{error}</p>}
        {!loading && !error && (
          <div className="panorama-list__grid">
            {list.map((item) => (
              <Link
                key={item.id}
                to={`/student/modes/panorama/${item.id}`}
                className="panorama-list__card"
              >
                <span className="panorama-list__category">{item.category}</span>
                <h3>{item.title}</h3>
                <p>{item.description}</p>
              </Link>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
