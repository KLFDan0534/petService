<template>
  <div class="ka-page">
    <div class="ka-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="ka-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="ka-crumb-link">首页</router-link>
        <span class="ka-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/profile" class="ka-crumb-link">个人中心</router-link>
        <span class="ka-crumb-sep" aria-hidden="true">›</span>
        <span class="ka-crumb-here">申请成为照护师</span>
      </nav>

      <!-- ═══ Page header ═══ -->
      <header class="ka-head">
        <div class="ka-head-copy">
          <div class="ka-eyebrow" aria-hidden="true">
            <span class="ka-eyebrow-line"></span>
            <span>Become a Keeper</span>
          </div>
          <h1 class="ka-title">申请成为照护师</h1>
          <p class="ka-sub">照护师需加入一家已认证门店，并通过资质核验后才能接单。审核通常在 3 个工作日内完成。</p>
        </div>
      </header>

      <!-- ═══ Loading skeleton ═══ -->
      <template v-if="loading">
        <div class="ka-skeleton-stack">
          <div class="ka-skeleton" style="height: 220px"></div>
          <div class="ka-skeleton" style="height: 300px"></div>
        </div>
      </template>

      <!-- ═══════════════════════════════════════════
           已有申请 → 显示审核进度
           ═══════════════════════════════════════════ -->
      <template v-else-if="existing">
        <section class="ka-section" aria-label="申请进度">
          <header class="ka-sec-head">
            <p class="ka-eyebrow">
              <span class="ka-idx">01</span>
              <span class="ka-line" aria-hidden="true"></span>
              <span>Application</span>
            </p>
            <h2 class="ka-sec-title">申请进度</h2>
            <p class="ka-sec-desc">资料提交后由门店初审、平台复核。</p>
          </header>

          <div class="ka-progress" :class="{ 'is-approved': isApproved, 'is-pending': isPending }">
            <div class="ka-progress-head">
              <div class="ka-progress-copy">
                <h3 class="ka-progress-title">{{ existing.name_wsh || '照护师申请' }}</h3>
                <p class="ka-progress-sub">
                  申请加入 {{ existing.merchant_name_wsh || '门店' }}
                  <template v-if="existing.created_at_wsh"> · {{ formatDate(existing.created_at_wsh) }}</template>
                </p>
              </div>
              <span :class="['badge', statusBadge(existing.status_wsh)]">{{ statusLabel(existing.status_wsh) }}</span>
            </div>

            <dl class="ka-progress-grid">
              <div>
                <dt>从业年限</dt>
                <dd class="tabular">{{ existing.experience_years_wsh ?? 0 }} 年</dd>
              </div>
              <div>
                <dt>日均报价</dt>
                <dd class="tabular">¥{{ existing.price_per_day_wsh ?? 0 }}</dd>
              </div>
              <div>
                <dt>可同时照护</dt>
                <dd class="tabular">{{ existing.max_pets_wsh ?? 0 }} 只</dd>
              </div>
              <div>
                <dt>审核阶段</dt>
                <dd>{{ isPending ? '门店初审中' : isApproved ? '已通过' : '未通过' }}</dd>
              </div>
            </dl>

            <p v-if="existing.bio_wsh" class="ka-progress-bio">{{ existing.bio_wsh }}</p>

            <div class="ka-progress-actions">
              <router-link v-if="isApproved" to="/keeper-workflow" class="cta cta-primary">进入照护师工作台</router-link>
              <router-link v-else-if="isPending" to="/profile" class="cta cta-outline">返回个人中心</router-link>
              <button v-else type="button" class="cta cta-primary" @click="resetAndShowForm">重新提交申请</button>
            </div>
          </div>
        </section>
      </template>

      <form v-else @submit.prevent="submitApplication" class="ka-form">
        <!-- ═══════════════════════════════════════════
             01 · Your details
             ═══════════════════════════════════════════ -->
        <section class="ka-section">
          <header class="ka-sec-head">
            <p class="ka-eyebrow">
              <span class="ka-idx">01</span>
              <span class="ka-line" aria-hidden="true"></span>
              <span>Your Details</span>
            </p>
            <h2 class="ka-sec-title">基本资料</h2>
            <p class="ka-sec-desc">姓名与手机号仅用于门店核验与派单联系，从业信息帮助门店判断你的照护能力。</p>
          </header>

          <div class="ka-card">
            <div class="ka-grid">
              <div class="ka-field">
                <label class="ka-label" for="ka-merchant">加入门店 <span class="ka-req">*</span></label>
                <select
                  id="ka-merchant"
                  v-model="form.merchant_id_wsh"
                  class="ka-input"
                  :class="{ 'is-placeholder': !form.merchant_id_wsh }"
                  required
                >
                  <option value="" disabled>{{ merchants.length ? '请选择门店' : '暂无可加入的门店' }}</option>
                  <option v-for="m in merchants" :key="m.id_wsh" :value="m.id_wsh">{{ m.name_wsh }}</option>
                </select>
                <p class="ka-hint">申请提交后由门店负责人审核</p>
              </div>

              <div class="ka-field ka-field--muted">
                <label class="ka-label" for="ka-name">真实姓名 <span class="ka-req">*</span></label>
                <input id="ka-name" class="ka-input" v-model="form.name_wsh" placeholder="自动读取实名信息" disabled>
                <p class="ka-hint">使用实名认证的姓名</p>
              </div>

              <div class="ka-field ka-field--muted">
                <label class="ka-label" for="ka-phone">手机号 <span class="ka-req">*</span></label>
                <input id="ka-phone" class="ka-input" v-model="form.phone_wsh" placeholder="自动读取绑定手机" disabled>
                <p class="ka-hint">使用账户绑定手机号</p>
              </div>

              <div class="ka-field">
                <label class="ka-label" for="ka-years">从业年限</label>
                <input id="ka-years" class="ka-input" type="number" min="0" v-model.number="form.experience_years_wsh" placeholder="如：3">
              </div>

              <div class="ka-field">
                <label class="ka-label" for="ka-price">每日费用（元）<span class="ka-req">*</span></label>
                <input id="ka-price" class="ka-input" type="number" min="0" step="0.01" v-model.number="form.price_per_day_wsh" placeholder="如：150" required>
                <p class="ka-hint">门店会在此基础上核定对外价格</p>
              </div>

              <div class="ka-field">
                <label class="ka-label" for="ka-maxpets">最大接待宠物数</label>
                <input id="ka-maxpets" class="ka-input" type="number" min="1" v-model.number="form.max_pets_wsh" placeholder="如：5">
                <p class="ka-hint">满位后系统不会再派新单给你</p>
              </div>

              <div class="ka-field ka-field--wide">
                <label class="ka-label" for="ka-bio">个人简介</label>
                <textarea id="ka-bio" class="ka-textarea" v-model="form.bio_wsh" placeholder="介绍一下您的经验和优势，擅长的品种与性格类型、照护习惯等..." rows="4"></textarea>
              </div>
            </div>
          </div>
        </section>

        <!-- ═══════════════════════════════════════════
             02 · Credentials
             ═══════════════════════════════════════════ -->
        <section class="ka-section">
          <header class="ka-sec-head">
            <p class="ka-eyebrow">
              <span class="ka-idx">02</span>
              <span class="ka-line" aria-hidden="true"></span>
              <span>Credentials</span>
            </p>
            <h2 class="ka-sec-title">资质证明</h2>
            <p class="ka-sec-desc">请上传资格证正面和反面，至少 2 张。资质图片仅用于门店与平台核验，不会公开展示原件。</p>
          </header>

          <div class="ka-card">
            <div class="ka-qual-grid">
              <div v-for="(image, index) in qualImages" :key="index" class="ka-qual">
                <p class="ka-qual-label">{{ index === 0 ? '正面' : '反面' }}</p>
                <div class="ka-qual-body">
                  <div v-if="image" class="ka-qual-preview">
                    <img :src="image" :alt="`资质${index === 0 ? '正面' : '反面'}`">
                    <button type="button" class="ka-qual-remove" :aria-label="`删除资质${index === 0 ? '正面' : '反面'}`" @click="qualImages[index] = ''">✕</button>
                  </div>
                  <label class="ka-qual-btn" :class="{ uploading: uploadingIdx === index }">
                    <input type="file" accept="image/*" hidden @change="uploadQualImage(index, $event)">
                    <span class="ka-qual-plus" aria-hidden="true">+</span>
                    <span>{{ uploadingIdx === index ? '上传中...' : (image ? '重选图片' : '选择图片') }}</span>
                  </label>
                </div>
              </div>
            </div>

            <p class="ka-qual-note">
              已上传 <b class="ka-num">{{ qualImages.filter(Boolean).length }}</b> / 至少 2 张
            </p>

            <div v-if="error" class="ka-error" role="alert">{{ error }}</div>

            <div class="ka-footer">
              <button type="button" class="cta cta-outline" @click="$router.push('/profile')">返回个人中心</button>
              <button type="submit" class="cta cta-primary" :disabled="submitting">
                {{ submitting ? '提交中...' : '提交申请' }}
              </button>
            </div>
          </div>
        </section>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import request from '@/utils/request'
import { getMyKeeperInfo } from '@/api/keeper'

const router = useRouter()
const appStore = useAppStore()

const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const merchants = ref([])
const qualImages = ref(['', ''])
const uploadingIdx = ref(-1)
const existing = ref(null)

const isPending = computed(() => existing.value?.status_wsh === 0)
const isApproved = computed(() => [1, 3, 4].includes(existing.value?.status_wsh))

function statusLabel(status) {
  return {
    0: '待审核',
    1: '已通过',
    2: '已拒绝',
    3: '已通过',
    4: '已通过',
  }[status] || '未知'
}

function statusBadge(status) {
  return {
    0: 'badge-warning',
    1: 'badge-success',
    2: 'badge-danger',
    3: 'badge-success',
    4: 'badge-success',
  }[status] || 'badge-info'
}

function formatDate(dt) {
  if (!dt) return ''
  return String(dt).slice(0, 10)
}

function resetAndShowForm() {
  existing.value = null
}

const form = reactive({
  merchant_id_wsh: '',
  name_wsh: '',
  phone_wsh: '',
  experience_years_wsh: null,
  price_per_day_wsh: null,
  max_pets_wsh: null,
  bio_wsh: '',
})

onMounted(async () => {
  try {
    const [mr, ur, er] = await Promise.all([
      request.get('/api/merchants'),
      request.get('/users/me'),
      getMyKeeperInfo().catch(() => null),
    ])
    if (mr.data.code === 200) {
      merchants.value = mr.data.data || []
    }
    if (ur.data.code === 200 && ur.data.data) {
      const u = ur.data.data
      form.name_wsh = u.real_name_wsh || u.nickname_wsh || u.username_wsh || ''
      form.phone_wsh = u.phone_wsh || ''
    }
    if (er && er.code === 200 && er.data) {
      existing.value = er.data
    }
  } catch (e) {
    appStore.addToast('获取数据失败', 'error')
  } finally {
    loading.value = false
  }
})

async function uploadQualImage(index, e) {
  const file = e.target?.files?.[0]
  if (!file) return
  uploadingIdx.value = index
  try {
    const formData = new FormData()
    formData.append('file', file)
    const r = await request.post('/files/upload?directory=qualifications', formData)
    if (r.data.code === 200 && r.data.data?.url_wsh) {
      qualImages.value[index] = r.data.data.url_wsh
    } else {
      appStore.addToast('图片上传失败', 'error')
    }
  } catch (e) {
    appStore.addToast('图片上传失败', 'error')
  } finally {
    uploadingIdx.value = -1
  }
}

async function submitApplication() {
  if (submitting.value) return
  error.value = ''
  const validImages = qualImages.value.filter(Boolean)
  if (validImages.length < 2) {
    error.value = '请上传资格证正面和反面，至少 2 张图片'
    return
  }
  submitting.value = true
  try {
    const payload = {
      merchant_id_wsh: Number(form.merchant_id_wsh),
      name_wsh: form.name_wsh,
      phone_wsh: form.phone_wsh,
      experience_years_wsh: form.experience_years_wsh || 0,
      price_per_day_wsh: form.price_per_day_wsh || 0,
      max_pets_wsh: form.max_pets_wsh || 1,
      bio_wsh: form.bio_wsh || '',
      qualification_image_wsh: validImages.join(','),
    }
    const r = await request.post('/api/keepers', payload)
    if (r.data.code === 200) {
      appStore.addToast('申请提交成功，请等待商户审核', 'success')
      router.push('/profile')
    } else {
      error.value = r.data.message || '提交失败'
    }
  } catch (e) {
    error.value = e.response?.data?.message || '提交失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   Editorial warm — uses shared --ref-* tokens from
   assets/css/design-tokens.css. Dark mode is handled globally.
   ═══════════════════════════════════════════════════════ */
.ka-page {
  --r-tag: 6px;
  --r-btn: 10px;
  --r-card: 14px;
  --r-panel: 20px;
  width: 100%;
  padding: 6px 0 72px;
  min-height: 60vh;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.ka-shell { max-width: 1180px; margin: 0 auto; padding: 0 24px; }

/* ═══ Breadcrumb ═══ */
.ka-crumb {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  padding: 6px 0; font-size: 12.5px; color: var(--ref-muted);
}
.ka-crumb-link { color: var(--ref-muted); text-decoration: none; }
.ka-crumb-link:hover { color: var(--ref-ink); }
.ka-crumb-sep { color: var(--ref-line); }
.ka-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Page header ═══ */
.ka-head {
  display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between;
  gap: 20px 32px; padding: 22px 0 8px;
}
.ka-head-copy { min-width: 0; }
.ka-eyebrow {
  display: flex; align-items: center; gap: 10px;
  font-size: 10px; letter-spacing: 0.22em; text-transform: uppercase; color: var(--ref-muted);
}
.ka-eyebrow-line { width: 24px; height: 1px; background: var(--ref-line); }
.ka-title {
  margin: 12px 0 0; font-family: var(--ref-font-display);
  font-size: clamp(28px, 3.6vw, 38px); font-weight: 500; line-height: 1.15;
  letter-spacing: -0.01em; color: var(--ref-ink);
}
.ka-sub { margin: 12px 0 0; max-width: 620px; font-size: 13px; line-height: 1.8; color: var(--ref-ink-soft); opacity: 0.85; }

/* ═══ Sections ═══ */
.ka-section { margin-top: 48px; }
.ka-sec-head { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: 12px 24px; }
.ka-idx { font-variant-numeric: tabular-nums; }
.ka-line { width: 24px; height: 1px; background: var(--ref-line); }
.ka-sec-title {
  margin: 10px 0 0; font-family: var(--ref-font-display);
  font-size: 24px; font-weight: 400; line-height: 1.2; letter-spacing: -0.02em; color: var(--ref-ink);
}
.ka-sec-desc { margin: 8px 0 0; max-width: 560px; font-size: 13px; line-height: 1.7; color: var(--ref-ink-soft); opacity: 0.8; }

/* ═══ Card ═══ */
.ka-card {
  margin-top: 20px; padding: 28px;
  border: 1px solid var(--ref-line); border-radius: var(--r-panel);
  background: var(--ref-surface);
  box-shadow: 0 2px 4px -2px rgba(13, 33, 26, 0.06);
}

/* ═══ Form grid ═══ */
.ka-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px 24px; }
.ka-field { display: grid; gap: 8px; min-width: 0; }
.ka-field--wide { grid-column: 1 / -1; }
.ka-field--muted { opacity: 0.82; }
.ka-label { font-size: 12.5px; font-weight: 500; color: var(--ref-ink-soft); }
.ka-req { color: var(--ref-brand); }
.ka-input {
  width: 100%; height: 44px; padding: 0 14px;
  border: 1px solid var(--ref-line); border-radius: var(--r-btn);
  background: var(--ref-surface); color: var(--ref-ink); font-size: 14px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.ka-input::placeholder { color: var(--ref-muted); }
.ka-input:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
.ka-input:disabled {
  background: var(--ref-sand); color: var(--ref-ink-soft); cursor: not-allowed;
}
.ka-textarea {
  width: 100%; padding: 12px 14px; resize: vertical; min-height: 96px;
  border: 1px solid var(--ref-line); border-radius: var(--r-btn);
  background: var(--ref-surface); color: var(--ref-ink); font-size: 14px; line-height: 1.7;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.ka-textarea::placeholder { color: var(--ref-muted); }
.ka-textarea:focus {
  outline: none;
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ref-brand) 12%, transparent);
}
.ka-hint { margin: 0; font-size: 11.5px; line-height: 1.6; color: var(--ref-muted); }

/* ═══ Qualifications ═══ */
.ka-qual-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
.ka-qual { display: grid; gap: 8px; min-width: 0; }
.ka-qual-label { margin: 0; font-size: 12px; font-weight: 500; color: var(--ref-ink-soft); }
.ka-qual-body { display: flex; flex-direction: column; align-items: stretch; gap: 12px; }
.ka-qual-preview {
  position: relative; width: 100%; aspect-ratio: 16 / 11; overflow: hidden;
  border: 1px solid var(--ref-line); border-radius: var(--r-card); background: var(--ref-sand);
}
.ka-qual-preview img { width: 100%; height: 100%; object-fit: cover; }
.ka-qual-remove {
  position: absolute; top: 10px; right: 10px;
  display: flex; align-items: center; justify-content: center;
  width: 26px; height: 26px; padding: 0; border-radius: 50%; border: none;
  background: color-mix(in srgb, var(--ref-ink) 65%, transparent); color: var(--ref-cream);
  font-size: 12px; cursor: pointer; transition: background 0.15s, transform 0.15s;
}
.ka-qual-remove:hover { background: var(--ref-brand); transform: scale(1.06); }
.ka-qual-btn {
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px;
  width: 100%; aspect-ratio: 16 / 8; min-height: 96px;
  border: 1px dashed var(--ref-line); border-radius: var(--r-card);
  background: color-mix(in srgb, var(--ref-cream) 40%, var(--ref-surface));
  color: var(--ref-muted); font-size: 12px; cursor: pointer;
  transition: border-color 0.15s, color 0.15s, background 0.15s;
}
.ka-qual-btn:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent);
  background: color-mix(in srgb, var(--ref-sand) 50%, var(--ref-surface));
  color: var(--ref-ink);
}
.ka-qual-btn.uploading { opacity: 0.55; pointer-events: none; }
.ka-qual-plus { font-size: 22px; font-weight: 300; line-height: 1; }
.ka-qual-note { margin: 18px 0 0; font-size: 12px; color: var(--ref-muted); }
.ka-num { color: var(--ref-ink-soft); font-variant-numeric: tabular-nums; font-weight: 600; }

/* ═══ Badges (progress) ═══ */
.badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}
.badge-warning { background: color-mix(in srgb, #b45309 12%, transparent); color: #92400e; }
.badge-success { background: color-mix(in srgb, var(--ref-moss, #3f5347) 13%, transparent); color: var(--ref-moss, #3f5347); }
.badge-danger { background: color-mix(in srgb, #b3402a 10%, transparent); color: color-mix(in srgb, #b3402a 90%, transparent); }
.badge-info { background: color-mix(in srgb, var(--ref-brand) 11%, transparent); color: var(--ref-brand-deep); }

/* ═══ Progress (已有申请) ═══ */
.ka-progress {
  margin-top: 20px;
  padding: 28px;
  border: 1px solid var(--ref-line);
  border-radius: var(--r-panel);
  background: var(--ref-surface);
  box-shadow: 0 2px 4px -2px rgba(13, 33, 26, 0.06);
}
.ka-progress-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px 20px;
}
.ka-progress-copy { min-width: 0; }
.ka-progress-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 22px;
  font-weight: 500;
  line-height: 1.25;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
}
.ka-progress-sub { margin: 8px 0 0; font-size: 13px; line-height: 1.7; color: var(--ref-muted); }
.ka-progress-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px 24px;
  margin: 24px 0 0;
  padding: 22px 0 0;
  border-top: 1px solid var(--ref-line);
}
.ka-progress-grid dt {
  font-size: 10.5px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.ka-progress-grid dd {
  margin: 6px 0 0;
  font-size: 13.5px;
  color: var(--ref-ink);
}
.ka-progress-bio {
  margin: 0;
  padding: 20px 0 0;
  border-top: 1px solid var(--ref-line);
  font-size: 13px;
  line-height: 1.8;
  color: var(--ref-ink-soft);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.ka-progress-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}
@media (max-width: 760px) {
  .ka-progress-grid { grid-template-columns: 1fr; }
}

/* ═══ Error ═══ */
.ka-error {
  margin-top: 18px; padding: 12px 14px;
  border: 1px solid color-mix(in srgb, #b3402a 30%, transparent); border-radius: var(--r-btn);
  background: color-mix(in srgb, #b3402a 6%, var(--ref-surface));
  color: color-mix(in srgb, #b3402a 85%, transparent); font-size: 13px; line-height: 1.6;
}

/* ═══ Footer / CTA ═══ */
.ka-footer {
  display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 12px;
  margin-top: 26px; padding-top: 22px; border-top: 1px solid var(--ref-line);
}
.cta {
  display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  height: 44px; padding: 0 22px; border-radius: var(--r-btn);
  font-size: 13px; font-weight: 500; cursor: pointer; border: 1px solid transparent;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline { background: var(--ref-surface); color: var(--ref-ink); border-color: var(--ref-line); }
.cta-outline:hover { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }

/* ═══ Skeleton ═══ */
.ka-skeleton-stack { display: grid; gap: 20px; margin-top: 32px; }
.ka-skeleton {
  border-radius: var(--r-panel); border: 1px solid var(--ref-line);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%; animation: ka-shimmer 1.3s linear infinite;
}

/* ═══ Animations ═══ */
@keyframes ka-shimmer { to { background-position: -200% 0; } }

/* ═══ Responsive ═══ */
@media (max-width: 760px) {
  .ka-grid { grid-template-columns: 1fr; }
  .ka-qual-grid { grid-template-columns: 1fr; }
  .ka-card { padding: 22px; }
}
@media (max-width: 520px) {
  .ka-shell { padding: 0 16px; }
  .ka-section { margin-top: 40px; }
  .ka-card { padding: 18px; }
  .ka-footer { flex-direction: column-reverse; }
  .ka-footer .cta { width: 100%; }
  .ka-qual-btn { aspect-ratio: auto; min-height: 120px; }
}
@media (prefers-reduced-motion: reduce) {
  .ka-skeleton { animation: none; }
}
</style>
