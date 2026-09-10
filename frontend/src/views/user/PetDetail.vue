<template>
  <div class="pd-page">
    <div class="pd-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="pd-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="pd-crumb-link">首页</router-link>
        <span class="pd-crumb-sep" aria-hidden="true">›</span>
        <router-link to="/pets" class="pd-crumb-link">我的宠物</router-link>
        <span class="pd-crumb-sep" aria-hidden="true">›</span>
        <span class="pd-crumb-here">{{ pet ? pet.name_wsh : '档案' }}</span>
      </nav>

      <!-- ═══ Loading skeleton ═══ -->
      <template v-if="loading">
        <div class="pd-hero-skel" aria-hidden="true">
          <div class="pd-skel-media"></div>
          <div class="pd-skel-copy">
            <div class="pd-skel-line w-28"></div>
            <div class="pd-skel-line w-56"></div>
            <div class="pd-skel-line w-40"></div>
            <div class="pd-skel-facts">
              <div class="pd-skel-line w-20"></div>
              <div class="pd-skel-line w-20"></div>
              <div class="pd-skel-line w-20"></div>
              <div class="pd-skel-line w-20"></div>
            </div>
          </div>
        </div>
        <div class="pd-skel-notes" aria-hidden="true">
          <div class="pd-skel-line w-24"></div>
          <div class="pd-skel-line full"></div>
          <div class="pd-skel-line w-2-3"></div>
        </div>
      </template>

      <!-- ═══ Detail ═══ -->
      <template v-else-if="pet">
        <!-- Hero -->
        <div class="pd-hero">
          <div class="pd-media">
            <div class="pd-media-frame">
              <MediaWithFallback
                :src="pet.avatar_wsh"
                :alt="`${pet.name_wsh} 的照片`"
                class="pd-media-img"
                placeholder="还没有照片"
                loading="eager"
              />
              <label
                class="pd-media-edit"
                :class="{ 'is-busy': avatarUploading }"
                title="更换照片"
              >
                <input type="file" accept="image/*" class="pd-file" @change="onAvatarChange">
                <svg v-if="!avatarUploading" class="pd-cam-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>
                <span v-if="avatarUploading" class="pd-media-spin" aria-hidden="true"></span>
                {{ avatarUploading ? '上传中' : '更换照片' }}
              </label>
            </div>
          </div>

          <div class="pd-copy">
            <p class="pd-eyebrow" aria-hidden="true">
              <span class="pd-eyebrow-idx">宠物档案</span>
              <span v-if="pet.id_wsh" class="pd-eyebrow-no">· No.{{ pet.id_wsh }}</span>
            </p>
            <h1 class="pd-name">{{ pet.name_wsh }}</h1>
            <p class="pd-meta">
              {{ petTypeLabel(pet.type_wsh) }}
              <span class="pd-meta-sep" aria-hidden="true">·</span>
              {{ pet.breed_wsh || '未知品种' }}
            </p>

            <div class="pd-badges">
              <span :class="['badge', pet.vaccinated_wsh === 1 ? 'badge-done' : 'badge-muted']">
                {{ pet.vaccinated_wsh === 1 ? '已免疫' : '未免疫' }}
              </span>
              <span :class="['badge', pet.sterilized_wsh === 1 ? 'badge-done' : 'badge-muted']">
                {{ pet.sterilized_wsh === 1 ? '已绝育' : '未绝育' }}
              </span>
              <span v-if="pet.owner_name_wsh" class="tag-pill">主人 · {{ pet.owner_name_wsh }}</span>
            </div>

            <dl class="pd-facts">
              <div>
                <dt>年龄</dt>
                <dd>{{ formatAge(pet.age_wsh) }}</dd>
              </div>
              <div>
                <dt>体重</dt>
                <dd>{{ pet.weight_wsh ? `${pet.weight_wsh} kg` : '—' }}</dd>
              </div>
              <div>
                <dt>性别</dt>
                <dd>{{ genderLabel(pet.gender_wsh) }}</dd>
              </div>
              <div>
                <dt>照护记录</dt>
                <dd>{{ orders.length }} 次</dd>
              </div>
            </dl>

            <div class="pd-actions">
              <button type="button" class="cta cta-primary" @click="showEditDialog = true">编辑档案</button>
              <router-link to="/services" class="cta cta-outline">为它预约服务</router-link>
              <button type="button" class="text-link" @click="goBack">
                <span class="tl-text">返回列表</span>
                <span class="tl-arrow" aria-hidden="true">→</span>
              </button>
            </div>
          </div>
        </div>

        <!-- 01 · Care Notes -->
        <section class="pd-section">
          <header class="pd-head">
            <div class="pd-head-copy">
              <p class="pd-eyebrow">
                <span class="pd-eyebrow-idx">01</span>
                <span class="pd-eyebrow-line" aria-hidden="true"></span>
                <span>照护记录</span>
              </p>
              <h2 class="pd-title">照护要点</h2>
              <p class="pd-desc">寄养入住时，照护师会逐条核对这些信息。</p>
            </div>
          </header>

          <div class="pd-notes">
            <div class="pd-note">
              <h3 class="pd-note-label">性格与照护说明</h3>
              <p class="pd-note-text">{{ pet.description_wsh || '暂未填写，补齐后照护师能更快掌握它的习惯。' }}</p>
            </div>
            <div class="pd-note">
              <h3 class="pd-note-label">过敏 / 禁忌</h3>
              <p class="pd-note-text pd-note-warn">{{ pet.allergies_wsh || '暂未填写。' }}</p>
            </div>
            <div class="pd-note">
              <h3 class="pd-note-label">生活习惯</h3>
              <p class="pd-note-text">{{ pet.habits_wsh || '暂未填写。' }}</p>
            </div>
          </div>
        </section>

        <!-- 02 · AI Health Reports -->
        <section class="pd-section">
          <header class="pd-head">
            <div class="pd-head-copy">
              <p class="pd-eyebrow">
                <span class="pd-eyebrow-idx">02</span>
                <span class="pd-eyebrow-line" aria-hidden="true"></span>
                <span>AI 健康报告</span>
              </p>
              <h2 class="pd-title">AI 健康报告</h2>
              <p class="pd-desc">寄养期间每日生成照护小结，离店时生成一份健康总结。</p>
            </div>
          </header>

          <div v-if="reports.length" class="pd-reports">
            <article v-for="r in reports" :key="r.id_wsh" class="pd-report">
              <div class="pd-report-head">
                <strong class="pd-report-type">
                  {{ r.type_wsh === 'daily' ? '日常报告' : r.type_wsh === 'final' ? '总结报告' : r.type_wsh || '报告' }}
                </strong>
                <span class="pd-report-date">{{ formatDate(r.created_at_wsh) }}</span>
                <button type="button" class="cta cta-outline cta-sm" @click="viewReport(r)">查看</button>
              </div>
              <p v-if="r.content_wsh" class="pd-report-text">
                {{ r.content_wsh.slice(0, 120) }}{{ r.content_wsh.length > 120 ? '...' : '' }}
              </p>
            </article>
          </div>

          <div v-else class="pd-empty">
            <p class="pd-empty-title">还没有健康报告</p>
            <p class="pd-empty-desc">完成一次寄养后，每日照护小结与离店健康总结会自动归档到这里。</p>
            <div class="pd-empty-actions">
              <button v-if="canGenerate" type="button" class="cta cta-primary" @click="generateReport">生成报告</button>
              <router-link to="/services" class="cta cta-outline">浏览照护服务</router-link>
            </div>
          </div>
        </section>

        <!-- 03 · Service History -->
        <section class="pd-section">
          <header class="pd-head">
            <div class="pd-head-copy">
              <p class="pd-eyebrow">
                <span class="pd-eyebrow-idx">03</span>
                <span class="pd-eyebrow-line" aria-hidden="true"></span>
                <span>服务记录</span>
              </p>
              <h2 class="pd-title">服务记录</h2>
              <p class="pd-desc">这只宠物参与过的寄养、洗护与陪伴订单。</p>
            </div>
            <div v-if="orders.length" class="pd-head-action">
              <router-link to="/orders" class="text-link">
                <span class="tl-text">全部订单</span>
                <span class="tl-arrow" aria-hidden="true">→</span>
              </router-link>
            </div>
          </header>

          <div v-if="orders.length" class="pd-orders">
            <router-link v-for="o in orders" :key="o.id_wsh" :to="`/orders/${o.id_wsh}`" class="pd-order">
              <span class="pd-order-main">
                <span class="pd-order-name">{{ o.service_name_wsh || '寄养服务' }}</span>
                <span class="pd-order-meta">
                  {{ formatDate(o.start_date_wsh) }} 至 {{ formatDate(o.end_date_wsh) }}
                  <template v-if="o.order_no_wsh">
                    <span class="pd-sep" aria-hidden="true">·</span>
                    #{{ o.order_no_wsh }}
                  </template>
                </span>
              </span>
              <span :class="['badge', orderBadge(o.status_wsh)]">{{ orderLabel(o.status_wsh) }}</span>
            </router-link>
          </div>

          <div v-else class="pd-empty">
            <p class="pd-empty-title">还没有服务记录</p>
            <p class="pd-empty-desc">下单时选择这只宠物，照护日报会自动归档到这里。</p>
            <router-link to="/services" class="cta cta-outline">浏览照护服务</router-link>
          </div>
        </section>
      </template>

      <!-- ═══ Not found ═══ -->
      <div v-else class="pd-notfound">
        <p class="pd-notfound-title">这份宠物档案不存在</p>
        <p class="pd-notfound-desc">它可能已被删除。回到列表看看其他宠物，或新建一份档案。</p>
        <button type="button" class="cta cta-primary" @click="goBack">返回我的宠物</button>
      </div>
    </div>

    <!-- ═══ Edit Dialog ═══ -->
    <PetFormDialog
      :visible="showEditDialog"
      :pet="pet"
      :saving="saving"
      :server-error="serverError"
      @save="handleSave"
      @close="showEditDialog = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { usePetDetail } from '@/composables/usePetDetail'
import MediaWithFallback from '@/components/common/MediaWithFallback.vue'
import PetFormDialog from '@/components/pet/PetFormDialog.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const {
  loading, pet, orders, reports, canGenerate,
  formatAge, genderLabel, orderLabel, orderBadge, formatDate,
  load,
  updateAvatar, updateProfile,
} = usePetDetail(route.params.id)

const showEditDialog = ref(false)
const saving = ref(false)
const serverError = ref('')
const avatarUploading = ref(false)

function petTypeLabel(type) {
  const m = { dog: '狗', cat: '猫', rabbit: '兔子', bird: '鸟', fish: '鱼', hamster: '仓鼠', other: '其他' }
  return m[String(type || 'other').toLowerCase()] || type || '其他'
}

function petTypeIcon(type) {
  const m = { dog: '犬', cat: '猫', rabbit: '兔', bird: '鸟', fish: '鱼', hamster: '鼠', other: '宠' }
  return m[String(type || 'other').toLowerCase()] || '宠'
}

async function onAvatarChange(e) {
  const file = e.target?.files?.[0]
  if (!file) return
  avatarUploading.value = true
  try {
    await updateAvatar(file)
    appStore.addToast('头像更新成功', 'success')
  } catch (err) {
    appStore.addToast('上传失败: ' + (err.message || '未知错误'), 'error')
  } finally {
    avatarUploading.value = false
    e.target.value = ''
  }
}

async function handleSave(payload) {
  saving.value = true
  serverError.value = ''
  try {
    await updateProfile(payload)
    showEditDialog.value = false
    appStore.addToast('更新成功', 'success')
  } catch (e) {
    serverError.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

function viewReport(r) {
  appStore.addToast('报告内容：' + (r.content_wsh?.slice(0, 100) || '无内容'), 'info')
}

async function generateReport() {
  appStore.addToast('报告生成功能即将上线', 'info')
}

function goBack() {
  router.push('/pets')
}

onMounted(load)
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   Editorial warm style — shared --ref-* tokens from
   assets/css/design-tokens.css. Dark mode via html[data-theme="dark"].
   ═══════════════════════════════════════════════════════ */
.pd-page {
  --paper: #17130f;
  --cream-fixed: #f5efe7;
  --r-tag: 6px;
  --r-btn: 10px;
  --r-card: 14px;
  --r-panel: 20px;
  --r-frame: 26px;
  width: 100%;
  min-height: 60vh;
  padding: 6px 0 72px;
  background: var(--ref-canvas);
  color: var(--ref-ink);
}

.pd-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.pd-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.pd-crumb-link { color: var(--ref-muted); text-decoration: none; }
.pd-crumb-link:hover { color: var(--ref-ink); }
.pd-crumb-sep { color: var(--ref-line); }
.pd-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Eyebrow / section heading ═══ */
.pd-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 10px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.pd-eyebrow-idx { font-variant-numeric: tabular-nums; }
.pd-eyebrow-line { width: 24px; height: 1px; background: var(--ref-line); }
.pd-section { margin-top: 52px; }
.pd-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
}
.pd-head-copy { min-width: 0; }
.pd-title {
  margin: 12px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(22px, 3vw, 28px);
  line-height: 1.2;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
}
.pd-desc {
  margin: 8px 0 0;
  max-width: 520px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
  opacity: 0.8;
}
.pd-head-action { padding-bottom: 4px; flex-shrink: 0; }

/* ═══ Hero ═══ */
.pd-hero {
  display: grid;
  grid-template-columns: minmax(0, 5fr) minmax(0, 7fr);
  gap: 40px;
  align-items: start;
  padding: 40px 0 8px;
}
.pd-media-frame {
  position: relative;
  aspect-ratio: 4 / 5;
  overflow: hidden;
  border-radius: var(--r-frame);
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
}
.pd-media-img { width: 100%; height: 100%; }
.pd-media-img :deep(.media-image) { transition: transform 0.3s cubic-bezier(0.23, 1, 0.32, 1); }
.pd-media-frame:hover :deep(.media-image) { transform: scale(1.03); }
.pd-media-img :deep(.media-placeholder) { background: var(--ref-sand); color: var(--ref-muted); }
.pd-media-img :deep(.media-placeholder .el-icon) { color: var(--ref-brand); }

.pd-file { display: none; }
.pd-media-edit {
  position: absolute;
  right: 14px;
  bottom: 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  border-radius: 10px;
  cursor: pointer;
  background: color-mix(in srgb, var(--paper) 62%, transparent);
  color: var(--cream-fixed);
  font-size: 12px;
  font-weight: 500;
  backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, 0.22);
  transition: background 0.15s, transform 0.15s;
}
.pd-media-edit:hover:not(.is-busy) { background: var(--paper); transform: translateY(-1px); }
.pd-media-edit.is-busy { opacity: 0.8; pointer-events: none; }
.pd-cam-icon { width: 14px; height: 14px; }
.pd-media-spin {
  width: 13px;
  height: 13px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-left-color: var(--cream-fixed);
  animation: pd-spin 0.7s linear infinite;
}

/* ═══ Hero copy ═══ */
.pd-copy { min-width: 0; padding-top: 8px; }
.pd-name {
  margin: 14px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(32px, 4.4vw, 46px);
  line-height: 1.08;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.pd-meta {
  margin: 12px 0 0;
  font-size: 14px;
  color: var(--ref-ink-soft);
}
.pd-meta-sep { margin: 0 8px; color: var(--ref-line); }
.pd-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 20px;
}

/* ═══ Facts ═══ */
.pd-facts {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px 20px;
  margin: 32px 0 0;
  padding: 26px 0 0;
  border-top: 1px solid var(--ref-line);
}
.pd-facts dt {
  font-size: 10px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.pd-facts dd {
  margin: 8px 0 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  line-height: 1;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}

/* ═══ Hero actions ═══ */
.pd-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 30px;
}

/* ═══ Care notes ═══ */
.pd-notes {
  margin-top: 20px;
  padding: 6px 28px;
  border: 1px solid var(--ref-line);
  border-radius: var(--r-card);
  background: var(--ref-surface);
}
.pd-note { padding: 20px 0; border-top: 1px solid var(--ref-line); }
.pd-note:first-child { border-top: 0; }
.pd-note-label {
  margin: 0;
  font-size: 10px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--ref-muted);
  font-weight: 500;
}
.pd-note-text {
  margin: 10px 0 0;
  max-width: 680px;
  font-size: 14px;
  line-height: 1.75;
  color: color-mix(in srgb, var(--ref-ink-soft) 90%, transparent);
}
.pd-note-warn { color: var(--color-danger); }

/* ═══ Reports ═══ */
.pd-reports {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 20px;
}
.pd-report {
  padding: 18px 22px;
  border: 1px solid var(--ref-line);
  border-radius: var(--r-card);
  background: var(--ref-surface);
  transition: border-color 0.15s;
}
.pd-report:hover { border-color: color-mix(in srgb, var(--ref-ink) 18%, transparent); }
.pd-report-head {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.pd-report-type {
  font-family: var(--ref-font-display);
  font-size: 16px;
  font-weight: 500;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
}
.pd-report-date { font-size: 12px; color: var(--ref-muted); }
.pd-report-head .cta-sm { margin-left: auto; }
.pd-report-text {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
}

/* ═══ Orders ═══ */
.pd-orders {
  margin-top: 20px;
  overflow: hidden;
  border: 1px solid var(--ref-line);
  border-radius: var(--r-card);
  background: var(--ref-surface);
}
.pd-order {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--ref-line);
  text-decoration: none;
  transition: background 0.15s;
}
.pd-order:last-child { border-bottom: 0; }
.pd-order:hover { background: color-mix(in srgb, var(--ref-cream) 50%, transparent); }
.pd-order-main { display: flex; flex-direction: column; min-width: 0; flex: 1; }
.pd-order-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--ref-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pd-order-meta {
  margin-top: 5px;
  font-size: 12px;
  color: var(--ref-muted);
  font-variant-numeric: tabular-nums;
}
.pd-sep { margin: 0 8px; color: var(--ref-line); }

/* ═══ Empty / not found ═══ */
.pd-empty {
  margin-top: 20px;
  padding: 48px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: var(--r-card);
  background: var(--ref-surface);
  text-align: center;
}
.pd-empty-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 18px;
  font-weight: 500;
  color: var(--ref-ink);
}
.pd-empty-desc {
  margin: 8px auto 0;
  max-width: 420px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-muted);
}
.pd-empty-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 20px;
}
.pd-notfound {
  padding: 72px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: var(--r-frame);
  background: var(--ref-surface);
  text-align: center;
}
.pd-notfound-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 22px;
  font-weight: 500;
  color: var(--ref-ink);
}
.pd-notfound-desc {
  margin: 10px auto 0;
  max-width: 420px;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ref-muted);
}
.pd-notfound .cta { margin-top: 22px; }

/* ═══ Badges ═══ */
.badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: var(--r-tag);
  border: 1px solid transparent;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
}
.badge-done { background: color-mix(in srgb, var(--ref-ink) 5%, transparent); color: var(--ref-ink-soft); }
.badge-muted { background: transparent; color: var(--ref-muted); border-color: var(--ref-line); }
.badge-success { background: color-mix(in srgb, var(--color-success) 14%, transparent); color: var(--color-success); border-color: color-mix(in srgb, var(--color-success) 30%, transparent); }
.badge-info { background: color-mix(in srgb, var(--color-info) 14%, transparent); color: var(--color-info); border-color: color-mix(in srgb, var(--color-info) 30%, transparent); }
.badge-warning { background: color-mix(in srgb, var(--color-warning) 16%, transparent); color: color-mix(in srgb, var(--color-warning) 78%, var(--ref-ink)); border-color: color-mix(in srgb, var(--color-warning) 35%, transparent); }
.badge-primary { background: color-mix(in srgb, var(--ref-brand) 12%, transparent); color: var(--ref-brand-deep); border-color: color-mix(in srgb, var(--ref-brand) 25%, transparent); }
.badge-danger { background: color-mix(in srgb, var(--color-danger) 12%, transparent); color: var(--color-danger); border-color: color-mix(in srgb, var(--color-danger) 28%, transparent); }
.tag-pill {
  display: inline-flex;
  align-items: center;
  border-radius: var(--r-tag);
  border: 1px solid var(--ref-line);
  background: color-mix(in srgb, var(--ref-cream) 70%, var(--ref-surface));
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  color: var(--ref-ink-soft);
}

/* ═══ CTA / text-link ═══ */
.cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  padding: 0 18px;
  border-radius: var(--r-btn);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  text-decoration: none;
  transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}
.cta:hover { transform: translateY(-1px); }
.cta:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.cta-sm { height: 32px; padding: 0 12px; font-size: 12px; }
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline {
  background: var(--ref-surface);
  color: var(--ref-ink);
  border-color: var(--ref-line);
}
.cta-outline:hover:not(:disabled) { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }
.text-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--ref-ink);
  text-decoration: none;
  transition: color 0.15s;
}
.text-link:hover { color: var(--ref-brand); }
.tl-text { border-bottom: 1px solid color-mix(in srgb, var(--ref-ink) 25%, transparent); padding-bottom: 2px; }
.text-link:hover .tl-text { border-color: var(--ref-brand); }
.tl-arrow { transition: transform 0.15s; }
.text-link:hover .tl-arrow { transform: translateX(4px); }

/* ═══ Loading skeleton ═══ */
.pd-hero-skel {
  display: grid;
  grid-template-columns: minmax(0, 5fr) minmax(0, 7fr);
  gap: 40px;
  padding: 40px 0 8px;
}
.pd-skel-media {
  aspect-ratio: 4 / 5;
  border-radius: var(--r-frame);
  border: 1px solid var(--ref-line);
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: pd-shimmer 1.3s linear infinite;
}
.pd-skel-copy { padding-top: 8px; }
.pd-skel-line {
  height: 12px;
  border-radius: 6px;
  background: var(--ref-sand);
  margin-bottom: 12px;
  animation: pd-pulse 1.4s ease-in-out infinite;
}
.pd-skel-line.w-20 { width: 80px; }
.pd-skel-line.w-24 { width: 96px; }
.pd-skel-line.w-28 { width: 112px; }
.pd-skel-line.w-40 { width: 160px; }
.pd-skel-line.w-56 { width: 224px; }
.pd-skel-line.w-2-3 { width: 66%; }
.pd-skel-line.full { width: 100%; }
.pd-skel-facts {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px 20px;
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid var(--ref-line);
}
.pd-skel-notes {
  margin-top: 52px;
  padding: 28px;
  border: 1px solid var(--ref-line);
  border-radius: var(--r-card);
  background: var(--ref-surface);
}

/* ═══ Animations ═══ */
@keyframes pd-spin { to { transform: rotate(360deg); } }
@keyframes pd-shimmer { to { background-position: -200% 0; } }
@keyframes pd-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.55; } }

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  .pd-facts { grid-template-columns: repeat(2, 1fr); }
  .pd-skel-facts { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 760px) {
  .pd-hero { grid-template-columns: 1fr; gap: 28px; }
  .pd-hero-skel { grid-template-columns: 1fr; gap: 28px; }
  .pd-media-frame { aspect-ratio: 4 / 3; }
  .pd-skel-media { aspect-ratio: 4 / 3; }
}
@media (max-width: 520px) {
  .pd-shell { padding: 0 16px; }
  .pd-section { margin-top: 44px; }
  .pd-notes { padding: 4px 20px; }
  .pd-order { padding: 14px 16px; }
  .pd-report { padding: 16px 18px; }
  .pd-actions .cta { flex: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .pd-media-spin,
  .pd-skel-media,
  .pd-skel-line { animation: none; }
}
</style>
