<template>
  <div class="pets-page">
    <!-- Header -->
    <div class="pets-header">
      <div class="pets-header-left">
        <h1 class="pets-title">我的宠物</h1>
        <p class="pets-subtitle">
          <template v-if="loading">正在同步宠物档案</template>
          <template v-else>
            共 {{ pets.length }} 只
            <span v-if="incompleteCount > 0" class="pets-incomplete">
              <span class="pets-dot-sep">·</span>
              {{ incompleteCount }} 份档案待完善
            </span>
          </template>
        </p>
      </div>
      <div class="pets-header-actions">
        <button class="btn-icon" @click="loadPets(true)" :disabled="loading" aria-label="刷新列表">
          <svg class="icon-refresh" :class="{ spinning: refreshing }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.3"/>
          </svg>
        </button>
        <button class="btn btn-primary" @click="openAddForm">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="icon-sm"><path d="M12 5v14M5 12h14"/></svg>
          添加宠物
        </button>
      </div>
    </div>

    <!-- Type Filter Tabs -->
    <nav v-if="!loading && !error && typeGroups.length > 1" class="pets-filter" aria-label="按类型筛选">
      <ul class="pets-filter-list">
        <li v-for="group in [{ type: 'all', count: pets.length }, ...typeGroups]" :key="group.type">
          <button
            type="button"
            class="pets-filter-btn"
            :class="{ active: typeFilter === group.type }"
            @click="typeFilter = group.type"
          >
            <span class="pets-filter-label">{{ group.type === 'all' ? '全部' : petTypeLabel(group.type) }}</span>
            <span class="pets-filter-count">{{ group.count }}</span>
          </button>
        </li>
      </ul>
    </nav>

    <!-- Content -->
    <div class="pets-content">
      <!-- Loading Skeleton -->
      <div v-if="loading" class="pets-skeleton-grid">
        <div v-for="i in 3" :key="i" class="pets-skeleton-card">
          <div class="skeleton-header">
            <div class="skeleton-avatar"></div>
            <div class="skeleton-lines">
              <div class="skeleton-line w-28"></div>
              <div class="skeleton-line w-20"></div>
            </div>
          </div>
          <div class="skeleton-specs">
            <div class="skeleton-spec" v-for="j in 3" :key="j">
              <div class="skeleton-line w-8"></div>
              <div class="skeleton-line w-14"></div>
            </div>
          </div>
          <div class="skeleton-badges">
            <div class="skeleton-line w-16"></div>
            <div class="skeleton-line w-16"></div>
          </div>
          <div class="skeleton-text">
            <div class="skeleton-line full"></div>
            <div class="skeleton-line w-2-3"></div>
          </div>
          <div class="skeleton-actions">
            <div class="skeleton-btn"></div>
            <div class="skeleton-btn"></div>
          </div>
        </div>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="pets-state-panel pets-state-danger">
        <span class="pets-state-icon pets-state-icon-danger">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 1l22 22M16.72 11.06A10.94 10.94 0 0 1 19 12.55M5 12.55a10.94 10.94 0 0 1 5.17-2.39M10.71 5.05A16 16 0 0 1 22.56 9M1.42 9a15.91 15.91 0 0 1 4.7-2.88M8.53 16.11a6 6 0 0 1 6.95 0M12 20h.01"/></svg>
        </span>
        <h2 class="pets-state-title">宠物列表加载失败</h2>
        <p class="pets-state-desc">{{ error }}</p>
        <button class="btn btn-outline" @click="loadPets()">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="icon-sm"><path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.3"/></svg>
          重新加载
        </button>
      </div>

      <!-- Empty State -->
      <div v-else-if="pets.length === 0" class="pets-state-panel">
        <span class="pets-state-icon pets-state-icon-brand">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11.5 9C11.5 9 9 7 7 9s-2 5 2 7 7-2 7-2-1.5-2-4.5-4z"/><circle cx="6.5" cy="6.5" r="1.5"/><circle cx="17.5" cy="6.5" r="1.5"/><circle cx="11.5" cy="5.5" r="1.5"/><path d="M12 18c-4 0-6-2-6-5 0-2 2-4 6-4s6 2 6 4c0 3-2 5-6 5z"/></svg>
        </span>
        <h2 class="pets-state-title">还没有宠物</h2>
        <p class="pets-state-desc">添加宠物档案后，寄养商户可以更好地了解它的习惯与健康情况。</p>
        <button class="btn btn-primary" @click="openAddForm">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="icon-sm"><path d="M12 5v14M5 12h14"/></svg>
          添加宠物
        </button>
      </div>

      <!-- Filter Empty -->
      <div v-else-if="visiblePets.length === 0" class="pets-state-panel">
        <span class="pets-state-icon pets-state-icon-brand">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11.5 9C11.5 9 9 7 7 9s-2 5 2 7 7-2 7-2-1.5-2-4.5-4z"/><circle cx="6.5" cy="6.5" r="1.5"/><circle cx="17.5" cy="6.5" r="1.5"/><circle cx="11.5" cy="5.5" r="1.5"/><path d="M12 18c-4 0-6-2-6-5 0-2 2-4 6-4s6 2 6 4c0 3-2 5-6 5z"/></svg>
        </span>
        <h2 class="pets-state-title">没有{{ petTypeLabel(typeFilter) }}类型的宠物</h2>
        <p class="pets-state-desc">切换筛选条件即可查看其他宠物档案。</p>
        <button class="btn btn-outline" @click="typeFilter = 'all'">查看全部</button>
      </div>

      <!-- Pet List -->
      <PetList
        v-else
        :pets="visiblePets"
        @edit="editPet"
        @delete="deletePet"
        @view="viewPet"
      />
    </div>

    <!-- Form Dialog -->
    <PetFormDialog
      :visible="showAddForm"
      :pet="editingPet"
      :saving="saving"
      :server-error="serverError"
      @save="handleSave"
      @close="closeForm"
    />

    <!-- Delete Confirm -->
    <div v-if="confirmDelete !== null" class="modal-overlay" @mousedown.self="confirmDelete = null">
      <div class="modal modal-sm">
        <div class="modal-header">
          <h3>确认删除</h3>
          <button class="modal-close" @click="confirmDelete = null" aria-label="关闭">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="confirm-warning">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            <p>确定删除该宠物吗？此操作不可恢复。</p>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline btn-sm" @click="confirmDelete = null">取消</button>
          <button class="btn btn-danger btn-sm" @click="doDelete">确认删除</button>
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
.pets-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px;
}

/* Header */
.pets-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.pets-header-left { min-width: 0; }
.pets-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-foreground);
  letter-spacing: -0.02em;
}
.pets-subtitle {
  margin-top: 6px;
  font-size: 14px;
  color: var(--color-muted-foreground);
}
.pets-incomplete { }
.pets-dot-sep { margin: 0 8px; color: var(--color-border); }
.pets-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* Icon Button */
.btn-icon {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  border: 1px solid var(--color-border);
  background: var(--color-card);
  color: var(--color-foreground);
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-icon:hover { border-color: var(--color-border); background: var(--color-muted); }
.btn-icon:disabled { opacity: 0.5; pointer-events: none; }
.icon-refresh { width: 16px; height: 16px; }
.icon-refresh.spinning { animation: spin 1s linear infinite; }
.icon-sm { width: 16px; height: 16px; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

/* Filter Tabs */
.pets-filter {
  margin-top: 24px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}
.pets-filter::-webkit-scrollbar { display: none; }
.pets-filter-list {
  display: flex;
  width: max-content;
  align-items: center;
  gap: 4px;
  list-style: none;
  padding: 4px;
  border-radius: 16px;
  border: 1px solid var(--color-border);
  background: var(--color-card);
  box-shadow: var(--shadow-sm);
}
.pets-filter-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 12px;
  border-radius: 10px;
  border: none;
  background: transparent;
  font-size: 14px;
  color: var(--color-muted-foreground);
  cursor: pointer;
  transition: color 0.15s ease;
  white-space: nowrap;
}
.pets-filter-btn:hover { color: var(--color-foreground); }
.pets-filter-btn.active {
  color: var(--color-foreground);
  background: var(--color-muted);
}
.pets-filter-label { font-weight: 500; position: relative; z-index: 1; }
.pets-filter-count {
  font-size: 12px;
  position: relative;
  z-index: 1;
  color: var(--color-muted-foreground);
}

/* Content */
.pets-content { margin-top: 24px; }

/* State Panels */
.pets-state-panel {
  border-radius: 16px;
  border: 2px dashed var(--color-border);
  background: var(--color-card);
  padding: 56px 24px;
  text-align: center;
}
.pets-state-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
}
.pets-state-icon svg { width: 20px; height: 20px; }
.pets-state-icon-brand { background: rgba(249, 115, 22, 0.08); color: var(--color-primary); }
.pets-state-icon-danger { background: rgba(220, 38, 38, 0.08); color: var(--color-danger); }
.pets-state-title {
  margin-top: 16px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-foreground);
}
.pets-state-desc {
  margin-top: 6px;
  max-width: 400px;
  margin-left: auto;
  margin-right: auto;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-muted-foreground);
}
.pets-state-panel .btn { margin-top: 24px; }

/* Skeleton */
.pets-skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}
.pets-skeleton-card {
  border-radius: 16px;
  border: 1px solid var(--color-border);
  background: var(--color-card);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  animation: shimmer 1.6s ease-in-out infinite;
}
.skeleton-header { display: flex; align-items: center; gap: 14px; }
.skeleton-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-muted);
  flex-shrink: 0;
}
.skeleton-lines { flex: 1; }
.skeleton-line {
  height: 12px;
  border-radius: 6px;
  background: var(--color-muted);
  margin-bottom: 8px;
}
.skeleton-line.w-8 { width: 32px; }
.skeleton-line.w-14 { width: 56px; }
.skeleton-line.w-16 { width: 64px; }
.skeleton-line.w-20 { width: 80px; }
.skeleton-line.w-28 { width: 112px; }
.skeleton-line.full { width: 100%; }
.skeleton-line.w-2-3 { width: 66%; }
.skeleton-specs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin: 16px 0;
  padding: 14px 0;
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
}
.skeleton-badges { display: flex; gap: 12px; margin-bottom: 12px; }
.skeleton-text { margin-bottom: 12px; }
.skeleton-actions { display: flex; gap: 8px; margin-top: 16px; }
.skeleton-btn {
  width: 80px;
  height: 32px;
  border-radius: 8px;
  background: var(--color-muted);
}
@keyframes shimmer {
  0% { opacity: 0.55; }
  50% { opacity: 1; }
  100% { opacity: 0.55; }
}

/* Modal enhancements */
.modal-sm { max-width: 400px; }
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.modal-header h3 { font-size: 18px; }
.modal-close {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--color-muted-foreground);
  cursor: pointer;
  transition: all 0.15s ease;
}
.modal-close:hover { background: var(--color-muted); color: var(--color-foreground); }
.modal-close svg { width: 16px; height: 16px; }
.modal-body { margin-bottom: 0; }
.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}
.confirm-warning {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.confirm-warning svg {
  width: 20px;
  height: 20px;
  color: var(--color-danger);
  flex-shrink: 0;
  margin-top: 2px;
}
.confirm-warning p {
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-muted-foreground);
}

@media (max-width: 768px) {
  .pets-page { padding: 24px 16px; }
  .pets-header { flex-direction: column; align-items: stretch; }
  .pets-header-actions { justify-content: flex-end; }
}
</style>
