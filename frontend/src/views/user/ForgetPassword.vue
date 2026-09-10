<template>
  <div class="auth-page">
    <div class="auth-shell">
      <!-- ═══ 左侧：表单 / 成功态 ═══ -->
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
            <p class="auth-eyebrow">重置密码</p>
            <h1 class="auth-title">{{ sent ? '重置链接已发送' : '找回密码' }}</h1>
            <p class="auth-lede">
              {{ sent ? '请查收邮件或短信中的重置链接，链接 30 分钟内有效。' : '填写注册时使用的邮箱或手机号，我们会发送一条重置链接。' }}
            </p>
          </header>

          <!-- 服务端错误（非字段校验） -->
          <div v-if="formError" class="auth-alert auth-alert-error" role="alert">{{ formError }}</div>

          <!-- sent state -->
          <div v-if="sent" class="auth-sent">
            <span class="auth-sent-icon" aria-hidden="true">✉</span>
            <p class="auth-sent-main">已向 <b class="auth-sent-acc">{{ account }}</b> 发送重置链接。</p>
            <p class="auth-sent-sub">没有收到？请检查垃圾邮件，或稍后再试一次。</p>
            <div class="auth-sent-actions">
              <router-link to="/login" class="cta cta-outline">返回登录</router-link>
              <button type="button" class="cta cta-ghost" @click="resetForm">换一个账号</button>
            </div>
          </div>

          <!-- form -->
          <form v-else class="auth-form" novalidate @submit.prevent="handleReset">
            <div class="auth-field">
              <label class="auth-label" for="fp-account">邮箱或手机号<span class="auth-req">*</span></label>
              <input
                id="fp-account"
                v-model="account"
                class="auth-input"
                :class="{ 'auth-input-error': error }"
                type="text"
                autofocus
                placeholder="注册时使用的邮箱或手机号"
              >
              <p v-if="error" class="auth-error">{{ error }}</p>
            </div>

            <div class="auth-field">
              <div ref="turnstileRef" class="turnstile-wrap" aria-label="人机验证"></div>
              <p v-if="turnstileError" class="auth-error">{{ turnstileError }}</p>
            </div>

            <button type="submit" class="cta cta-dark cta-lg" :disabled="loading">
              {{ loading ? '发送中…' : '发送重置链接' }}
              <span class="cta-arrow" aria-hidden="true">→</span>
            </button>
          </form>

          <!-- footer -->
          <div class="auth-foot">
            <span>想起密码了？</span>
            <router-link to="/login" class="auth-link">返回登录</router-link>
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
import { ref } from 'vue'
import { forgotPassword } from '@/api/auth'
import { useTurnstile } from '@/composables/useTurnstile'

const account = ref('')
const error = ref('')
const formError = ref('')
const sent = ref(false)
const loading = ref(false)

// Cloudflare Turnstile 人机验证
const turnstileRef = ref(null)
const { token: turnstileToken, error: turnstileError, reset: resetTurnstile } = useTurnstile(turnstileRef)

function resetForm() {
  sent.value = false
  error.value = ''
  formError.value = ''
  account.value = ''
}

async function handleReset() {
  error.value = ''
  formError.value = ''
  if (!account.value.trim()) {
    error.value = '请输入注册时使用的邮箱或手机号'
    return
  }

  if (!turnstileToken.value) {
    formError.value = '请先完成人机验证'
    return
  }

  loading.value = true
  try {
    const r = await forgotPassword({ email_wsh: account.value.trim(), turnstileToken: turnstileToken.value })
    if (r.code === 200) {
      sent.value = true
    } else {
      formError.value = r.message || r.msg || '发送失败，请确认账号是否正确'
      resetTurnstile()
    }
  } catch (e) {
    formError.value = e.response?.data?.message || '重置服务暂时无法连接，请稍后重试'
    resetTurnstile()
  } finally {
    loading.value = false
  }
}
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
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }
.cta-ghost { background: transparent; color: var(--ref-ink-soft); }
.cta-ghost:hover { color: var(--ref-brand); background: transparent; }
.cta-lg { height: var(--control-height-lg); width: 100%; font-size: 14px; padding: 0 20px; }
.cta .cta-arrow { font-size: 15px; transition: transform 0.15s; }
.cta:hover .cta-arrow { transform: translateX(3px); }

/* ═══ Sent state ═══ */
.auth-sent {
  margin-top: 30px; padding: 32px 26px;
  border: 1px solid var(--ref-line); border-radius: var(--r-card);
  background: var(--ref-surface); text-align: center;
}
.auth-sent-icon {
  display: flex; align-items: center; justify-content: center;
  width: 48px; height: var(--control-height-lg); margin: 0 auto; border-radius: 50%;
  background: var(--ref-sand); color: var(--ref-brand); font-size: 18px;
}
.auth-sent-main { margin: 20px 0 0; font-size: 13.5px; line-height: 1.7; color: var(--ref-ink-soft); }
.auth-sent-acc { color: var(--ref-ink); font-weight: 500; word-break: break-all; }
.auth-sent-sub { margin: 8px 0 0; font-size: 11px; line-height: 1.7; color: var(--ref-muted); }
.auth-sent-actions { margin-top: 24px; display: flex; flex-direction: column; gap: 10px; }

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