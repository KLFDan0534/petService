<template>
  <AppDialog :visible="visible" :width="640" @close="closeForm">
    <template #header>
      <div class="modal-header">
        <div>
          <h2>{{ pet ? '编辑宠物' : '添加宠物' }}</h2>
          <p v-if="pet" class="modal-desc">更新「{{ pet.name_wsh }}」的档案信息</p>
          <p v-else class="modal-desc">填写基础信息，之后可随时补充</p>
        </div>
      </div>
    </template>

    <form @submit.prevent="savePet" class="pet-form">
        <div v-if="formError" class="form-error" role="alert">
          {{ formError }}
        </div>

        <!-- Name + Type -->
        <div class="form-row">
          <div class="form-group">
            <label class="form-label" for="pf-name">宠物名称 <span class="required">*</span></label>
            <input id="pf-name" v-model.trim="petForm.name_wsh" placeholder="请输入宠物名称" required class="form-input">
          </div>
          <div class="form-group">
            <label class="form-label" for="pf-type">类型</label>
            <select id="pf-type" v-model="petForm.type_wsh" class="form-input">
              <option v-for="option in petTypeOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </div>
        </div>

        <!-- Breed + Gender -->
        <div class="form-row">
          <div class="form-group">
            <label class="form-label" for="pf-breed">品种</label>
            <input id="pf-breed" v-model.trim="petForm.breed_wsh" placeholder="如：金毛、英短" class="form-input">
          </div>
          <div class="form-group">
            <label class="form-label" for="pf-gender">性别</label>
            <select id="pf-gender" v-model.number="petForm.gender_wsh" class="form-input">
              <option :value="0">未知</option>
              <option :value="1">公</option>
              <option :value="2">母</option>
            </select>
          </div>
        </div>

        <!-- Age + Weight -->
        <div class="form-row">
          <div class="form-group">
            <label class="form-label" for="pf-age">年龄（月）</label>
            <input id="pf-age" v-model.number="petForm.age_wsh" type="number" min="1" placeholder="如：12" class="form-input">
          </div>
          <div class="form-group">
            <label class="form-label" for="pf-weight">体重（kg）</label>
            <input id="pf-weight" v-model.number="petForm.weight_wsh" type="number" min="0.1" step="0.1" placeholder="如：5.5" class="form-input">
          </div>
        </div>

        <!-- Avatar Upload -->
        <div class="form-group">
          <label class="form-label">头像</label>
          <div class="avatar-upload-section">
            <div class="avatar-preview-wrap">
              <img v-if="avatarPreview" class="avatar-preview-img" :src="avatarPreview" alt="头像预览">
              <div v-else class="avatar-preview-placeholder">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M11.5 9C11.5 9 9 7 7 9s-2 5 2 7 7-2 7-2-1.5-2-4.5-4z"/><circle cx="6.5" cy="6.5" r="1.5"/><circle cx="17.5" cy="6.5" r="1.5"/><circle cx="11.5" cy="5.5" r="1.5"/></svg>
              </div>
              <span v-if="avatarUploading" class="avatar-loading">
                <svg class="icon-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg>
              </span>
            </div>
            <div class="avatar-actions">
              <label class="btn btn-outline btn-sm avatar-upload-btn">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="icon-sm"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12"/></svg>
                选择照片
                <input type="file" accept="image/*" hidden @change="onAvatarChange">
              </label>
              <button v-if="avatarPreview && !avatarUploading" type="button" class="btn btn-quiet btn-sm" @click="removeAvatar">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="icon-sm"><path d="M18 6L6 18M6 6l12 12"/></svg>
                移除
              </button>
            </div>
            <p class="avatar-hint">建议正方形图片，JPG / PNG，5MB 以内</p>
          </div>
        </div>

        <!-- Check Cards -->
        <div class="form-row">
          <label class="check-card" :class="{ checked: petForm.vaccinated_wsh === 1 }">
            <input type="checkbox" v-model="petForm.vaccinated_wsh" :true-value="1" :false-value="0" class="sr-only">
            <span class="check-indicator">
              <svg v-if="petForm.vaccinated_wsh === 1" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
            </span>
            已完成疫苗 / 免疫
          </label>
          <label class="check-card" :class="{ checked: petForm.sterilized_wsh === 1 }">
            <input type="checkbox" v-model="petForm.sterilized_wsh" :true-value="1" :false-value="0" class="sr-only">
            <span class="check-indicator">
              <svg v-if="petForm.sterilized_wsh === 1" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
            </span>
            已绝育
          </label>
        </div>

        <!-- Description -->
        <div class="form-group">
          <label class="form-label" for="pf-desc">性格与照护说明</label>
          <textarea id="pf-desc" v-model.trim="petForm.description_wsh" rows="3" placeholder="如：胆小、亲人、需要慢慢适应新环境" class="form-textarea"></textarea>
        </div>

        <!-- Allergies + Habits -->
        <div class="form-row">
          <div class="form-group">
            <label class="form-label" for="pf-allergies">过敏 / 禁忌</label>
            <textarea id="pf-allergies" v-model.trim="petForm.allergies_wsh" rows="3" placeholder="如：鸡肉过敏、不能吃牛奶" class="form-textarea"></textarea>
          </div>
          <div class="form-group">
            <label class="form-label" for="pf-habits">生活习惯</label>
            <textarea id="pf-habits" v-model.trim="petForm.habits_wsh" rows="3" placeholder="如：每天早晚各遛一次" class="form-textarea"></textarea>
          </div>
        </div>
      </form>

    <template #footer>
      <button type="button" class="btn btn-outline btn-sm" @click="closeForm" :disabled="saving">取消</button>
      <button type="submit" form="pf-form" class="btn btn-primary btn-sm" :disabled="saving" @click="savePet">
        {{ saving ? '保存中...' : pet ? '保存' : '添加' }}
      </button>
    </template>
  </AppDialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import AppDialog from '@/components/common/AppDialog.vue'
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
  { value: 'dog', label: '狗' }, { value: 'cat', label: '猫' },
  { value: 'rabbit', label: '兔子' }, { value: 'bird', label: '鸟' },
  { value: 'fish', label: '鱼' }, { value: 'hamster', label: '仓鼠' },
  { value: 'other', label: '其他' },
]

function createEmptyPetForm() {
  return {
    name_wsh: '', type_wsh: 'dog', breed_wsh: '', age_wsh: null,
    weight_wsh: null, gender_wsh: 0, sterilized_wsh: 0, vaccinated_wsh: 0,
    avatar_wsh: '', description_wsh: '', allergies_wsh: '', habits_wsh: '',
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
  emit('save', toPayload())
}

function closeForm() {
  emit('close')
}

watch(() => props.serverError, (val) => {
  if (val) formError.value = val
})

watch([() => props.visible, () => props.pet], () => {
  if (!props.visible) { resetForm(); return }
  props.pet ? populateForm(props.pet) : resetForm()
})
</script>

<style scoped>
.pet-modal { max-width: 640px; }

.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}
.modal-header h2 { font-size: 18px; margin-bottom: 0; }
.modal-desc { margin-top: 4px; font-size: 14px; color: var(--color-muted-foreground); }
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
  flex-shrink: 0;
}
.modal-close:hover { background: var(--color-muted); color: var(--color-foreground); }
.modal-close svg { width: 16px; height: 16px; }

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}

.pet-form { display: flex; flex-direction: column; gap: 20px; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-group { display: flex; flex-direction: column; }
.form-label {
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-foreground);
}
.required { color: var(--color-primary); }
.form-input {
  height: 40px;
  padding: 0 12px;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  background: var(--color-card);
  font-size: 14px;
  color: var(--color-foreground);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.form-input:hover { border-color: var(--color-border); }
.form-input:focus {
  border-color: var(--color-primary);
  outline: none;
  box-shadow: 0 0 0 3px rgba(249, 115, 22, 0.1);
}
.form-textarea {
  resize: none;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  background: var(--color-card);
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-foreground);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.form-textarea:hover { border-color: var(--color-border); }
.form-textarea:focus {
  border-color: var(--color-primary);
  outline: none;
  box-shadow: 0 0 0 3px rgba(249, 115, 22, 0.1);
}

.form-error {
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid rgba(220, 38, 38, 0.15);
  background: rgba(220, 38, 38, 0.06);
  font-size: 14px;
  color: var(--color-danger);
}

/* Avatar Upload */
.avatar-upload-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-muted);
  flex-wrap: wrap;
}
.avatar-preview-wrap {
  position: relative;
  width: 64px;
  height: 64px;
  flex-shrink: 0;
}
.avatar-preview-img {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--color-border);
}
.avatar-preview-placeholder {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: 2px dashed var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-muted-foreground);
}
.avatar-preview-placeholder svg { width: 24px; height: 24px; }
.avatar-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.75);
}
.icon-spin {
  width: 16px;
  height: 16px;
  animation: spin 1s linear infinite;
  color: var(--color-muted-foreground);
}
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.avatar-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.avatar-hint {
  width: 100%;
  font-size: 12px;
  color: var(--color-muted-foreground);
  margin-top: 4px;
}
.icon-sm { width: 14px; height: 14px; }

/* Check Cards */
.check-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  background: var(--color-card);
  font-size: 14px;
  font-weight: 500;
  color: var(--color-muted-foreground);
  cursor: pointer;
  transition: all 0.15s ease;
}
.check-card:hover { border-color: var(--color-border); }
.check-card.checked {
  border-color: var(--color-primary);
  background: rgba(249, 115, 22, 0.06);
  color: var(--color-foreground);
}
.check-indicator {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  border: 2px solid var(--color-border);
  background: var(--color-card);
  flex-shrink: 0;
  transition: all 0.15s ease;
}
.check-card.checked .check-indicator {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}
.check-indicator svg { width: 12px; height: 12px; }

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}

.btn-quiet {
  background: transparent;
  border: none;
  color: var(--color-muted-foreground);
  cursor: pointer;
}
.btn-quiet:hover {
  background: rgba(220, 38, 38, 0.06);
  color: var(--color-danger);
}

@media (max-width: 520px) {
  .form-row { grid-template-columns: 1fr; }
}
</style>
