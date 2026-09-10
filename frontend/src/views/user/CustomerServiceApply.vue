<template>
  <div class="csa-page">
    <div class="csa-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="csa-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="csa-crumb-link">首页</router-link>
        <span class="csa-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/profile" class="csa-crumb-link">个人中心</router-link>
        <span class="csa-crumb-sep" aria-hidden="true">›</span>
        <span class="csa-crumb-here">客服入驻申请</span>
      </nav>

      <!-- ═══ Hero ═══ -->
      <header class="csa-head">
        <div class="csa-head-copy">
          <div class="csa-eyebrow" aria-hidden="true">
            <span class="csa-eyebrow-line"></span>
            <span>客服申请</span>
          </div>
          <h1 class="csa-title">申请成为平台客服</h1>
          <p class="csa-sub">客服负责处理某个商家的用户工单与投诉。通过审核后，你会获得该商家的客服工作台权限。</p>
          <dl class="csa-facts" aria-label="申请概览">
            <div class="csa-fact">
              <dd class="tabular">{{ loading ? '--' : applications.length }}</dd>
              <dt>已提交申请</dt>
            </div>
          </dl>
        </div>
      </header>

      <!-- ═══ 01 · 申请流程 ═══ -->
      <section class="csa-section" aria-label="申请流程">
        <header class="csa-sec-head">
          <p class="csa-eyebrow csa-sec-eyebrow">
            <span class="csa-idx">01</span>
            <span class="csa-line" aria-hidden="true"></span>
            <span>流程说明</span>
          </p>
          <h2 class="csa-sec-title">申请流程</h2>
          <p class="csa-sec-desc">三步完成，审核结果会在这个页面更新。</p>
        </header>

        <ol class="csa-steps">
          <li v-for="(step, index) in steps" :key="step.label" class="csa-step">
            <p class="csa-step-idx tabular">0{{ index + 1 }}</p>
            <h3 class="csa-step-title">{{ step.label }}</h3>
            <p class="csa-step-detail">{{ step.detail }}</p>
          </li>
        </ol>
      </section>

      <!-- ═══ 02 · 填写申请 ═══ -->
      <section class="csa-section" aria-label="填写申请">
        <header class="csa-sec-head">
          <p class="csa-eyebrow csa-sec-eyebrow">
            <span class="csa-idx">02</span>
            <span class="csa-line" aria-hidden="true"></span>
            <span>申请</span>
          </p>
          <h2 class="csa-sec-title">填写申请</h2>
        </header>

        <div class="csa-form-wrap">
          <div v-if="loading" class="csa-skeleton" />
          <div v-else-if="error" class="csa-error">
            <p class="csa-error-title">加载失败</p>
            <p class="csa-error-desc">{{ error }}</p>
            <button type="button" class="cta cta-outline" @click="loadMerchantsAndApplications">重试</button>
          </div>

          <form v-else class="csa-form" novalidate @submit.prevent="submitApplication">
            <p v-if="pending" class="csa-pending">
              你已有一份申请正在审核中。可以继续申请其他商家，同一商家请等待当前结果。
            </p>

            <div class="csa-field">
              <div class="csa-field-head">
                <label class="csa-label" for="csa-merchant">服务商家</label>
                <span class="csa-required" aria-hidden="true">*</span>
              </div>
              <select
                id="csa-merchant"
                v-model="form.merchant_id_wsh"
                class="csa-select"
                :class="{ invalid: errors.merchant }"
                @change="errors.merchant = ''"
              >
                <option value="" disabled>选择你要服务的商家</option>
                <option v-for="merchant in merchants" :key="merchant.id_wsh" :value="merchant.id_wsh">
                  {{ merchant.name_wsh || `商家 #${merchant.id_wsh}` }}
                </option>
              </select>
              <p v-if="errors.merchant" class="csa-hint csa-hint-error">{{ errors.merchant }}</p>
              <p v-else class="csa-hint">客服权限按商家隔离，你只能处理所服务商家的工单与投诉。</p>
            </div>

            <div class="csa-field">
              <div class="csa-field-head">
                <label class="csa-label" for="csa-note">申请说明</label>
                <span class="csa-required" aria-hidden="true">*</span>
              </div>
              <textarea
                id="csa-note"
                v-model="form.applicant_note_wsh"
                rows="6"
                maxlength="500"
                class="csa-textarea"
                :class="{ invalid: errors.note }"
                placeholder="介绍你的客服或宠物服务经验、每天可投入的时间段，以及为什么想服务这家门店。"
                @input="errors.note = ''"
              />
              <div v-if="errors.note" class="csa-hint csa-hint-error">{{ errors.note }}</div>
              <div v-else class="csa-hint">{{ form.applicant_note_wsh.length }}/500 · 商家主要依据这段说明做判断。</div>
            </div>

            <div class="csa-form-foot">
              <button type="submit" class="cta cta-primary" :disabled="submitting || !merchants.length">
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M3 11h18" /><path d="M12 4v14" /><path d="M6 10c3 2 6 2 9 0" /><path d="M4 18h16" />
                </svg>
                {{ submitting ? '提交中…' : '提交申请' }}
              </button>
              <p v-if="!merchants.length" class="csa-foot-note">暂时没有可申请的商家。</p>
            </div>
          </form>
        </div>
      </section>

      <!-- ═══ 03 · 我的申请记录 ═══ -->
      <section class="csa-section" aria-label="我的申请记录">
        <header class="csa-sec-head">
          <p class="csa-eyebrow csa-sec-eyebrow">
            <span class="csa-idx">03</span>
            <span class="csa-line" aria-hidden="true"></span>
            <span>历史</span>
          </p>
          <h2 class="csa-sec-title">我的申请记录</h2>
          <p class="csa-sec-desc">每一次提交的审核状态与商家意见。</p>
        </header>

        <div v-if="!loading && !applications.length" class="csa-empty">
          <svg viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M9 11l3 3L22 4" /><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
          </svg>
          <h3 class="csa-empty-title">还没有提交过申请</h3>
          <p class="csa-empty-desc">选择一个商家并说明你的经验，提交后审核结果会显示在这里。</p>
        </div>

        <ul v-else-if="applications.length" class="csa-list">
          <li v-for="(item, index) in applications" :key="item.id_wsh ?? index" class="csa-row">
            <div class="csa-row-main">
              <p class="csa-row-title">{{ item.nickname_wsh || item.username_wsh || '客服申请' }}</p>
              <p v-if="item.applicant_note_wsh" class="csa-row-clamp">{{ item.applicant_note_wsh }}</p>
              <p v-if="item.review_note_wsh" class="csa-row-review">审核意见：{{ item.review_note_wsh }}</p>
            </div>
            <div class="csa-row-side">
              <span :class="['csa-badge', badgeClass(item.status_wsh)]">{{ statusLabel(item.status_wsh) }}</span>
              <span class="csa-row-date tabular">{{ dateText(item.created_at_wsh) }}</span>
            </div>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { getMerchants } from '@/api/merchant'
import {
  applyMerchantCustomerService,
  getMyCustomerServiceApplications,
} from '@/api/merchantCustomerService'

const appStore = useAppStore()

const STEPS = [
  { label: '选择商家', detail: '客服权限按商家授予，你只会看到该商家的工单与投诉。' },
  { label: '说明经验', detail: '写清楚你的服务经验与可投入时间，这是商家判断的主要依据。' },
  { label: '等待审核', detail: '商家或平台管理员审核通过后，客服工作台会自动开放。' },
]
const steps = STEPS

const merchants = ref([])
const applications = ref([])
const loading = ref(true)
const error = ref('')
const submitting = ref(false)

const form = reactive({
  merchant_id_wsh: '',
  applicant_note_wsh: '',
})
const errors = reactive({ merchant: '', note: '' })

const pending = computed(() => applications.value.some((a) => a.status_wsh === 'pending'))

function statusLabel(status) {
  return {
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝',
  }[status] || status || '未知'
}

function badgeClass(status) {
  return {
    pending: 'badge-warning',
    approved: 'badge-success',
    rejected: 'badge-danger',
  }[status] || 'badge-info'
}

function dateText(value) {
  if (!value) return ''
  try {
    return new Date(value).toLocaleString('zh-CN', { dateStyle: 'short', timeStyle: 'short' })
  } catch (e) {
    return String(value)
  }
}

function validate() {
  errors.merchant = form.merchant_id_wsh ? '' : '请选择要服务的商家。'
  if (!form.applicant_note_wsh.trim()) {
    errors.note = '请填写申请说明。'
  } else if (form.applicant_note_wsh.trim().length < 10) {
    errors.note = '申请说明至少需要 10 个字，商家才能更好判断。'
  } else {
    errors.note = ''
  }
  return !errors.merchant && !errors.note
}

async function loadMerchantsAndApplications() {
  error.value = ''
  loading.value = true
  try {
    await Promise.all([loadMerchants(), loadApplications()])
  } catch (e) {
    error.value = e?.message || '内容加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

async function loadMerchants() {
  const response = await getMerchants()
  if (response.code === 200) {
    merchants.value = response.data || []
  }
}

async function loadApplications() {
  const response = await getMyCustomerServiceApplications()
  if (response.code === 200) {
    applications.value = (response.data || []).map((item) => ({
      ...item,
      merchant_name_wsh: item.merchant_name_wsh || `商家 #${item.merchant_id_wsh}`,
      applicant_note_wsh: item.applicant_note_wsh || '',
      review_note_wsh: item.review_note_wsh || '',
    }))
  }
}

async function submitApplication() {
  if (submitting.value) return
  if (!validate()) return
  submitting.value = true
  try {
    const response = await applyMerchantCustomerService({
      merchant_id_wsh: Number(form.merchant_id_wsh),
      applicant_note_wsh: form.applicant_note_wsh.trim(),
    })
    if (response.code === 200) {
      appStore.addToast('申请已提交，请等待商家审核', 'success')
      form.merchant_id_wsh = ''
      form.applicant_note_wsh = ''
      errors.merchant = ''
      errors.note = ''
      await loadApplications()
    } else {
      appStore.addToast(response.message || '提交申请失败', 'error')
    }
  } catch (e) {
    appStore.addToast(e?.response?.data?.message || '提交申请失败', 'error')
  } finally {
    submitting.value = false
  }
}

onMounted(loadMerchantsAndApplications)
</script>

<style scoped>
.csa-page {
  width: 100%;
  padding: 6px 0 72px;
  min-height: 60vh;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}
.csa-shell { max-width: 1180px; margin: 0 auto; padding: 0 24px; }

/* Breadcrumb */
.csa-crumb { display: flex; align-items: center; gap: 8px; padding: 6px 0; font-size: 12.5px; color: var(--ref-muted); }
.csa-crumb-link { color: var(--ref-muted); text-decoration: none; }
.csa-crumb-link:hover { color: var(--ref-ink); }
.csa-crumb-sep { color: var(--ref-line); }
.csa-crumb-here { color: var(--ref-ink-soft); }

/* Eyebrow */
.csa-eyebrow { display: flex; align-items: center; gap: 10px; font-size: 9.5px; letter-spacing: 0.28em; text-transform: uppercase; color: var(--ref-muted); }
.csa-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.csa-idx { font-variant-numeric: tabular-nums; }
.csa-line { width: 24px; height: 1px; background: var(--ref-line); }

/* Hero */
.csa-head { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 20px 24px; padding: 40px 0 28px; }
.csa-head-copy { min-width: 0; }
.csa-title { margin: 18px 0 0; font-family: var(--ref-font-display); font-size: clamp(34px, 4.4vw, 52px); line-height: 1.12; letter-spacing: -0.01em; font-weight: 500; color: var(--ref-ink); text-wrap: balance; }
.csa-sub { margin: 14px 0 0; max-width: 640px; font-size: 14px; line-height: 1.75; color: color-mix(in srgb, var(--ref-ink-soft) 82%, transparent); }
.csa-facts { display: flex; flex-wrap: wrap; gap: 0 28px; margin: 26px 0 0; }
.csa-fact { display: flex; flex-direction: column; }
.csa-fact dd { margin: 0; font-family: var(--ref-font-display); font-size: 28px; line-height: 1; letter-spacing: -0.01em; color: var(--ref-ink); }
.csa-fact dt { margin-top: 8px; font-size: 12px; color: var(--ref-muted); }

/* Section */
.csa-section { margin-top: 48px; }
.csa-sec-eyebrow { font-size: 10px; letter-spacing: 0.22em; }
.csa-sec-title { margin: 10px 0 0; font-family: var(--ref-font-display); font-size: 24px; font-weight: 400; line-height: 1.2; letter-spacing: -0.02em; color: var(--ref-ink); }
.csa-sec-desc { margin: 8px 0 0; font-size: 13.5px; color: var(--ref-muted); }

/* Steps */
.csa-steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  margin: 24px 0 0;
  padding: 0;
  list-style: none;
  overflow: hidden;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-lg);
  background: var(--ref-line);
}
.csa-step {
  background: var(--ref-surface);
  padding: 20px;
}
.csa-step-idx { margin: 0; font-size: 11px; letter-spacing: 0.2em; text-transform: uppercase; color: var(--ref-muted); }
.csa-step-title { margin: 10px 0 0; font-family: var(--ref-font-display); font-size: 17px; font-weight: 500; line-height: 1.3; color: var(--ref-ink); }
.csa-step-detail { margin: 8px 0 0; font-size: 12.5px; line-height: 1.7; color: var(--ref-muted); }

/* Form */
.csa-form-wrap { margin-top: 24px; }
.csa-form {
  max-width: 640px;
  padding: 28px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-card);
  background: var(--ref-surface);
}
.csa-pending {
  margin: 0 0 20px;
  padding: 12px 16px;
  border: 1px solid color-mix(in srgb, var(--ref-brand) 30%, transparent);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--ref-brand) 6%, transparent);
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
}
.csa-field { margin-top: 18px; }
.csa-field:first-child { margin-top: 0; }
.csa-field-head { display: flex; align-items: center; gap: 4px; margin-bottom: 8px; }
.csa-label { font-size: 13px; font-weight: 600; color: var(--ref-ink); }
.csa-required { color: var(--color-danger, #e5484d); font-size: 12px; }
.csa-select,
.csa-textarea {
  width: 100%;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-control);
  background: var(--ref-canvas);
  color: var(--ref-ink);
  font-size: 13.5px;
  line-height: 1.6;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.csa-select { height: var(--control-height); padding: 0 12px; }
.csa-textarea { padding: 12px; resize: vertical; min-height: 120px; }
.csa-select:focus,
.csa-textarea:focus {
  outline: none;
  border-color: var(--ref-brand);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 18%, transparent);
}
.csa-select.invalid,
.csa-textarea.invalid { border-color: var(--color-danger, #e5484d); }
.csa-hint { margin: 8px 2px 0; font-size: 12px; line-height: 1.7; color: var(--ref-muted); }
.csa-hint-error { color: var(--color-danger, #e5484d); }
.csa-form-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 26px;
  padding-top: 22px;
  border-top: 1px solid var(--ref-line);
}
.csa-foot-note { margin: 0; font-size: 11.5px; color: var(--ref-muted); }

/* Skeleton / Error */
.csa-skeleton { height: 260px; border: 1px solid var(--ref-line); border-radius: var(--radius-card); background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%); background-size: 200% 100%; animation: csa-shimmer 1.3s linear infinite; }
.csa-error { padding: 40px 24px; border: 1px dashed var(--ref-line); border-radius: var(--radius-card); background: var(--ref-surface); text-align: center; }
.csa-error-title { margin: 0; font-family: var(--ref-font-display); font-size: 19px; font-weight: 500; color: var(--ref-ink); }
.csa-error-desc { margin: 10px auto 0; max-width: 420px; font-size: 13px; line-height: 1.7; color: var(--ref-muted); }
.csa-error .cta { margin-top: 18px; }

/* List */
.csa-list {
  margin: 24px 0 0;
  padding: 0;
  list-style: none;
  overflow: hidden;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-lg);
  background: var(--ref-surface);
}
.csa-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px 16px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--ref-line);
}
.csa-row:last-child { border-bottom: 0; }
.csa-row-main { min-width: 0; flex: 1; }
.csa-row-title { margin: 0; font-size: 13.5px; font-weight: 600; color: var(--ref-ink); }
.csa-row-clamp {
  margin: 6px 0 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ref-muted);
}
.csa-row-review { margin: 8px 0 0; font-size: 12px; line-height: 1.7; color: var(--ref-ink-soft); }
.csa-row-side { display: flex; flex-shrink: 0; flex-direction: column; align-items: flex-end; gap: 8px; }
.csa-row-date { font-size: 11.5px; color: var(--ref-muted); }

/* Badges */
.csa-badge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: var(--radius-pill);
  font-size: 11.5px;
  font-weight: 600;
  white-space: nowrap;
}
.badge-warning { background: color-mix(in srgb, #d97706 14%, transparent); color: #b45309; }
.badge-success { background: color-mix(in srgb, #16a34a 14%, transparent); color: #15803d; }
.badge-danger { background: color-mix(in srgb, #dc2626 14%, transparent); color: #b91c1c; }
.badge-info { background: color-mix(in srgb, var(--ref-brand) 12%, transparent); color: var(--ref-brand-deep); }

/* Empty */
.csa-empty { margin: 24px 0 0; padding: 56px 24px; border: 1px dashed var(--ref-line); border-radius: var(--radius-lg); background: var(--ref-surface); text-align: center; }
.csa-empty svg { color: var(--ref-brand); }
.csa-empty-title { margin: 16px 0 0; font-family: var(--ref-font-display); font-size: 20px; font-weight: 500; color: var(--ref-ink); }
.csa-empty-desc { margin: 10px auto 0; max-width: 440px; font-size: 13px; line-height: 1.7; color: var(--ref-muted); }

/* CTA */
.cta { display: inline-flex; align-items: center; justify-content: center; gap: 8px; height: 42px; padding: 0 18px; border-radius: var(--radius-control); font-size: 13px; font-weight: 500; cursor: pointer; border: 1px solid transparent; text-decoration: none; transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s; }
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }

/* Animations */
@keyframes csa-shimmer { to { background-position: -200% 0; } }

/* Responsive */
@media (max-width: 900px) {
  .csa-steps { grid-template-columns: 1fr; }
}
@media (max-width: 600px) {
  .csa-shell { padding: 0 16px; }
  .csa-head { padding: 30px 0 22px; }
  .csa-sub { font-size: 13.5px; }
  .csa-section { margin-top: 36px; }
  .csa-form { padding: 20px; }
}
@media (prefers-reduced-motion: reduce) { .csa-skeleton { animation: none; } }
</style>