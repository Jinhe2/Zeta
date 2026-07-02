const ACCESS_TOKEN_KEY = 'zeta_access_token'
const REFRESH_TOKEN_KEY = 'zeta_refresh_token'

/**
 * API 基地址，优先级：
 *  1. Electron 运行时配置（window.electronAPI.getSettings().apiBaseUrl）
 *  2. Vite 编译时环境变量（VITE_API_BASE_URL）
 *  3. 空字符串（开发环境，走 Vite proxy）
 */
const BASE = window.electronAPI?.getSettings?.()?.apiBaseUrl
          || import.meta.env.VITE_API_BASE_URL
          || ''

/** 拼接完整 API URL：开发走代理，生产直连 */
export function apiUrl(path) {
  return BASE + path
}

/** 将后端返回的图片相对路径转为完整 URL */
export function imageUrl(path) {
  if (!path) return ''
  if (path.startsWith('http://') || path.startsWith('https://') || path.startsWith('data:')) return path
  return BASE + path
}

let refreshPromise = null

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setTokens(accessToken, refreshToken) {
  if (accessToken) {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  } else {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
  }
  if (refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  } else {
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }
}

export function clearTokens() {
  setTokens(null, null)
}

async function parseResponse(res) {
  const text = await res.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return { message: text }
  }
}

/** 登录/刷新等鉴权接口：不自动续期 */
async function authRequest(path, options = {}) {
  const res = await fetch(apiUrl(path), {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
  })
  const data = await parseResponse(res)
  if (!res.ok) {
    throw new Error(data?.message || `请求失败 (${res.status})`)
  }
  return data
}

async function refreshAccessToken() {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new Error('登录已过期，请重新登录')
  }
  const data = await authRequest('/api/auth/refresh', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  })
  setTokens(data.accessToken, data.refreshToken)
  return data
}

async function ensureRefreshed() {
  if (!refreshPromise) {
    refreshPromise = refreshAccessToken().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

async function request(path, options = {}, retried = false) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }
  const accessToken = getAccessToken()
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }

  const res = await fetch(apiUrl(path), { ...options, headers })
  const data = await parseResponse(res)

  if (res.status === 401 && !retried && getRefreshToken() && !path.includes('/api/auth/')) {
    await ensureRefreshed()
    return request(path, options, true)
  }

  if (!res.ok) {
    throw new Error(data?.message || `请求失败 (${res.status})`)
  }
  return data
}

async function uploadRequest(path, formData, retried = false) {
  const headers = {}
  const accessToken = getAccessToken()
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`
  }

  const res = await fetch(apiUrl(path), { method: 'POST', headers, body: formData })
  const data = await parseResponse(res)

  if (res.status === 401 && !retried && getRefreshToken() && !path.includes('/api/auth/')) {
    await ensureRefreshed()
    return uploadRequest(path, formData, true)
  }

  if (!res.ok) {
    throw new Error(data?.message || `请求失败 (${res.status})`)
  }
  return data
}

export const api = {
  login(username, password) {
    return authRequest('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    })
  },

  refresh() {
    return refreshAccessToken()
  },

  me() {
    return request('/api/auth/me')
  },

  logout() {
    const refreshToken = getRefreshToken()
    if (!refreshToken) return Promise.resolve()
    return authRequest('/api/auth/logout', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    })
  },

  changePassword(oldPassword, newPassword) {
    return request('/api/auth/change-password', {
      method: 'POST',
      body: JSON.stringify({ oldPassword, newPassword }),
    })
  },

  listProtectionLogics() {
    return request('/api/protection-logics')
  },

  getKnowledgeTree() {
    return request('/api/knowledge/tree')
  },

  listKnowledgeCabinets() {
    return request('/api/knowledge/cabinets')
  },

  getKnowledgeCabinet(id) {
    return request(`/api/knowledge/cabinets/${id}`)
  },

  getKnowledgeDevice(id) {
    return request(`/api/knowledge/devices/${id}`)
  },

  listKnowledgeDeviceProtectionLogics(deviceId) {
    return request(`/api/knowledge/devices/${deviceId}/protection-logics`)
  },

  listKnowledgeDeviceDisplayItems(deviceId) {
    return request(`/api/knowledge/devices/${deviceId}/display-items`)
  },

  listKnowledgeCabinetDisplayItems(cabinetId) {
    return request(`/api/knowledge/cabinets/${cabinetId}/display-items`)
  },

  getProtectionLogic(id) {
    return request(`/api/protection-logics/${id}`)
  },

  getSections(id) {
    return request(`/api/protection-logics/${id}/sections`)
  },

  triggerExperiment(logicId) {
    return request(`/api/protection-logics/${logicId}/snapshots`, { method: 'POST' })
  },

  importSnapshotJson(logicId, snapshotJson) {
    return request(`/api/protection-logics/${logicId}/snapshots/import`, {
      method: 'POST',
      body: JSON.stringify({ snapshotJson }),
    })
  },

  listMySnapshots() {
    return request('/api/snapshots')
  },

  listSnapshotsByLogic(logicId) {
    return request(`/api/snapshots?logicId=${logicId}`)
  },

  getSnapshotSections(snapshotId) {
    return request(`/api/snapshots/${snapshotId}/sections`)
  },

  getSnapshotDetail(snapshotId) {
    return request(`/api/snapshots/${snapshotId}`)
  },

  // ── Monitor 命令交互 ──────────────────────────────────

  triggerPressboardStatus(cabinetId) {
    return request('/api/monitor/commands/pressboard-status', {
      method: 'POST',
      body: JSON.stringify({ cabinetId }),
    })
  },

  getPressboardStatus(cabinetId) {
    return request(`/api/monitor/pressboard-status/${cabinetId}`)
  },

  triggerTerminalStatus(cabinetId) {
    return request('/api/monitor/commands/terminal-status', {
      method: 'POST',
      body: JSON.stringify({ cabinetId }),
    })
  },

  getTerminalStatus(cabinetId) {
    return request(`/api/monitor/terminal-status/${cabinetId}`)
  },

  listHardPressboards(cabinetId) {
    return request(`/api/hard-pressboards?cabinetId=${cabinetId}`)
  },

  listTerminalStrips(cabinetId) {
    return request(`/api/terminals/strips?cabinetId=${cabinetId}`)
  },

  listTerminals(cabinetId) {
    return request(`/api/terminals?cabinetId=${cabinetId}`)
  },

  listUsers(role) {
    if (!role) {
      return Promise.reject(new Error('缺少用户角色参数'))
    }
    return request(`/api/users?role=${encodeURIComponent(role)}`)
  },

  getUser(id) {
    return request(`/api/users/${id}`)
  },

  createUser(payload) {
    return request('/api/users', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  updateUser(id, payload) {
    return request(`/api/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  resetUserPassword(id, password) {
    return request(`/api/users/${id}/password`, {
      method: 'PUT',
      body: JSON.stringify({ password }),
    })
  },

  deleteUser(id) {
    return request(`/api/users/${id}`, { method: 'DELETE' })
  },

  listCabinetDisplayItems(cabinetId) {
    return request(`/api/cabinets/${cabinetId}/display-items`)
  },

  createCabinetDisplayItem(cabinetId, payload) {
    return request(`/api/cabinets/${cabinetId}/display-items`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  updateCabinetDisplayItem(id, payload) {
    return request(`/api/admin/cabinet-display-items/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  deleteCabinetDisplayItem(id) {
    return request(`/api/admin/cabinet-display-items/${id}`, { method: 'DELETE' })
  },

  getCabinetDisplayItem(id) {
    return request(`/api/cabinet-display-items/${id}`)
  },

  uploadCabinetDisplayImage(file) {
    const formData = new FormData()
    formData.append('file', file)
    return uploadRequest('/api/admin/cabinet-display-images', formData)
  },

  listCognitionDevices(itemId) {
    return request(`/api/cabinet-display-items/${itemId}/cognition-devices`)
  },

  createCognitionDevice(itemId, payload) {
    return request(`/api/cabinet-display-items/${itemId}/cognition-devices`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  getCognitionDevice(id) {
    return request(`/api/admin/cognition-devices/${id}`)
  },

  updateCognitionDevice(id, payload) {
    return request(`/api/admin/cognition-devices/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  deleteCognitionDevice(id) {
    return request(`/api/admin/cognition-devices/${id}`, { method: 'DELETE' })
  },

  listCognitionDeviceDisplayItems(cognitionDeviceId) {
    return request(`/api/cognition-devices/${cognitionDeviceId}/display-items`)
  },

  createCognitionDeviceDisplayItem(cognitionDeviceId, payload) {
    return request(`/api/cognition-devices/${cognitionDeviceId}/display-items`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  updateDeviceDisplayItem(id, payload) {
    return request(`/api/admin/device-display-items/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  deleteDeviceDisplayItem(id) {
    return request(`/api/admin/device-display-items/${id}`, { method: 'DELETE' })
  },

  uploadDeviceDisplayImage(file) {
    const formData = new FormData()
    formData.append('file', file)
    return uploadRequest('/api/admin/device-display-images', formData)
  },

  listKnowledgeCognitionDevices(itemId) {
    return request(`/api/knowledge/cabinet-display-items/${itemId}/cognition-devices`)
  },

  listKnowledgeCognitionDeviceDisplayItems(cognitionDeviceId) {
    return request(`/api/knowledge/cognition-devices/${cognitionDeviceId}/display-items`)
  },

  // ── 设备绑定 ──

  checkBinding(bindId) {
    return request(`/api/bindings/check?bindId=${encodeURIComponent(bindId)}`)
  },

  listBindingCabinets() {
    return request('/api/bindings/cabinets')
  },

  bindCabinet(cabinetId, bindId, bindLabel) {
    return request(`/api/bindings/cabinets/${cabinetId}`, {
      method: 'POST',
      body: JSON.stringify({ bindId, bindLabel }),
    })
  },

  unbindCabinet(cabinetId) {
    return request(`/api/bindings/cabinets/${cabinetId}`, { method: 'DELETE' })
  },

  forceUnbindCabinet(cabinetId) {
    return request(`/api/bindings/cabinets/${cabinetId}/force`, { method: 'DELETE' })
  },
}
