<template>
  <div class="auth-page">
    <div class="auth-card card">
      <h1>找回密码</h1>
      <p class="subtitle">请输入您的注册邮箱或手机号</p>
      <div v-if="error" class="auth-error">{{ error }}</div>
      <div v-if="success" class="auth-success">{{ success }}</div>
      <form @submit.prevent="handleReset">
        <div class="form-group">
          <label>邮箱或手机号</label>
          <input v-model="account" type="text" placeholder="请输入注册邮箱或手机号" required>
        </div>
        <button type="submit" class="btn btn-primary" style="width:100%" :disabled="loading">
          {{ loading ? '发送中...' : '发送重置链接' }}
        </button>
      </form>
      <div class="auth-links">
        <router-link to="/login">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/utils/request'

const account = ref('')
const error = ref('')
const success = ref('')
const loading = ref(false)

async function handleReset() {
  error.value = ''
  success.value = ''
  loading.value = true
  try {
    const r = await request.post('/auth/forgot-password', { email_wsh: account.value })
    if (r.data.code === 200) {
      success.value = '重置链接已发送，请检查您的邮箱'
    } else {
      error.value = r.data.message || '操作失败'
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
.auth-success { background: #f0fdf4; color: #065f46; padding: 10px; border-radius: 8px; margin-bottom: 16px; font-size: 13px; }
.auth-links { margin-top: 20px; font-size: 13px; }
</style>
