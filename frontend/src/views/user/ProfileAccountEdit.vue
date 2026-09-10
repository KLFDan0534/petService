<template>
  <div class="ae-page">
    <div class="ae-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="ae-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="ae-crumb-link">首页</router-link>
        <span class="ae-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/profile" class="ae-crumb-link">个人中心</router-link>
        <span class="ae-crumb-sep" aria-hidden="true">›</span>
        <span class="ae-crumb-here">{{ config.title }}</span>
      </nav>

      <!-- ═══ Hero ═══ -->
      <header class="ae-head">
        <div class="ae-head-copy">
          <div class="ae-eyebrow" aria-hidden="true">
            <span class="ae-eyebrow-line"></span>
            <span>{{ config.en }}</span>
          </div>
          <h1 class="ae-title">{{ config.title }}</h1>
          <p class="ae-sub">{{ config.lede }}</p>
        </div>
      </header>

      <!-- ═══ 01 · 更新 ═══ -->
      <section class="ae-section" aria-label="填写信息">
        <header class="ae-sec-head">
          <div class="ae-head-copy">
            <p class="ae-eyebrow ae-sec-eyebrow">
              <span class="ae-idx">01</span>
              <span class="ae-line" aria-hidden="true"></span>
              <span>更新</span>
            </p>
            <h2 class="ae-sec-title">填写信息</h2>
          </div>
        </header>

        <!-- 加载骨架 -->
        <div v-if="loading" class="ae-skel" aria-hidden="true" />

        <!-- 成功完成态 -->
        <div v-else-if="done" class="ae-success">
          <span class="ae-success-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 6 9 17l-5-5" />
            </svg>
          </span>
          <h2 class="ae-success-title">{{ type === 'realName' ? '实名信息已提交' : '修改已生效' }}</h2>
          <p class="ae-success-desc">
            {{
              type === 'realName'
                ? '平台会在 1–2 个工作日内完成审核，结果将通过站内通知告知你。'
                : '你可以返回个人中心继续其他设置。'
            }}
          </p>
          <div class="ae-success-actions">
            <router-link to="/profile" class="cta cta-primary">返回个人中心</router-link>
          </div>
        </div>

        <!-- 表单 -->
        <form v-else class="ae-panel" @submit.prevent="submit">
          <div class="ae-current">
            <div class="ae-current-copy">
              <p class="ae-current-label">当前设置</p>
              <p class="ae-current-value">{{ current }}</p>
            </div>
            <span class="ae-current-icon" aria-hidden="true">
              <svg v-if="type === 'paymentPassword'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
              <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
              </svg>
            </span>
          </div>

          <div class="ae-fields">
            <template v-if="type === 'phone'">
              <div class="ae-field">
                <label class="ae-label">新手机号 <i>*</i></label>
                <input
                  v-model.trim="form.phone_wsh"
                  class="ae-input"
                  type="tel"
                  inputmode="tel"
                  maxlength="11"
                  placeholder="请输入 11 位手机号"
                  autocomplete="tel"
                  :class="{ 'has-error': errors.phone }"
                  @input="errors.phone = ''"
                >
                <p v-if="errors.phone" class="ae-error">{{ errors.phone }}</p>
                <p v-else class="ae-hint">修改后请使用新号码登录。</p>
              </div>
            </template>

            <template v-else-if="type === 'email'">
              <div class="ae-field">
                <label class="ae-label">新邮箱 <i>*</i></label>
                <input
                  v-model.trim="form.email_wsh"
                  class="ae-input"
                  type="email"
                  inputmode="email"
                  placeholder="name@example.com"
                  autocomplete="email"
                  :class="{ 'has-error': errors.email }"
                  @input="errors.email = ''"
                >
                <p v-if="errors.email" class="ae-error">{{ errors.email }}</p>
              </div>
            </template>

            <template v-else-if="type === 'realName'">
              <div class="ae-field">
                <label class="ae-label">真实姓名 <i>*</i></label>
                <input
                  v-model.trim="form.real_name_wsh"
                  class="ae-input"
                  placeholder="与证件完全一致"
                  autocomplete="name"
                  :class="{ 'has-error': errors.realName }"
                  @input="errors.realName = ''"
                >
                <p v-if="errors.realName" class="ae-error">{{ errors.realName }}</p>
              </div>
              <div class="ae-field">
                <label class="ae-label">身份证号 <i>*</i></label>
                <input
                  v-model.trim="form.id_card_no_wsh"
                  class="ae-input"
                  maxlength="18"
                  placeholder="18 位身份证号码"
                  autocomplete="off"
                  :class="{ 'has-error': errors.idCard }"
                  @input="errors.idCard = ''"
                >
                <p v-if="errors.idCard" class="ae-error">{{ errors.idCard }}</p>
                <p v-else class="ae-hint">证件信息仅用于本次核验，平台不会在页面上再次完整展示。</p>
              </div>
            </template>

            <template v-else>
              <div class="ae-field">
                <label class="ae-label">支付密码 <i>*</i></label>
                <input
                  v-model="form.payment_password_wsh"
                  class="ae-input"
                  type="password"
                  inputmode="numeric"
                  maxlength="6"
                  autocomplete="new-password"
                  placeholder="6 位数字"
                  :class="{ 'has-error': errors.password }"
                  @input="form.payment_password_wsh = form.payment_password_wsh.replace(/\D/g, ''); errors.password = ''"
                >
                <p v-if="errors.password" class="ae-error">{{ errors.password }}</p>
              </div>
              <div class="ae-field">
                <label class="ae-label">确认支付密码 <i>*</i></label>
                <input
                  v-model="form.confirm_payment_password_wsh"
                  class="ae-input"
                  type="password"
                  inputmode="numeric"
                  maxlength="6"
                  autocomplete="new-password"
                  placeholder="再次输入"
                  :class="{ 'has-error': errors.confirm }"
                  @input="form.confirm_payment_password_wsh = form.confirm_payment_password_wsh.replace(/\D/g, ''); errors.confirm = ''"
                >
                <p v-if="errors.confirm" class="ae-error">{{ errors.confirm }}</p>
                <p v-else class="ae-hint">确认项仅在本地校验，不会发送到服务器。</p>
              </div>
            </template>
          </div>

          <div class="ae-actions">
            <button type="submit" class="cta cta-primary" :disabled="submitting">
              {{ submitting ? '保存中…' : (type === 'realName' ? '提交审核' : '保存修改') }}
            </button>
            <router-link to="/profile" class="cta cta-quiet">取消</router-link>
          </div>
        </form>
      </section>
    </div>
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
const loading = ref(true)
const done = ref(false)
const profile = ref(null)

const form = reactive({
  phone_wsh: '',
  email_wsh: '',
  real_name_wsh: '',
  id_card_no_wsh: '',
  payment_password_wsh: '',
  confirm_payment_password_wsh: '',
})
const errors = reactive({
  phone: '',
  email: '',
  realName: '',
  idCard: '',
  password: '',
  confirm: '',
})

const type = computed(() => route.meta.accountType || 'phone')
const configMap = {
  phone: { en: 'Phone', title: '更改手机号', lede: '手机号将用于订单联系和账号安全通知，修改后请使用新号码登录。', endpoint: '/users/me/phone' },
  email: { en: 'Email', title: '更改邮箱', lede: '邮箱将用于账号通知和找回密码。', endpoint: '/users/me/email' },
  realName: { en: 'Real name', title: '实名认证', lede: '实名信息单独提交，避免在个人资料页误改。', endpoint: '/users/me/real-name' },
  paymentPassword: { en: 'Payment password', title: '设置支付密码', lede: '支付密码仅用于你明确授权 Agent 自动支付时校验，请在支付前妥善保管。', endpoint: '/users/me/payment-password' },
}
const config = computed(() => configMap[type.value] || configMap.phone)

function maskPhone(value) {
  const phone = String(value ?? '')
  return phone.length === 11 ? `${phone.slice(0, 3)}****${phone.slice(7)}` : phone || '未设置'
}
function maskEmail(value) {
  const email = String(value ?? '')
  const at = email.indexOf('@')
  if (at <= 1) return email || '未设置'
  return `${email.slice(0, 2)}***${email.slice(at)}`
}

const current = computed(() => {
  const p = profile.value || {}
  if (type.value === 'phone') return maskPhone(p.phone_wsh)
  if (type.value === 'email') return maskEmail(p.email_wsh)
  if (type.value === 'realName') return p.real_name_wsh || '未认证'
  return p.payment_password_set_wsh ? '已设置' : '未设置'
})

onMounted(async () => {
  if (type.value !== 'paymentPassword') {
    try {
      const r = await request.get('/users/me')
      if (r.data.code === 200 && r.data.data) {
        profile.value = r.data.data
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
  }
  loading.value = false
})

function buildPayload() {
  if (type.value === 'phone') return { phone_wsh: form.phone_wsh }
  if (type.value === 'email') return { email_wsh: form.email_wsh }
  if (type.value === 'paymentPassword') return { payment_password_wsh: form.payment_password_wsh }
  return { real_name_wsh: form.real_name_wsh, id_card_no_wsh: form.id_card_no_wsh }
}

function validate() {
  errors.phone = ''
  errors.email = ''
  errors.realName = ''
  errors.idCard = ''
  errors.password = ''
  errors.confirm = ''
  if (type.value === 'phone') {
    if (!/^1\d{10}$/.test(form.phone_wsh)) { errors.phone = '请输入 11 位有效手机号'; return false }
  }
  if (type.value === 'email') {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email_wsh)) { errors.email = '请输入有效的邮箱地址'; return false }
  }
  if (type.value === 'realName') {
    if (!form.real_name_wsh.trim()) { errors.realName = '请输入真实姓名'; return false }
    if (!/^\d{17}[\dXx]$/.test(form.id_card_no_wsh)) { errors.idCard = '请输入 18 位身份证号码'; return false }
  }
  if (type.value === 'paymentPassword') {
    if (!/^\d{6}$/.test(form.payment_password_wsh)) { errors.password = '支付密码必须是 6 位数字'; return false }
    if (form.payment_password_wsh !== form.confirm_payment_password_wsh) { errors.confirm = '两次输入的支付密码不一致'; return false }
  }
  return true
}

async function submit() {
  if (submitting.value || !validate()) return
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
      done.value = true
    } else {
      appStore.addToast(r.data.message || '保存失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e.response?.data?.message || '保存失败', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.ae-page {
  width: 100%;
  padding: 6px 0 72px;
  min-height: 60vh;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}
.ae-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.ae-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.ae-crumb-link { color: var(--ref-muted); text-decoration: none; }
.ae-crumb-link:hover { color: var(--ref-ink); }
.ae-crumb-sep { color: var(--ref-line); }
.ae-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow ═══ */
.ae-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ae-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.ae-idx { font-variant-numeric: tabular-nums; }
.ae-line { width: 24px; height: 1px; background: var(--ref-line); }

/* ═══ Hero ═══ */
.ae-head { padding: 40px 0 8px; }
.ae-head-copy { min-width: 0; }
.ae-title {
  margin: 18px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(34px, 4.4vw, 52px);
  line-height: 1.12;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.ae-sub {
  margin: 14px 0 0;
  max-width: 640px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent);
}

/* ═══ Section ═══ */
.ae-section { margin-top: 40px; }
.ae-sec-eyebrow { font-size: 10px; letter-spacing: 0.22em; }
.ae-sec-title {
  margin: 10px 0 0;
  font-family: var(--ref-font-display);
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ref-ink);
}

/* ═══ Skeleton ═══ */
.ae-skel {
  margin-top: 20px;
  max-width: 620px;
  height: 320px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-card);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: ae-shimmer 1.3s linear infinite;
}

/* ═══ Success ═══ */
.ae-success {
  margin-top: 20px;
  max-width: 620px;
  padding: 44px 24px;
  border: 1px solid var(--ref-line);
  border-radius: 20px;
  background: var(--ref-surface);
  text-align: center;
}
.ae-success-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin: 0 auto;
  border-radius: 50%;
  background: color-mix(in srgb, var(--ref-brand) 12%, transparent);
  color: var(--ref-brand);
}
.ae-success-title {
  margin: 20px 0 0;
  font-family: var(--ref-font-display);
  font-size: 22px;
  font-weight: 500;
  color: var(--ref-ink);
}
.ae-success-desc {
  margin: 12px auto 0;
  max-width: 380px;
  font-size: 13.5px;
  line-height: 1.75;
  color: var(--ref-muted);
}
.ae-success-actions { margin-top: 26px; }

/* ═══ Panel ═══ */
.ae-panel {
  margin-top: 20px;
  max-width: 620px;
  padding: 22px 24px;
  border: 1px solid var(--ref-line);
  border-radius: 20px;
  background: var(--ref-surface);
}
.ae-current {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 20px;
  margin-bottom: 22px;
  border-bottom: 1px solid var(--ref-line);
}
.ae-current-copy { min-width: 0; }
.ae-current-label {
  margin: 0;
  font-size: 10px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ae-current-value {
  margin: 6px 0 0;
  font-size: 15px;
  font-weight: 500;
  color: var(--ref-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ae-current-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 50%;
  background: color-mix(in srgb, var(--ref-cream) 80%, var(--ref-surface));
  color: var(--ref-muted);
}

/* ═══ Fields ═══ */
.ae-fields { display: grid; gap: 20px; }
.ae-field { display: grid; gap: 8px; }
.ae-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--ref-ink-soft);
}
.ae-label i { color: var(--color-danger, #ef4444); font-style: normal; }
.ae-input {
  height: var(--control-height);
  padding: 0 14px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-control);
  background: var(--ref-surface);
  color: var(--ref-ink);
  font-size: 14px;
  font-family: inherit;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.ae-input::placeholder { color: color-mix(in srgb, var(--ref-muted) 70%, transparent); }
.ae-input:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-brand) 60%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 14%, transparent);
}
.ae-input.has-error { border-color: var(--color-danger, #ef4444); }
.ae-hint { margin: 0; font-size: 12px; color: var(--ref-muted); }
.ae-error { margin: 0; font-size: 12px; color: var(--color-danger, #ef4444); }

/* ═══ Actions ═══ */
.ae-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--ref-line);
}

/* ═══ CTA ═══ */
.cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  padding: 0 18px;
  border-radius: var(--radius-control);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  text-decoration: none;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-quiet {
  background: transparent;
  color: var(--ref-ink-soft);
  border-color: transparent;
}
.cta-quiet:hover { color: var(--ref-ink); }

/* ═══ Animations ═══ */
@keyframes ae-shimmer { to { background-position: -200% 0; } }

/* ═══ Responsive ═══ */
@media (max-width: 520px) {
  .ae-shell { padding: 0 16px; }
  .ae-head { padding: 30px 0 4px; }
  .ae-sub { font-size: 13.5px; }
  .ae-section { margin-top: 32px; }
  .ae-panel { padding: 18px; }
}
@media (prefers-reduced-motion: reduce) {
  .ae-skel { animation: none; }
}
</style>