import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { resolve } from 'node:path'

// Vite 配置：
// - 开发环境：把 /api 代理到后端 8080
// - 生产构建：产物直接输出到后端 static 目录，打包进 jar（前后端一体部署）
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 后端接口统一前缀 /api/v1，转发到 http://localhost:8080
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 后端静态资源（图片/视频）暴露在 /storage 下
      '/storage': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    // 生产构建产物输出到后端 static 目录，随 jar 一起部署
    outDir: resolve(__dirname, '../backend/src/main/resources/static'),
    emptyOutDir: true,
    chunkSizeWarningLimit: 1500
  }
})
