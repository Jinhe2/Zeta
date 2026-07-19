import { useEffect, useRef, useState } from 'react'
import { api } from '../../../api/client'

export default function IedBaselineSettingContent({
  cognitionDeviceId,
  item,
  navigationEvent,
  showTable = true,
  showControls = true,
}) {
  const [settings, setSettings] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [comparing, setComparing] = useState(false)
  const [dialog, setDialog] = useState(null)
  const promptedSequence = useRef(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')
    api.listKnowledgeCognitionDeviceBaselineSettings(cognitionDeviceId)
      .then((items) => {
        if (!cancelled) setSettings(items)
      })
      .catch((err) => {
        if (!cancelled) setError(err.message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => { cancelled = true }
  }, [cognitionDeviceId, item.id])

  useEffect(() => {
    if (!showControls || navigationEvent?.source !== 'next' || promptedSequence.current === navigationEvent.sequence) return
    promptedSequence.current = navigationEvent.sequence
    setDialog({ type: 'prompt', message: '现在，请你尝试整定以下这些基准定值' })
  }, [navigationEvent?.sequence, navigationEvent?.source, showControls])

  const compare = async () => {
    setComparing(true)
    setError('')
    try {
      const result = await api.compareCognitionDeviceBaselineSettings(cognitionDeviceId)
      const failedItems = (result.items ?? []).filter((entry) => entry.equal === false || entry.matched === false)
      if (failedItems.length) {
        setDialog({
          type: 'result',
          message: failedItems.map((entry) => `${entry.description || entry.setting_ref || '该定值'} 还存在问题，请重新进行整定`).join('\n'),
        })
      } else {
        setDialog({ type: 'result', message: '定值整定正确，请继续学习' })
      }
    } catch (err) {
      setError(err.message || '定值比对失败，请稍后重试')
    } finally {
      setComparing(false)
    }
  }

  return (
    <div className="ied-baseline-setting">
      {showTable && loading && <p className="cabinet-section__paragraph">加载基准定值中…</p>}
      {showTable && error && <p className="cabinet-section__paragraph cabinet-section__paragraph--error">{error}</p>}
      {showTable && !loading && !error && settings.length === 0 && (
        <p className="cabinet-section__paragraph">该装置暂未配置基准定值，无法开始整定。</p>
      )}
      {showTable && !loading && settings.length > 0 && (
        <div className="ied-baseline-setting__table-wrap">
          <table className="ied-baseline-setting__table">
            <thead><tr><th>定值描述</th><th>基准值</th></tr></thead>
            <tbody>
              {settings.map((setting, index) => (
                <tr key={`${setting.description}-${index}`}><td>{setting.description || '—'}</td><td>{setting.baselineValue}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {showControls && <>
      <h3 className="cabinet-section__cognition-title">{item.title}</h3>
      <p className="cabinet-section__paragraph">{item.content}</p>
      {error && <p className="cabinet-section__paragraph cabinet-section__paragraph--error">{error}</p>}
      <button
        type="button"
        className="ied-baseline-setting__complete"
        disabled={loading || Boolean(error) || settings.length === 0 || comparing}
        onClick={compare}
      >
        {comparing ? '定值比对中…' : '整定完成'}
      </button>
      </>}
      {showControls && dialog && (
        <div className="pressboard-practice-dialog" role="dialog" aria-modal="true">
          <div className="pressboard-practice-dialog__mask" />
          <div className="pressboard-practice-dialog__panel">
            <p className="pressboard-practice-dialog__message" style={{ whiteSpace: 'pre-line' }}>{dialog.message}</p>
            <button type="button" className="pressboard-practice-dialog__btn" onClick={() => setDialog(null)}>确定</button>
          </div>
        </div>
      )}
    </div>
  )
}
