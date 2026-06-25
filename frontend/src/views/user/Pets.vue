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

    <div v-else class="pet-grid">
      <div v-for="pet in pets" :key="pet.id_wsh" class="pet-card">
        <div class="pet-card-header">
          <img
            v-if="pet.avatar_wsh"
            class="pet-avatar-img"
            :src="pet.avatar_wsh"
            :alt="pet.name_wsh"
          >
          <div v-else class="pet-avatar">{{ petTypeIcon(pet.type_wsh) }}</div>
          <div class="pet-title">
            <div class="pet-name">{{ pet.name_wsh }}</div>
            <div class="pet-info">{{ petTypeLabel(pet.type_wsh) }} / {{ pet.breed_wsh || '未知品种' }}</div>
          </div>
        </div>

        <div class="pet-detail-grid">
          <div>
            <span>年龄</span>
            <strong>{{ formatAge(pet.age_wsh) }}</strong>
          </div>
          <div>
            <span>体重</span>
            <strong>{{ formatWeight(pet.weight_wsh) }}</strong>
          </div>
          <div>
            <span>性别</span>
            <strong>{{ genderLabel(pet.gender_wsh) }}</strong>
          </div>
          <div>
            <span>档案</span>
            <strong>{{ pet.description_wsh ? '已补充' : '待完善' }}</strong>
          </div>
        </div>

        <div class="pet-badges">
          <span :class="['badge', pet.vaccinated_wsh === 1 ? 'badge-success' : 'badge-disabled']">
            {{ pet.vaccinated_wsh === 1 ? '已免疫' : '未免疫' }}
          </span>
          <span :class="['badge', pet.sterilized_wsh === 1 ? 'badge-info' : 'badge-disabled']">
            {{ pet.sterilized_wsh === 1 ? '已绝育' : '未绝育' }}
          </span>
        </div>

        <p v-if="pet.description_wsh" class="pet-note">{{ pet.description_wsh }}</p>
        <p v-if="pet.allergies_wsh" class="pet-warning">过敏/禁忌：{{ pet.allergies_wsh }}</p>
        <p v-if="pet.habits_wsh" class="pet-note">习惯：{{ pet.habits_wsh }}</p>

        <div class="pet-actions">
          <button class="btn btn-sm btn-outline" @click="editPet(pet)">编辑</button>
          <button class="btn btn-sm btn-danger" @click="deletePet(pet.id_wsh)">删除</button>
        </div>
      </div>
    </div>

    <div v-if="showAddForm" class="modal-overlay" @mousedown.self="closeForm">
      <div class="modal pet-modal">
        <h2>{{ editingPet ? '编辑宠物' : '添加宠物' }}</h2>
        <form @submit.prevent="savePet">
          <div v-if="formError" class="form-error">
            {{ formError }}
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>宠物名称</label>
              <input v-model.trim="petForm.name_wsh" placeholder="请输入宠物名称" required>
            </div>
            <div class="form-group">
              <label>类型</label>
              <select v-model="petForm.type_wsh" required>
                <option v-for="option in petTypeOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>品种</label>
              <input v-model.trim="petForm.breed_wsh" placeholder="如：金毛、英短">
            </div>
            <div class="form-group">
              <label>性别</label>
              <select v-model.number="petForm.gender_wsh">
                <option :value="0">未知</option>
                <option :value="1">公</option>
                <option :value="2">母</option>
              </select>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>年龄（月）</label>
              <input v-model.number="petForm.age_wsh" type="number" min="1" placeholder="如：12">
            </div>
            <div class="form-group">
              <label>体重（kg）</label>
              <input v-model.number="petForm.weight_wsh" type="number" min="0.1" step="0.1" placeholder="如：5.5">
            </div>
          </div>

          <div class="form-group">
            <label>头像</label>
            <div class="avatar-upload">
              <img v-if="avatarPreview" class="avatar-preview" :src="avatarPreview" alt="头像预览">
              <div v-else class="avatar-placeholder">无头像</div>
              <div class="avatar-actions">
                <label class="btn btn-sm btn-outline upload-btn">
                  选择照片
                  <input type="file" accept="image/*" hidden @change="onAvatarChange">
                </label>
                <button v-if="avatarPreview" type="button" class="btn btn-sm btn-danger" @click="removeAvatar">移除</button>
              </div>
              <div v-if="avatarUploading" class="uploading-hint">上传中...</div>
            </div>
          </div>

          <div class="pet-checks">
            <label class="check-item">
              <input v-model="petForm.vaccinated_wsh" type="checkbox" :true-value="1" :false-value="0">
              已完成疫苗/免疫
            </label>
            <label class="check-item">
              <input v-model="petForm.sterilized_wsh" type="checkbox" :true-value="1" :false-value="0">
              已绝育
            </label>
          </div>

          <div class="form-group">
            <label>性格与照护说明</label>
            <textarea v-model.trim="petForm.description_wsh" rows="3" placeholder="如：胆小、亲人、需要慢慢适应新环境"></textarea>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>过敏/禁忌</label>
              <textarea v-model.trim="petForm.allergies_wsh" rows="3" placeholder="如：鸡肉过敏、不能吃牛奶"></textarea>
            </div>
            <div class="form-group">
              <label>生活习惯</label>
              <textarea v-model.trim="petForm.habits_wsh" rows="3" placeholder="如：每天早晚各遛一次、喜欢猫砂盆靠墙"></textarea>
            </div>
          </div>

          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" @click="closeForm">取消</button>
            <button type="submit" class="btn btn-primary btn-sm" :disabled="saving">
              {{ saving ? '保存中...' : editingPet ? '保存' : '添加' }}
            </button>
          </div>
        </form>
      </div>
    </div>

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
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'

const authStore = useAuthStore()
const appStore = useAppStore()
const pets = ref([])
const loading = ref(true)
const saving = ref(false)
const showAddForm = ref(false)
const editingPet = ref(null)
const formError = ref('')
const avatarPreview = ref('')
const avatarUploading = ref(false)

const petTypeOptions = [
  { value: 'dog', label: '狗' },
  { value: 'cat', label: '猫' },
  { value: 'rabbit', label: '兔子' },
  { value: 'bird', label: '鸟' },
  { value: 'fish', label: '鱼' },
  { value: 'hamster', label: '仓鼠' },
  { value: 'other', label: '其他' },
]

const typeMap = {
  dog: '狗',
  cat: '猫',
  rabbit: '兔子',
  bird: '鸟',
  fish: '鱼',
  hamster: '仓鼠',
  other: '其他',
}

const iconMap = {
  dog: '犬',
  cat: '猫',
  rabbit: '兔',
  bird: '鸟',
  fish: '鱼',
  hamster: '鼠',
  other: '宠',
}

function createEmptyPetForm() {
  return {
    name_wsh: '',
    type_wsh: 'dog',
    breed_wsh: '',
    age_wsh: null,
    weight_wsh: null,
    gender_wsh: 0,
    sterilized_wsh: 0,
    vaccinated_wsh: 0,
    avatar_wsh: '',
    description_wsh: '',
    allergies_wsh: '',
    habits_wsh: '',
  }
}

const petForm = ref(createEmptyPetForm())

function normalizeType(type) {
  return String(type || 'other').toLowerCase()
}

function petTypeLabel(type) {
  return typeMap[normalizeType(type)] || type || '其他'
}

function petTypeIcon(type) {
  return iconMap[normalizeType(type)] || '宠'
}

function genderLabel(gender) {
  const value = Number(gender)
  if (value === 1) return '公'
  if (value === 2) return '母'
  return '未知'
}

function formatAge(age) {
  return age ? `${age}个月` : '-'
}

function formatWeight(weight) {
  return weight ? `${weight}kg` : '-'
}

function emptyToNull(value) {
  return value === '' || value === undefined ? null : value
}

function resetForm() {
  Object.assign(petForm.value, createEmptyPetForm())
  formError.value = ''
  avatarPreview.value = ''
}

function openAddForm() {
  editingPet.value = null
  resetForm()
  showAddForm.value = true
}

function closeForm() {
  showAddForm.value = false
  editingPet.value = null
  resetForm()
}

function toPayload() {
  return {
    name_wsh: petForm.value.name_wsh,
    type_wsh: petForm.value.type_wsh,
    breed_wsh: emptyToNull(petForm.value.breed_wsh),
    age_wsh: emptyToNull(petForm.value.age_wsh),
    weight_wsh: emptyToNull(petForm.value.weight_wsh),
    gender_wsh: Number(petForm.value.gender_wsh) || 0,
    sterilized_wsh: petForm.value.sterilized_wsh ? 1 : 0,
    vaccinated_wsh: petForm.value.vaccinated_wsh ? 1 : 0,
    avatar_wsh: emptyToNull(petForm.value.avatar_wsh),
    description_wsh: emptyToNull(petForm.value.description_wsh),
    allergies_wsh: emptyToNull(petForm.value.allergies_wsh),
    habits_wsh: emptyToNull(petForm.value.habits_wsh),
  }
}

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

function editPet(pet) {
  editingPet.value = pet
  Object.assign(petForm.value, {
    name_wsh: pet.name_wsh || '',
    type_wsh: normalizeType(pet.type_wsh),
    breed_wsh: pet.breed_wsh || '',
    age_wsh: pet.age_wsh ?? null,
    weight_wsh: pet.weight_wsh ?? null,
    gender_wsh: pet.gender_wsh ?? 0,
    sterilized_wsh: pet.sterilized_wsh ?? 0,
    vaccinated_wsh: pet.vaccinated_wsh ?? 0,
    avatar_wsh: pet.avatar_wsh || '',
    description_wsh: pet.description_wsh || '',
    allergies_wsh: pet.allergies_wsh || '',
    habits_wsh: pet.habits_wsh || '',
  })
  avatarPreview.value = pet.avatar_wsh || ''
  formError.value = ''
  showAddForm.value = true
}

async function onAvatarChange(e) {
  const file = e.target.files[0]
  if (!file) return
  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const r = await authStore.apiPost('/api/files/upload?directory=pets', formData)
    if (r.code === 200 && r.data.url_wsh) {
      petForm.value.avatar_wsh = r.data.url_wsh
      avatarPreview.value = r.data.url_wsh
    } else {
      appStore.addToast('上传失败', 'error')
    }
  } catch (err) {
    appStore.addToast('上传失败: ' + (err.message || '未知错误'), 'error')
  } finally {
    avatarUploading.value = false
    e.target.value = ''
  }
}

function removeAvatar() {
  petForm.value.avatar_wsh = ''
  avatarPreview.value = ''
}

async function savePet() {
  formError.value = ''
  saving.value = true
  try {
    const payload = toPayload()
    const r = editingPet.value
      ? await authStore.apiPut(`/api/pets/${editingPet.value.id_wsh}`, payload)
      : await authStore.apiPost('/api/pets', payload)

    if (r.code !== 200) {
      formError.value = r.message || '保存失败'
      return
    }

    appStore.addToast(editingPet.value ? '更新成功' : '添加成功', 'success')
    closeForm()
    await loadPets()
  } catch (e) {
    formError.value = e.response?.data?.message || e.message || '操作失败'
    appStore.addToast(formError.value, 'error')
  } finally {
    saving.value = false
  }
}

const confirmDelete = ref(null)

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

.pet-card-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.pet-title {
  min-width: 0;
}

.pet-avatar-img {
  width: 72px;
  height: 72px;
  border-radius: 999px;
  object-fit: cover;
  background: var(--color-muted);
  flex: 0 0 auto;
}

.pet-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 14px 0;
}

.pet-detail-grid div {
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-muted);
}

.pet-detail-grid span {
  display: block;
  font-size: 12px;
  color: var(--color-muted-foreground);
}

.pet-detail-grid strong {
  display: block;
  margin-top: 2px;
  font-size: 14px;
  color: var(--color-foreground);
}

.pet-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.pet-note,
.pet-warning {
  margin-top: 8px;
  font-size: 13px;
  color: var(--color-muted-foreground);
}

.pet-warning {
  color: var(--color-destructive);
}

.pet-actions {
  margin-top: 14px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pet-modal {
  max-width: 760px;
}

.form-error {
  margin-bottom: 12px;
  padding: 10px;
  border-radius: 8px;
  background: #fef2f2;
  color: #991b1b;
  font-size: 13px;
}

.pet-checks {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.check-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
}

.check-item input {
  width: 18px;
  height: 18px;
  padding: 0;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.avatar-preview {
  width: 72px;
  height: 72px;
  border-radius: 999px;
  object-fit: cover;
  border: 2px solid var(--color-border);
}

.avatar-placeholder {
  width: 72px;
  height: 72px;
  border-radius: 999px;
  background: var(--color-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--color-muted-foreground);
  border: 2px dashed var(--color-border);
}

.avatar-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.upload-btn {
  cursor: pointer;
}

.uploading-hint {
  font-size: 13px;
  color: var(--color-muted-foreground);
}
</style>
