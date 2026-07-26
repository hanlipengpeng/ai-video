import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// Vite 配置：开发环境把 /api 代理到后端 8080
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
  }
})
