import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import './TabletShell.css'
import './PanoramaListPage.css'

export default function PanoramaListPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { logout } = useAuth()
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const fromCoach = location.state?.from === 'coach'
  const backLabel = fromCoach ? '← 返回教练模式' : '← 返回首页'
  const backTo = fromCoach ? '/student/modes/coach' : '/student'
  const pageTitle = fromCoach ? '采样测试 · 保护逻辑' : '全景模式 · 保护逻辑'
  const introText = fromCoach
    ? '选择保护逻辑，进行采样值测试与信号校验。'
    : '选择保护逻辑，进入逻辑框图全景浏览。'

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
        <button type="button" className="tablet-shell__back" onClick={() => navigate(backTo)}>
          {backLabel}
        </button>
        <h1>{pageTitle}</h1>
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
        <p className="panorama-list__intro">{introText}</p>
        {loading && <p className="panorama-list__status">加载中…</p>}
        {error && <p className="panorama-list__status panorama-list__status--error">{error}</p>}
        {!loading && !error && (
          <div className="panorama-list__grid">
            {list.length === 0 ? (
              <p className="panorama-list__empty">暂无保护逻辑配置</p>
            ) : (
              list.map((item) => (
                <Link
                  key={item.id}
                  to={`/student/modes/panorama/${item.id}`}
                  className="panorama-list__card"
                >
                  <div className="panorama-list__card-header">
                    <h3>{item.title}</h3>
                    {item.category && (
                      <span className="panorama-list__category">{item.category}</span>
                    )}
                  </div>
                  <p>{item.description || '暂无描述'}</p>
                  <div className="panorama-list__meta">
                    <span className="panorama-list__meta-item">输入 <span>{item.inputCount}</span></span>
                    <span className="panorama-list__meta-item">逻辑门 <span>{item.gateCount}</span></span>
                    <span className="panorama-list__meta-item">输出 <span>{item.outputCount}</span></span>
                  </div>
                </Link>
              ))
            )}
          </div>
        )}
      </main>
    </div>
  )
}
