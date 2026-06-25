import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router, { addDynamicRoutes } from './router'
import { useDesignStore } from './stores/design'
import permission from './directives/permission'
import './assets/css/app.css'

const app = createApp(App)

app.use(createPinia())

// Load dynamic routes BEFORE app.use(router) → admin routes exist when resolve() runs
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
  } catch (e) {
    // Invalid stored user data
  }
}

app.use(router)

// Register v-permission directive globally
app.directive('permission', permission)

// Initialize design system for pet tech app
const designStore = useDesignStore()
designStore.applyDesignSystem('pet boarding platform', '宠物寄养平台')

app.mount('#app')
