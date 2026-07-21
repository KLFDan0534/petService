<template>
  <div class="apply-container">
    <div class="page-header">
      <h1 class="page-title">申请成为寄养员</h1>
      <p class="page-subtitle">选择归属商户，填写寄养服务信息</p>
    </div>

    <div class="apply-card">
      <div v-if="loading" class="loading-state">
        <div class="loading-icon"></div>
        <span>数据加载中...</span>
      </div>

      <form v-else @submit.prevent="submitApplication" class="data-form">
        <div class="form-item">
          <label class="form-label">选择归属商户 <span class="required">*</span></label>
          <select class="form-input" v-model="form.merchant_id_wsh" required>
            <option value="" disabled>请选择商户</option>
            <option v-for="m in merchants" :key="m.id_wsh" :value="m.id_wsh">{{ m.name_wsh }}</option>
          </select>
        </div>

        <div class="form-item is-disabled">
          <label class="form-label">姓名 <span class="required">*</span></label>
          <input class="form-input" v-model="form.name_wsh" placeholder="自动读取实名信息" disabled>
          <span class="form-hint">使用实名认证的姓名</span>
        </div>

        <div class="form-item is-disabled">
          <label class="form-label">手机号 <span class="required">*</span></label>
          <input class="form-input" v-model="form.phone_wsh" placeholder="自动读取绑定手机" disabled>
          <span class="form-hint">使用账户绑定手机号</span>
        </div>

        <div class="form-item">
          <label class="form-label">从业年限</label>
          <input class="form-input" type="number" min="0" v-model.number="form.experience_years_wsh" placeholder="如：3">
        </div>

        <div class="form-item">
          <label class="form-label">每日费用 (元) <span class="required">*</span></label>
          <input class="form-input" type="number" min="0" step="0.01" v-model.number="form.price_per_day_wsh" placeholder="如：150" required>
        </div>

        <div class="form-item">
          <label class="form-label">最大接待宠物数</label>
          <input class="form-input" type="number" min="1" v-model.number="form.max_pets_wsh" placeholder="如：5">
        </div>

        <div class="form-item form-item--wide">
          <label class="form-label">个人简介</label>
          <textarea class="form-textarea" v-model="form.bio_wsh" placeholder="介绍一下您的经验和优势..." rows="4"></textarea>
        </div>

        <div class="form-item form-item--wide">
          <label class="form-label">资质图片（资格证正反面） <span class="required">*</span></label>
          <p class="form-hint">请上传资格证正面和反面，至少 2 张</p>
          <div class="qual-grid">
            <div class="qual-slot">
              <div class="qual-slot-label">正面</div>
              <div class="qual-upload-area">
                <div v-if="qualImages[0]" class="qual-preview">
                  <img :src="qualImages[0]" alt="正面">
                  <button type="button" class="qual-remove" @click="qualImages[0] = ''">×</button>
                </div>
                <label class="qual-upload-btn" :class="{ uploading: uploadingIdx === 0 }">
                  <input type="file" accept="image/*" hidden @change="uploadQualImage(0, $event)">
                  <span>{{ uploadingIdx === 0 ? '上传中...' : (qualImages[0] ? '重选' : '选择图片') }}</span>
                </label>
              </div>
            </div>
            <div class="qual-slot">
              <div class="qual-slot-label">反面</div>
              <div class="qual-upload-area">
                <div v-if="qualImages[1]" class="qual-preview">
                  <img :src="qualImages[1]" alt="反面">
                  <button type="button" class="qual-remove" @click="qualImages[1] = ''">×</button>
                </div>
                <label class="qual-upload-btn" :class="{ uploading: uploadingIdx === 1 }">
                  <input type="file" accept="image/*" hidden @change="uploadQualImage(1, $event)">
                  <span>{{ uploadingIdx === 1 ? '上传中...' : (qualImages[1] ? '重选' : '选择图片') }}</span>
                </label>
              </div>
            </div>
          </div>
        </div>

        <div v-if="error" class="form-error">{{ error }}</div>

        <div class="form-footer">
          <button type="button" class="btn btn-outline" @click="$router.push('/profile')">返回</button>
          <button type="submit" class="submit-button" :disabled="submitting">
            {{ submitting ? '提交中...' : '提交申请' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import request from '@/utils/request'

const router = useRouter()
const appStore = useAppStore()

const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const merchants = ref([])
const qualImages = ref(['', ''])
const uploadingIdx = ref(-1)

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
    const [mr, ur] = await Promise.all([
      request.get('/api/merchants'),
      request.get('/users/me'),
    ])
    if (mr.data.code === 200) {
      merchants.value = mr.data.data || []
    }
    if (ur.data.code === 200 && ur.data.data) {
      const u = ur.data.data
      form.name_wsh = u.real_name_wsh || u.nickname_wsh || u.username_wsh || ''
      form.phone_wsh = u.phone_wsh || ''
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
.apply-container {
  max-width: 640px;
  margin: 0 auto;
}
.page-header {
  margin-bottom: 24px;
}
.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #1f2329;
  margin: 0 0 4px 0;
}
.page-subtitle {
  font-size: 13px;
  color: #8f959e;
  margin: 0;
}
.apply-card {
  background: #ffffff;
  border: 1px solid #dee0e3;
  border-radius: 6px;
  padding: 32px;
}
.data-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-item--wide {
  grid-column: 1 / -1;
}
.form-label {
  font-size: 13px;
  font-weight: 500;
  color: #1f2329;
}
.required {
  color: #e53935;
}
.form-input {
  height: 36px;
  padding: 0 12px;
  border: 1px solid #bbbfc4;
  border-radius: 4px;
  font-size: 14px;
  color: #1f2329;
  background-color: #ffffff;
  transition: border-color 0.2s;
}
.form-input:focus {
  outline: none;
  border-color: #3f51b5;
}
.form-textarea {
  padding: 10px 12px;
  border: 1px solid #bbbfc4;
  border-radius: 4px;
  font-size: 14px;
  color: #1f2329;
  background-color: #ffffff;
  resize: vertical;
  min-height: 80px;
  transition: border-color 0.2s;
}
.form-textarea:focus {
  outline: none;
  border-color: #3f51b5;
}
.form-error {
  color: #e53935;
  font-size: 13px;
  padding: 8px 12px;
  background: #fff1f0;
  border: 1px solid #ffccc7;
  border-radius: 4px;
}
.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
}
.submit-button {
  height: 36px;
  padding: 0 24px;
  background: #3f51b5;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}
.submit-button:hover:not(:disabled) {
  background: #303f9f;
}
.submit-button:disabled {
  background: #cbd0d6;
  color: #8f959e;
  cursor: not-allowed;
}
.btn {
  height: 36px;
  padding: 0 16px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  border: 1px solid #bbbfc4;
  background: #ffffff;
  color: #1f2329;
}
.btn-outline {
  background: #ffffff;
  color: #646a73;
}
.form-hint {
  font-size: 12px;
  color: #8f959e;
  margin: 0;
}
.qual-grid {
  display: flex;
  gap: 16px;
}
.qual-slot {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.qual-slot-label {
  font-size: 12px;
  font-weight: 500;
  color: #646a73;
}
.qual-upload-area {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}
.qual-preview {
  position: relative;
  width: 100%;
  aspect-ratio: 1.4;
  max-width: 200px;
  border: 1px solid #dee0e3;
  border-radius: 6px;
  overflow: hidden;
}
.qual-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.qual-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: none;
  background: rgba(0,0,0,0.5);
  color: #fff;
  font-size: 14px;
  line-height: 22px;
  text-align: center;
  cursor: pointer;
  padding: 0;
}
.qual-upload-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 14px;
  border: 1px solid #bbbfc4;
  border-radius: 4px;
  font-size: 13px;
  color: #1f2329;
  background: #ffffff;
  cursor: pointer;
  transition: border-color 0.2s;
}
.qual-upload-btn:hover {
  border-color: #3f51b5;
  color: #3f51b5;
}
.qual-upload-btn.uploading {
  opacity: 0.6;
  pointer-events: none;
}
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 60px 0;
  color: #646a73;
  font-size: 14px;
}
.loading-icon {
  width: 16px;
  height: 16px;
  border: 2px solid #dee0e3;
  border-left-color: #3f51b5;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
