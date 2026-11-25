<template>
  <div class="as-page">
    <div class="as-shell">
      <!-- ═══ 面包屑 ═══ -->
      <nav class="as-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="as-crumb-link">首页</router-link>
        <span class="as-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/profile" class="as-crumb-link">个人中心</router-link>
        <span class="as-crumb-sep" aria-hidden="true">›</span>
        <span class="as-crumb-here">账户与安全</span>
      </nav>

      <!-- ═══ 标题 ═══ -->
      <header class="as-head">
        <h1 class="as-title">账户与安全</h1>
        <p class="as-lede">手机号、邮箱、实名与支付密码都在这里维护，改动会立刻影响登录与支付。</p>
      </header>

      <div v-if="loading" class="as-skel-stack" aria-hidden="true">
        <div class="as-skel"></div>
        <div class="as-skel"></div>
      </div>

      <template v-else>
        <!-- ═══ 账户信息 ═══ -->
        <section class="as-list" aria-label="账户信息">
          <div v-for="row in rows" :key="row.key" class="as-row">
            <span class="as-label">{{ row.label }}</span>
            <span class="as-value">
              <span class="as-value-text">{{ row.value }}</span>
              <span v-if="row.badge" :class="['as-badge', `as-badge-${row.badge.tone}`]">{{ row.badge.label }}</span>
            </span>
            <button
              v-if="row.action"
              type="button"
              class="as-action"
              @click="row.action.onClick()"
            >
              {{ row.action.label }}
            </button>
            <span v-else class="as-action as-action-static" aria-hidden="true">—</span>
          </div>
        </section>

        <!-- ═══ 危险操作 ═══ -->
        <section class="as-danger" aria-label="危险操作">
          <div class="as-danger-copy">
            <h2 class="as-danger-title">删除账号</h2>
            <p class="as-danger-desc">删除后资料、宠物档案与订单记录都会一并删除，且不可恢复。</p>
          </div>
          <button type="button" class="as-danger-btn" :disabled="deleting" @click="deleteDialogOpen = true">
            {{ deleting ? '删除中...' : '删除账号' }}
          </button>
        </section>
      </template>
    </div>

    <!-- ═══ 修改基本资料 ═══ -->
    <div v-if="editOpen" class="as-overlay" @click.self="editOpen = false">
      <div class="as-dialog" role="dialog" aria-modal="true" aria-label="修改基本资料">
        <header class="as-dialog-head">
          <h3 class="as-dialog-title">修改基本资料</h3>
          <button type="button" class="as-dialog-close" aria-label="关闭" @click="editOpen = false">✕</button>
        </header>

        <div class="as-dialog-body">
          <div class="as-field">
            <label class="as-field-label" for="as-nickname">昵称</label>
            <input
              id="as-nickname"
              v-model="draft.nickname_wsh"
              class="as-input"
              type="text"
              maxlength="20"
              placeholder="请输入昵称"
            >
          </div>

          <div class="as-field">
            <span class="as-field-label">性别</span>
            <div class="as-seg" role="group" aria-label="性别">
              <button
                v-for="option in genderOptions"
                :key="option.value"
                type="button"
                :class="['as-seg-btn', { 'is-active': draft.gender_wsh === option.value }]"
                :aria-pressed="draft.gender_wsh === option.value"
                @click="draft.gender_wsh = option.value"
              >
                {{ option.label }}
              </button>
            </div>
          </div>
        </div>

        <footer class="as-dialog-foot">
          <button type="button" class="as-btn-ghost" @click="editOpen = false">取消</button>
          <button type="button" class="as-btn-primary" :disabled="saving" @click="saveProfile">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </footer>
      </div>
    </div>

    <!-- ═══ 删除账号确认 ═══ -->
    <div v-if="deleteDialogOpen" class="as-overlay" @click.self="deleteDialogOpen = false">
      <div class="as-dialog" role="dialog" aria-modal="true" aria-label="确认删除账号">
        <header class="as-dialog-head">
          <h3 class="as-dialog-title">确认删除账号？</h3>
          <button type="button" class="as-dialog-close" aria-label="关闭" @click="deleteDialogOpen = false">✕</button>
        </header>

        <div class="as-dialog-body">
          <p class="as-warn">此操作不可恢复，资料、宠物档案与订单记录都会一并删除。</p>
          <p class="as-detail">
            {{ profile.nickname_wsh || profile.username_wsh || '当前账号' }} ·
            {{ summary.pets_wsh }} 份宠物档案 ·
            {{ summary.completed_orders_wsh }} 次已完成照护
          </p>
        </div>

        <footer class="as-dialog-foot">
          <button type="button" class="as-btn-ghost" @click="deleteDialogOpen = false">取消</button>
          <button type="button" class="as-danger-btn" :disabled="deleting" @click="confirmDeleteAccount">
            {{ deleting ? '删除中...' : '确认删除' }}
          </button>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { deleteCurrentUser, getCurrentUser, updateCurrentUser } from '@/api/auth'
import { getUserStatistics } from '@/api/statistics'
import { getOrders } from '@/api/order'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const loading = ref(true)
const saving = ref(false)
const deleting = ref(false)
const editOpen = ref(false)
const deleteDialogOpen = ref(false)

const profile = reactive({
  username_wsh: '', nickname_wsh: '', phone_wsh: '', email_wsh: '',
  avatar_wsh: '', gender_wsh: 0, real_name_wsh: '', real_name_status_wsh: 0,
  payment_password_set_wsh: false,
})
const stats = ref({ pets_wsh: 0, completed_orders_wsh: 0 })
const orders = ref([])
const draft = reactive({ nickname_wsh: '', gender_wsh: 0 })

const genderOptions = [
  { label: '未设置', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 },
]
const GENDER_LABEL = { 0: '未设置', 1: '男', 2: '女' }

function realNameBadgeMeta(status) {
  const map = {
    0: { label: '未实名', tone: 'closed' }, 1: { label: '实名审核中', tone: 'action' },
    2: { label: '已实名', tone: 'active' }, 3: { label: '实名被驳回', tone: 'action' },
  }
  return map[Number(status || 0)] || map[0]
}

const summary = computed(() => ({
  pets_wsh: stats.value.pets_wsh ?? 0,
  completed_orders_wsh: stats.value.completed_orders_wsh
    ?? orders.value.filter(o => o.status_wsh === 'completed').length,
}))

const rows = computed(() => {
  const verified = Number(profile.real_name_status_wsh || 0) === 2
  return [
    { key: 'username', label: '用户名', value: profile.username_wsh || '—' },
    {
      key: 'nickname', label: '昵称', value: profile.nickname_wsh || '未设置',
      action: { label: '修改', onClick: openEdit },
    },
    {
      key: 'gender', label: '性别', value: GENDER_LABEL[Number(profile.gender_wsh || 0)] ?? '未设置',
      action: { label: '修改', onClick: openEdit },
    },
    {
      key: 'phone', label: '手机号', value: profile.phone_wsh || '未绑定',
      action: { label: profile.phone_wsh ? '修改' : '绑定', onClick: () => goAccountEdit('phone') },
    },
    {
      key: 'email', label: '邮箱', value: profile.email_wsh || '未绑定',
      action: { label: profile.email_wsh ? '修改' : '绑定', onClick: () => goAccountEdit('email') },
    },
    {
      key: 'realName', label: '实名认证', value: profile.real_name_wsh || '未提交',
      badge: realNameBadgeMeta(profile.real_name_status_wsh),
      action: verified ? undefined : { label: '去认证', onClick: () => goAccountEdit('real-name') },
    },
    {
      key: 'paymentPassword', label: '支付密码',
      value: profile.payment_password_set_wsh ? '已设置' : '未设置',
      action: {
        label: profile.payment_password_set_wsh ? '修改' : '设置',
        onClick: () => goAccountEdit('payment-password'),
      },
    },
  ]
})

function goAccountEdit(type) { router.push('/profile/' + type) }

function openEdit() {
  draft.nickname_wsh = profile.nickname_wsh || ''
  draft.gender_wsh = Number(profile.gender_wsh || 0)
  editOpen.value = true
}

function syncUser(userData) {
  if (!userData) return
  Object.assign(profile, userData)
  if (authStore.user) {
    authStore.user = { ...authStore.user, ...userData }
    localStorage.setItem('user', JSON.stringify(authStore.user))
  }
}

async function saveProfile() {
  if (saving.value) return
  saving.value = true
  try {
    const payload = {
      nickname_wsh: draft.nickname_wsh,
      avatar_wsh: profile.avatar_wsh,
      gender_wsh: draft.gender_wsh,
    }
    const r = await updateCurrentUser(payload)
    if (r.code === 200) {
      appStore.addToast('保存成功', 'success')
      syncUser(r.data || { ...profile, ...payload })
      editOpen.value = false
    }
  } catch (e) {
    appStore.addToast('保存失败', 'error')
  } finally { saving.value = false }
}

async function confirmDeleteAccount() {
  if (deleting.value) return
  deleting.value = true
  try {
    const r = await deleteCurrentUser()
    if (r.code === 200) {
      appStore.addToast('账号已删除', 'success')
      authStore.clearAuth()
      router.replace('/login')
    }
  } catch (e) {
    appStore.addToast(e.response?.data?.message || '删除失败', 'error')
  } finally { deleting.value = false }
}

onMounted(async () => {
  try {
    const [ur, sr] = await Promise.all([getCurrentUser(), getUserStatistics()])
    if (ur.code === 200) syncUser(ur.data)
    if (sr.code === 200) stats.value = sr.data || {}
  } catch (e) {
    appStore.addToast('账户信息加载失败', 'error')
  } finally { loading.value = false }

  try {
    const r = await getOrders()
    if (r?.code === 200) orders.value = Array.isArray(r.data) ? r.data : (r.data?.list || [])
  } catch (e) { orders.value = [] }
})
</script>

<style scoped>
.as-page {
  --r-btn: 10px;
  --r-card: 14px;
  width: 100%;
  min-height: 60vh;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}
.as-shell { max-width: 860px; margin: 0 auto; padding: 0 24px; }

.as-crumb { display: flex; align-items: center; gap: 8px; padding: 6px 0; font-size: 12.5px; color: var(--ref-muted); }
.as-crumb-link { color: var(--ref-muted); text-decoration: none; }
.as-crumb-link:hover { color: var(--ref-ink); }
.as-crumb-sep { color: var(--ref-line); }
.as-crumb-here { color: var(--ref-ink-soft); }

.as-head { margin: 18px 0 24px; }
.as-title {
  margin: 0; font-family: var(--ref-font-display);
  font-size: 30px; font-weight: 400; line-height: 1.2; letter-spacing: -0.02em; color: var(--ref-ink);
}
.as-lede { margin: 10px 0 0; max-width: 620px; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); }

/* 账户信息列表 */
.as-list {
  overflow: hidden; border: 1px solid var(--ref-line);
  border-radius: var(--r-card); background: var(--ref-surface);
}
.as-row {
  display: flex; align-items: center; gap: 16px;
  padding: 14px 20px; border-bottom: 1px solid var(--ref-line);
}
.as-row:last-child { border-bottom: 0; }
.as-label { width: 88px; flex-shrink: 0; font-size: 12.5px; color: var(--ref-muted); }
.as-value { display: flex; align-items: center; gap: 8px; min-width: 0; flex: 1; }
.as-value-text {
  min-width: 0; font-size: 13px; color: var(--ref-ink);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.as-action {
  flex-shrink: 0; height: 30px; padding: 0 12px;
  border: 1px solid color-mix(in srgb, var(--ref-ink) 18%, transparent);
  border-radius: 8px; background: transparent; color: var(--ref-ink);
  font-size: 12px; font-weight: 500; cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.as-action:hover { background: var(--ref-ink); border-color: var(--ref-ink); color: var(--ref-canvas); }
.as-action-static {
  display: inline-flex; align-items: center; justify-content: center;
  width: 30px; border: 0; color: var(--ref-line); cursor: default;
}

.as-badge {
  display: inline-flex; align-items: center; flex-shrink: 0;
  padding: 3px 8px; border-radius: 6px; border: 1px solid transparent;
  font-size: 10px; font-weight: 500; line-height: 1; white-space: nowrap;
}
.as-badge-action { background: var(--ref-brand); color: #fff; }
.as-badge-active {
  background: color-mix(in srgb, var(--ref-moss, #3f5347) 12%, transparent);
  color: var(--ref-moss, #3f5347);
  border-color: color-mix(in srgb, var(--ref-moss, #3f5347) 25%, transparent);
}
.as-badge-closed { background: transparent; color: var(--ref-muted); border-color: var(--ref-line); }

/* 危险操作 */
.as-danger {
  display: flex; align-items: center; justify-content: space-between; gap: 20px;
  margin-top: 16px; padding: 18px 20px;
  border: 1px solid color-mix(in srgb, var(--ref-brand-deep, #9a411a) 30%, var(--ref-line));
  border-radius: var(--r-card); background: var(--ref-surface);
}
.as-danger-copy { min-width: 0; }
.as-danger-title { margin: 0; font-size: 14px; font-weight: 500; color: var(--ref-ink); }
.as-danger-desc { margin: 6px 0 0; font-size: 11.5px; line-height: 1.6; color: var(--ref-muted); }
.as-danger-btn {
  flex-shrink: 0; height: 34px; padding: 0 14px;
  border: 1px solid color-mix(in srgb, var(--ref-brand-deep, #9a411a) 55%, transparent);
  border-radius: var(--r-btn); background: transparent;
  color: var(--ref-brand-deep, #9a411a); font-size: 12.5px; font-weight: 500; cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.as-danger-btn:hover { background: var(--ref-brand-deep, #9a411a); color: #fff; }
.as-danger-btn:disabled { opacity: 0.55; cursor: not-allowed; }

/* 弹层 */
.as-overlay {
  position: fixed; inset: 0; z-index: 80;
  display: flex; align-items: center; justify-content: center;
  padding: 24px; background: rgba(24, 18, 12, 0.45);
  animation: as-fade 0.15s ease-out;
}
.as-dialog {
  width: 100%; max-width: 420px; overflow: hidden;
  border: 1px solid var(--ref-line); border-radius: 18px;
  background: var(--ref-surface); box-shadow: 0 40px 80px -40px rgba(0, 0, 0, 0.5);
  animation: as-pop 0.2s ease-out;
}
.as-dialog-head {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 16px 20px; border-bottom: 1px solid var(--ref-line);
}
.as-dialog-title { margin: 0; font-size: 15px; font-weight: 500; color: var(--ref-ink); }
.as-dialog-close {
  width: 28px; height: 28px; border: 0; border-radius: 8px;
  background: transparent; color: var(--ref-muted); font-size: 13px; cursor: pointer;
}
.as-dialog-close:hover { background: var(--ref-sand); color: var(--ref-ink); }
.as-dialog-body { display: flex; flex-direction: column; gap: 16px; padding: 20px; }
.as-dialog-foot {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 14px 20px; border-top: 1px solid var(--ref-line); background: var(--ref-cream);
}

.as-field { display: flex; flex-direction: column; gap: 8px; }
.as-field-label { font-size: 11.5px; letter-spacing: 0.02em; color: var(--ref-muted); }
.as-input {
  height: 40px; padding: 0 12px;
  border: 1px solid var(--ref-line); border-radius: var(--r-btn);
  background: var(--ref-surface); color: var(--ref-ink);
  font-size: 13px; font-family: inherit; outline: none;
  transition: border-color 0.15s;
}
.as-input:focus { border-color: color-mix(in srgb, var(--ref-ink) 40%, transparent); }

.as-seg { display: flex; gap: 8px; }
.as-seg-btn {
  flex: 1; height: 36px; border: 1px solid var(--ref-line); border-radius: var(--r-btn);
  background: var(--ref-surface); color: var(--ref-ink-soft);
  font-size: 12.5px; cursor: pointer; transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.as-seg-btn:hover { border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); }
.as-seg-btn.is-active { background: var(--ref-ink); border-color: var(--ref-ink); color: var(--ref-canvas); }

.as-btn-ghost {
  height: 36px; padding: 0 16px; border: 1px solid var(--ref-line); border-radius: var(--r-btn);
  background: var(--ref-surface); color: var(--ref-ink-soft); font-size: 12.5px; cursor: pointer;
}
.as-btn-ghost:hover { border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); color: var(--ref-ink); }
.as-btn-primary {
  height: 36px; padding: 0 18px; border: 1px solid var(--ref-ink); border-radius: var(--r-btn);
  background: var(--ref-ink); color: var(--ref-canvas); font-size: 12.5px; font-weight: 500; cursor: pointer;
}
.as-btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }

.as-warn {
  margin: 0; padding: 10px 12px; border-radius: var(--r-btn);
  background: color-mix(in srgb, var(--ref-brand) 10%, transparent);
  color: var(--ref-brand-deep, #9a411a); font-size: 12px; line-height: 1.6;
}
.as-detail { margin: 0; font-size: 12px; line-height: 1.7; color: var(--ref-muted); }

.as-skel-stack { display: flex; flex-direction: column; gap: 12px; }
.as-skel {
  height: 120px; border-radius: var(--r-card); border: 1px solid var(--ref-line);
  background: linear-gradient(100deg, var(--ref-sand) 30%, var(--ref-cream) 50%, var(--ref-sand) 70%);
  background-size: 200% 100%;
  animation: as-shimmer 1.6s linear infinite;
}

@keyframes as-fade { from { opacity: 0; } to { opacity: 1; } }
@keyframes as-pop { from { opacity: 0; transform: translateY(10px) scale(0.98); } to { opacity: 1; transform: none; } }
@keyframes as-shimmer { to { background-position: -200% 0; } }

@media (max-width: 640px) {
  .as-shell { padding: 0 16px; }
  .as-title { font-size: 24px; }
  .as-label { width: 72px; }
  .as-danger { flex-direction: column; align-items: stretch; }
  .as-danger-btn { width: 100%; }
}
@media (prefers-reduced-motion: reduce) {
  .as-overlay, .as-dialog, .as-skel { animation: none; }
}
</style>
