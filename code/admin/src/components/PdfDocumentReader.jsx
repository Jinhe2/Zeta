import { useEffect, useRef, useState } from 'react'
import { Document, Page, pdfjs } from 'react-pdf'
import { TransformComponent, TransformWrapper } from 'react-zoom-pan-pinch'
import PdfWorker from 'pdfjs-dist/build/pdf.worker.min.mjs?worker'
import 'react-pdf/dist/Page/AnnotationLayer.css'
import 'react-pdf/dist/Page/TextLayer.css'
import './PdfDocumentReader.css'

// 直接交给 Vite 创建 Worker，避免 Electron file:// 页面触发 PDF.js 的动态 import fake worker。
pdfjs.GlobalWorkerOptions.workerPort = new PdfWorker()

export default function PdfDocumentReader({ fileUrl, title }) {
  const viewportRef = useRef(null)
  const [pageCount, setPageCount] = useState(0)
  const [pageNumber, setPageNumber] = useState(1)
  const [viewportSize, setViewportSize] = useState({ width: 320, height: 480 })
  const [pageAspect, setPageAspect] = useState(0.707)
  const [loadError, setLoadError] = useState('')

  useEffect(() => {
    const viewport = viewportRef.current
    if (!viewport) return undefined
    const updateSize = () => setViewportSize({ width: viewport.clientWidth, height: viewport.clientHeight })
    updateSize()
    const observer = new ResizeObserver(updateSize)
    observer.observe(viewport)
    return () => observer.disconnect()
  }, [])

  // 初始状态适应“整页”而非仅适应宽度，避免纵向 PDF 在 PC 上只露出半页。
  const pageWidth = Math.max(220, Math.min(
    viewportSize.width - 32,
    (viewportSize.height - 32) * pageAspect,
  ))

  return (
    <section className="pdf-document-reader" aria-label={`${title} PDF 阅读器`}>
      <TransformWrapper initialScale={1} minScale={1} maxScale={4} centerOnInit wheel={{ step: 0.18 }} doubleClick={{ mode: 'reset' }}>
        {({ zoomIn, zoomOut, resetTransform }) => {
          const changePage = (delta) => {
            setPageNumber((current) => {
              const next = Math.max(1, Math.min(pageCount || 1, current + delta))
              if (next !== current) window.setTimeout(resetTransform, 0)
              return next
            })
          }
          return <>
            <div className="pdf-document-reader__toolbar">
              <button type="button" onClick={() => changePage(-1)} disabled={pageNumber <= 1}>上一页</button>
              <label><span className="pdf-document-reader__sr-only">页码</span><input type="number" min="1" max={pageCount || 1} value={pageNumber} onChange={(event) => { const next = Math.max(1, Math.min(pageCount || 1, Number(event.target.value) || 1)); setPageNumber(next); window.setTimeout(resetTransform, 0) }} /></label>
              <span className="pdf-document-reader__page-count">/ {pageCount || '—'}</span>
              <button type="button" onClick={() => changePage(1)} disabled={!pageCount || pageNumber >= pageCount}>下一页</button>
              <span className="pdf-document-reader__divider" />
              <button type="button" aria-label="缩小" onClick={() => zoomOut()}>−</button>
              <button type="button" aria-label="放大" onClick={() => zoomIn()}>＋</button>
              <button type="button" className="pdf-document-reader__fit" onClick={() => resetTransform()}>适应整页</button>
            </div>
            <div ref={viewportRef} className="pdf-document-reader__viewport" tabIndex="0" onKeyDown={(event) => {
              if (event.key === 'ArrowLeft' || event.key === 'PageUp') { event.preventDefault(); changePage(-1) }
              if (event.key === 'ArrowRight' || event.key === 'PageDown') { event.preventDefault(); changePage(1) }
            }} aria-label="PDF 内容。可用左右方向键翻页，触屏可双指缩放和单指拖动。">
              <TransformComponent wrapperClass="pdf-document-reader__transform-wrapper" contentClass="pdf-document-reader__transform-content">
                <Document file={fileUrl} loading={<p className="pdf-document-reader__state">正在加载 PDF…</p>} error={<p className="pdf-document-reader__state pdf-document-reader__state--error">{loadError || 'PDF 加载失败'}</p>} onLoadSuccess={({ numPages }) => { setPageCount(numPages); setPageNumber(1) }} onLoadError={(error) => setLoadError(error.message || 'PDF 加载失败')}>
                  <Page pageNumber={pageNumber} width={pageWidth} renderTextLayer renderAnnotationLayer onLoadSuccess={(page) => {
                    const viewport = page.getViewport({ scale: 1 })
                    setPageAspect(viewport.width / viewport.height)
                  }} />
                </Document>
              </TransformComponent>
            </div>
          </>
        }}
      </TransformWrapper>
    </section>
  )
}
