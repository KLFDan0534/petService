<template>
  <div class="account-edit-page">
    <div class="page-header">
      <h1 class="page-title">{{ config.title }}</h1>
      <p class="page-subtitle">{{ config.subtitle }}</p>
    </div>

    <form class="account-card" @submit.prevent="submit">
      <template v-if="type === 'phone'">
        <label class="form-item">
          <span>新手机号</span>
          <input v-model.trim="form.phone_wsh" class="form-input" type="tel" autocomplete="tel" required>
        </label>
      </template>

      <template v-else-if="type === 'email'">
        <label class="form-item">
          <span>新邮箱</span>
          <input v-model.trim="form.email_wsh" class="form-input" type="email" autocomplete="email" required>
        </label>
      </template>

      <template v-else-if="type === 'paymentPassword'">
        <label class="form-item">
          <span>支付密码</span>
          <input
            v-model.trim="form.payment_password_wsh"
            class="form-input"
            type="password"
            inputmode="numeric"
            maxlength="6"
            autocomplete="new-password"
            required
          >
        </label>
        <label class="form-item">
          <span>确认支付密码</span>
          <input
            v-model.trim="form.confirm_payment_password_wsh"
            class="form-input"
            type="password"
            inputmode="numeric"
            maxlength="6"
            autocomplete="new-password"
            required
          >
        </label>
      </template>

      <template v-else>
        <label class="form-item">
          <span>真实姓名</span>
          <input v-model.trim="form.real_name_wsh" class="form-input" autocomplete="name" required>
        </label>
        <label class="form-item">
          <span>身份证号</span>
          <input v-model.trim="form.id_card_no_wsh" class="form-input" autocomplete="off" required>
        </label>
      </template>

      <div class="actions">
        <button type="button" class="btn btn-outline" @click="router.push('/profile')">返回</button>
        <button type="submit" class="btn btn-primary" :disabled="submitting">{{ submitting ? '保存中...' : '保存' }}</button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const submitting = ref(false)

const form = reactive({
  phone_wsh: '',
  email_wsh: '',
  real_name_wsh: '',
  id_card_no_wsh: '',
  payment_password_wsh: '',
  confirm_payment_password_wsh: '',
})

const type = computed(() => route.meta.accountType || 'phone')
const configMap = {
  phone: { title: '更改手机号', subtitle: '手机号将用于订单联系和账号安全通知', endpoint: '/users/me/phone' },
  email: { title: '更改邮箱', subtitle: '邮箱将用于账号通知和找回密码', endpoint: '/users/me/email' },
  realName: { title: '实名认证', subtitle: '实名信息单独提交，避免在个人资料页误改', endpoint: '/users/me/real-name' },
  paymentPassword: { title: '设置支付密码', subtitle: '支付密码仅用于你明确授权 Agent 自动支付时校验', endpoint: '/users/me/payment-password' },
}
const config = computed(() => configMap[type.value] || configMap.phone)

onMounted(async () => {
  if (type.value === 'paymentPassword') return
  try {
    const r = await request.get('/users/me')
    if (r.data.code === 200 && r.data.data) {
      Object.assign(form, {
        phone_wsh: r.data.data.phone_wsh || '',
        email_wsh: r.data.data.email_wsh || '',
        real_name_wsh: r.data.data.real_name_wsh || '',
        id_card_no_wsh: r.data.data.id_card_no_wsh || '',
      })
    }
  } catch (e) {
    appStore.addToast('获取资料失败', 'error')
  }
})

function buildPayload() {
  if (type.value === 'phone') return { phone_wsh: form.phone_wsh }
  if (type.value === 'email') return { email_wsh: form.email_wsh }
  if (type.value === 'paymentPassword') return { payment_password_wsh: form.payment_password_wsh }
  return { real_name_wsh: form.real_name_wsh, id_card_no_wsh: form.id_card_no_wsh }
}

function validatePaymentPassword() {
  if (type.value !== 'paymentPassword') return true
  if (!/^\d{6}$/.test(form.payment_password_wsh)) {
    appStore.addToast('支付密码必须是 6 位数字', 'error')
    return false
  }
  if (form.payment_password_wsh !== form.confirm_payment_password_wsh) {
    appStore.addToast('两次输入的支付密码不一致', 'error')
    return false
  }
  return true
}

async function submit() {
  if (submitting.value || !validatePaymentPassword()) return
  submitting.value = true
  try {
    const r = await request.put(config.value.endpoint, buildPayload())
    if (r.data.code === 200) {
      if (type.value === 'paymentPassword') {
        if (authStore.user) {
          authStore.user = { ...authStore.user, payment_password_set_wsh: true }
          localStorage.setItem('user', JSON.stringify(authStore.user))
        }
      } else if (authStore.user) {
        authStore.user = { ...authStore.user, ...r.data.data }
        localStorage.setItem('user', JSON.stringify(authStore.user))
      }
      form.payment_password_wsh = ''
      form.confirm_payment_password_wsh = ''
      appStore.addToast('保存成功', 'success')
      router.push('/profile')
    }
  } catch (e) {
    appStore.addToast(e.response?.data?.message || '保存失败', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.account-edit-page {
  max-width: 640px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #1f2329;
  margin: 0 0 4px;
}

.page-subtitle {
  color: #8f959e;
  margin: 0;
  font-size: 13px;
}

.account-card {
  background: #fff;
  border: 1px solid #dee0e3;
  border-radius: 6px;
  padding: 24px;
  display: grid;
  gap: 18px;
}

.form-item {
  display: grid;
  gap: 8px;
  color: #646a73;
  font-size: 13px;
}

.form-input {
  height: 40px;
  border: 1px solid #dee0e3;
  border-radius: 4px;
  padding: 0 12px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
