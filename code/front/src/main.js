const { app, BrowserWindow, Menu, screen, dialog, ipcMain } = require('electron')
const path = require('path')
const fs = require('fs')

// ── 日志（写入文件，方便排查 Windows 上无法启动的问题） ───────
const logFile = path.join(
  app.getPath('userData'),
  'zeta-debug.log'
)

function log(...args) {
  const line = `[${new Date().toISOString()}] ${args.join(' ')}\n`
  try { fs.appendFileSync(logFile, line) } catch {}
  console.log(...args)
}

// 全局异常捕获
process.on('uncaughtException', (err) => {
  log('❌ uncaughtException:', err.stack || err.message || err)
  try {
    dialog.showErrorBox('Zeta 启动错误', `${err.message}\n\n日志: ${logFile}`)
  } catch {}
  process.exit(1)
})

process.on('unhandledRejection', (reason) => {
  log('❌ unhandledRejection:', reason)
})

log('=== Zeta 启动 ===')
log('platform:', process.platform, 'arch:', process.arch)
log('electron:', process.versions.electron, 'node:', process.versions.node)
log('userData:', app.getPath('userData'))

// ── 运行时配置 ──────────────────────────────────────────────

const DEFAULT_SETTINGS = {
  apiBaseUrl: 'https://zeta-api.qyabc.cn',
  windowTitle: 'Zeta 继电保护逻辑教学系统',
}

function getSettingsPaths() {
  return {
    userPath: path.join(app.getPath('userData'), 'settings.json'),
    resourcePath: path.join(process.resourcesPath || '', 'settings.json'),
    devPath: path.join(__dirname, '..', 'settings.json'),
  }
}

function readJsonFile(filePath) {
  try {
    if (fs.existsSync(filePath)) {
      return JSON.parse(fs.readFileSync(filePath, 'utf-8'))
    }
  } catch (e) {
    log('⚠️ Failed to load settings from', filePath, e.message)
  }
  return null
}

function loadSettings() {
  const { userPath, resourcePath, devPath } = getSettingsPaths()

  log('resourcesPath:', process.resourcesPath)
  log('looking for settings at:', userPath, resourcePath, devPath)

  let merged = { ...DEFAULT_SETTINGS }

  const packaged = readJsonFile(resourcePath) || readJsonFile(devPath)
  if (packaged) {
    merged = { ...merged, ...packaged }
    log('✅ loaded packaged settings')
  } else {
    log('⚠️ using default packaged settings')
  }

  const user = readJsonFile(userPath)
  if (user) {
    merged = { ...merged, ...user }
    log('✅ applied user settings from', userPath)
  }

  return merged
}

function saveUserSettings(partial) {
  const { userPath } = getSettingsPaths()
  const currentUser = readJsonFile(userPath) || {}
  const next = { ...currentUser, ...partial }

  if ('apiBaseUrl' in partial && !partial.apiBaseUrl) {
    delete next.apiBaseUrl
  }

  try {
    if (Object.keys(next).length === 0) {
      if (fs.existsSync(userPath)) {
        fs.unlinkSync(userPath)
      }
    } else {
      fs.mkdirSync(path.dirname(userPath), { recursive: true })
      fs.writeFileSync(userPath, JSON.stringify(next, null, 2), 'utf-8')
    }
    log('✅ saved user settings to', userPath)
  } catch (e) {
    log('❌ failed to save user settings:', e.message)
    throw e
  }

  return loadSettings()
}

let settings = loadSettings()

ipcMain.on('settings:get-sync', (event) => {
  event.returnValue = { ...settings }
})

ipcMain.handle('settings:save', (_, partial) => {
  settings = saveUserSettings(partial)
  return { ...settings }
})

// ── 窗口创建 ────────────────────────────────────────────────

let mainWindow = null

function createWindow() {
  log('createWindow() called')
  const { width, height } = screen.getPrimaryDisplay().workAreaSize
  log('screen size:', width, 'x', height)

  const preloadPath = path.join(__dirname, 'preload.js')
  log('preload path:', preloadPath, 'exists:', fs.existsSync(preloadPath))

  mainWindow = new BrowserWindow({
    width,
    height,
    title: settings.windowTitle || 'Zeta 教学系统',
    show: false,
    autoHideMenuBar: true,
    webPreferences: {
      preload: preloadPath,
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false,
    },
  })

  log('BrowserWindow created')

  // 加载 admin SPA（打包后在 resources/app/）
  const appPath = path.join(process.resourcesPath || '', 'app', 'index.html')
  const devPath = path.join(__dirname, '..', '..', 'admin', 'dist', 'index.html')

  log('appPath:', appPath, 'exists:', fs.existsSync(appPath))
  log('devPath:', devPath, 'exists:', fs.existsSync(devPath))

  const htmlPath = fs.existsSync(appPath) ? appPath : devPath
  log('loading:', htmlPath)
  mainWindow.loadFile(htmlPath)

  // 捕获页面加载错误
  mainWindow.webContents.on('did-fail-load', (event, code, desc) => {
    log('❌ did-fail-load:', code, desc)
  })

  // 捕获渲染进程崩溃
  mainWindow.webContents.on('crashed', () => {
    log('❌ renderer crashed')
  })

  // 捕获页面 JS 错误
  mainWindow.webContents.on('console-message', (event, level, message) => {
    log(`[renderer:${level}]`, message)
  })

  // 准备好后显示，避免白屏闪烁
  mainWindow.once('ready-to-show', () => {
    log('ready-to-show, maximizing')
    mainWindow.maximize()
    mainWindow.show()
  })

  // 禁用右键菜单
  mainWindow.webContents.on('context-menu', (e) => e.preventDefault())

  mainWindow.on('closed', () => {
    log('window closed')
    mainWindow = null
  })
}

// ── App 生命周期 ─────────────────────────────────────────────

// 禁用默认菜单栏
Menu.setApplicationMenu(null)

log('waiting for app.whenReady()...')

app.whenReady().then(() => {
  log('app ready')
  createWindow()
}).catch((err) => {
  log('❌ whenReady failed:', err.stack || err.message || err)
  try {
    dialog.showErrorBox('Zeta 启动失败', err.message)
  } catch {}
})

app.on('window-all-closed', () => {
  log('all windows closed, quitting')
  app.quit()
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow()
  }
})
