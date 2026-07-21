<template>
  <section class="card section-card">
    <h3>看护资质</h3>

    <div v-if="keeperInfo" class="profile-summary">
      <strong>{{ keeperInfo.name_wsh }}</strong>
      <span>从业 {{ keeperInfo.experience_years_wsh || 0 }} 年 · ¥{{ keeperInfo.price_per_day_wsh || 0 }}/天 · 容量 {{ keeperInfo.current_pets_wsh || 0 }}/{{ keeperInfo.max_pets_wsh || '-' }}</span>
      <div class="qualification-list">
        <span v-if="!(keeperInfo.qualifications_wsh || []).length" class="hint">暂无资质记录</span>
        <span v-for="q in keeperInfo.qualifications_wsh || []" :key="q.id_wsh" class="badge badge-info">
          {{ q.title_wsh || '资质证明' }} · {{ q.status_wsh === 'approved' ? '已认证' : '待审核' }}
        </span>
      </div>
    </div>

    <div v-else class="form-grid">
      <label>
        所属商家
        <select v-model="selectedMerchantId" class="form-control">
          <option value="">请选择商家</option>
          <option v-for="merchant in merchants" :key="merchant.id_wsh" :value="merchant.id_wsh">{{ merchant.name_wsh }}</option>
        </select>
      </label>
      <label>姓名<input v-model="certForm.name_wsh" class="form-control"></label>
      <label>手机<input v-model="certForm.phone_wsh" class="form-control"></label>
      <label>从业年限<input v-model.number="certForm.experience_years_wsh" type="number" class="form-control"></label>
      <label>日收费<input v-model.number="certForm.price_per_day_wsh" type="number" class="form-control"></label>
      <label>最大接单数<input v-model.number="certForm.max_pets_wsh" type="number" class="form-control"></label>
      <label class="form-grid__wide">
        资质照片
        <input type="file" accept="image/*" class="form-control" @change="uploadQualification">
        <span class="hint">{{ certForm.qualification_image_wsh ? '已上传资质照片' : '请上传培训证书、从业证明或健康证明照片' }}</span>
      </label>
      <label class="form-grid__wide">介绍<textarea v-model="certForm.bio_wsh" class="form-control" rows="3"></textarea></label>
      <button class="btn btn-primary" :disabled="!selectedMerchantId" @click="submit">提交认证</button>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { uploadFileToDirectory } from '@/api/file'

const props = defineProps({
  merchants: { type: Array, default: () => [] },
  keeperInfo: { type: Object, default: null },
})
const emit = defineEmits(['submit'])

const selectedMerchantId = ref('')
const certForm = reactive({
  name_wsh: '',
  phone_wsh: '',
  avatar_wsh: '',
  experience_years_wsh: 3,
  price_per_day_wsh: 100,
  max_pets_wsh: 3,
  bio_wsh: '',
  qualification_image_wsh: '',
})

async function uploadQualification(event) {
  const file = event.target.files?.[0]
  if (!file) return
  const data = new FormData()
  data.append('file', file)
  const res = await uploadFileToDirectory('qualifications/keepers', data)
  if (res.code === 200 && res.data?.url_wsh) {
    certForm.qualification_image_wsh = res.data.url_wsh
  }
  event.target.value = ''
}

function submit() {
  if (!selectedMerchantId.value) return
  emit('submit', { ...certForm, merchant_id_wsh: Number(selectedMerchantId.value) })
}
</script>

<style scoped>
.section-card { padding: 22px; display: grid; gap: 14px; }
.profile-summary { display: grid; gap: 6px; color: var(--color-muted-foreground); }
.profile-summary strong { color: var(--color-foreground); }
.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; }
.form-grid label { display: grid; gap: 6px; font-size: 13px; color: var(--color-muted-foreground); }
.form-grid__wide { grid-column: 1 / -1; }
.qualification-list { display: flex; flex-wrap: wrap; gap: 6px; }
.hint { font-size: 12px; color: var(--color-muted-foreground); }
</style>
