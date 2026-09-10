import { createApp } from 'vue'
import { createPinia } from 'pinia'
// 完全移除 Element Plus 框架；图标由自封装 <AppIcon> 包裹，字形来自 @element-plus/icons-vue。
import AppIcon from '@/components/common/AppIcon.vue'
import App from './App.vue'
import router, { addDynamicRoutes } from './router'
import { useAppStore } from './stores/app'
import permission from './directives/permission'
import './assets/css/app.css'

const app = createApp(App)

// Global error handler: prevents white screens on component errors.
app.config.errorHandler = (err, _instance, info) => {
  console.error('[Global ErrorHandler]', err, info)
  try {
    const store = useAppStore()
    store.addToast(err?.message || '页面渲染异常，请刷新重试', 'error')
  } catch (_) {}
}

window.addEventListener('unhandledrejection', (event) => {
  console.error('[Unhandled Promise Rejection]', event.reason)
  event.preventDefault()
})

app.use(createPinia())

// Load dynamic routes before app.use(router), so admin routes exist when resolve() runs.
const token = localStorage.getItem('token')
if (token) {
  try {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const user = JSON.parse(userStr)
      if (user.roles_wsh) {
        addDynamicRoutes(user.roles_wsh)
      }
    }
  } catch (_) {}
}

app.use(router)
app.component('AppIcon', AppIcon)
app.directive('permission', permission)

// 设计令牌单一来源为 design-tokens.css；运行时不再注入/覆盖 --color-* 与 --ref-*。
const appStore = useAppStore()
appStore.applyTheme()

app.mount('#app')
