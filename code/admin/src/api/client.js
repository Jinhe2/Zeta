const ACCESS_TOKEN_KEY = 'zeta_access_token'
const REFRESH_TOKEN_KEY = 'zeta_refresh_token'

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
  const res = await fetch(path, {
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

  const res = await fetch(path, { ...options, headers })
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

  listKnowledgeDeviceCognitionItems(deviceId) {
    return request(`/api/knowledge/devices/${deviceId}/cognition-items`)
  },

  getProtectionLogic(id) {
    return request(`/api/protection-logics/${id}`)
  },

  getSections(id) {
    return request(`/api/protection-logics/${id}/sections`)
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

  listCabinets() {
    return request('/api/cabinets')
  },

  getCabinet(id) {
    return request(`/api/cabinets/${id}`)
  },

  createCabinet(payload) {
    return request('/api/cabinets', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  updateCabinet(id, payload) {
    return request(`/api/cabinets/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  deleteCabinet(id) {
    return request(`/api/cabinets/${id}`, { method: 'DELETE' })
  },

  listCabinetDevices(cabinetId) {
    return request(`/api/cabinets/${cabinetId}/devices`)
  },

  createCabinetDevice(cabinetId, payload) {
    return request(`/api/cabinets/${cabinetId}/devices`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  getDevice(id) {
    return request(`/api/devices/${id}`)
  },

  updateDevice(id, payload) {
    return request(`/api/devices/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  deleteDevice(id) {
    return request(`/api/devices/${id}`, { method: 'DELETE' })
  },

  listDeviceProtectionLogics(deviceId) {
    return request(`/api/devices/${deviceId}/protection-logics`)
  },

  createDeviceProtectionLogic(deviceId, payload) {
    return request(`/api/devices/${deviceId}/protection-logics`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  updateProtectionLogicAdmin(id, payload) {
    return request(`/api/admin/protection-logics/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  deleteProtectionLogicAdmin(id) {
    return request(`/api/admin/protection-logics/${id}`, { method: 'DELETE' })
  },

  listDeviceCognitionItems(deviceId) {
    return request(`/api/devices/${deviceId}/cognition-items`)
  },

  createDeviceCognitionItem(deviceId, payload) {
    return request(`/api/devices/${deviceId}/cognition-items`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  updateDeviceCognitionItem(id, payload) {
    return request(`/api/admin/device-cognition-items/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  deleteDeviceCognitionItem(id) {
    return request(`/api/admin/device-cognition-items/${id}`, { method: 'DELETE' })
  },

  getProtectionLogicConfig(id) {
    return request(`/api/admin/protection-logics/${id}/config`)
  },

  updateProtectionLogicConfig(id, configJson) {
    return request(`/api/admin/protection-logics/${id}/config`, {
      method: 'PUT',
      body: JSON.stringify({ configJson }),
    })
  },
}
