<template>
  <div class="profile-container">
    <div class="page-header">
      <h1 class="page-title">个人资料</h1>
      <p class="page-subtitle">管理您的个人基本信息及系统权限</p>
    </div>

    <div class="profile-card">
      <!-- 骨架屏/加载中（更符合传统业务系统规范） -->
      <div v-if="loading" class="loading-state">
        <div class="loading-icon"></div>
        <span>数据加载中...</span>
      </div>

      <template v-else>
        <!-- 用户基础信息栏：去掉了渐变，采用纯色质感 -->
        <div class="user-profile-section">
          <div class="avatar-wrapper">
            {{ (authStore.user?.nickname_wsh || authStore.user?.username_wsh || '?')[0].toUpperCase() }}
          </div>
          <div class="user-info-text">
            <h2 class="user-nickname">{{ authStore.user?.nickname_wsh || authStore.user?.username_wsh }}</h2>
            <div class="role-tags-group">
              <span v-for="role in (authStore.user?.roles_wsh || [])" :key="role" class="sys-tag">
                {{ role }}
              </span>
            </div>
          </div>
        </div>

        <!-- 统计面板：去掉了Emoji，强化了数字与标签的严谨排版 -->
        <div class="dashboard-stats">
          <div class="stat-item">
            <div class="stat-label">我的宠物</div>
            <div class="stat-number">{{ stats.pets ?? 0 }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">进行中订单</div>
            <div class="stat-number">{{ stats.activeOrders ?? 0 }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">已完成订单</div>
            <div class="stat-number">{{ stats.completedOrders ?? 0 }}</div>
          </div>
          <div class="stat-item highlight-item">
            <div class="stat-label">累计消费</div>
            <div class="stat-number">¥ {{ formatPrice(stats.totalSpent) }}</div>
          </div>
        </div>

        <!-- 商家信息 -->
        <div class="merchant-section">
          <div class="section-label">商家服务</div>
          <div v-if="merchantStatus === null" class="merchant-entry" @click="applyMerchant">
            <div class="merchant-icon">🏪</div>
            <div class="merchant-info">
              <div class="merchant-title">申请成为商家</div>
              <div class="merchant-desc">入驻平台，发布宠物寄养服务</div>
            </div>
            <span class="merchant-arrow">→</span>
          </div>
          <div v-else-if="merchantStatus === 0" class="merchant-entry" style="cursor:default">
            <div class="merchant-icon">⏳</div>
            <div class="merchant-info">
              <div class="merchant-title">商家审核中</div>
              <div class="merchant-desc">管理员正在审核您的申请，请耐心等待</div>
            </div>
            <span class="badge badge-warning">待审核</span>
          </div>
          <div v-else-if="merchantStatus === 1" class="merchant-entry" @click="$router.push('/merchant/dashboard')">
            <div class="merchant-icon">✅</div>
            <div class="merchant-info">
              <div class="merchant-title">商家中心</div>
              <div class="merchant-desc">管理您的商家信息、订单和服务</div>
            </div>
            <span class="merchant-arrow">→</span>
          </div>
          <div v-else-if="merchantStatus === 2" class="merchant-entry" @click="applyMerchant">
            <div class="merchant-icon">❌</div>
            <div class="merchant-info">
              <div class="merchant-title">申请被拒绝</div>
              <div class="merchant-desc">点击重新申请</div>
            </div>
            <span class="merchant-arrow">→</span>
          </div>
        </div>

        <!-- 表单区域：2x2经典布局，增强了无障碍交互（Focus和Disabled状态） -->
        <form @submit.prevent="saveProfile" class="data-form">
          <div class="form-grid">
            <div class="form-item is-disabled">
              <label class="form-label">账户用户名</label>
              <input class="form-input" v-model="profile.username_wsh" disabled title="用户名系统锁定">
            </div>
            <div class="form-item">
              <label class="form-label">用户昵称</label>
              <input class="form-input" v-model="profile.nickname_wsh" placeholder="请输入昵称" required>
            </div>
            <div class="form-item">
              <label class="form-label">绑定手机</label>
              <input class="form-input" v-model="profile.phone_wsh" placeholder="请输入手机号" type="tel">
            </div>
            <div class="form-item">
              <label class="form-label">电子邮箱</label>
              <input class="form-input" v-model="profile.email_wsh" type="email" placeholder="请输入邮箱">
            </div>
          </div>

          <div class="form-footer">
            <button type="submit" class="submit-button" :disabled="submitting">
              {{ submitting ? '正在保存...' : '保存更改' }}
            </button>
          </div>
        </form>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import request from '@/utils/request'

const router = useRouter()

const authStore = useAuthStore()
const appStore = useAppStore()

const loading = ref(true)
const submitting = ref(false)
const profile = reactive({ username_wsh: '', nickname_wsh: '', phone_wsh: '', email_wsh: '' })
const stats = ref({ pets: 0, activeOrders: 0, completedOrders: 0, totalSpent: 0 })
const merchantStatus = ref(null)

const formatPrice = (val) => {
  if (!val) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadMerchantStatus() {
  try {
    const r = await request.get('/merchants/my')
    if (r.data.code === 200 && r.data.data) {
      merchantStatus.value = r.data.data.status_wsh
    }
  } catch (e) {
    // Not a merchant yet
  }
}

function applyMerchant() {
  router.push('/merchants')
}

onMounted(async () => {
  try {
    const [ur, sr] = await Promise.all([
      request.get('/users/me'),
      request.get('/statistics/user'),
    ])
    if (ur.data.code === 200) Object.assign(profile, ur.data.data)
    if (sr.data.code === 200) stats.value = sr.data.data
  } catch (e) {
    appStore.addToast('获取个人资料失败', 'error')
  } finally {
    loading.value = false
  }
  loadMerchantStatus()
})

async function saveProfile() {
  if (submitting.value) return
  submitting.value = true
  try {
    const r = await request.put('/users/me', profile)
    if (r.data.code === 200) {
      appStore.addToast('保存成功', 'success')
      if (authStore.user) {
        authStore.user = { ...authStore.user, ...profile }
      }
    }
  } catch (e) {
    appStore.addToast('保存失败', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
/* 去掉所有花哨的 animation 渐入，回归直白渲染 */
.profile-container {
  max-width: 800px;
  margin: 0 auto;
}

/* 头部规范化 */
.page-header {
  margin-bottom: 24px;
}
.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #1f2329; /* 经典中台暗灰 */
  margin: 0 0 4px 0;
}
.page-subtitle {
  font-size: 13px;
  color: #8f959e;
  margin: 0;
}

/* 卡片容器：微小阴影与坚固边框 */
.profile-card {
  background: #ffffff;
  border: 1px solid #dee0e3;
  border-radius: 6px;
  padding: 32px;
}

/* 用户区块：洗白，采用标准的左头像右文本 */
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
  border-radius: 4px; /* 改为偏方正的微圆角 */
  background: #3f51b5; /* 使用严谨的工业标准蓝/紫 */
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 600;
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

/* 统计卡片：去掉卡片色块，改用简洁的线框和上下结构排版 */
.dashboard-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  background: #dee0e3; /* 利用网格线实现边框效果 */
  border: 1px solid #dee0e3;
  border-radius: 4px;
  margin: 24px 0;
  overflow: hidden;
}

.stat-item {
  background: #ffffff;
  padding: 16px 20px;
}

.highlight-item {
  background: #fafafa; /* 特殊数据轻微突出 */
}

.stat-label {
  font-size: 12px;
  color: #646a73;
  margin-bottom: 8px;
}

.stat-number {
  font-size: 20px;
  font-weight: 700;
  color: #1f2329;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

/* 表单规整 */
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

/* 坚固的聚焦态 */
.form-input:focus {
  outline: none;
  border-color: #3f51b5;
}

/* 禁用态表现 */
.form-item.is-disabled .form-input {
  background-color: #f5f6f7;
  color: #8f959e;
  border-color: #dee0e3;
  cursor: not-allowed;
}

.form-footer {
  margin-top: 32px;
  display: flex;
  justify-content: flex-end;
}

/* 实体标准按钮，无阴影 */
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

/* 严谨的加载状态 */
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

.merchant-section {
  margin: 24px 0;
  padding: 20px 0;
  border-top: 1px solid #dee0e3;
}
.section-label {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 12px;
}
.merchant-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid #dee0e3;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}
.merchant-entry:hover {
  background: #f5f6f7;
}
.merchant-icon {
  font-size: 24px;
}
.merchant-info {
  flex: 1;
}
.merchant-title {
  font-size: 14px;
  font-weight: 500;
  color: #1f2329;
}
.merchant-desc {
  font-size: 12px;
  color: #8f959e;
  margin-top: 2px;
}
.merchant-arrow {
  font-size: 16px;
  color: #8f959e;
}
</style>