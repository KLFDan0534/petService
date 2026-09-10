<template>
  <div class="auth-page">
    <div class="auth-shell">
      <!-- ═══ 左侧：表单 ═══ -->
      <main class="auth-main">
        <div class="auth-inner">
          <!-- 品牌标识 -->
          <router-link to="/dashboard" class="auth-brand" aria-label="返回栖屿宠护首页">
            <span class="auth-brand-mark" aria-hidden="true">栖</span>
            <span class="auth-brand-copy">
              <span class="auth-brand-name">栖屿宠护</span>
              <span class="auth-brand-sub">宠物寄养</span>
            </span>
          </router-link>

          <!-- 标题区 -->
          <header class="auth-head">
            <p class="auth-eyebrow">登录</p>
            <h1 class="auth-title">欢迎回来</h1>
            <p class="auth-lede">登录后可以查看订单进度、照护日报，以及管理宠物档案。</p>
          </header>

          <!-- 服务端错误（非字段校验） -->
          <div v-if="formError" class="auth-alert auth-alert-error" role="alert">{{ formError }}</div>

          <!-- 表单 -->
          <form class="auth-form" novalidate @submit.prevent="handleLogin">
            <div class="auth-field">
              <label class="auth-label" for="login-username">用户名<span class="auth-req">*</span></label>
              <input
                id="login-username"
                v-model="username_wsh"
                class="auth-input"
                :class="{ 'auth-input-error': errors.username }"
                type="text"
                autocomplete="username"
                autofocus
                placeholder="手机号或用户名"
                :aria-invalid="!!errors.username"
              >
              <p v-if="errors.username" class="auth-error">{{ errors.username }}</p>
            </div>

            <div class="auth-field">
              <label class="auth-label" for="login-password">密码<span class="auth-req">*</span></label>
              <input
                id="login-password"
                v-model="password_wsh"
                class="auth-input"
                :class="{ 'auth-input-error': errors.password }"
                type="password"
                autocomplete="current-password"
                placeholder="请输入登录密码"
                aria-invalid="false"
              >
              <p v-if="errors.password" class="auth-error">{{ errors.password }}</p>
            </div>

            <div class="auth-forgot">
              <router-link to="/forget-password" class="auth-link-sm">忘记密码？</router-link>
            </div>

            <div class="auth-field">
              <div ref="turnstileRef" class="turnstile-wrap" aria-label="人机验证"></div>
              <p v-if="turnstileError" class="auth-error">{{ turnstileError }}</p>
            </div>

            <button type="submit" class="cta cta-dark cta-lg" :disabled="loading">
              {{ loading ? '登录中…' : '登录' }}
              <span class="cta-arrow" aria-hidden="true">→</span>
            </button>
          </form>

          <p class="auth-note">登录即表示你同意《服务协议》与《隐私政策》。账号仅用于订单与照护记录，不会用于营销推送。</p>

          <!-- 页脚 -->
          <div class="auth-foot">
            <span>还没有账号？</span>
            <router-link to="/register" class="auth-link">注册新账号</router-link>
          </div>

          <!-- 游客访问 -->
          <div class="auth-guest">
            <router-link to="/dashboard" class="text-link">
              <span class="tl-text">以游客身份访问</span>
              <span class="tl-arrow">→</span>
            </router-link>
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
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { addDynamicRoutes } from '@/router'
import { safeRedirect } from '@/utils/safeRedirect'
import { useTurnstile } from '@/composables/useTurnstile'
import axios from 'axios'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const username_wsh = ref('')
const password_wsh = ref('')
const errors = reactive({ username: '', password: '' })
const formError = ref('')
const loading = ref(false)

// Cloudflare Turnstile 人机验证
const turnstileRef = ref(null)
const { token: turnstileToken, error: turnstileError, reset: resetTurnstile } = useTurnstile(turnstileRef)

async function handleLogin() {
  // 逐字段校验
  const next = { username: '', password: '' }
  if (!username_wsh.value.trim()) next.username = '请输入用户名'
  if (password_wsh.value.length < 6) next.password = '密码至少 6 位'
  errors.username = next.username
  errors.password = next.password
  formError.value = ''
  if (next.username || next.password) return

  if (!turnstileToken.value) {
    formError.value = '请先完成人机验证'
    return
  }

  loading.value = true
  try {
    const r = await axios.post('/api/auth/login', {
      username_wsh: username_wsh.value.trim(),
      password_wsh: password_wsh.value,
      turnstileToken: turnstileToken.value,
    })
    if (r.data.code === 200) {
      authStore.setAuth(r.data.data)
      addDynamicRoutes(r.data.data.roles_wsh || [])
      const redirect = safeRedirect(route.query.redirect) || '/dashboard'
      router.push(redirect)
    } else {
      errors.password = r.data.msg || r.data.message || '用户名或密码不正确'
      resetTurnstile()
    }
  } catch (e) {
    formError.value = e.response?.data?.message || '登录服务暂时无法连接，请稍后重试'
    resetTurnstile()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   使用 assets/css/design-tokens.css 中的共享 --ref-* 设计令牌。
   暗色模式由全局的 html[data-theme="dark"] 统一处理。
   右侧深色「纸张」侧栏与 Profile.vue 复用同一块固定色板。
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

/* ═══ 品牌标识 ═══ */
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

/* ═══ 标题区 ═══ */
.auth-head { margin-top: 44px; }
.auth-eyebrow { font-size: 11px; letter-spacing: 0.22em; text-transform: uppercase; color: var(--ref-muted); }
.auth-title {
  margin: 14px 0 0; font-family: var(--ref-font-display);
  font-size: clamp(30px, 3.6vw, 40px); font-weight: 400; line-height: 1.12;
  letter-spacing: -0.02em; color: var(--ref-ink);
}
.auth-lede { margin: 14px 0 0; font-size: 13.5px; line-height: 1.75; color: var(--ref-ink-soft); }

/* ═══ 警告提示 ═══ */
.auth-alert {
  margin-top: 24px; padding: 12px 14px;
  border-radius: var(--r-btn); font-size: 13px; line-height: 1.6;
}
.auth-alert-error {
  background: color-mix(in srgb, var(--ref-brand-deep) 8%, var(--ref-surface));
  border: 1px solid color-mix(in srgb, var(--ref-brand-deep) 24%, transparent);
  color: var(--ref-brand-deep);
}

/* ═══ 表单 ═══ */
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
.auth-forgot { display: flex; justify-content: flex-end; }
.auth-link-sm { font-size: 12.5px; color: var(--ref-muted); text-decoration: none; transition: color 0.15s; }
.auth-link-sm:hover { color: var(--ref-brand); }

/* ═══ Turnstile 人机验证 ═══ */
.turnstile-wrap {
  min-height: 65px; width: 100%;
  display: flex; align-items: center; justify-content: flex-start;
}
.turnstile-wrap:empty + p { display: none; }

/* ═══ 主按钮（CTA） ═══ */
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

.auth-note { margin: 18px 0 0; font-size: 11px; line-height: 1.7; color: var(--ref-muted); }

/* ═══ 页脚 ═══ */
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

/* ═══ 游客访问 ═══ */
.auth-guest { margin-top: 22px; }
.text-link {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 13px; font-weight: 500; color: var(--ref-ink);
  text-decoration: none; transition: color 0.15s;
}
.text-link:hover { color: var(--ref-brand); }
.tl-text { border-bottom: 1px solid color-mix(in srgb, var(--ref-ink) 25%, transparent); padding-bottom: 2px; }
.text-link:hover .tl-text { border-color: var(--ref-brand); }
.tl-arrow { transition: transform 0.15s; }
.text-link:hover .tl-arrow { transform: translateX(4px); }

/* ═══ 右侧品牌栏 ═══ */
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

/* ═══ 响应式 ═══ */
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