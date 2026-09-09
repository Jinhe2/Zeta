import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { api } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import { resolveStudentCabinetId, useStudentCabinetId } from './studentCabinet'

const CoachLearningAvailabilityContext = createContext(null)

function cabinetRequestKey(selectedCabinetId) {
  if (selectedCabinetId === undefined) return 'pending'
  return selectedCabinetId ? `cabinet:${selectedCabinetId}` : 'fallback'
}

function availabilityErrorMessage(error) {
  const message = error?.message
  return message && /[\u4e00-\u9fff]/.test(message)
    ? message
    : '加载学习模块失败，请检查网络后重试'
}

export function CoachLearningAvailabilityProvider({ children }) {
  const { session } = useAuth()
  const selectedCabinetId = useStudentCabinetId()
  const requestKey = cabinetRequestKey(selectedCabinetId)
  const [retrySequence, setRetrySequence] = useState(0)
  const [result, setResult] = useState({
    requestKey: null,
    status: 'loading',
    cabinetId: null,
    hasVirtualCircuit: null,
    error: null,
  })

  useEffect(() => {
    if (selectedCabinetId === undefined) return undefined

    let cancelled = false

    async function loadAvailability() {
      try {
        let cabinetId = selectedCabinetId
        if (!cabinetId && session?.role === 'ADMIN') {
          const tree = await api.getKnowledgeTree()
          cabinetId = resolveStudentCabinetId(tree, selectedCabinetId)
        }
        if (!cabinetId) throw new Error('未找到当前绑定屏柜')

        const devices = await api.listVirtualCircuitDevices(cabinetId)
        if (!cancelled) {
          setResult({
            requestKey,
            status: 'ready',
            cabinetId,
            hasVirtualCircuit: Array.isArray(devices) && devices.length > 0,
            error: null,
          })
        }
      } catch (error) {
        if (!cancelled) {
          setResult({
            requestKey,
            status: 'error',
            cabinetId: selectedCabinetId || null,
            hasVirtualCircuit: null,
            error: availabilityErrorMessage(error),
          })
        }
      }
    }

    loadAvailability()
    return () => {
      cancelled = true
    }
  }, [requestKey, retrySequence, selectedCabinetId, session?.role])

  const retry = useCallback(() => {
    setResult((current) => ({ ...current, requestKey: null, status: 'loading', error: null }))
    setRetrySequence((current) => current + 1)
  }, [])

  const value = useMemo(() => {
    if (result.requestKey !== requestKey) {
      return {
        status: 'loading',
        cabinetId: selectedCabinetId || null,
        hasVirtualCircuit: null,
        error: null,
        retry,
      }
    }
    return { ...result, retry }
  }, [requestKey, result, retry, selectedCabinetId])

  return (
    <CoachLearningAvailabilityContext.Provider value={value}>
      {children}
    </CoachLearningAvailabilityContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useCoachLearningAvailability() {
  const value = useContext(CoachLearningAvailabilityContext)
  if (!value) throw new Error('useCoachLearningAvailability 必须在能力判断 Provider 内使用')
  return value
}

export function PhysicalCircuitRouteGuard() {
  const { status, hasVirtualCircuit, error, retry } = useCoachLearningAvailability()

  if (status === 'loading') {
    return <div role="status">正在判断当前装置的学习方式…</div>
  }

  if (status === 'error') {
    return (
      <div role="alert">
        <p>{error}</p>
        <button type="button" onClick={retry}>重新加载</button>
      </div>
    )
  }

  if (hasVirtualCircuit) {
    return (
      <Navigate
        to="/student/modes/coach"
        replace
        state={{ coachNotice: '当前装置使用虚回路，请进入虚回路学习' }}
      />
    )
  }

  return <Outlet />
}
