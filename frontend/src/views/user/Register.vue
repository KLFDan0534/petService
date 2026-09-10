<template>
  <div class="auth-page">
    <div class="auth-shell">
      <!-- ═══ 左侧：表单 ═══ -->
      <main class="auth-main">
        <div class="auth-inner">
          <!-- brand mark -->
          <router-link to="/dashboard" class="auth-brand" aria-label="返回栖屿宠护首页">
            <span class="auth-brand-mark" aria-hidden="true">栖</span>
            <span class="auth-brand-copy">
              <span class="auth-brand-name">栖屿宠护</span>
              <span class="auth-brand-sub">宠物寄养</span>
            </span>
          </router-link>

          <!-- header -->
          <header class="auth-head">
            <p class="auth-eyebrow">创建账号</p>
            <h1 class="auth-title">开始为它安排照护</h1>
            <p class="auth-lede">注册后即可建立宠物档案、预约寄养，并实时查看照护日报。</p>
          </header>

          <!-- 服务端错误（非字段校验） -->
          <div v-if="formError" class="auth-alert auth-alert-error" role="alert">{{ formError }}</div>

          <!-- form -->
          <form class="auth-form" novalidate @submit.prevent="handleRegister">
          <!--用户名-->
            <div class="auth-grid">
              <div class="auth-field">
                <label class="auth-label" for="reg-username">用户名<span class="auth-req">*</span></label>
                <input
                  id="reg-username"
                  v-model.trim="form.username_wsh"
                  class="auth-input"
                  :class="{ 'auth-input-error': errors.username }"
                  type="text"
                  autocomplete="username"
                  autofocus
                  maxlength="20"
                  placeholder="用于登录"
                >
                <p v-if="errors.username" class="auth-error">{{ errors.username }}</p>
              </div>


              <div class="auth-field">
                <label class="auth-label" for="reg-nickname">昵称<span class="auth-req">*</span></label>
                <input
                  id="reg-nickname"
                  v-model.trim="form.nickname_wsh"
                  class="auth-input"
                  :class="{ 'auth-input-error': errors.nickname }"
                  type="text"
                  maxlength="20"
                  placeholder="照护师会这样称呼你"
                >
                <p v-if="errors.nickname" class="auth-error">{{ errors.nickname }}</p>
              </div>
            </div>

            <div class="auth-field">
              <label class="auth-label" for="reg-phone">手机号<span class="auth-req">*</span></label>
              <input
                id="reg-phone"
                :value="form.phone_wsh"
                @input="form.phone_wsh = $event.target.value.replace(/\D/g, '')"
                class="auth-input"
                :class="{ 'auth-input-error': errors.phone }"
                type="tel"
                inputmode="numeric"
                maxlength="11"
                autocomplete="tel"
                placeholder="用于接收订单与照护提醒"
              >
              <p v-if="errors.phone" class="auth-error">{{ errors.phone }}</p>
            </div>

            <div class="auth-field">
              <div class="auth-captcha-row">
                <div class="auth-captcha-input">
                  <label class="auth-label" for="reg-captcha">短信验证码<span class="auth-req">*</span></label>
                  <input
                    id="reg-captcha"
                    :value="form.captcha_wsh"
                    @input="form.captcha_wsh = $event.target.value.replace(/\s/g, '')"
                    class="auth-input"
                    :class="{ 'auth-input-error': errors.captcha }"
                    type="text"
                    inputmode="numeric"
                    maxlength="6"
                    placeholder="6 位数字"
                  >
                </div>
                <button
                  type="button"
                  class="auth-send"
                  :disabled="captchaSending || captchaCountdown > 0"
                  @click="sendCaptcha"
                >
                  {{ captchaCountdown > 0 ? `${captchaCountdown} 秒后重发` : (captchaSending ? '发送中…' : '获取验证码') }}
                </button>
              </div>
              <p v-if="errors.captcha" class="auth-error">{{ errors.captcha }}</p>
              <p v-else-if="captchaHint" class="auth-hint">{{ captchaHint }}</p>
            </div>



            <div class="auth-grid">
              <div class="auth-field">
                <label class="auth-label" for="reg-password">设置密码<span class="auth-req">*</span></label>
                <input
                  id="reg-password"
                  v-model="form.password_wsh"
                  class="auth-input"
                  :class="{ 'auth-input-error': errors.password }"
                  type="password"
                  autocomplete="new-password"
                  placeholder="至少 6 位"
                >
                <p v-if="errors.password" class="auth-error">{{ errors.password }}</p>
              </div>

              <div class="auth-field">
                <label class="auth-label" for="reg-confirm">确认密码<span class="auth-req">*</span></label>
                <input
                  id="reg-confirm"
                  v-model="confirm_wsh"
                  class="auth-input"
                  :class="{ 'auth-input-error': errors.confirm }"
                  type="password"
                  autocomplete="new-password"
                  placeholder="再输入一次"
                >
                <p v-if="errors.confirm" class="auth-error">{{ errors.confirm }}</p>
              </div>

              <div class="auth-field">
                <div ref="turnstileRef" class="turnstile-wrap" aria-label="人机验证"></div>
                <p v-if="turnstileError" class="auth-error">{{ turnstileError }}</p>
              </div>

            </div>

            <button type="submit" class="cta cta-dark cta-lg" :disabled="loading">
              {{ loading ? '注册中…' : '注册并登录' }}
              <span class="cta-arrow" aria-hidden="true">→</span>
            </button>
          </form>

          <!-- footer -->
          <div class="auth-foot">
            <span>已经有账号？</span>
            <router-link to="/login" class="auth-link">直接登录</router-link>
          </div>
        </div>
      </main>

      <!-- ═══ 右侧：品牌承诺 ═══ -->
      <aside class="auth-side" aria-label="选择栖屿的理由">
        <p class="auth-side-label">Why 栖屿</p>

        <div>
          <h2 class="auth-side-title">把它交给谁，你应该看得见。</h2>
          <ul class="auth-side-list">
            <li class="auth-side-item">
              <span class="auth-side-idx">01</span>
              <span class="auth-side-body">
                <span class="auth-side-item-title">实名认证的照护师</span>
                <span class="auth-side-item-desc">每位照护师的资质由门店与平台双重核验。</span>
              </span>
            </li>
            <li class="auth-side-item">
              <span class="auth-side-idx">02</span>
              <span class="auth-side-body">
                <span class="auth-side-item-title">每日两次照护日报</span>
                <span class="auth-side-item-desc">进食、活动、排泄与情绪逐项记录，附实拍。</span>
              </span>
            </li>
            <li class="auth-side-item">
              <span class="auth-side-idx">03</span>
              <span class="auth-side-body">
                <span class="auth-side-item-title">20 分钟应急通道</span>
                <span class="auth-side-item-desc">与合作宠物医院直连，异常第一时间处置。</span>
              </span>
            </li>
          </ul>
        </div>

        <p class="auth-side-addr">上海 · 徐汇 / 静安 / 前滩 · 021-6420 8866</p>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { addDynamicRoutes } from '@/router'
import { register, requestRegisterCaptcha } from '@/api/auth'
import { useTurnstile } from '@/composables/useTurnstile'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const captchaSending = ref(false)
const captchaCountdown = ref(0)
const captchaHint = ref('')
const formError = ref('')
const confirm_wsh = ref('')
const countdownTimer = ref(null)

// Cloudflare Turnstile 人机验证
const turnstileRef = ref(null)
const { token: turnstileToken, error: turnstileError, reset: resetTurnstile } = useTurnstile(turnstileRef)

const form = reactive({
  username_wsh: '',
  nickname_wsh: '',
  phone_wsh: '',
  captcha_wsh: '',
  password_wsh: '',
})

const errors = reactive({
  username: '', nickname: '', phone: '', captcha: '', password: '', confirm: '',
})

// 发送验证码前校验手机号
const canSendCaptcha = computed(() => /^1\d{10}$/.test(String(form.phone_wsh || '').trim()))

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
  formError.value = ''
  if (!canSendCaptcha.value) {
    errors.phone = '请填写 11 位手机号后再获取验证码'
    return
  }
  errors.phone = ''
  captchaSending.value = true
  try {
    const r = await requestRegisterCaptcha({ phone_wsh: form.phone_wsh })
    if (r.code === 200 && r.data?.captcha_wsh) {
      captchaHint.value = `模拟验证码：${r.data.captcha_wsh}，有效期 ${r.data.expires_in_seconds_wsh || 300} 秒`
      startCountdown(60)
    } else {
      formError.value = r.message || r.msg || '验证码发送失败'
    }
  } catch (e) {
    formError.value = e.response?.data?.message || '验证码服务暂时无法连接，请稍后重试'
  } finally {
    captchaSending.value = false
  }
}

function validate() {
  const next = { username: '', nickname: '', phone: '', captcha: '', password: '', confirm: '' }
  if (form.username_wsh.trim().length < 3) next.username = '用户名至少 3 位'
  if (!form.nickname_wsh.trim()) next.nickname = '请填写昵称'
  if (!/^1\d{10}$/.test(form.phone_wsh)) next.phone = '请填写 11 位手机号'
  if (form.captcha_wsh.trim().length < 4) next.captcha = '请填写收到的验证码'
  if (form.password_wsh.length < 6) next.password = '密码至少 6 位'
  if (confirm_wsh.value !== form.password_wsh) next.confirm = '两次输入的密码不一致'
  Object.assign(errors, next)
  return Boolean(next.username || next.nickname || next.phone || next.captcha || next.password || next.confirm)
}

async function handleRegister() {
  formError.value = ''
  if (validate()) return

  if (!turnstileToken.value) {
    formError.value = '请先完成人机验证'
    return
  }

  loading.value = true
  try {
    const r = await register({
      username_wsh: form.username_wsh.trim(),
      nickname_wsh: form.nickname_wsh.trim(),
      phone_wsh: form.phone_wsh,
      captcha_wsh: form.captcha_wsh.trim(),
      password_wsh: form.password_wsh,
      turnstileToken: turnstileToken.value,
    })
    if (r.code === 200) {
      authStore.setAuth(r.data)
      addDynamicRoutes(r.data.roles_wsh || [])
      router.push('/dashboard')
    } else {
      formError.value = r.message || r.msg || '注册失败，请检查填写内容'
      resetTurnstile()
    }
  } catch (e) {
    formError.value = e.response?.data?.message || '注册服务暂时无法连接，请稍后重试'
    resetTurnstile()
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (countdownTimer.value) clearInterval(countdownTimer.value)
})
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   Uses shared --ref-* tokens from assets/css/design-tokens.css.
   Dark mode handled globally via html[data-theme="dark"].
   The dark "paper" aside uses the same fixed slab as Profile.vue.
   ═══════════════════════════════════════════════════════ */
.auth-page {
  --paper: #17130f;
  --cream-fixed: #f5efe7;
  --r-btn: 10px;
  --r-card: 14px;
  min-height: 100vh;
  width: 100%;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.auth-shell { display: flex; width: 100%; min-height: 100vh; }

.auth-main {
  flex: 1 1 52%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 56px 24px 72px;
}

.auth-inner {
  width: 100%; max-width: 420px; margin: 0 auto;
  animation: auth-in 0.3s ease both;
}
@keyframes auth-in {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ═══ Brand ═══ */
.auth-brand {
  display: inline-flex; align-items: center; gap: 12px;
  text-decoration: none; color: var(--ref-ink);
}
.auth-brand-mark {
  display: flex; align-items: center; justify-content: center;
  width: 36px; height: 36px; border-radius: 9px;
  border: 1px solid color-mix(in srgb, var(--ref-ink) 15%, transparent);
  background: var(--ref-surface);
  font-family: var(--ref-font-display); font-size: 15px; color: var(--ref-ink);
  transition: border-color 0.15s, color 0.15s;
}
.auth-brand:hover .auth-brand-mark {
  border-color: color-mix(in srgb, var(--ref-brand) 60%, transparent);
  color: var(--ref-brand);
}
.auth-brand-copy { display: flex; flex-direction: column; }
.auth-brand-name { font-size: 15px; font-weight: 500; letter-spacing: -0.01em; line-height: 1.1; }
.auth-brand-sub { margin-top: 5px; font-size: 9px; letter-spacing: 0.28em; text-transform: uppercase; color: var(--ref-muted); }

/* ═══ Header ═══ */
.auth-head { margin-top: 44px; }
.auth-eyebrow { font-size: 11px; letter-spacing: 0.22em; text-transform: uppercase; color: var(--ref-muted); }
.auth-title {
  margin: 14px 0 0; font-family: var(--ref-font-display);
  font-size: clamp(30px, 3.6vw, 40px); font-weight: 400; line-height: 1.12;
  letter-spacing: -0.02em; color: var(--ref-ink);
}
.auth-lede { margin: 14px 0 0; font-size: 13.5px; line-height: 1.75; color: var(--ref-ink-soft); }

/* ═══ Alerts ═══ */
.auth-alert {
  margin-top: 24px; padding: 12px 14px;
  border-radius: var(--r-btn); font-size: 13px; line-height: 1.6;
}
.auth-alert-error {
  background: color-mix(in srgb, var(--ref-brand-deep) 8%, var(--ref-surface));
  border: 1px solid color-mix(in srgb, var(--ref-brand-deep) 24%, transparent);
  color: var(--ref-brand-deep);
}

/* ═══ Form ═══ */
.auth-form { margin-top: 30px; display: grid; gap: 20px; }
.auth-grid { display: grid; grid-template-columns: 1fr; gap: 20px; }
.auth-field { display: grid; gap: 8px; }
.auth-label { font-size: 12.5px; font-weight: 500; color: var(--ref-ink-soft); }
.auth-req { margin-left: 3px; color: var(--ref-brand); }
.auth-input {
  width: 100%; height: var(--control-height); padding: 0 14px;
  border: 1px solid var(--ref-line); border-radius: var(--r-btn);
  background: var(--ref-surface); color: var(--ref-ink); font-size: 14px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.auth-input::placeholder { color: color-mix(in srgb, var(--ref-muted) 75%, transparent); }
.auth-input:focus {
  outline: none; border-color: var(--ref-brand);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 18%, transparent);
}
.auth-input-error { border-color: color-mix(in srgb, var(--ref-brand-deep) 60%, transparent); }
.auth-error {
  margin: 0; font-size: 11px; line-height: 1.6; color: var(--ref-brand-deep);
}
.auth-hint {
  margin: 0; font-size: 11px; line-height: 1.7; color: var(--ref-muted);
}

/* ═══ 验证码行 ═══ */
.auth-captcha-row { display: flex; align-items: flex-end; gap: 12px; }
.auth-captcha-input { flex: 1; min-width: 0; display: grid; gap: 8px; }
.auth-send {
  height: var(--control-height); padding: 0 16px; border-radius: var(--r-btn);
  border: 1px solid var(--ref-line); background: var(--ref-surface);
  color: var(--ref-ink-soft); font-size: 13px; font-weight: 500; cursor: pointer;
  white-space: nowrap; transition: border-color 0.15s, color 0.15s, background 0.15s;
}
.auth-send:hover:not(:disabled) { border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); color: var(--ref-ink); }
.auth-send:disabled { opacity: 0.5; cursor: not-allowed; }

/* ═══ Turnstile 人机验证 ═══ */
.turnstile-wrap {
  min-height: 65px; width: 100%;
  display: flex; align-items: center; justify-content: flex-start;
}
.turnstile-wrap:empty + p { display: none; }

/* ═══ CTA ═══ */
.cta {
  display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  height: 42px; padding: 0 18px; border-radius: var(--r-btn);
  font-size: 13px; font-weight: 500; cursor: pointer; border: 1px solid transparent;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover:not(:disabled) { transform: translateY(-1px); }
.cta:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.cta-dark { background: var(--ref-ink); color: var(--ref-cream); }
.cta-dark:hover:not(:disabled) { filter: brightness(1.18); }
.cta-lg { height: var(--control-height-lg); width: 100%; font-size: 14px; padding: 0 20px; }
.cta .cta-arrow { font-size: 15px; transition: transform 0.15s; }
.cta:hover .cta-arrow { transform: translateX(3px); }

/* ═══ Footer ═══ */
.auth-foot {
  display: flex; flex-wrap: wrap; align-items: center; gap: 6px;
  margin-top: 32px; padding-top: 22px; border-top: 1px solid var(--ref-line);
  font-size: 13px; color: var(--ref-muted);
}
.auth-link {
  color: var(--ref-ink); text-decoration: none; font-weight: 500;
  border-bottom: 1px solid color-mix(in srgb, var(--ref-ink) 25%, transparent);
  padding-bottom: 2px; transition: color 0.15s, border-color 0.15s;
}
.auth-link:hover { color: var(--ref-brand); border-color: var(--ref-brand); }

/* ═══ Aside ═══ */
.auth-side { display: none; }
.auth-side-label { font-size: 11px; letter-spacing: 0.22em; text-transform: uppercase; color: rgba(255, 255, 255, 0.4); }
.auth-side-title {
  max-width: 420px; margin: 0;
  font-family: var(--ref-font-display); font-size: clamp(28px, 2.6vw, 38px);
  font-weight: 400; line-height: 1.15; letter-spacing: -0.02em; color: var(--cream-fixed);
}
.auth-side-list { list-style: none; margin: 40px 0 0; padding: 0; display: grid; gap: 28px; }
.auth-side-item { display: flex; gap: 18px; }
.auth-side-idx { font-size: 11px; padding-top: 2px; color: rgba(255, 255, 255, 0.35); font-variant-numeric: tabular-nums; }
.auth-side-body { min-width: 0; border-left: 1px solid rgba(255, 255, 255, 0.12); padding-left: 18px; }
.auth-side-item-title { display: block; font-size: 15px; letter-spacing: -0.01em; color: var(--cream-fixed); }
.auth-side-item-desc { display: block; margin-top: 6px; max-width: 300px; font-size: 13px; line-height: 1.7; color: rgba(255, 255, 255, 0.55); }
.auth-side-addr { margin: 0; font-size: 11px; color: rgba(255, 255, 255, 0.35); font-variant-numeric: tabular-nums; }

/* ═══ Responsive ═══ */
@media (min-width: 900px) {
  .auth-main { flex: 1 1 52%; padding: 64px 40px 80px; }
  .auth-grid { grid-template-columns: 1fr 1fr; }
  .auth-side {
    display: flex; flex-direction: column; justify-content: space-between; gap: 48px;
    flex: 1 1 48%; padding: 56px 60px;
    background: var(--paper); color: var(--cream-fixed);
  }
}
@media (max-width: 520px) {
  .auth-main { padding: 40px 18px 56px; }
  .auth-head { margin-top: 36px; }
}
@media (prefers-reduced-motion: reduce) {
  .auth-inner { animation: none; }
}
</style>