import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/tabs': {
        target: 'http://localhost:9001',
        changeOrigin: true,
      },
    },
  },
})
