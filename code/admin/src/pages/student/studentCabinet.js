import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { api } from '../../api/client'
import { getDeviceBindId } from '../../api/deviceBinding'

const BINDING_REFRESH_INTERVAL = 15000

/**
 * 管理员从后台进入学员视图时，屏柜由 URL 显式指定；真实学员仍以设备绑定为准。
 */
export function useStudentCabinetId() {
  const { session } = useAuth()
  const [searchParams] = useSearchParams()
  const [boundCabinetId, setBoundCabinetId] = useState(undefined)

  const refreshBoundCabinetId = useCallback(async () => {
    const binding = await api.checkBinding(getDeviceBindId())
    const nextCabinetId = Number(binding?.cabinetId)
    return Number.isSafeInteger(nextCabinetId) && nextCabinetId > 0 ? nextCabinetId : null
  }, [])

  const adminCabinetId = useMemo(() => {
    if (session?.role !== 'ADMIN') return null
    const fromUrl = Number(searchParams.get('cabinetId'))
    if (Number.isSafeInteger(fromUrl) && fromUrl > 0) return fromUrl
    const fromSession = Number(sessionStorage.getItem('zeta_admin_student_cabinet_id'))
    return Number.isSafeInteger(fromSession) && fromSession > 0 ? fromSession : null
  }, [searchParams, session?.role])

  useEffect(() => {
    if (session?.role === 'ADMIN' && adminCabinetId) {
      sessionStorage.setItem('zeta_admin_student_cabinet_id', String(adminCabinetId))
    }
  }, [adminCabinetId, session?.role])

  useEffect(() => {
    if (session?.role !== 'STUDENT') {
      queueMicrotask(() => setBoundCabinetId(undefined))
      return undefined
    }

    let cancelled = false
    queueMicrotask(() => {
      if (!cancelled) setBoundCabinetId(undefined)
    })

    refreshBoundCabinetId()
      .then((nextCabinetId) => {
        if (cancelled) return
        setBoundCabinetId(nextCabinetId)
      })
      .catch(() => {
        if (!cancelled) setBoundCabinetId(null)
      })

    const timer = window.setInterval(() => {
      if (document.hidden) return
      refreshBoundCabinetId()
        .then((nextCabinetId) => {
          if (!cancelled) setBoundCabinetId(nextCabinetId)
        })
        .catch(() => {
          if (!cancelled) setBoundCabinetId(null)
        })
    }, BINDING_REFRESH_INTERVAL)

    return () => {
      cancelled = true
      window.clearInterval(timer)
    }
  }, [refreshBoundCabinetId, session?.role])

  if (session?.role === 'ADMIN') return adminCabinetId
  if (session?.role === 'STUDENT') return boundCabinetId
  return null
}

export function resolveStudentCabinetId(tree, selectedCabinetId) {
  if (selectedCabinetId === undefined) return null
  if (selectedCabinetId) return selectedCabinetId
  return tree?.cabinets?.[0]?.id ?? null
}
