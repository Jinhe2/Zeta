/**
 * 设备绑定 ID 管理。
 * 每台平板首次访问时自动生成 UUID，存储在 localStorage 中。
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

/** 获取当前设备的绑定 ID（自动生成并持久化） */
export function getDeviceBindId() {
  let id = localStorage.getItem(DEVICE_BIND_ID_KEY)
  if (!id) {
    id = generateUUID()
    localStorage.setItem(DEVICE_BIND_ID_KEY, id)
  }
  return id
}

/** 清除设备绑定 ID（重置设备） */
export function clearDeviceBindId() {
  localStorage.removeItem(DEVICE_BIND_ID_KEY)
}
