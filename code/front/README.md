# Zeta 教学系统 — Windows 平板客户端

Electron 壳工程，加载 `admin/dist/` 构建产物，打包为 Windows exe 安装程序。

## 目录结构

```text
front/
├── package.json          # Electron + electron-builder 配置
├── settings.json         # 运行时配置（API 地址等，安装后可修改）
├── build/
│   └── icon.ico          # 应用图标（256x256，需自行添加）
└── src/
    ├── main.js           # 主进程：窗口创建、配置加载
    └── preload.js        # 预加载：暴露 electronAPI 给渲染进程
```

## 构建步骤

```bash
# 1. 安装依赖（首次）
cd code/front
npm install

# 2. 构建（自动先构建 admin SPA，再打包 Electron）
npm run build

# 产物：release/Zeta 教学系统 Setup x.x.x.exe
```

## 开发调试

```bash
# 先构建 admin（Electron 模式）
cd code/admin && npm run build:electron

# 启动 Electron 开发模式
cd code/front && npm start
```

## 运行时配置

安装后修改 `resources/settings.json`（在安装目录下），可更改 API 地址：

```json
{
  "apiBaseUrl": "https://zeta-api.qyabc.cn",
  "windowTitle": "Zeta 继电保护逻辑教学系统"
}
```

修改后重启应用生效。

## 应用图标

将 256×256 的 `.ico` 文件放在 `build/icon.ico`。
推荐使用 [icoconvert.com](https://icoconvert.com/) 从 PNG 转换。
