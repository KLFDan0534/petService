import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import App from './App.vue'
import router, { addDynamicRoutes } from './router'
import { useDesignStore } from './stores/design'
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
app.use(ElementPlus)

app.directive('permission', permission)

const designStore = useDesignStore()
designStore.applyDesignSystem('pet boarding platform', '宠物寄养平台')

const appStore = useAppStore()
appStore.applyTheme()

app.mount('#app')
