import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { loadSampleList } from '../sampleLoader'
import { useAuth } from '../auth/AuthContext'
import { getProtectionDisplay } from '../data/protectionCatalog'
import './RolePages.css'

interface SampleItem {
  filename: string
  name: string
  data: {
    description?: string
    inputs?: unknown[]
    gates?: unknown[]
    timers?: unknown[]
    outputs?: unknown[]
  }
}

export default function StudentHomePage() {
  const { session, logout } = useAuth()
  const [samples, setSamples] = useState<SampleItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    loadSampleList()
      .then((list) => {
        if (!cancelled) setSamples(list)
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : String(err))
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="role-page role-page--student">
      <header className="role-page__header">
        <div>
          <p className="role-page__eyebrow">学员</p>
          <h1 className="role-page__title">保护逻辑学习</h1>
        </div>
        <div className="role-page__actions">
          <span className="role-page__user">{session?.displayName}</span>
          <Link to="/login" className="role-page__btn" onClick={logout}>
            退出登录
          </Link>
        </div>
      </header>

      <main className="role-page__main">
        <div className="student-home__intro">
          <h2>选择保护逻辑框图</h2>
          <p>点击下方卡片查看对应的保护逻辑框图，支持节点点击高亮与配置信息查看。</p>
        </div>

        {loading && <div className="student-loading">正在加载保护逻辑列表…</div>}
        {error && <div className="student-error">{error}</div>}

        {!loading && !error && (
          <div className="protection-grid">
            {samples.map((sample) => {
              const display = getProtectionDisplay(sample.filename, sample.name)
              const data = sample.data
              return (
                <Link
                  key={sample.filename}
                  to={`/student/diagram/${encodeURIComponent(sample.filename)}`}
                  className="protection-card"
                >
                  <span className="protection-card__category">{display.category}</span>
                  <h3 className="protection-card__title">{display.title}</h3>
                  <p className="protection-card__desc">
                    {display.description || data.description || '继电保护逻辑框图配置'}
                  </p>
                  <div className="protection-card__stats">
                    <span className="protection-card__stat">
                      输入 {data.inputs?.length ?? 0}
                    </span>
                    <span className="protection-card__stat">
                      逻辑门 {data.gates?.length ?? 0}
                    </span>
                    <span className="protection-card__stat">
                      输出 {data.outputs?.length ?? 0}
                    </span>
                  </div>
                  <span className="protection-card__action">查看逻辑框图 →</span>
                </Link>
              )
            })}
          </div>
        )}
      </main>
    </div>
  )
}
