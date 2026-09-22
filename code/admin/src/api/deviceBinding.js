/**
 * 设备绑定 ID 管理。
 * Electron 优先使用主进程读取的原生设备标识；浏览器开发环境使用稳定的模拟 ID。
 */

const DEVICE_BIND_ID_KEY = 'zeta_device_bind_id'

function generateUUID() {
  // crypto.randomUUID() where available, fallback to manual
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

function getBrowserDeviceId() {
  let id = localStorage.getItem(DEVICE_BIND_ID_KEY)
  if (!id) {
    // 浏览器无法访问硬件设备 ID，使用带前缀的稳定模拟 ID进行开发调试。
    id = `browser-dev-${generateUUID()}`
    localStorage.setItem(DEVICE_BIND_ID_KEY, id)
  }
  return id
}

/** 获取当前设备的绑定 ID（原生设备标识优先，浏览器模拟 ID 兜底） */
export function getDeviceBindId() {
  const nativeId = window.electronAPI?.getDeviceId?.()
  return nativeId || getBrowserDeviceId()
}

/** 清除设备绑定 ID（重置设备） */
export function clearDeviceBindId() {
  localStorage.removeItem(DEVICE_BIND_ID_KEY)
}
