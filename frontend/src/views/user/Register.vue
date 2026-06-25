<template>
  <div class="auth-page">
    <div class="auth-card card">
      <h1>注册</h1>
      <p class="subtitle">加入宠物寄养平台</p>
      <div v-if="error" class="auth-error">{{ error }}</div>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>昵称</label>
          <input v-model="form.nickname_wsh" type="text" placeholder="请输入昵称" required>
        </div>
        <div class="form-group">
          <label>账号</label>
          <input v-model="form.username_wsh" type="text" placeholder="请输入账号" minlength="3" required>
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="form.password_wsh" type="password" placeholder="请输入密码" minlength="6" required>
        </div>

        <button type="submit" class="btn btn-primary" style="width:100%" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>
      <div class="auth-links">
        <router-link to="/login">已有账号？立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { addDynamicRoutes } from '@/router'
import axios from 'axios'

const router = useRouter()
const authStore = useAuthStore()
const error = ref('')
const loading = ref(false)
const form = reactive({ username_wsh: '', nickname_wsh: '', password_wsh: '' })

async function handleRegister() {
  error.value = ''
  loading.value = true
  try {
    const r = await axios.post('/api/auth/register', form)
    if (r.data.code === 200) {
      authStore.setAuth(r.data.data)
      addDynamicRoutes(r.data.data.roles_wsh || [])
      router.push('/dashboard')
    } else {
      error.value = r.data.message || '注册失败'
    }
  } catch (e) {
    error.value = '网络错误，请稍后重试'
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
</style>
