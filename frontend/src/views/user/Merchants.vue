<template>
  <div>
    <PageHero title="商家列表" subtitle="浏览宠物服务商家" />

    <div class="toolbar">
      <div>
        <label>半径(km)</label>
        <input v-model="radius" class="input radius-input" placeholder="半径" />
      </div>
      <button class="btn btn-primary" :disabled="locating" @click="nearbySearch">{{ locating ? '定位中...' : '附近搜索' }}</button>
      <button class="btn btn-outline" @click="resetSearch">重置</button>
      <button class="btn btn-primary btn-sm" @click="openRegister">注册商家</button>
      <button class="btn btn-outline btn-sm" @click="openMyMerchant">我的商家</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="pet-grid">
      <div v-for="m in merchants" :key="m.id_wsh" class="pet-card" @click="viewMerchant(m.id_wsh)">
        <div class="merchant-badges">
          <span :class="['badge', getStatusBadge(MerchantStoreStatus, m.store_status_wsh)]">
            {{ getStatusLabel(MerchantStoreStatus, m.store_status_wsh) }}
          </span>
        </div>
        <div class="pet-avatar"><span>店</span></div>
        <div class="pet-name">{{ m.name_wsh || m.username_wsh }}</div>
        <div class="pet-info">{{ m.description_wsh || '暂无介绍' }}</div>
        <div class="pet-info">评分 {{ m.rating_wsh || '暂无' }}</div>
        <div class="qualification-list">
          <span v-for="q in m.qualifications_wsh || []" :key="q.id_wsh" class="badge badge-info">
            {{ q.status_wsh === 'approved' ? '已认证' : '资质待审' }}
          </span>
        </div>
      </div>
    </div>

    <div v-if="showMerchantForm" class="modal-overlay" @mousedown.self="showMerchantForm = false">
      <div class="modal detail-modal">
        <h2>{{ editingMerchantId ? '编辑商家' : '注册商家' }}</h2>
        <form @submit.prevent="saveMerchant">
          <div class="form-group"><label>商家名称</label><input v-model="merchantForm.name_wsh" required></div>
          <div class="form-group"><label>描述</label><textarea v-model="merchantForm.description_wsh" rows="3"></textarea></div>
          <div class="form-group"><label>联系电话</label><input v-model="merchantForm.phone_wsh"></div>
          <div class="form-group">
            <label>资质照片</label>
            <div class="upload-row">
              <input ref="merchantQualificationInput" type="file" accept="image/*" class="hidden-input" @change="uploadMerchantQualification">
              <button type="button" class="btn btn-outline btn-sm" @click="merchantQualificationInput?.click()">{{ qualificationUploading ? '上传中...' : '上传资质' }}</button>
              <span>{{ merchantForm.qualification_image_wsh ? '已上传资质' : '未上传资质' }}</span>
            </div>
          </div>
          <div class="form-group">
            <label>地址</label>
            <AmapAddressPicker
              v-model="merchantForm.address_wsh"
              v-model:latitude="merchantForm.latitude_wsh"
              v-model:longitude="merchantForm.longitude_wsh"
              placeholder="搜索地址或点选位置"
            />
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" @click="showMerchantForm = false">取消</button>
            <button type="submit" class="btn btn-primary btn-sm">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { getMerchants, getMyMerchant, getNearbyMerchants, createMerchant, updateMerchant } from '@/api/merchant'
import { uploadFileToDirectory } from '@/api/file'
import PageHero from '@/components/common/PageHero.vue'
import AmapAddressPicker from '@/components/common/AmapAddressPicker.vue'
import { getCurrentAddress } from '@/composables/useAmapLocation'
import { ensureProfileRequirement, PROFILE_ACTIONS } from '@/utils/profileRequirements'
import { MerchantStoreStatus, getStatusBadge, getStatusLabel } from '@/constants/statusMaps'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const merchants = ref([])
const loading = ref(true)
const locating = ref(false)
const qualificationUploading = ref(false)
const radius = ref('')
const showMerchantForm = ref(false)
const editingMerchantId = ref(null)
const merchantQualificationInput = ref(null)

const merchantForm = reactive({
  name_wsh: '',
  description_wsh: '',
  phone_wsh: '',
  address_wsh: '',
  latitude_wsh: null,
  longitude_wsh: null,
  qualification_image_wsh: '',
  business_license_wsh: '',
})

async function loadMerchants() {
  loading.value = true
  try {
    const r = await getMerchants()
    if (r.code === 200) merchants.value = r.data || []
  } finally {
    loading.value = false
  }
}

async function nearbySearch() {
  locating.value = true
  loading.value = true
  try {
    const location = await getCurrentAddress()
    const r = await getNearbyMerchants({ lat: location.latitude_wsh, lng: location.longitude_wsh, radius: radius.value || undefined })
    if (r.code === 200) merchants.value = r.data || []
  } catch (e) {
    appStore.addToast(e.message || '定位失败，无法搜索附近商家', 'warning')
  } finally {
    locating.value = false
    loading.value = false
  }
}

function resetSearch() {
  radius.value = ''
  loadMerchants()
}

async function openRegister() {
  const ok = await ensureProfileRequirement(PROFILE_ACTIONS.APPLY_MERCHANT, { authStore, appStore, router })
  if (!ok) return
  editingMerchantId.value = null
  resetMerchantForm()
  showMerchantForm.value = true
}

async function openMyMerchant() {
  const r = await getMyMerchant()
  if (r.code === 200 && r.data) router.push(`/merchants/${r.data.id_wsh}`)
  else appStore.addToast('您还没有注册商家', 'info')
}

function viewMerchant(id) {
  router.push(`/merchants/${id}`)
}

function resetMerchantForm() {
  merchantForm.name_wsh = ''
  merchantForm.description_wsh = ''
  merchantForm.phone_wsh = ''
  merchantForm.address_wsh = ''
  merchantForm.latitude_wsh = null
  merchantForm.longitude_wsh = null
  merchantForm.qualification_image_wsh = ''
  merchantForm.business_license_wsh = ''
}

async function uploadMerchantQualification(event) {
  const file = event.target.files?.[0]
  if (!file) return
  qualificationUploading.value = true
  try {
    const data = new FormData()
    data.append('file', file)
    const r = await uploadFileToDirectory('qualifications/merchants', data)
    if (r.code === 200 && r.data?.url_wsh) {
      merchantForm.qualification_image_wsh = r.data.url_wsh
      merchantForm.business_license_wsh = r.data.url_wsh
      appStore.addToast('资质上传成功', 'success')
    }
  } finally {
    qualificationUploading.value = false
    event.target.value = ''
  }
}

async function saveMerchant() {
  if (!editingMerchantId.value) {
    const ok = await ensureProfileRequirement(PROFILE_ACTIONS.APPLY_MERCHANT, { authStore, appStore, router })
    if (!ok) return
  }
  if (!merchantForm.address_wsh || merchantForm.latitude_wsh == null || merchantForm.longitude_wsh == null) {
    appStore.addToast('请选择商家地址', 'warning')
    return
  }
  const r = editingMerchantId.value
    ? await updateMerchant(editingMerchantId.value, merchantForm)
    : await createMerchant(merchantForm)
  if (r.code === 200) {
    appStore.addToast(editingMerchantId.value ? '更新成功' : '注册成功', 'success')
    showMerchantForm.value = false
    loadMerchants()
  }
}

onMounted(loadMerchants)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; align-items: flex-end; flex-wrap: wrap; margin-bottom: 20px; }
.radius-input { width: 100px; }
.pet-card { cursor: pointer; }
.merchant-badges { display: flex; justify-content: flex-end; margin-bottom: 8px; }
.qualification-list { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.qualification-badge { margin-left: 6px; }
.detail-modal { max-width: 640px; }
.detail-grid { display: grid; gap: 10px; padding: 16px; border: 1px solid var(--color-border); border-radius: 6px; }
.upload-row { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; color: var(--color-muted-foreground); font-size: 13px; }
.hidden-input { display: none; }
</style>
