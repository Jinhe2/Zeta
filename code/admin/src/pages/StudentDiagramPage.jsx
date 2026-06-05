import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'
import SectionSelector from '../components/SectionSelector'
import ConfigPanel from '../components/ConfigPanel'
import { useAuth } from '../auth/AuthContext'
import './student/TabletShell.css'
import './StudentPages.css'

export default function StudentDiagramPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { logout } = useAuth()
  const [detail, setDetail] = useState(null)
  const [sections, setSections] = useState([])
  const [selectedSectionId, setSelectedSectionId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    Promise.all([api.getProtectionLogic(id), api.getSections(id)])
      .then(([detailData, sectionData]) => {
        if (cancelled) return
        setDetail(detailData)
        setSections(sectionData)
        setSelectedSectionId(sectionData[0]?.id ?? null)
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
  }, [id])

  useEffect(() => {
    if (detail?.title) {
      document.title = detail.title
    }
  }, [detail])

  const nodeStates = useMemo(() => {
    const section = sections.find((s) => s.id === selectedSectionId)
    return section?.states ?? null
  }, [sections, selectedSectionId])

  const satisfiedCount = nodeStates
    ? Object.values(nodeStates).filter(Boolean).length
    : 0
  const totalCount = nodeStates ? Object.keys(nodeStates).length : 0

  return (
    <div className="tablet-shell diagram-page">
      <header className="tablet-shell__header diagram-page__toolbar">
        <button
          type="button"
          className="tablet-shell__back"
          onClick={() => navigate('/student/modes/panorama')}
        >
          ← 返回列表
        </button>
        <h1>{detail?.title || '逻辑框图'}</h1>
        <button
          type="button"
          className="tablet-shell__logout"
          onClick={() => logout().then(() => navigate('/login', { replace: true }))}
        >
          退出登录
        </button>
      </header>

      {error && <div className="diagram-page__error">{error}</div>}

      <div className="diagram-page__body">
        <div className="diagram-page__workspace">
          <div className="diagram-canvas">
            {loading ? (
              <p className="diagram-canvas__placeholder">正在加载逻辑框图…</p>
            ) : (
              <>
                <div className="diagram-canvas__header">
                  <span>逻辑框图</span>
                  {nodeStates && (
                    <span>
                      当前断面：满足 {satisfiedCount} / {totalCount}
                    </span>
                  )}
                </div>
                <div className="diagram-canvas__area">
                  <p>逻辑框图渲染区域</p>
                  <p className="diagram-canvas__hint">
                    待接入框图渲染组件；当前断面节点状态已由后端返回，可在右侧配置面板查看。
                  </p>
                  {nodeStates && (
                    <ul className="diagram-canvas__states">
                      {Object.entries(nodeStates).map(([nodeId, ok]) => (
                        <li key={nodeId} className={ok ? 'ok' : 'fail'}>
                          {nodeId}：{ok ? '满足' : '不满足'}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </>
            )}
          </div>

          {!loading && sections.length > 0 && (
            <SectionSelector
              sections={sections}
              selectedId={selectedSectionId}
              onSelect={setSelectedSectionId}
            />
          )}
        </div>

        <ConfigPanel config={detail?.config} title={detail?.title} loading={loading} />
      </div>
    </div>
  )
}
