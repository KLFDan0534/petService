<template>
  <div class="profile-container">
    <div class="page-header">
      <h1 class="page-title">Personal Profile</h1>
      <p class="page-subtitle">Manage your profile and account settings</p>
    </div>

    <div class="profile-card">
      <div v-if="loading" class="loading-state">
        <div class="loading-icon"></div>
        <span>Loading...</span>
      </div>

      <template v-else>
        <div class="user-profile-section">
          <div class="avatar-wrapper">
            <img v-if="avatarUrl" :src="avatarUrl" alt="avatar">
            <span v-else>{{ avatarInitial }}</span>
          </div>
          <div class="user-info-text">
            <h2 class="user-nickname">{{ profile.nickname_wsh || authStore.user?.nickname_wsh || authStore.user?.username_wsh }}</h2>
            <div class="role-tags-group">
              <span v-for="role in (authStore.user?.roles_wsh || [])" :key="role" class="sys-tag">{{ role }}</span>
              <span :class="['real-status', realNameStatusClass]">{{ realNameStatusText }}</span>
            </div>
          </div>
        </div>

        <StatisticsPanel :stats="stats" />
        <section v-if="!isSupportOnly" class="benefits-section" aria-labelledby="benefits-title">
          <h2 id="benefits-title" class="section-label">我的权益</h2>
          <nav class="benefit-links" aria-label="个人权益">
            <router-link to="/coupons" class="benefit-entry">
              <span class="benefit-icon" aria-hidden="true">
                <el-icon><Ticket /></el-icon>
              </span>
              <span class="benefit-info">
                <span class="benefit-title">我的优惠券</span>
                <span class="benefit-desc">查看可领取和已拥有的优惠券</span>
              </span>
              <el-icon class="benefit-arrow" aria-hidden="true"><ArrowRight /></el-icon>
            </router-link>
            <router-link to="/membership" class="benefit-entry">
              <span class="benefit-icon" aria-hidden="true">
                <el-icon><Medal /></el-icon>
              </span>
              <span class="benefit-info">
                <span class="benefit-title">会员中心</span>
                <span class="benefit-desc">查看会员套餐、权益和订单</span>
              </span>
              <el-icon class="benefit-arrow" aria-hidden="true"><ArrowRight /></el-icon>
            </router-link>
          </nav>
        </section>
        <KeeperPanel :keeper-status="keeperStatus" :loading="keeperLoading" @apply="applyKeeper" @go-workflow="goToKeeperWorkflow" />
        <MerchantPanel :merchant-status="merchantStatus" @apply="applyMerchant" @go-dashboard="goToDashboard" />
        <section v-if="!isSupportOnly" class="customer-service-section">
          <div class="section-label">商家客服</div>
          <button class="service-entry" type="button" @click="goCustomerServiceApply">
            <div class="service-icon">CS</div>
            <div class="service-info">
              <div class="service-title">{{ customerServiceTitle }}</div>
              <div class="service-desc">{{ customerServiceDesc }}</div>
            </div>
            <span v-if="latestCustomerServiceApplication" :class="['badge', customerServiceBadgeClass]">
              {{ customerServiceStatusText }}
            </span>
            <span v-else class="service-arrow">→</span>
          </button>
        </section>

        <form @submit.prevent="saveProfile" class="data-form">
          <div class="form-grid">
            <AvatarUpload :avatar-url="avatarUrl" :avatar-initial="avatarInitial" :uploading="avatarUploading" @upload="uploadAvatar" />

            <div class="form-item is-disabled">
              <label class="form-label">Username</label>
              <input class="form-input" v-model="profile.username_wsh" disabled title="Username is managed by the system">
            </div>
            <div class="form-item">
              <label class="form-label">Nickname</label>
              <input class="form-input" v-model="profile.nickname_wsh" placeholder="Enter nickname" required>
            </div>
            <div class="form-item">
              <label class="form-label">Gender</label>
              <select class="form-input" v-model.number="profile.gender_wsh">
                <option :value="0">Unknown</option>
                <option :value="1">Male</option>
                <option :value="2">Female</option>
              </select>
            </div>
            <div class="form-item account-readonly">
              <label class="form-label">Phone</label>
              <div class="readonly-field">
                <span>{{ profile.phone_wsh || 'Not bound' }}</span>
                <button type="button" class="account-action" @click="goAccountEdit('phone')">Change</button>
              </div>
            </div>
            <div class="form-item account-readonly">
              <label class="form-label">Email</label>
              <div class="readonly-field">
                <span>{{ profile.email_wsh || 'Not bound' }}</span>
                <button type="button" class="account-action" @click="goAccountEdit('email')">Change</button>
              </div>
            </div>
            <div class="form-item account-readonly form-item--wide">
              <label class="form-label">Real Name Verification</label>
              <div class="readonly-field">
                <span>{{ realNameDisplay }}</span>
                <button type="button" class="account-action" @click="goAccountEdit('real-name')">Verify</button>
              </div>
            </div>
            <div class="form-item account-readonly form-item--wide">
              <label class="form-label">Payment Password</label>
              <div class="readonly-field">
                <span>{{ profile.payment_password_set_wsh ? 'Set' : 'Not set' }}</span>
                <button type="button" class="account-action" @click="goAccountEdit('payment-password')">
                  {{ profile.payment_password_set_wsh ? 'Change' : 'Set' }}
                </button>
              </div>
            </div>
          </div>

          <div class="form-footer">
            <button type="button" class="danger-button" :disabled="submitting || deleting" @click="handleDeleteAccount">
              {{ deleting ? 'Deleting...' : 'Delete Account' }}
            </button>
            <button type="submit" class="submit-button" :disabled="submitting || deleting">
              {{ submitting ? 'Saving...' : 'Save Changes' }}
            </button>
          </div>
        </form>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Medal, Ticket } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { deleteCurrentUser } from '@/api/auth'
import { getMyCustomerServiceApplications } from '@/api/merchantCustomerService'
import request from '@/utils/request'
import AvatarUpload from '@/components/profile/AvatarUpload.vue'
import MerchantPanel from '@/components/profile/MerchantPanel.vue'
import StatisticsPanel from '@/components/profile/StatisticsPanel.vue'
import KeeperPanel from '@/components/profile/KeeperPanel.vue'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const loading = ref(true)
const submitting = ref(false)
const deleting = ref(false)
const avatarUploading = ref(false)
const profile = reactive({
  username_wsh: '',
  nickname_wsh: '',
  phone_wsh: '',
  avatar_wsh: '',
  gender_wsh: 0,
  email_wsh: '',
  real_name_wsh: '',
  id_card_no_wsh: '',
  real_name_status_wsh: 0,
  payment_password_set_wsh: false,
})
const stats = ref({ pets: 0, activeOrders: 0, completedOrders: 0, totalSpent: 0 })
const merchantStatus = ref(null)
const keeperStatus = ref(null)
const keeperLoading = ref(true)
const customerServiceApplications = ref([])

const avatarUrl = computed(() => profile.avatar_wsh || authStore.user?.avatar_wsh || '')
const avatarInitial = computed(() => (profile.nickname_wsh || authStore.user?.nickname_wsh || authStore.user?.username_wsh || '?')[0].toUpperCase())
const realNameStatusText = computed(() => {
  const status = Number(profile.real_name_status_wsh || 0)
  if (status === 2) return 'Verified'
  if (status === 1) return 'Pending'
  if (status === 3) return 'Rejected'
  return 'Unverified'
})
const realNameStatusClass = computed(() => {
  const status = Number(profile.real_name_status_wsh || 0)
  if (status === 2) return 'real-status--verified'
  if (status === 1) return 'real-status--pending'
  if (status === 3) return 'real-status--rejected'
  return 'real-status--empty'
})
const realNameDisplay = computed(() => {
  if (profile.real_name_wsh) return profile.real_name_wsh + ' - ' + realNameStatusText.value
  return realNameStatusText.value
})
const latestCustomerServiceApplication = computed(() => customerServiceApplications.value[0] || null)
const isSupportOnly = computed(() =>
  authStore.isCs
  && !authStore.isAdmin
  && !authStore.isMerchant
  && !authStore.isOwner
  && !authStore.hasRole('KEEPER')
)
const customerServiceStatusText = computed(() => customerServiceStatusLabel(latestCustomerServiceApplication.value?.status_wsh))
const customerServiceBadgeClass = computed(() => ({
  pending: 'badge-warning',
  approved: 'badge-success',
  rejected: 'badge-danger',
  resigned: 'badge-muted',
  terminated: 'badge-muted',
}[latestCustomerServiceApplication.value?.status_wsh] || 'badge-info'))
const customerServiceTitle = computed(() => {
  const latest = latestCustomerServiceApplication.value
  if (!latest) return '申请成为商家客服'
  return latest.status_wsh === 'approved' ? '商家客服已开通' : '客服申请'
})
const customerServiceDesc = computed(() => {
  const latest = latestCustomerServiceApplication.value
  if (!latest) return '选择要服务的商家，提交后由商家负责人审核'
  const merchantName = latest.merchant_name_wsh || `商家 #${latest.merchant_id_wsh}`
  return `${merchantName} · ${customerServiceStatusText.value}`
})

async function loadMerchantStatus() {
  try {
    const r = await request.get('/merchants/my')
    if (r.data.code === 200 && r.data.data) {
      merchantStatus.value = r.data.data.status_wsh
    }
  } catch (e) {}
}

async function loadKeeperStatus() {
  keeperLoading.value = true
  try {
    const r = await request.get('/api/keepers/my-application')
    if (r.data.code === 200 && r.data.data) {
      keeperStatus.value = r.data.data.status_wsh
    } else {
      keeperStatus.value = null
    }
  } catch (e) {
    keeperStatus.value = null
  } finally {
    keeperLoading.value = false
  }
}

async function loadCustomerServiceApplications() {
  try {
    const response = await getMyCustomerServiceApplications()
    if (response.code === 200) {
      customerServiceApplications.value = response.data || []
    }
  } catch (e) {
    customerServiceApplications.value = []
  }
}

function applyMerchant() {
  router.push('/merchants')
}

function goToDashboard() {
  router.push('/merchant/dashboard')
}

function applyKeeper() {
  router.push('/keeper-apply')
}

function goCustomerServiceApply() {
  router.push('/customer-service/apply')
}

async function goToKeeperWorkflow() {
  try {
    const r = await request.get('/users/me')
    if (r.data.code === 200) syncUser(r.data.data)
  } catch (e) {}
  router.push('/keeper-workflow')
}

function goAccountEdit(type) {
  router.push('/profile/' + type)
}

function syncUser(userData) {
  if (!userData) return
  Object.assign(profile, userData)
  if (authStore.user) {
    authStore.user = { ...authStore.user, ...userData }
    localStorage.setItem('user', JSON.stringify(authStore.user))
  }
}

onMounted(async () => {
  try {
    const result = await Promise.all([
      request.get('/users/me'),
      request.get('/statistics/user'),
    ])
    const ur = result[0]
    const sr = result[1]
    if (ur.data.code === 200) syncUser(ur.data.data)
    if (sr.data.code === 200) stats.value = sr.data.data
  } catch (e) {
    appStore.addToast('Failed to load profile', 'error')
  } finally {
    loading.value = false
  }
  loadMerchantStatus()
  loadKeeperStatus()
  loadCustomerServiceApplications()
})

function customerServiceStatusLabel(status) {
  return {
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝',
    resigned: '已退出',
    terminated: '已终止',
  }[status] || '未申请'
}

async function uploadAvatar(file) {
  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const r = await request.post('/files/upload?directory=avatars', formData)
    if (r.data.code === 200 && r.data.data?.url_wsh) {
      profile.avatar_wsh = r.data.data.url_wsh
      const updateRes = await request.put('/users/me', { avatar_wsh: profile.avatar_wsh })
      if (updateRes.data.code === 200) syncUser(updateRes.data.data || { avatar_wsh: profile.avatar_wsh })
      appStore.addToast('Avatar uploaded', 'success')
    }
  } catch (error) {
    appStore.addToast('Avatar upload failed', 'error')
  } finally {
    avatarUploading.value = false
  }
}

async function saveProfile() {
  if (submitting.value) return
  submitting.value = true
  try {
    const payload = {
      nickname_wsh: profile.nickname_wsh,
      avatar_wsh: profile.avatar_wsh,
      gender_wsh: profile.gender_wsh,
    }
    const r = await request.put('/users/me', payload)
    if (r.data.code === 200) {
      appStore.addToast('Saved successfully', 'success')
      syncUser(r.data.data || profile)
    }
  } catch (e) {
    appStore.addToast('Save failed', 'error')
  } finally {
    submitting.value = false
  }
}

async function handleDeleteAccount() {
  if (submitting.value || deleting.value) return
  if (!window.confirm('Are you sure you want to delete this account? This cannot be undone.')) return

  deleting.value = true
  try {
    const r = await deleteCurrentUser()
    if (r.code === 200) {
      appStore.addToast('Account deleted', 'success')
      authStore.clearAuth()
      router.replace('/login')
    }
  } catch (e) {
    appStore.addToast(e.response?.data?.message || 'Delete failed', 'error')
  } finally {
    deleting.value = false
  }
}
</script>

<style scoped>
.profile-container {
  max-width: 800px;
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
.profile-card {
  background: #ffffff;
  border: 1px solid #dee0e3;
  border-radius: 6px;
  padding: 32px;
}
.user-profile-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 24px;
  border-bottom: 1px solid #dee0e3;
}
.avatar-wrapper {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  background: #3f51b5;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 600;
  overflow: hidden;
}
.avatar-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.user-nickname {
  font-size: 18px;
  font-weight: 600;
  color: #1f2329;
  margin: 0 0 6px 0;
}
.role-tags-group {
  display: flex;
  gap: 6px;
}
.sys-tag {
  font-size: 12px;
  padding: 1px 6px;
  background: #f5f6f7;
  border: 1px solid #dee0e3;
  color: #646a73;
  border-radius: 2px;
}
.real-status {
  font-size: 12px;
  padding: 1px 6px;
  border-radius: 2px;
  border: 1px solid #dee0e3;
  color: #646a73;
  background: #ffffff;
}
.real-status--verified {
  color: #237804;
  border-color: #b7eb8f;
  background: #f6ffed;
}
.real-status--pending {
  color: #ad6800;
  border-color: #ffe58f;
  background: #fffbe6;
}
.real-status--rejected {
  color: #a8071a;
  border-color: #ffccc7;
  background: #fff1f0;
}
.benefits-section {
  margin: 24px 0;
  padding: 20px 0;
  border-top: 1px solid #dee0e3;
}
.benefit-links {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  border-top: 1px solid #dee0e3;
  border-bottom: 1px solid #dee0e3;
}
.benefit-entry {
  min-height: 72px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  color: #1f2329;
  text-decoration: none;
  transition: background-color 0.2s;
}
.benefit-entry + .benefit-entry {
  border-left: 1px solid #dee0e3;
}
.benefit-entry:hover {
  background: #f5f6f7;
}
.benefit-entry:focus-visible {
  position: relative;
  outline: 3px solid #3f51b5;
  outline-offset: 2px;
  border-radius: 4px;
}
.benefit-icon {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  background: #eef2ff;
  color: #3f51b5;
  font-size: 20px;
}
.benefit-info {
  min-width: 0;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
}
.benefit-title {
  color: #1f2329;
  font-size: 14px;
  font-weight: 600;
}
.benefit-desc {
  color: #646a73;
  font-size: 12px;
  line-height: 1.5;
}
.benefit-arrow {
  flex: 0 0 auto;
  color: #8f959e;
  font-size: 16px;
}
.customer-service-section {
  margin: 24px 0;
  padding: 20px 0;
  border-top: 1px solid #dee0e3;
}
.section-label {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
  margin: 0 0 12px;
}
.service-entry {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid #dee0e3;
  border-radius: 4px;
  background: #ffffff;
  cursor: pointer;
  text-align: left;
  transition: background 0.2s, border-color 0.2s;
}
.service-entry:hover {
  background: #f5f6f7;
  border-color: #c9cdd4;
}
.service-icon {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  background: #eef2ff;
  color: #3f51b5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}
.service-info {
  flex: 1;
  min-width: 0;
}
.service-title {
  font-size: 14px;
  font-weight: 500;
  color: #1f2329;
}
.service-desc {
  font-size: 12px;
  color: #8f959e;
  margin-top: 2px;
}
.service-arrow {
  color: #8f959e;
  font-size: 16px;
}
.badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 2px;
  font-weight: 500;
  white-space: nowrap;
}
.badge-warning {
  color: #ad6800;
  background: #fffbe6;
  border: 1px solid #ffe58f;
}
.badge-success {
  color: #237804;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
}
.badge-danger {
  color: #a8071a;
  background: #fff1f0;
  border: 1px solid #ffccc7;
}
.badge-muted,
.badge-info {
  color: #646a73;
  background: #f5f6f7;
  border: 1px solid #dee0e3;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
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
.form-item.is-disabled .form-input {
  background-color: #f5f6f7;
  color: #8f959e;
  border-color: #dee0e3;
  cursor: not-allowed;
}
.account-readonly {
  gap: 6px;
}
.readonly-field {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid #dee0e3;
  border-radius: 4px;
  background: #f8f9fb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #1f2329;
}
.account-action {
  border: 0;
  background: transparent;
  color: #3f51b5;
  cursor: pointer;
  font-size: 13px;
  white-space: nowrap;
}
.form-footer {
  margin-top: 32px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.danger-button {
  height: 36px;
  padding: 0 24px;
  background: #fff1f0;
  color: #a8071a;
  border: 1px solid #ffccc7;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}
.danger-button:hover:not(:disabled) {
  background: #fff7f6;
  border-color: #ffa39e;
}
.danger-button:disabled {
  background: #f5f5f5;
  color: #bfbfbf;
  border-color: #d9d9d9;
  cursor: not-allowed;
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
@media (max-width: 640px) {
  .benefit-links {
    grid-template-columns: 1fr;
  }
  .benefit-entry + .benefit-entry {
    border-top: 1px solid #dee0e3;
    border-left: 0;
  }
}
</style>
