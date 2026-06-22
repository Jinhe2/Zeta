import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, __dirname, '')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://localhost:8080'

  return {
    // Electron 模式使用相对路径，Web 模式使用默认绝对路径
    base: mode === 'electron' ? './' : '/',
    plugins: [react()],
    resolve: {
      alias: {
        '@zeta/diagram': path.resolve(__dirname, '../packages/diagram/src'),
        react: path.resolve(__dirname, 'node_modules/react'),
        'react-dom': path.resolve(__dirname, 'node_modules/react-dom'),
        '@xyflow/react': path.resolve(__dirname, 'node_modules/@xyflow/react'),
        elkjs: path.resolve(__dirname, 'node_modules/elkjs'),
      },
    },
    server: {
      port: 5173,
      fs: {
        allow: [path.resolve(__dirname, '..')],
      },
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
        },
        '/uploads': {
          target: proxyTarget,
          changeOrigin: true,
        },
      },
    },
  }
})
