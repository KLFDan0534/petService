import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// 开发服务器和打包的配置
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // 代码里写 @/xxx 就等于写 src/xxx
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
    host: '0.0.0.0',   // 允许局域网和外部域名访问
    port: 5173,
    // 这几个域名允许访问开发服务器，不写会被 Vite 挡掉
    allowedHosts: ['web.petservice.danquan.dpdns.org', 'web.petservice.cc.cd'],
    proxy: {
      // 开发时把 /api 转给本地后端，省得跨域
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // MinIO 的图片也走代理，转发前把 /minio 前缀去掉
      '/minio': {
        target: 'http://localhost:9000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/minio/, ''),
      },
    },
  },
  build: {
    outDir: path.resolve(__dirname, 'dist'),
    emptyOutDir: true,   // 打包前先清空 dist
  },
})
