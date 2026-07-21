<template>
  <div v-if="visible" class="modal-overlay" @mousedown.self="closeForm">
    <div class="modal pet-modal">
      <h2>{{ pet ? '编辑宠物' : '添加宠物' }}</h2>
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
            {{ saving ? '保存中...' : pet ? '保存' : '添加' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

const props = defineProps({
  visible: { type: Boolean, default: false },
  pet: { type: Object, default: null },
  saving: { type: Boolean, default: false },
  serverError: { type: String, default: '' },
})

const emit = defineEmits(['save', 'close'])

const authStore = useAuthStore()
const appStore = useAppStore()

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

function emptyToNull(value) {
  return value === '' || value === undefined ? null : value
}

function resetForm() {
  Object.assign(petForm.value, createEmptyPetForm())
  formError.value = ''
  avatarPreview.value = ''
}

function populateForm(pet) {
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

function savePet() {
  formError.value = ''
  const payload = toPayload()
  emit('save', payload)
}

function closeForm() {
  emit('close')
}

watch(() => props.serverError, (val) => {
  if (val) {
    formError.value = val
  }
})

watch([() => props.visible, () => props.pet], () => {
  if (!props.visible) {
    resetForm()
    return
  }
  if (props.pet) {
    populateForm(props.pet)
  } else {
    resetForm()
  }
})
</script>

<style scoped>
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
