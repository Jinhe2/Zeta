import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { api } from '../../api/client'
import StructureCognitionContent from './cabinet/StructureCognitionContent'
import DeviceCognitionContent from './cabinet/DeviceCognitionContent'
import PlateCognitionContent from './cabinet/PlateCognitionContent'
import TerminalCognitionContent from './cabinet/TerminalCognitionContent'
import { resolveStudentCabinetId, useStudentCabinetId } from './studentCabinet'
import './TabletShell.css'
import './CabinetCognitionPage.css'

const SECTIONS = [
  { id: 'structure', label: '结构认知' },
  { id: 'device', label: '设备认知' },
  { id: 'plate', label: '压板认知' },
  { id: 'terminal', label: '端子排认知' },
  { id: 'apparatus', label: '装置认知' },
]

const DEVICE_SECTION_TYPES = {
  device: ['IED', 'OTHER_DEVICE'],
  plate: 'PLATE_GROUP',
  terminal: 'TERMINAL_GROUP',
}

function fallbackPage(sectionId) {
  return {
    sectionId,
    key: `${sectionId}:fallback`,
    fallback: true,
  }
}

function pageKey(page) {
  if (page.key) return page.key
  return [
    page.sectionId,
    page.cabinetItemId ?? 'none',
    page.deviceId ?? 'none',
    page.slideIndex ?? 0,
    page.kind ?? 'display',
  ].join(':')
}

function withKey(page) {
  return {
    ...page,
    key: pageKey(page),
  }
}

async function buildDeviceSectionPages(sectionId, cabinetItems) {
  const deviceType = DEVICE_SECTION_TYPES[sectionId]
  const pages = []

  const itemDevicePairs = await Promise.all(
    cabinetItems.map(async (item) => {
      const devices = await api.listKnowledgeCognitionDevices(item.id)
      return {
        item,
        devices: devices.filter((device) =>
          Array.isArray(deviceType) ? deviceType.includes(device.deviceType) : device.deviceType === deviceType,
        ),
      }
    }),
  )

  for (const { item, devices } of itemDevicePairs) {
    const deviceItemsPairs = await Promise.all(
      devices.map(async (device) => {
        const displayItems = await api.listKnowledgeCognitionDeviceDisplayItems(device.id)
        return { device, displayItems }
      }),
    )

    for (const { device, displayItems } of deviceItemsPairs) {
      if (displayItems.length > 0) {
        displayItems.forEach((displayItem, slideIndex) => {
          pages.push(withKey({
            sectionId,
            cabinetItemId: item.id,
            deviceId: device.id,
            displayItemId: displayItem.id,
            slideIndex,
            kind: 'display',
          }))
        })
      } else if (sectionId !== 'plate') {
        pages.push(withKey({
          sectionId,
          cabinetItemId: item.id,
          deviceId: device.id,
          slideIndex: 0,
          kind: 'empty',
        }))
      }

      if (sectionId === 'plate') {
        pages.push(withKey({
          sectionId,
          cabinetItemId: item.id,
          deviceId: device.id,
          slideIndex: displayItems.length,
          kind: 'status',
        }))
      }
    }
  }

  return pages.length > 0 ? pages : [fallbackPage(sectionId)]
}

export default function CabinetCognitionPage() {
  const navigate = useNavigate()
  const { logout } = useAuth()
  const selectedCabinetId = useStudentCabinetId()
  const [activeSection, setActiveSection] = useState(SECTIONS[0].id)
  const [navigationPages, setNavigationPages] = useState(SECTIONS.map((section) => fallbackPage(section.id)))
  const [currentPageKey, setCurrentPageKey] = useState(pageKey(fallbackPage(SECTIONS[0].id)))
  const [pageNavigationEvent, setPageNavigationEvent] = useState({
    pageKey: pageKey(fallbackPage(SECTIONS[0].id)),
    source: 'initial',
    sequence: 0,
  })
  const [navigationLoading, setNavigationLoading] = useState(true)
  const [navigationError, setNavigationError] = useState(null)
  const currentSection = SECTIONS.find((s) => s.id === activeSection)
  const currentPage = navigationPages.find((page) => page.key === currentPageKey)
    ?? navigationPages.find((page) => page.sectionId === activeSection)
    ?? navigationPages[0]

  useEffect(() => {
    let cancelled = false

    async function loadNavigationPages() {
      setNavigationLoading(true)
      setNavigationError(null)
      try {
        const tree = await api.getKnowledgeTree()
        const cabinetId = resolveStudentCabinetId(tree, selectedCabinetId)
        if (!cabinetId) {
          throw new Error('未找到屏柜学习数据')
        }

        const cabinetItems = await api.listKnowledgeCabinetDisplayItems(cabinetId)
        const structurePages = cabinetItems.length > 0
          ? cabinetItems.map((item) => withKey({
            sectionId: 'structure',
            cabinetItemId: item.id,
            kind: 'display',
          }))
          : [fallbackPage('structure')]

        const [devicePages, platePages, terminalPages] = await Promise.all([
          buildDeviceSectionPages('device', cabinetItems),
          buildDeviceSectionPages('plate', cabinetItems),
          buildDeviceSectionPages('terminal', cabinetItems),
        ])

        const nextPages = [
          ...structurePages,
          ...devicePages,
          ...platePages,
          ...terminalPages,
          fallbackPage('apparatus'),
        ]

        if (!cancelled) {
          setNavigationPages(nextPages)
        }
      } catch (err) {
        if (!cancelled) {
          setNavigationError(err.message)
          setNavigationPages(SECTIONS.map((section) => fallbackPage(section.id)))
        }
      } finally {
        if (!cancelled) setNavigationLoading(false)
      }
    }

    loadNavigationPages()
    return () => {
      cancelled = true
    }
  }, [selectedCabinetId])

  const currentPageIndex = useMemo(
    () => navigationPages.findIndex((page) => page.key === currentPage?.key),
    [currentPage?.key, navigationPages],
  )

  const applyPage = useCallback((page, source = 'direct') => {
    if (!page) return
    setActiveSection(page.sectionId)
    setCurrentPageKey(page.key)
    setPageNavigationEvent((prev) => ({
      pageKey: page.key,
      source,
      sequence: prev.sequence + 1,
    }))
  }, [])

  const requestPage = useCallback((partialPage) => {
    const target = navigationPages.find((page) => {
      if (page.sectionId !== partialPage.sectionId) return false
      if (partialPage.cabinetItemId != null && page.cabinetItemId !== partialPage.cabinetItemId) return false
      if (partialPage.deviceId != null && page.deviceId !== partialPage.deviceId) return false
      if (partialPage.slideIndex != null && page.slideIndex !== partialPage.slideIndex) return false
      if (partialPage.kind != null && page.kind !== partialPage.kind) return false
      return true
    }) ?? navigationPages.find((page) => page.sectionId === partialPage.sectionId)

    applyPage(target)
  }, [applyPage, navigationPages])

  const handleSectionSelect = (sectionId) => {
    const nextPage = navigationPages.find((page) => page.sectionId === sectionId) ?? fallbackPage(sectionId)
    applyPage(nextPage, 'section')
  }

  const goPrevious = () => {
    if (currentPageIndex <= 0) return
    applyPage(navigationPages[currentPageIndex - 1], 'previous')
  }

  const goNext = () => {
    if (currentPageIndex < 0 || currentPageIndex >= navigationPages.length - 1) return
    applyPage(navigationPages[currentPageIndex + 1], 'next')
  }

  const renderSectionContent = () => {
    if (activeSection === 'structure') {
      return <StructureCognitionContent navigationTarget={currentPage} onPageChange={requestPage} />
    }
    if (activeSection === 'device') {
      return <DeviceCognitionContent navigationTarget={currentPage} onPageChange={requestPage} />
    }
    if (activeSection === 'plate') {
      return (
        <PlateCognitionContent
          navigationTarget={currentPage}
          navigationEvent={pageNavigationEvent}
          onPageChange={requestPage}
        />
      )
    }
    if (activeSection === 'terminal') {
      return (
        <TerminalCognitionContent
          navigationTarget={currentPage}
          navigationEvent={pageNavigationEvent}
          onPageChange={requestPage}
        />
      )
    }
    return (
      <div className="cabinet-page__content-placeholder">
        <h2>{currentSection?.label}</h2>
        <p>内容开发中，后续逐步补充。</p>
      </div>
    )
  }

  return (
    <div className="tablet-shell">
      <header className="tablet-shell__header">
        <div className="tablet-shell__header-left">
          <button type="button" className="tablet-shell__back" onClick={() => navigate('/student/modes/coach')}>
            ← 返回上级
          </button>
          <button type="button" className="tablet-shell__home" onClick={() => navigate('/student')}>
            返回首页
          </button>
        </div>
        <h1>屏柜学习</h1>
        <div className="tablet-shell__header-actions">
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
        </div>
      </header>

      <main className="tablet-shell__main tablet-shell__main--cabinet">
        <div className="cabinet-page__layout">
          <nav className="cabinet-page__nav" aria-label="屏柜学习分类">
            {SECTIONS.map((section) => (
              <button
                key={section.id}
                type="button"
                className={`cabinet-page__nav-btn${activeSection === section.id ? ' cabinet-page__nav-btn--active' : ''}`}
                onClick={() => handleSectionSelect(section.id)}
              >
                {section.label}
              </button>
            ))}
          </nav>

          <section className="cabinet-page__content" aria-live="polite">
            {navigationError && (
              <p className="cabinet-page__nav-error">{navigationError}</p>
            )}
            {renderSectionContent()}
            <div className="cabinet-page__step-actions" aria-label="屏柜学习步骤导航">
              <button
                type="button"
                className="cabinet-page__step-btn"
                disabled={navigationLoading || currentPageIndex <= 0}
                onClick={goPrevious}
              >
                上一步
              </button>
              <button
                type="button"
                className="cabinet-page__step-btn cabinet-page__step-btn--primary"
              disabled={navigationLoading || currentPageIndex < 0 || currentPageIndex >= navigationPages.length - 1}
              onClick={goNext}
            >
              下一步
            </button>
          </div>
        </section>
        </div>
      </main>
    </div>
  )
}
