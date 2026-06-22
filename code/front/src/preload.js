const { contextBridge } = require('electron')
const path = require('path')
const fs = require('fs')

// 加载运行时配置
function loadSettings() {
  const resourcePath = path.join(process.resourcesPath || '', 'settings.json')
  const devPath = path.join(__dirname, '..', 'settings.json')

  for (const p of [resourcePath, devPath]) {
    try {
      if (fs.existsSync(p)) {
        return JSON.parse(fs.readFileSync(p, 'utf-8'))
      }
    } catch {
      // ignore
    }
  }

  return {
    apiBaseUrl: 'https://zeta-api.qyabc.cn',
    windowTitle: 'Zeta 继电保护逻辑教学系统',
  }
}

const settings = loadSettings()

// 通过 contextBridge 安全地暴露 API 给渲染进程
contextBridge.exposeInMainWorld('electronAPI', {
  /** 获取运行时配置 */
  getSettings: () => ({ ...settings }),

  /** 平台信息 */
  platform: process.platform,

  /** 是否为 Electron 环境 */
  isElectron: true,
})
