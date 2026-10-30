import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// 单元测试配置，别名和 vite 保持一致
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // 测试里也认 @/xxx 这个写法
      '@': path.resolve(__dirname, 'src'),
    },
  },
  test: {
    environment: 'jsdom',   // 用假浏览器环境跑，组件里能拿到 document
    globals: true,          // 不用每次 import describe / it
    include: ['src/**/*.{test,spec}.{js,mjs}'],
  },
})
