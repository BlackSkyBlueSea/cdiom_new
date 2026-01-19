import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  },
  build: {
    // 生产环境移除console语句
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true, // 移除所有console语句
        drop_debugger: true, // 移除debugger语句
      },
    },
  },
})









