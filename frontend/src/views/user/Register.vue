<template>
  <div class="auth-page">
    <div class="auth-card card">
      <h1>注册</h1>
      <p class="subtitle">先绑定手机号，再用模拟验证码完成注册</p>

      <div v-if="error" class="auth-error">{{ error }}</div>
      <div v-if="captchaHint" class="captcha-hint">{{ captchaHint }}</div>

      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>昵称</label>
          <input v-model.trim="form.nickname_wsh" type="text" placeholder="请输入昵称">
        </div>

        <div class="form-group">
          <label>用户名</label>
          <input v-model.trim="form.username_wsh" type="text" placeholder="请输入用户名" minlength="3" required>
        </div>

        <div class="form-group">
          <label>手机号</label>
          <div class="phone-row">
            <input
              v-model.trim="form.phone_wsh"
              type="tel"
              placeholder="请输入手机号"
              inputmode="numeric"
              maxlength="11"
              required
            >
            <button
              type="button"
              class="btn btn-outline"
              :disabled="captchaSending || captchaCountdown > 0 || !canSendCaptcha"
              @click="sendCaptcha"
            >
              {{ captchaCountdown > 0 ? `${captchaCountdown}s` : (captchaSending ? '发送中...' : '获取验证码') }}
            </button>
          </div>
        </div>

        <div class="form-group">
          <label>验证码</label>
          <input v-model.trim="form.captcha_wsh" type="text" placeholder="请输入6位验证码" maxlength="6" required>
        </div>

        <div class="form-group">
          <label>密码</label>
          <input v-model.trim="form.password_wsh" type="password" placeholder="请输入密码" minlength="6" required>
        </div>

        <button type="submit" class="btn btn-primary" style="width:100%" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>

      <div class="auth-links">
        <router-link to="/login">已有账号？直接登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { addDynamicRoutes } from '@/router'
import { register, requestRegisterCaptcha } from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()
const error = ref('')
const loading = ref(false)
const captchaSending = ref(false)
const captchaCountdown = ref(0)
const captchaHint = ref('')
const countdownTimer = ref(null)

const form = reactive({
  username_wsh: '',
  nickname_wsh: '',
  phone_wsh: '',
  captcha_wsh: '',
  password_wsh: '',
})

const canSendCaptcha = computed(() => /^1[3-9]\d{9}$/.test(String(form.phone_wsh || '').trim()))

function startCountdown(seconds = 60) {
  captchaCountdown.value = seconds
  if (countdownTimer.value) clearInterval(countdownTimer.value)
  countdownTimer.value = setInterval(() => {
    captchaCountdown.value -= 1
    if (captchaCountdown.value <= 0) {
      clearInterval(countdownTimer.value)
      countdownTimer.value = null
    }
  }, 1000)
}

async function sendCaptcha() {
  error.value = ''
  if (!canSendCaptcha.value) {
    error.value = '请先输入正确的手机号'
    return
  }

  captchaSending.value = true
  try {
    const r = await requestRegisterCaptcha({ phone_wsh: form.phone_wsh })
    if (r.code === 200 && r.data?.captcha_wsh) {
      captchaHint.value = `模拟验证码：${r.data.captcha_wsh}，有效期 ${r.data.expires_in_seconds_wsh || 300} 秒`
      startCountdown(60)
    } else {
      error.value = r.message || '验证码发送失败'
    }
  } catch (e) {
    error.value = e.response?.data?.message || '验证码发送失败'
  } finally {
    captchaSending.value = false
  }
}

async function handleRegister() {
  error.value = ''
  loading.value = true
  try {
    const r = await register(form)
    if (r.code === 200) {
      authStore.setAuth(r.data)
      addDynamicRoutes(r.data.roles_wsh || [])
      router.push('/dashboard')
    } else {
      error.value = r.message || '注册失败'
    }
  } catch (e) {
    error.value = e.response?.data?.message || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (countdownTimer.value) clearInterval(countdownTimer.value)
})
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-primary) 0%, #FB923C 100%);
}

.auth-card {
  width: 400px;
  padding: 40px;
  text-align: center;
}

.auth-card h1 {
  font-size: 24px;
  margin-bottom: 8px;
}

.subtitle {
  color: var(--color-muted-foreground);
  margin-bottom: 24px;
}

.auth-error {
  background: #fef2f2;
  color: var(--color-destructive);
  padding: 10px;
  border-radius: 8px;
  margin-bottom: 12px;
  font-size: 13px;
}

.captcha-hint {
  background: #f0f9ff;
  color: #0369a1;
  padding: 10px;
  border-radius: 8px;
  margin-bottom: 12px;
  font-size: 13px;
}

.form-group {
  display: grid;
  gap: 8px;
  text-align: left;
  margin-bottom: 14px;
}

.form-group label {
  font-size: 13px;
  color: var(--color-muted-foreground);
}

.form-group input {
  height: 40px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0 12px;
}

.phone-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.auth-links {
  margin-top: 20px;
  font-size: 13px;
}
</style>
