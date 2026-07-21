<template>
  <div class="auth-page">
    <div class="auth-card card">
      <h1>登录</h1>
      <p class="subtitle">欢迎回到宠物寄养平台</p>
      <div v-if="error" class="auth-error">{{ error }}</div>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>用户名</label>
          <input v-model="username_wsh" type="text" placeholder="请输入用户名" required>
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="password_wsh" type="password" placeholder="请输入密码" required>
        </div>
        <button type="submit" class="btn btn-primary" style="width:100%" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
      <div class="auth-links">
        <router-link to="/register">还没有账号？立即注册</router-link>
        <router-link to="/forget-password" style="margin-left:16px">忘记密码？</router-link>
      </div>
      <div class="guest-link">
        <router-link to="/dashboard">以游客身份访问</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { addDynamicRoutes } from '@/router'
import axios from 'axios'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const username_wsh = ref('')
const password_wsh = ref('')
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    const r = await axios.post('/api/auth/login', {
      username_wsh: username_wsh.value,
      password_wsh: password_wsh.value,
    })
    if (r.data.code === 200) {
      authStore.setAuth(r.data.data)
      addDynamicRoutes(r.data.data.roles_wsh || [])
      const redirect = route.query.redirect || '/dashboard'
      router.push(redirect)
    } else {
      error.value = r.data.message || '登录失败'
    }
  } catch (e) {
    error.value = e.response?.data?.message || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, var(--color-primary) 0%, #FB923C 100%); }
.auth-card { width: 400px; padding: 40px; text-align: center; }
.auth-card h1 { font-size: 24px; margin-bottom: 8px; }
.subtitle { color: var(--color-muted-foreground); margin-bottom: 24px; }
.auth-error { background: #fef2f2; color: var(--color-destructive); padding: 10px; border-radius: 8px; margin-bottom: 16px; font-size: 13px; }
.auth-links { margin-top: 20px; font-size: 13px; }
.guest-link { margin-top: 12px; font-size: 13px; }
.guest-link a { color: var(--color-muted-foreground); text-decoration: none; }
.guest-link a:hover { color: var(--color-primary); text-decoration: underline; }
</style>
