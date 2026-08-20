/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { api } from '../../api/client'
import '../admin/business/UsersPage.css'

const BASE_PATH = '/teacher/baselines'

export default function TeacherBaselinePage() {
  const { cabinetId, deviceId } = useParams()

  if (deviceId) return <DeviceBaselinePage deviceId={Number(deviceId)} />
  if (cabinetId) return <CabinetDevicePage cabinetId={Number(cabinetId)} />
  return <CabinetListPage />
}

function CabinetListPage() {
  const [cabinets, setCabinets] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setCabinets(await api.listKnowledgeCabinets())
    } catch (err) {
      setError(err.message || '加载屏柜列表失败')
      setCabinets([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  return (
    <div className="users-page">
      <h2 className="users-page__title">基准管理</h2>
      <p className="users-page__desc">选择屏柜与装置，维护装置、基础逻辑或组合逻辑的定值清单和软压板基准清单。</p>
      {error && <div className="users-page__error">{error}</div>}
      {loading ? <p className="users-page__loading">加载中…</p> : (
        <div className="users-page__table-wrap">
          <table className="users-page__table">
            <thead><tr><th>屏柜名称</th><th>编码</th><th>描述</th><th>装置数量</th><th>操作</th></tr></thead>
            <tbody>{cabinets.length === 0 ? <tr><td colSpan={5} className="users-page__empty-cell">暂无屏柜数据</td></tr> : cabinets.map((cabinet) => (
              <tr key={cabinet.id}>
                <td>{cabinet.name}</td>
                <td>{cabinet.code}</td>
                <td>{cabinet.description || '—'}</td>
                <td>{cabinet.deviceCount}</td>
                <td><Link className="users-page__link" to={`${BASE_PATH}/cabinets/${cabinet.id}`}>进入</Link></td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function CabinetDevicePage({ cabinetId }) {
  const [cabinet, setCabinet] = useState(null)
  const [devices, setDevices] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    if (!Number.isSafeInteger(cabinetId) || cabinetId <= 0) return
    setLoading(true)
    setError('')
    try {
      const [cabinetData, deviceData] = await Promise.all([
        api.getKnowledgeCabinet(cabinetId),
        api.listKnowledgeCabinetDevices(cabinetId),
      ])
      setCabinet(cabinetData)
      setDevices(deviceData)
    } catch (err) {
      setError(err.message || '加载装置列表失败')
      setCabinet(null)
      setDevices([])
    } finally {
      setLoading(false)
    }
  }, [cabinetId])

  useEffect(() => { load() }, [load])

  if (!Number.isSafeInteger(cabinetId) || cabinetId <= 0) return <Navigate to={BASE_PATH} replace />

  return (
    <div className="users-page">
      <div className="users-page__header">
        <div>
          <p className="users-page__breadcrumb"><Link to={BASE_PATH}>基准管理</Link><span> / </span><span>{cabinet?.name || '装置列表'}</span></p>
          <h2 className="users-page__title">{cabinet ? `${cabinet.name} — 装置` : '装置'}</h2>
          <p className="users-page__desc">装置级基准会作为基础逻辑和组合逻辑的默认基准。</p>
        </div>
      </div>
      {error && <div className="users-page__error">{error}</div>}
      {loading ? <p className="users-page__loading">加载中…</p> : (
        <div className="users-page__table-wrap">
          <table className="users-page__table">
            <thead><tr><th>装置名称</th><th>编码</th><th>描述</th><th>逻辑数</th><th>操作</th></tr></thead>
            <tbody>{devices.length === 0 ? <tr><td colSpan={5} className="users-page__empty-cell">暂无装置</td></tr> : devices.map((device) => (
              <tr key={device.id}>
                <td>{device.name}</td>
                <td>{device.code}</td>
                <td>{device.description || '—'}</td>
                <td>{device.logicCount}</td>
                <td className="users-page__actions">
                  <Link className="users-page__link" to={`${BASE_PATH}/devices/${device.id}`}>逻辑清单</Link>
                  <Link className="users-page__link" to={`${BASE_PATH}/devices/${device.id}/settings`}>装置定值</Link>
                  <Link className="users-page__link" to={`${BASE_PATH}/devices/${device.id}/soft-pressboards`}>装置软压板</Link>
                </td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function DeviceBaselinePage({ deviceId }) {
  const [device, setDevice] = useState(null)
  const [logics, setLogics] = useState([])
  const [groups, setGroups] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    if (!Number.isSafeInteger(deviceId) || deviceId <= 0) return
    setLoading(true)
    setError('')
    try {
      const [deviceData, logicData, groupData] = await Promise.all([
        api.getKnowledgeDevice(deviceId),
        api.listKnowledgeDeviceProtectionLogics(deviceId),
        api.listKnowledgeLogicGroups(deviceId),
      ])
      setDevice(deviceData)
      setLogics(logicData)
      setGroups(groupData)
    } catch (err) {
      setError(err.message || '加载逻辑列表失败')
      setDevice(null)
      setLogics([])
      setGroups([])
    } finally {
      setLoading(false)
    }
  }, [deviceId])

  useEffect(() => { load() }, [load])

  if (!Number.isSafeInteger(deviceId) || deviceId <= 0) return <Navigate to={BASE_PATH} replace />

  return (
    <div className="users-page">
      <div className="users-page__header">
        <div>
          <p className="users-page__breadcrumb">
            <Link to={BASE_PATH}>基准管理</Link>
            {device && <><span> / </span><Link to={`${BASE_PATH}/cabinets/${device.cabinetId}`}>{device.cabinetName}</Link><span> / </span><span>{device.name}</span></>}
          </p>
          <h2 className="users-page__title">{device ? `${device.name} — 基准清单` : '基准清单'}</h2>
          <p className="users-page__desc">可配置装置级默认基准，也可为单个基础逻辑或组合逻辑配置独立基准。</p>
        </div>
      </div>
      {error && <div className="users-page__error">{error}</div>}
      {loading ? <p className="users-page__loading">加载中…</p> : (
        <>
          <Section title="装置级默认基准">
            <tr>
              <td>{device?.name || '当前装置'}</td>
              <td>装置级</td>
              <td className="users-page__actions">
                <Link className="users-page__link" to={`${BASE_PATH}/devices/${deviceId}/settings`}>定值清单</Link>
                <Link className="users-page__link" to={`${BASE_PATH}/devices/${deviceId}/soft-pressboards`}>软压板基准</Link>
              </td>
            </tr>
          </Section>
          <Section title="基础逻辑独立基准" empty={logics.length === 0 ? '暂无基础逻辑' : ''}>
            {logics.map((logic) => (
              <tr key={logic.id}>
                <td>{logic.title}</td>
                <td>{logic.code || '—'}</td>
                <td className="users-page__actions">
                  <Link className="users-page__link" to={`${BASE_PATH}/logics/${logic.id}/settings`}>独立定值</Link>
                  <Link className="users-page__link" to={`${BASE_PATH}/logics/${logic.id}/soft-pressboards`}>独立软压板</Link>
                </td>
              </tr>
            ))}
          </Section>
          <Section title="组合逻辑独立基准" empty={groups.length === 0 ? '暂无组合逻辑' : ''}>
            {groups.map((group) => (
              <tr key={group.id}>
                <td>{group.name}</td>
                <td>{group.memberCount} 个成员</td>
                <td className="users-page__actions">
                  <Link className="users-page__link" to={`${BASE_PATH}/groups/${group.id}/settings`}>独立定值</Link>
                  <Link className="users-page__link" to={`${BASE_PATH}/groups/${group.id}/soft-pressboards`}>独立软压板</Link>
                </td>
              </tr>
            ))}
          </Section>
        </>
      )}
    </div>
  )
}

function Section({ title, empty = '', children }) {
  return (
    <section className="users-page__table-wrap" style={{ marginBottom: 20 }}>
      <h3 className="users-page__section-title">{title}</h3>
      <table className="users-page__table">
        <thead><tr><th>名称</th><th>范围</th><th>操作</th></tr></thead>
        <tbody>{empty ? <tr><td colSpan={3} className="users-page__empty-cell">{empty}</td></tr> : children}</tbody>
      </table>
    </section>
  )
}
