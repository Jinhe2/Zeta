import { useState, useRef, useCallback, useEffect } from 'react'
import { loadSampleList, loadSample, parseUploadedJson } from '../sampleLoader'

export function useDiagramConfig() {
  const [samples, setSamples] = useState([])
  const [config, setConfig] = useState(null)
  const [selectedSample, setSelectedSample] = useState('')
  const [configName, setConfigName] = useState('')
  const [configRevision, setConfigRevision] = useState(0)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)
  const fileInputRef = useRef(null)

  const applyConfig = useCallback((data, filename, name) => {
    setConfig(data)
    setConfigName(name || data.name || filename || '')
    setConfigRevision((r) => r + 1)
  }, [])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        const list = await loadSampleList()
        if (cancelled) return
        setSamples(list)
        if (list.length > 0) {
          setSelectedSample(list[0].filename)
          applyConfig(list[0].data, list[0].filename, list[0].name)
        }
      } catch (err) {
        if (!cancelled) setError(err.message)
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => { cancelled = true }
  }, [applyConfig])

  const loadSampleByName = useCallback(async (filename) => {
    if (!filename) return
    setError(null)
    setLoading(true)
    try {
      const cached = samples.find((s) => s.filename === filename)
      const data = cached?.data ?? (await loadSample(filename))
      setSelectedSample(filename)
      applyConfig(data, filename, data.name || filename)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [samples, applyConfig])

  const handleUpload = useCallback(async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    setError(null)
    setLoading(true)
    try {
      const data = await parseUploadedJson(file)
      setSelectedSample('')
      applyConfig(data, '', data.name || file.name)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
      e.target.value = ''
    }
  }, [applyConfig])

  return {
    samples,
    config,
    selectedSample,
    configName,
    configRevision,
    error,
    loading,
    fileInputRef,
    loadSampleByName,
    handleUpload,
  }
}
