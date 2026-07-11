const { contextBridge, ipcRenderer } = require('electron')

let settingsCache = ipcRenderer.sendSync('settings:get-sync')

contextBridge.exposeInMainWorld('electronAPI', {
  /** 获取运行时配置（含用户在界面保存的覆盖项） */
  getSettings: () => ({ ...settingsCache }),

  /** 保存用户配置到 userData/settings.json */
  saveSettings: async (partial) => {
    settingsCache = await ipcRenderer.invoke('settings:save', partial)
    return { ...settingsCache }
  },

  /** 平台信息 */
  platform: process.platform,

  /** 是否为 Electron 环境 */
  isElectron: true,
})
