import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { api, imageUrl } from '../../api/client'
import { ImageRegionViewer } from '../../components/ImageRegionEditor'
import { hasRegion, normalizeRegion } from '../../utils/imageRegionUtils'
import { resolveStudentCabinetId, useStudentCabinetId } from './studentCabinet'
import './TabletShell.css'
import './DrawingLearningPage.css'

const TYPE_LABELS = { BLUEPRINT: '蓝图', WHITEPRINT: '白图' }
const DISPLAY_TYPES = ['WHITEPRINT']

function makeStep(group, page, item, pageIndex, itemIndex) {
  return {
    key: `${group.id}:${page.id}:${item.id}`,
    group,
    page,
    item,
    pageIndex,
    itemIndex,
  }
}

export default function DrawingLearningPage() {
  const navigate = useNavigate()
  const { logout } = useAuth()
  const selectedCabinetId = useStudentCabinetId()
  const [groups, setGroups] = useState([])
  const [groupDetails, setGroupDetails] = useState({})
  const [activeGroupId, setActiveGroupId] = useState(null)
  const [currentStepKey, setCurrentStepKey] = useState(null)
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    async function loadGroups() {
      if (selectedCabinetId === undefined) {
        setLoading(true)
        return
      }

      setLoading(true)
      setError('')
      try {
        const tree = await api.getKnowledgeTree()
        const cabinetId = resolveStudentCabinetId(tree, selectedCabinetId)
        if (!cabinetId) throw new Error('未找到图纸学习数据')
        const data = await api.listKnowledgeDrawingGroups(cabinetId)
        if (!cancelled) {
          setGroups(data)
          setGroupDetails({})
          setActiveGroupId(null)
          setCurrentStepKey(null)
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message || '加载图纸学习失败')
          setGroups([])
          setGroupDetails({})
          setActiveGroupId(null)
          setCurrentStepKey(null)
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    loadGroups()
    return () => { cancelled = true }
  }, [selectedCabinetId])

  const loadGroupDetail = useCallback(async (groupId) => {
    if (!groupId) return null
    if (groupDetails[groupId]) return groupDetails[groupId]
    setDetailLoading(true)
    setError('')
    try {
      const detail = await api.getKnowledgeDrawingGroup(groupId)
      setGroupDetails((current) => ({ ...current, [groupId]: detail }))
      return detail
    } catch (err) {
      setError(err.message || '加载图纸分组失败')
      return null
    } finally {
      setDetailLoading(false)
    }
  }, [groupDetails])

  const sortedGroups = useMemo(() => {
    return DISPLAY_TYPES.flatMap((type) => groups.filter((group) => group.drawingType === type))
  }, [groups])

  const allSteps = useMemo(() => {
    const steps = []
    sortedGroups.forEach((group) => {
      const detail = groupDetails[group.id]
      detail?.pages?.forEach((page, pageIndex) => {
        page.items?.forEach((item, itemIndex) => {
          steps.push(makeStep(group, page, item, pageIndex, itemIndex))
        })
      })
    })
    return steps
  }, [groupDetails, sortedGroups])

  const currentStep = allSteps.find((step) => step.key === currentStepKey) ?? null
  const currentIndex = allSteps.findIndex((step) => step.key === currentStepKey)
  const activeDetail = activeGroupId ? groupDetails[activeGroupId] : null

  const startGroup = async (groupId) => {
    const detail = await loadGroupDetail(groupId)
    if (!detail) return
    setActiveGroupId(groupId)
    const group = groups.find((candidate) => candidate.id === groupId) ?? detail
    const firstPage = detail.pages?.[0]
    const firstItem = firstPage?.items?.[0]
    setCurrentStepKey(firstPage && firstItem ? makeStep(group, firstPage, firstItem, 0, 0).key : null)
  }

  const selectGroup = async (groupId) => {
    await startGroup(groupId)
  }

  const goPrevious = () => {
    if (currentIndex <= 0) return
    const nextStep = allSteps[currentIndex - 1]
    setActiveGroupId(nextStep.group.id)
    setCurrentStepKey(nextStep.key)
  }

  const goNext = async () => {
    if (currentIndex < 0) return
    if (currentIndex < allSteps.length - 1) {
      const nextStep = allSteps[currentIndex + 1]
      setActiveGroupId(nextStep.group.id)
      setCurrentStepKey(nextStep.key)
      return
    }
    const activeIndex = sortedGroups.findIndex((group) => group.id === activeGroupId)
    const nextUnloaded = sortedGroups.slice(activeIndex + 1).find((group) => !groupDetails[group.id])
    if (nextUnloaded) {
      await startGroup(nextUnloaded.id)
    }
  }

  const renderOverviewTable = (type) => {
    const rows = groups.filter((group) => group.drawingType === type)
    return (
      <section className="drawing-learning__overview-section">
        <h2>{TYPE_LABELS[type]}</h2>
        <table className="drawing-learning__table">
          <thead>
            <tr><th>分组</th><th>图纸数</th><th>认知条目数</th><th>操作</th></tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr><td colSpan={4}>暂无{TYPE_LABELS[type]}分组</td></tr>
            ) : rows.map((group) => (
              <tr key={group.id}>
                <td>{group.name}</td>
                <td>{group.pageCount}</td>
                <td>{group.cognitionItemCount}</td>
                <td><button type="button" onClick={() => startGroup(group.id)}>开始学习</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    )
  }

  const renderLearning = () => (
    <div className="drawing-learning__workspace">
      <aside className="drawing-learning__sidebar" aria-label="图纸分组">
        {DISPLAY_TYPES.map((type) => (
          <div key={type} className="drawing-learning__nav-block">
            <h2>{TYPE_LABELS[type]}</h2>
            {groups.filter((group) => group.drawingType === type).map((group) => (
              <button
                key={group.id}
                type="button"
                className={`drawing-learning__nav-btn${group.id === activeGroupId ? ' drawing-learning__nav-btn--active' : ''}`}
                onClick={() => selectGroup(group.id)}
              >
                <span>{group.name}</span>
                <small>{group.pageCount} 张</small>
              </button>
            ))}
          </div>
        ))}
      </aside>
      <section className="drawing-learning__media">
        {detailLoading && <p>加载中…</p>}
        {!detailLoading && currentStep && (
          <>
            {activeDetail?.pages?.length > 1 && (
              <div className="drawing-learning__page-tabs">
                {activeDetail.pages.map((page) => (
                  <button
                    key={page.id}
                    type="button"
                    className={page.id === currentStep.page.id ? 'drawing-learning__page-tab--active' : ''}
                    onClick={() => {
                      const firstItem = page.items?.[0]
                      if (!firstItem) return
                      const group = groups.find((candidate) => candidate.id === activeGroupId)
                      setCurrentStepKey(makeStep(group, page, firstItem, 0, 0).key)
                    }}
                  >
                    {page.title}
                  </button>
                ))}
              </div>
            )}
            <ImageRegionViewer
              imageUrl={imageUrl('drawing-page', currentStep.page.id)}
              region={hasRegion(currentStep.item) ? normalizeRegion(currentStep.item) : null}
              alt={currentStep.page.title}
            />
          </>
        )}
        {!detailLoading && !currentStep && (
          <p className="drawing-learning__empty">该分组暂无可学习的图纸认知条目。</p>
        )}
      </section>
      <aside className="drawing-learning__text">
        {currentStep ? (
          <>
            <div className="drawing-learning__text-body">
              <p className="drawing-learning__type">{TYPE_LABELS[currentStep.group.drawingType]} / {currentStep.group.name}</p>
              <h2>{currentStep.page.title}</h2>
              <h3>{currentStep.item.title}</h3>
              <p>{currentStep.item.content}</p>
            </div>
            <div className="drawing-learning__steps">
              <button type="button" disabled={currentIndex <= 0} onClick={goPrevious}>上一步</button>
              <button type="button" disabled={detailLoading || currentIndex < 0} onClick={goNext}>下一步</button>
            </div>
          </>
        ) : (
          <>
            <p>请选择左侧图纸分组开始学习。</p>
            <div className="drawing-learning__steps">
              <button type="button" disabled>上一步</button>
              <button type="button" disabled>下一步</button>
            </div>
          </>
        )}
      </aside>
    </div>
  )

  return (
    <div className="tablet-shell">
      <header className="tablet-shell__header">
        <div className="tablet-shell__header-left">
          <button type="button" className="tablet-shell__back" onClick={() => navigate('/student/modes/coach')}>← 返回上级</button>
          <button type="button" className="tablet-shell__home" onClick={() => navigate('/student')}>返回首页</button>
        </div>
        <h1>图纸学习</h1>
        <div className="tablet-shell__header-actions">
          <button type="button" className="tablet-shell__logout" onClick={async () => {
            await logout()
            navigate('/login', { replace: true })
          }}>退出登录</button>
        </div>
      </header>
      <main className="tablet-shell__main drawing-learning">
        {error && <div className="drawing-learning__error">{error}</div>}
        {loading ? (
          <p className="drawing-learning__loading">加载中…</p>
        ) : activeGroupId ? (
          renderLearning()
        ) : (
          <div className="drawing-learning__overview">
            {renderOverviewTable('WHITEPRINT')}
          </div>
        )}
      </main>
    </div>
  )
}
