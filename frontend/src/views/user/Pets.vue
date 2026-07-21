<template>
  <div>
    <PageHero title="我的宠物" subtitle="管理你的宠物信息" />

    <div class="page-actions">
      <button class="btn btn-primary" @click="openAddForm">+ 添加宠物</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="pets.length === 0" class="empty-state">
      <div class="icon">宠</div>
      <h3>还没有宠物</h3>
      <p>添加你的第一只宠物吧</p>
      <button class="btn btn-primary" @click="openAddForm">添加宠物</button>
    </div>

    <PetList v-else :pets="pets" @edit="editPet" @delete="deletePet" @view="viewPet" />

    <PetFormDialog
      :visible="showAddForm"
      :pet="editingPet"
      :saving="saving"
      :server-error="serverError"
      @save="handleSave"
      @close="closeForm"
    />

    <div v-if="confirmDelete !== null" class="modal-overlay" @mousedown.self="confirmDelete = null">
      <div class="modal" style="max-width:400px">
        <h3>确认删除</h3>
        <p style="margin:16px 0">确定删除该宠物吗？此操作不可恢复。</p>
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" @click="confirmDelete = null">取消</button>
          <button class="btn btn-danger btn-sm" @click="doDelete">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'
import PetList from '@/components/pet/PetList.vue'
import PetFormDialog from '@/components/pet/PetFormDialog.vue'
import { ensureProfileRequirement, PROFILE_ACTIONS } from '@/utils/profileRequirements'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const pets = ref([])
const loading = ref(true)
const saving = ref(false)
const showAddForm = ref(false)
const editingPet = ref(null)
const serverError = ref('')
const confirmDelete = ref(null)

async function loadPets() {
  loading.value = true
  try {
    const r = await authStore.apiGet('/api/pets')
    if (r.code === 200) pets.value = Array.isArray(r.data) ? r.data : []
  } catch (e) {
    appStore.addToast(e.message || '加载宠物失败', 'error')
  } finally {
    loading.value = false
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
    await loadPets()
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
    await loadPets()
  } catch (e) {
    appStore.addToast(e.response?.data?.message || e.message || '删除失败', 'error')
  }
}

onMounted(loadPets)
</script>

<style scoped>
.page-actions {
  margin-bottom: 24px;
}
</style>
