import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import GraphView from '../v4/components/GraphView'
import ConfigInfoPanel from '../v4/components/ConfigInfoPanel'
import SectionSelector from '../components/SectionSelector'
import { parseGraphJson } from '../v4/demo/parseGraphJson'
import { loadSample } from '../sampleLoader'
import { getProtectionDisplay } from '../data/protectionCatalog'
import { buildSectionSnapshots } from '../data/sectionSnapshots'
import { useAuth } from '../auth/AuthContext'
import '../App.css'
import './RolePages.css'

export default function StudentDiagramPage() {
  const { filename } = useParams<{ filename: string }>()
  const { logout } = useAuth()
  const [config, setConfig] = useState<Record<string, unknown> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [revision, setRevision] = useState(0)
  const [selectedSectionId, setSelectedSectionId] = useState<string | null>(null)

  const decodedFilename = filename ? decodeURIComponent(filename) : ''

  const display = getProtectionDisplay(
    decodedFilename,
    typeof config?.name === 'string' ? config.name : undefined,
  )

  useEffect(() => {
    document.title = display.title || '逻辑框图'
    return () => {
      document.title = '继电保护逻辑教学系统'
    }
  }, [display.title])

  useEffect(() => {
    if (!decodedFilename) {
      setError('未指定保护逻辑样本')
      setLoading(false)
      return
    }

    let cancelled = false
    setLoading(true)
    setError(null)
    setSelectedSectionId(null)

    loadSample(decodedFilename)
      .then((data) => {
        if (!cancelled) {
          setConfig(data)
          setRevision((r) => r + 1)
        }
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
  }, [decodedFilename])

  const graphData = useMemo(() => {
    if (!config) return null
    try {
      return parseGraphJson(config)
    } catch (err) {
      console.error('[student diagram]', err)
      return { error: err instanceof Error ? err.message : String(err) }
    }
  }, [config, revision])

  const sections = useMemo(() => {
    if (!graphData || 'error' in graphData) return []
    const nodeIds = graphData.nodes.map((n) => n.id)
    return buildSectionSnapshots(nodeIds, config)
  }, [graphData, config])

  useEffect(() => {
    if (sections.length === 0) {
      setSelectedSectionId(null)
      return
    }
    setSelectedSectionId((prev) =>
      prev && sections.some((s) => s.id === prev) ? prev : sections[0].id,
    )
  }, [sections])

  const nodeStates = useMemo(() => {
    if (!selectedSectionId) return null
    const section = sections.find((s) => s.id === selectedSectionId)
    return section?.states ?? null
  }, [sections, selectedSectionId])

  const diagramKey = `student-${revision}-${decodedFilename}`

  return (
    <div className="role-page role-page--student student-diagram">
      <header className="student-diagram__toolbar">
        <Link to="/student" className="student-diagram__back">
          ← 返回列表
        </Link>
        <h1 className="student-diagram__title">{display.title}</h1>
        <Link to="/login" className="role-page__btn student-diagram__logout" onClick={logout}>
          退出登录
        </Link>
      </header>

      {error && <div className="banner-error">{error}</div>}

      <div className="student-diagram__body v4-workspace">
        <div className="student-diagram__workspace">
          <main className="main main--v4 v4-workspace__diagram">
            {loading ? (
              <div className="diagram-empty">正在加载逻辑框图…</div>
            ) : !graphData ? (
              <div className="diagram-empty">配置加载失败</div>
            ) : 'error' in graphData ? (
              <div className="diagram-error">渲染失败：{graphData.error}</div>
            ) : (
              <GraphView
                key={diagramKey}
                data={graphData}
                showDevInfo={false}
                nodeStates={nodeStates}
              />
            )}
          </main>

          {!loading && sections.length > 0 && (
            <SectionSelector
              sections={sections}
              selectedId={selectedSectionId}
              onSelect={setSelectedSectionId}
            />
          )}
        </div>

        <ConfigInfoPanel
          config={config}
          configName={display.title}
          filename={decodedFilename}
          loading={loading}
        />
      </div>
    </div>
  )
}
