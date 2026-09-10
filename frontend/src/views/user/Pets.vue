<template>
  <div class="pt-page">
    <div class="pt-shell">
      <!-- ═══ Breadcrumb ═══ -->
      <nav class="pt-crumb" aria-label="面包屑">
        <router-link to="/dashboard" class="pt-crumb-link">首页</router-link>
        <span class="pt-crumb-sep" aria-hidden="true">›</span>
        <span class="pt-crumb-here">我的宠物</span>
      </nav>

      <!-- ═══ Header ═══ -->
      <header class="pt-head">
        <div class="pt-head-copy">
          <div class="pt-eyebrow" aria-hidden="true">
            <span class="pt-eyebrow-line" />
            <span>宠物档案</span>
          </div>
          <h1 class="pt-title">我的宠物</h1>
          <p class="pt-sub" aria-live="polite">
            <template v-if="loading">正在同步宠物档案</template>
            <template v-else>
              共 {{ pets.length }} 只
              <span v-if="incompleteCount > 0" class="pt-sub-more">
                <span class="pt-dot-sep" aria-hidden="true">·</span>
                {{ incompleteCount }} 份档案待完善
              </span>
            </template>
          </p>
        </div>
        <div class="pt-actions">
          <button
            type="button"
            class="cta cta-outline pt-refresh"
            :disabled="loading"
            aria-label="刷新列表"
            @click="loadPets(true)"
          >
            <svg class="pt-refresh-icon" :class="{ spinning: refreshing }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.3"/>
            </svg>
            刷新
          </button>
          <button type="button" class="cta cta-primary" @click="openAddForm">
            <svg class="pt-plus-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12h14"/></svg>
            添加宠物
          </button>
        </div>
      </header>

      <!-- ═══ Facts ═══ -->
      <dl class="pt-facts" aria-label="宠物档案概览">
        <div>
          <dd>{{ loading ? '--' : pets.length }}</dd>
          <dt>宠物档案</dt>
        </div>
        <div>
          <dd>{{ loading ? '--' : incompleteCount }}</dd>
          <dt>待完善</dt>
        </div>
        <div>
          <dd>{{ loading ? '--' : typeGroups.length }}</dd>
          <dt>宠物类型</dt>
        </div>
      </dl>

      <!-- ═══ Type filter ═══ -->
      <nav v-if="!loading && typeGroups.length > 1" class="pt-rail" role="tablist" aria-label="按类型筛选">
        <button
          v-for="group in [{ type: 'all', count: pets.length }, ...typeGroups]"
          :key="group.type"
          type="button"
          role="tab"
          :aria-selected="typeFilter === group.type"
          class="pt-chip"
          :class="{ active: typeFilter === group.type }"
          @click="typeFilter = group.type"
        >
          {{ group.type === 'all' ? '全部' : petTypeLabel(group.type) }}
          <span class="pt-chip-count">{{ group.count }}</span>
        </button>
      </nav>

      <!-- ═══ Content ═══ -->
      <section class="pt-content" aria-label="宠物列表" :aria-busy="loading">
        <!-- Loading skeleton -->
        <div v-if="loading" class="pt-skel-grid" aria-hidden="true">
          <div v-for="i in 3" :key="i" class="pt-skel-card">
            <div class="pt-skel-media"></div>
            <div class="pt-skel-body">
              <div class="pt-skel-line w-40"></div>
              <div class="pt-skel-line w-60"></div>
              <div class="pt-skel-line w-80"></div>
              <div class="pt-skel-specs">
                <div class="pt-skel-line w-20"></div>
                <div class="pt-skel-line w-20"></div>
                <div class="pt-skel-line w-20"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Empty -->
        <div v-else-if="pets.length === 0" class="pt-empty">
          <p class="pt-empty-title">还没有宠物档案</p>
          <p class="pt-empty-desc">添加第一只宠物后，下单时它的品种、体重与照护要求会自动带入，不必每次重填。</p>
          <div class="pt-empty-actions">
            <button type="button" class="cta cta-primary" @click="openAddForm">添加宠物</button>
            <router-link to="/services" class="cta cta-outline">浏览照护服务</router-link>
          </div>
        </div>

        <!-- Filter empty -->
        <div v-else-if="visiblePets.length === 0" class="pt-empty">
          <p class="pt-empty-title">没有{{ petTypeLabel(typeFilter) }}类型的宠物</p>
          <p class="pt-empty-desc">切换筛选条件即可查看其他宠物档案。</p>
          <div class="pt-empty-actions">
            <button type="button" class="cta cta-outline" @click="typeFilter = 'all'">查看全部</button>
          </div>
        </div>

        <!-- List -->
        <PetList
          v-else
          :pets="visiblePets"
          @edit="editPet"
          @delete="deletePet"
          @view="viewPet"
        />
      </section>
    </div>

    <!-- ═══ Form Dialog ═══ -->
    <PetFormDialog
      :visible="showAddForm"
      :pet="editingPet"
      :saving="saving"
      :server-error="serverError"
      @save="handleSave"
      @close="closeForm"
    />

    <!-- ═══ Delete Confirm ═══ -->
    <div v-if="confirmDelete !== null" class="dlg-overlay" @mousedown.self="confirmDelete = null">
      <div class="dlg-panel dlg-panel-sm" role="dialog" aria-modal="true" aria-label="确认删除">
        <div class="dlg-head">
          <h3 class="dlg-title">删除这份宠物档案？</h3>
          <button type="button" class="dlg-close" aria-label="关闭" @click="confirmDelete = null">✕</button>
        </div>
        <div class="dlg-body">
          <p class="dlg-desc">宠物的照片、照护说明与健康信息会一并移除。此操作不可恢复，已完成的订单记录不受影响。</p>
        </div>
        <div class="dlg-foot">
          <button type="button" class="dlg-cancel" @click="confirmDelete = null">取消</button>
          <button type="button" class="cta cta-danger" @click="doDelete">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import PetList from '@/components/pet/PetList.vue'
import PetFormDialog from '@/components/pet/PetFormDialog.vue'
import { ensureProfileRequirement, PROFILE_ACTIONS } from '@/utils/profileRequirements'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const pets = ref([])
const loading = ref(true)
const refreshing = ref(false)
const saving = ref(false)
const showAddForm = ref(false)
const editingPet = ref(null)
const serverError = ref('')
const confirmDelete = ref(null)
const typeFilter = ref('all')

const typeMap = {
  dog: '狗', cat: '猫', rabbit: '兔子', bird: '鸟',
  fish: '鱼', hamster: '仓鼠', other: '其他',
}

function normalizeType(type) {
  return String(type || 'other').toLowerCase()
}

function petTypeLabel(type) {
  return typeMap[normalizeType(type)] || type || '其他'
}

function profileComplete(pet) {
  return Boolean(pet.description_wsh)
}

const typeGroups = computed(() => {
  const counts = new Map()
  pets.value.forEach((pet) => {
    const type = normalizeType(pet.type_wsh)
    counts.set(type, (counts.get(type) || 0) + 1)
  })
  return Array.from(counts.entries()).map(([type, count]) => ({ type, count }))
})

const visiblePets = computed(() =>
  typeFilter.value === 'all'
    ? pets.value
    : pets.value.filter((pet) => normalizeType(pet.type_wsh) === typeFilter.value)
)

const incompleteCount = computed(() => pets.value.filter((pet) => !profileComplete(pet)).length)

async function loadPets(silent = false) {
  if (silent) refreshing.value = true
  else loading.value = true
  try {
    const r = await authStore.apiGet('/api/pets')
    if (r.code === 200) pets.value = Array.isArray(r.data) ? r.data : []
  } catch (e) {
    appStore.addToast(e.message || '加载宠物失败', 'error')
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

async function openAddForm() {
  const ok = await ensureProfileRequirement(PROFILE_ACTIONS.ADD_PET, { authStore, appStore, router })
  if (!ok) return
  editingPet.value = null
  serverError.value = ''
  showAddForm.value = true
}

function viewPet(id) {
  router.push(`/pets/${id}`)
}

function editPet(pet) {
  editingPet.value = pet
  serverError.value = ''
  showAddForm.value = true
}

function closeForm() {
  showAddForm.value = false
  editingPet.value = null
  serverError.value = ''
}

async function handleSave(payload) {
  saving.value = true
  serverError.value = ''
  try {
    const r = editingPet.value
      ? await authStore.apiPut(`/api/pets/${editingPet.value.id_wsh}`, payload)
      : await authStore.apiPost('/api/pets', payload)

    if (r.code !== 200) {
      serverError.value = r.message || '保存失败'
      return
    }

    appStore.addToast(editingPet.value ? '更新成功' : '添加成功', 'success')
    closeForm()
    await loadPets(true)
  } catch (e) {
    serverError.value = e.response?.data?.message || e.message || '操作失败'
    appStore.addToast(serverError.value, 'error')
  } finally {
    saving.value = false
  }
}

function deletePet(id) {
  confirmDelete.value = id
}

async function doDelete() {
  if (!confirmDelete.value) return
  const id = confirmDelete.value
  confirmDelete.value = null
  try {
    const r = await authStore.apiDelete(`/api/pets/${id}`)
    if (r.code !== 200) return
    appStore.addToast('删除成功', 'success')
    await loadPets(true)
  } catch (e) {
    appStore.addToast(e.response?.data?.message || e.message || '删除失败', 'error')
  }
}

onMounted(() => loadPets())
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════
   Editorial warm style — shared --ref-* tokens from
   assets/css/design-tokens.css. Dark mode via html[data-theme="dark"].
   PetList / PetFormDialog 为共享组件，功能保持不变，仅外观通过 :deep 覆盖。
   ═══════════════════════════════════════════════════════ */
.pt-page {
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

.pt-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ═══ Breadcrumb ═══ */
.pt-crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12.5px;
  color: var(--ref-muted);
}
.pt-crumb-link { color: var(--ref-muted); text-decoration: none; }
.pt-crumb-link:hover { color: var(--ref-ink); }
.pt-crumb-sep { color: var(--ref-line); }
.pt-crumb-here { color: var(--ref-ink-soft); }

/* ═══ Header ═══ */
.pt-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px 28px;
  padding: 40px 0 0;
}
.pt-head-copy { min-width: 0; }
.pt-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
}
.pt-eyebrow-line { width: 32px; height: 1px; background: var(--ref-line); }
.pt-eyebrow > span:last-child {
  font-size: 9.5px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
.pt-title {
  margin: 16px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(32px, 4vw, 44px);
  line-height: 1.1;
  letter-spacing: -0.01em;
  font-weight: 500;
  color: var(--ref-ink);
  text-wrap: balance;
}
.pt-sub {
  margin: 12px 0 0;
  font-size: 13.5px;
  color: var(--ref-muted);
}
.pt-sub-more { color: var(--ref-ink-soft); }
.pt-dot-sep { margin: 0 8px; color: var(--ref-line); }
.pt-actions { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.pt-refresh-icon { width: 15px; height: 15px; }
.pt-refresh-icon.spinning { animation: pt-spin 0.9s linear infinite; }
.pt-plus-icon { width: 15px; height: 15px; }

/* ═══ Facts ═══ */
.pt-facts {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 0 28px;
  margin: 32px 0 0;
}
.pt-facts div { display: flex; flex-direction: column; }
.pt-facts dd {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 28px;
  line-height: 1;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}
.pt-facts dt { margin-top: 8px; font-size: 12px; color: var(--ref-muted); }

/* ═══ Type rail ═══ */
.pt-rail {
  display: flex;
  gap: 8px;
  margin-top: 28px;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: none;
}
.pt-rail::-webkit-scrollbar { display: none; }
.pt-chip {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 7px;
  height: 42px;
  padding: 0 16px;
  border-radius: var(--radius-control);
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 150ms ease, border-color 150ms ease, color 150ms ease;
}
.pt-chip:hover {
  border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent);
  background: var(--ref-sand);
}
.pt-chip.active {
  background: var(--ref-ink);
  border-color: var(--ref-ink);
  color: var(--ref-cream);
}
.pt-chip-count {
  font-size: 11px;
  opacity: 0.6;
  font-variant-numeric: tabular-nums;
}

/* ═══ Content ═══ */
.pt-content { margin-top: 28px; }

/* ═══ Skeleton ═══ */
.pt-skel-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.pt-skel-card {
  overflow: hidden;
  border: 1px solid var(--ref-line);
  border-radius: var(--r-card);
  background: var(--ref-surface);
}
.pt-skel-media {
  height: 132px;
  background: linear-gradient(90deg, var(--ref-sand) 25%, var(--ref-surface) 50%, var(--ref-sand) 75%);
  background-size: 200% 100%;
  animation: pt-shimmer 1.3s linear infinite;
}
.pt-skel-body { padding: 18px 20px 20px; }
.pt-skel-line {
  height: 12px;
  border-radius: var(--radius-inline);
  background: var(--ref-sand);
  margin-bottom: 10px;
  animation: pt-pulse 1.4s ease-in-out infinite;
}
.pt-skel-line.w-20 { width: 80px; }
.pt-skel-line.w-40 { width: 160px; }
.pt-skel-line.w-60 { width: 240px; }
.pt-skel-line.w-80 { width: 320px; }
.pt-skel-specs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--ref-line);
}

/* ═══ Empty ═══ */
.pt-empty {
  padding: 56px 24px;
  border: 1px dashed var(--ref-line);
  border-radius: var(--r-card);
  background: var(--ref-surface);
  text-align: center;
}
.pt-empty-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 19px;
  font-weight: 500;
  color: var(--ref-ink);
}
.pt-empty-desc {
  margin: 10px auto 0;
  max-width: 420px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ref-muted);
}
.pt-empty-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 22px;
}

/* ═══ CTA ═══ */
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
.cta-primary { background: var(--ref-brand); color: #fff; }
.cta-primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.cta-outline {
  background: var(--ref-surface);
  color: var(--ref-ink);
  border-color: var(--ref-line);
}
.cta-outline:hover:not(:disabled) { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }
.cta-danger {
  background: color-mix(in srgb, var(--color-danger) 9%, transparent);
  color: var(--color-danger);
  border-color: color-mix(in srgb, var(--color-danger) 28%, transparent);
}
.cta-danger:hover:not(:disabled) { background: color-mix(in srgb, var(--color-danger) 15%, transparent); }

/* ═══ Dialog ═══ */
.dlg-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(10, 8, 6, 0.5);
  backdrop-filter: blur(2px);
  animation: pt-fade 0.15s ease;
}
.dlg-panel {
  width: 100%;
  max-width: 440px;
  max-height: min(90vh, 720px);
  overflow-y: auto;
  border-radius: var(--r-panel);
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  box-shadow: var(--shadow-pop);
  animation: pt-pop 0.18s cubic-bezier(0.23, 1, 0.32, 1);
}
.dlg-panel-sm { max-width: 400px; }
.dlg-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 24px 0;
}
.dlg-title {
  margin: 0;
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 500;
  color: var(--ref-ink);
}
.dlg-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: transparent;
  border: none;
  color: var(--ref-muted);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.dlg-close:hover { background: var(--ref-sand); color: var(--ref-ink); }
.dlg-body { padding: 18px 24px 8px; }
.dlg-desc {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
}
.dlg-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 20px 24px 24px;
}
.dlg-cancel {
  height: 42px;
  padding: 0 18px;
  border-radius: var(--r-btn);
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.dlg-cancel:hover { background: var(--ref-sand); border-color: color-mix(in srgb, var(--ref-ink) 30%, transparent); }

/* ═══════════════════════════════════════════════════════
   PetList 共享组件外观覆盖（功能不变）
   ═══════════════════════════════════════════════════════ */
:deep(.pet-grid.pet-grid) {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 0;
}
:deep(.pet-card-wrap) { height: 100%; }
:deep(.pet-card) {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 18px;
  border-radius: var(--r-card);
  border: 1px solid var(--ref-line);
  background: var(--ref-surface);
  box-shadow: none;
  transition: transform 0.2s cubic-bezier(0.23, 1, 0.32, 1), border-color 0.2s, box-shadow 0.2s;
}
:deep(.pet-card:hover) {
  transform: translateY(-3px);
  border-color: color-mix(in srgb, var(--ref-ink) 16%, transparent);
  box-shadow: 0 28px 60px -40px color-mix(in srgb, var(--ref-ink) 45%, transparent);
}
:deep(.pet-avatar-img) {
  width: 78px;
  height: 78px;
  border-radius: 14px;
  border: 1px solid var(--ref-line);
}
:deep(.pet-avatar-placeholder) {
  width: 78px;
  height: 78px;
  border-radius: 14px;
  background: var(--ref-sand);
  color: var(--ref-ink-soft);
  border: 1px solid var(--ref-line);
}
:deep(.pet-avatar-placeholder svg) { width: 30px; height: 30px; }
:deep(.pet-name-btn) {
  font-family: var(--ref-font-display);
  font-size: 18px;
  font-weight: 500;
  letter-spacing: -0.01em;
  color: var(--ref-ink);
}
:deep(.pet-name-btn:hover) { text-decoration: none; color: var(--ref-brand); }
:deep(.pet-type-breed) {
  margin-top: 3px;
  font-size: 12px;
  color: var(--ref-muted);
}
:deep(.pet-sep) { margin: 0 6px; color: var(--ref-line); }
:deep(.pet-badge-warn) {
  padding: 4px 9px;
  border-radius: var(--r-tag);
  background: color-mix(in srgb, var(--ref-brand) 12%, transparent);
  color: var(--ref-brand-deep);
  font-size: 11px;
}
:deep(.pet-badge-warn svg) { width: 11px; height: 11px; }
:deep(.pet-specs) {
  margin: 14px 0;
  padding: 12px 0;
  border-top: 1px solid var(--ref-line);
  border-bottom: 1px solid var(--ref-line);
}
:deep(.pet-spec) { border-right: 1px solid var(--ref-line); }
:deep(.pet-spec dt) {
  font-size: 10px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--ref-muted);
}
:deep(.pet-spec dd) {
  margin-top: 3px;
  font-family: var(--ref-font-display);
  font-size: 15px;
  font-weight: 500;
  color: var(--ref-ink);
}
:deep(.pet-spec dd.pet-spec-empty) { color: var(--ref-muted); }
:deep(.pet-health) { gap: 16px; }
:deep(.pet-health-item) { font-size: 12px; color: var(--ref-muted); }
:deep(.pet-health-item.active) { color: var(--ref-ink-soft); }
:deep(.pet-notes) { margin-top: 10px; }
:deep(.pet-note-desc),
:deep(.pet-note-habit) { font-size: 13px; line-height: 1.65; color: var(--ref-ink-soft); }
:deep(.pet-note-allergy) { font-size: 12.5px; color: var(--color-danger); }
:deep(.pet-actions) {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--ref-line);
}
:deep(.pet-actions .btn-primary) {
  border: 1px solid transparent;
  border-radius: var(--r-btn);
  background: var(--ref-brand);
  color: #fff;
}
:deep(.pet-actions .btn-primary:hover) { background: var(--ref-brand-deep); }
:deep(.pet-actions .btn-outline) {
  border: 1px solid var(--ref-line);
  border-radius: var(--r-btn);
  background: var(--ref-surface);
  color: var(--ref-ink);
}
:deep(.pet-actions .btn-outline:hover) { border-color: color-mix(in srgb, var(--ref-ink) 35%, transparent); }
:deep(.pet-actions .btn-quiet) { color: var(--ref-muted); }
:deep(.pet-actions .btn-quiet:hover) {
  background: color-mix(in srgb, var(--color-danger) 8%, transparent);
  color: var(--color-danger);
}

/* ═══ Animations ═══ */
@keyframes pt-spin { to { transform: rotate(360deg); } }
@keyframes pt-shimmer { to { background-position: -200% 0; } }
@keyframes pt-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.55; } }
@keyframes pt-fade { from { opacity: 0; } to { opacity: 1; } }
@keyframes pt-pop {
  from { opacity: 0; transform: translateY(10px) scale(0.98); }
  to { opacity: 1; transform: none; }
}

/* ═══ Responsive ═══ */
@media (max-width: 900px) {
  :deep(.pet-grid.pet-grid) { grid-template-columns: repeat(2, 1fr); }
  .pt-skel-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 560px) {
  .pt-shell { padding: 0 16px; }
  .pt-head { padding-top: 30px; }
  .pt-actions { width: 100%; }
  .pt-actions .cta { flex: 1; }
  :deep(.pet-grid.pet-grid),
  .pt-skel-grid { grid-template-columns: 1fr; }
  .pt-facts { gap: 0 20px; }
  .pt-facts dd { font-size: 24px; }
  .pt-empty { padding: 44px 18px; }
}
@media (prefers-reduced-motion: reduce) {
  .pt-refresh-icon.spinning,
  .pt-skel-media,
  .pt-skel-line { animation: none; }
}
</style>
