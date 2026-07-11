import { useEffect, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'

/**
 * 管理员从后台进入学员视图时，屏柜由 URL 显式指定；真实学员仍以设备绑定为准。
 */
export function useStudentCabinetId() {
  const { session } = useAuth()
  const [searchParams] = useSearchParams()

  const cabinetId = useMemo(() => {
    if (session?.role !== 'ADMIN') return null
    const fromUrl = Number(searchParams.get('cabinetId'))
    if (Number.isSafeInteger(fromUrl) && fromUrl > 0) return fromUrl
    const fromSession = Number(sessionStorage.getItem('zeta_admin_student_cabinet_id'))
    return Number.isSafeInteger(fromSession) && fromSession > 0 ? fromSession : null
  }, [searchParams, session?.role])

  useEffect(() => {
    if (session?.role === 'ADMIN' && cabinetId) {
      sessionStorage.setItem('zeta_admin_student_cabinet_id', String(cabinetId))
    }
  }, [cabinetId, session?.role])

  return cabinetId
}

export function resolveStudentCabinetId(tree, selectedCabinetId) {
  if (selectedCabinetId) return selectedCabinetId
  return tree?.cabinets?.[0]?.id ?? null
}
