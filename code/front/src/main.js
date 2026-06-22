const { app, BrowserWindow, Menu, screen } = require('electron')
const path = require('path')
const fs = require('fs')

// ── 运行时配置 ──────────────────────────────────────────────

function loadSettings() {
  // 打包后在 resources/settings.json；开发时在项目根
  const resourcePath = path.join(process.resourcesPath || '', 'settings.json')
  const devPath = path.join(__dirname, '..', 'settings.json')

  for (const p of [resourcePath, devPath]) {
    try {
      if (fs.existsSync(p)) {
        return JSON.parse(fs.readFileSync(p, 'utf-8'))
      }
    } catch (e) {
      console.warn('Failed to load settings from', p, e.message)
    }
  }

  // 默认配置
  return {
    apiBaseUrl: 'https://zeta-api.qyabc.cn',
    windowTitle: 'Zeta 继电保护逻辑教学系统',
  }
}

const settings = loadSettings()

// ── 窗口创建 ────────────────────────────────────────────────

let mainWindow = null

function createWindow() {
  const { width, height } = screen.getPrimaryDisplay().workAreaSize

  mainWindow = new BrowserWindow({
    width,
    height,
    title: settings.windowTitle || 'Zeta 教学系统',
    show: false,
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false,
    },
  })

  // 加载 admin SPA（打包后在 resources/app/）
  const appPath = path.join(process.resourcesPath || '', 'app', 'index.html')
  const devPath = path.join(__dirname, '..', '..', 'admin', 'dist', 'index.html')

  const htmlPath = fs.existsSync(appPath) ? appPath : devPath
  mainWindow.loadFile(htmlPath)

  // 准备好后显示，避免白屏闪烁
  mainWindow.once('ready-to-show', () => {
    mainWindow.maximize()
    mainWindow.show()
  })

  // 禁用右键菜单
  mainWindow.webContents.on('context-menu', (e) => e.preventDefault())

  mainWindow.on('closed', () => {
    mainWindow = null
  })
}

// ── App 生命周期 ─────────────────────────────────────────────

// 禁用默认菜单栏
Menu.setApplicationMenu(null)

app.whenReady().then(createWindow)

app.on('window-all-closed', () => {
  app.quit()
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow()
  }
})
