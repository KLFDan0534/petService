<template>
  <div class="profile-container">
    <div class="page-header">
      <h1 class="page-title">个人中心</h1>
      <p class="page-subtitle">管理您的个人资料与账号设置</p>
    </div>

    <div class="profile-card">
      <div v-if="loading" class="loading-state">
        <div class="loading-icon"></div>
        <span>加载中...</span>
      </div>

      <template v-else>
        <div class="user-profile-section">
          <div class="avatar-wrapper">
            <img v-if="avatarUrl" :src="avatarUrl" alt="头像">
            <span v-else>{{ avatarInitial }}</span>
          </div>
          <div class="user-info-text">
            <h2 class="user-nickname">{{ profile.nickname_wsh || authStore.user?.nickname_wsh || authStore.user?.username_wsh }}</h2>
            <div class="role-tags-group">
              <span v-for="role in (authStore.user?.roles_wsh || [])" :key="role" class="badge badge-secondary">{{ role }}</span>
              <span :class="['badge', realNameBadgeClass]">{{ realNameStatusText }}</span>
            </div>
          </div>
        </div>

        <StatisticsPanel :stats="stats" />
        <section v-if="!isSupportOnly" class="wallet-section" aria-labelledby="wallet-title">
          <div class="wallet-card">
            <div class="wallet-label">账户余额</div>
            <div class="wallet-balance">¥ {{ walletBalance }}</div>
            <div class="wallet-actions">
              <router-link to="/recharge" class="btn btn-primary btn-sm">充值</router-link>
              <router-link to="/wallet" class="btn btn-outline btn-sm">钱包管理</router-link>
            </div>
          </div>
        </section>
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
        <section v-if="!isSupportOnly" class="benefits-section" aria-labelledby="feedback-title">
          <h2 id="feedback-title" class="section-label">问题反馈</h2>
          <nav class="benefit-links" aria-label="问题反馈">
            <router-link to="/tickets" class="benefit-entry">
              <span class="benefit-icon" aria-hidden="true">
                <el-icon><ChatDotRound /></el-icon>
              </span>
              <span class="benefit-info">
                <span class="benefit-title">我的工单</span>
                <span class="benefit-desc">查看申诉与客服沟通记录</span>
              </span>
              <el-icon class="benefit-arrow" aria-hidden="true"><ArrowRight /></el-icon>
            </router-link>
            <router-link to="/complaints" class="benefit-entry">
              <span class="benefit-icon" aria-hidden="true">
                <el-icon><Warning /></el-icon>
              </span>
              <span class="benefit-info">
                <span class="benefit-title">我的投诉</span>
                <span class="benefit-desc">提交并跟踪对订单、商家或寄养师的投诉</span>
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
              <label class="form-label">用户名</label>
              <input class="form-input" v-model="profile.username_wsh" disabled title="用户名由系统管理">
            </div>
            <div class="form-item">
              <label class="form-label">昵称</label>
              <input class="form-input" v-model="profile.nickname_wsh" placeholder="请输入昵称" required>
            </div>
            <div class="form-item">
              <label class="form-label">性别</label>
              <select class="form-input" v-model.number="profile.gender_wsh">
                <option :value="0">未知</option>
                <option :value="1">男</option>
                <option :value="2">女</option>
              </select>
            </div>
            <div class="form-item account-readonly">
              <label class="form-label">手机号</label>
              <div class="readonly-field">
                <span>{{ profile.phone_wsh || '未绑定' }}</span>
                <button type="button" class="account-action" @click="goAccountEdit('phone')">修改</button>
              </div>
            </div>
            <div class="form-item account-readonly">
              <label class="form-label">邮箱</label>
              <div class="readonly-field">
                <span>{{ profile.email_wsh || '未绑定' }}</span>
                <button type="button" class="account-action" @click="goAccountEdit('email')">修改</button>
              </div>
            </div>
            <div class="form-item account-readonly form-item--wide">
              <label class="form-label">实名认证</label>
              <div class="readonly-field">
                <span>{{ realNameDisplay }}</span>
                <button type="button" class="account-action" @click="goAccountEdit('real-name')">去认证</button>
              </div>
            </div>
            <div class="form-item account-readonly form-item--wide">
              <label class="form-label">支付密码</label>
              <div class="readonly-field">
                <span>{{ profile.payment_password_set_wsh ? '已设置' : '未设置' }}</span>
                <button type="button" class="account-action" @click="goAccountEdit('payment-password')">
                  {{ profile.payment_password_set_wsh ? '修改' : '设置' }}
                </button>
              </div>
            </div>
          </div>

          <div class="form-footer">
            <button type="button" class="btn btn-danger" :disabled="submitting || deleting" @click="handleDeleteAccount">
              {{ deleting ? '删除中...' : '删除账号' }}
            </button>
            <button type="submit" class="btn btn-primary" :disabled="submitting || deleting">
              {{ submitting ? '保存中...' : '保存修改' }}
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
import { ArrowRight, ChatDotRound, Medal, Ticket, Warning } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { deleteCurrentUser } from '@/api/auth'
import { getMyCustomerServiceApplications } from '@/api/merchantCustomerService'
import { getMyWallet } from '@/api/wallet'
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
const walletBalance = ref('0.00')
const merchantStatus = ref(null)
const keeperStatus = ref(null)
const keeperLoading = ref(true)
const customerServiceApplications = ref([])

const avatarUrl = computed(() => profile.avatar_wsh || authStore.user?.avatar_wsh || '')
const avatarInitial = computed(() => (profile.nickname_wsh || authStore.user?.nickname_wsh || authStore.user?.username_wsh || '?')[0].toUpperCase())
const realNameStatusText = computed(() => {
  const status = Number(profile.real_name_status_wsh || 0)
  if (status === 2) return '已认证'
  if (status === 1) return '待审核'
  if (status === 3) return '已驳回'
  return '未认证'
})
const realNameBadgeClass = computed(() => {
  const status = Number(profile.real_name_status_wsh || 0)
  if (status === 2) return 'badge-success'
  if (status === 1) return 'badge-warning'
  if (status === 3) return 'badge-danger'
  return 'badge-disabled'
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
  resigned: 'badge-disabled',
  terminated: 'badge-disabled',
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

async function loadWallet() {
  try {
    const res = await getMyWallet()
    if (res.code === 200 && res.data) {
      walletBalance.value = Number(res.data.balance_wsh || 0).toFixed(2)
    }
  } catch (e) {
    walletBalance.value = '-'
  }
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
    appStore.addToast('加载个人资料失败', 'error')
  } finally {
    loading.value = false
  }
  loadMerchantStatus()
  loadKeeperStatus()
  loadCustomerServiceApplications()
  loadWallet()
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
      appStore.addToast('头像上传成功', 'success')
    }
  } catch (error) {
    appStore.addToast('头像上传失败', 'error')
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
      appStore.addToast('保存成功', 'success')
      syncUser(r.data.data || profile)
    }
  } catch (e) {
    appStore.addToast('保存失败', 'error')
  } finally {
    submitting.value = false
  }
}

async function handleDeleteAccount() {
  if (submitting.value || deleting.value) return
  if (!window.confirm('确定要删除该账号吗？此操作不可恢复。')) return

  deleting.value = true
  try {
    const r = await deleteCurrentUser()
    if (r.code === 200) {
      appStore.addToast('账号已删除', 'success')
      authStore.clearAuth()
      router.replace('/login')
    }
  } catch (e) {
    appStore.addToast(e.response?.data?.message || '删除失败', 'error')
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
  color: var(--color-foreground);
  margin: 0 0 4px 0;
}
.page-subtitle {
  font-size: 13px;
  color: var(--color-muted-foreground);
  margin: 0;
}
.profile-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 32px;
}
.user-profile-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--color-border);
}
.avatar-wrapper {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-on-primary);
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
  color: var(--color-foreground);
  margin: 0 0 6px 0;
}
.role-tags-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.benefits-section {
  margin: 24px 0;
  padding: 20px 0;
  border-top: 1px solid var(--color-border);
}
.wallet-section {
  margin: 24px 0;
  padding: 20px 0;
  border-top: 1px solid var(--color-border);
}
.wallet-card {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 20px 24px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, color-mix(in srgb, var(--color-primary) 15%, var(--color-card)), var(--color-card));
  border: 1px solid var(--color-border);
  flex-wrap: wrap;
}
.wallet-label {
  font-size: 13px;
  color: var(--color-muted-foreground);
  margin-bottom: 4px;
}
.wallet-balance {
  font-size: 30px;
  font-weight: 700;
  color: var(--color-foreground);
  line-height: 1.1;
}
.wallet-actions {
  display: flex;
  gap: 10px;
  margin-left: auto;
  flex-wrap: wrap;
}
.benefit-links {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
}
.benefit-entry {
  min-height: 72px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  color: var(--color-foreground);
  text-decoration: none;
  transition: background-color 0.2s;
}
.benefit-entry + .benefit-entry {
  border-left: 1px solid var(--color-border);
}
.benefit-entry:hover {
  background: var(--color-muted);
}
.benefit-entry:focus-visible {
  position: relative;
  outline: 3px solid var(--color-ring);
  outline-offset: 2px;
  border-radius: var(--radius-sm);
}
.benefit-icon {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--color-primary) 12%, var(--color-card));
  color: var(--color-primary);
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
  color: var(--color-foreground);
  font-size: 14px;
  font-weight: 600;
}
.benefit-desc {
  color: var(--color-muted-foreground);
  font-size: 12px;
  line-height: 1.5;
}
.benefit-arrow {
  flex: 0 0 auto;
  color: var(--color-muted-foreground);
  font-size: 16px;
}
.customer-service-section {
  margin: 24px 0;
  padding: 20px 0;
  border-top: 1px solid var(--color-border);
}
.section-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-foreground);
  margin: 0 0 12px;
}
.service-entry {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  cursor: pointer;
  text-align: left;
  transition: background 0.2s, border-color 0.2s;
}
.service-entry:hover {
  background: var(--color-muted);
  border-color: var(--color-border);
}
.service-icon {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--color-primary) 12%, var(--color-card));
  color: var(--color-primary);
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
  color: var(--color-foreground);
}
.service-desc {
  font-size: 12px;
  color: var(--color-muted-foreground);
  margin-top: 2px;
}
.service-arrow {
  color: var(--color-muted-foreground);
  font-size: 16px;
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
  color: var(--color-foreground);
}
.form-input {
  height: 40px;
  border-radius: var(--radius-md);
}
.form-item.is-disabled .form-input {
  background: var(--color-muted);
  color: var(--color-muted-foreground);
  border-color: var(--color-border);
  cursor: not-allowed;
}
.account-readonly {
  gap: 6px;
}
.readonly-field {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-muted);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--color-foreground);
}
.account-action {
  border: 0;
  background: transparent;
  color: var(--color-primary);
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
.form-footer .btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 60px 0;
  color: var(--color-muted-foreground);
  font-size: 14px;
}
.loading-icon {
  width: 16px;
  height: 16px;
  border: 2px solid var(--color-border);
  border-left-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 640px) {
  .benefit-links {
    grid-template-columns: 1fr;
  }
  .benefit-entry + .benefit-entry {
    border-top: 1px solid var(--color-border);
    border-left: 0;
  }
}
</style>
