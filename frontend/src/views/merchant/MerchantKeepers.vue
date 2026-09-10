<template>
  <div class="review-container">
    <div class="page-header">
      <h1 class="page-title">寄养员管理</h1>
      <p class="page-subtitle">审核、考勤与休假安排</p>
    </div>

    <MerchantAttendanceLeavePanel :keepers="allKeepers" />

    <div class="review-card">
      <div v-if="loading" class="loading-state">
        <div class="loading-icon"></div>
        <span>数据加载中...</span>
      </div>

      <div v-else-if="keepers.length === 0" class="empty-state">
        <p>暂无待审核的寄养员申请</p>
      </div>

      <div v-else class="keeper-list">
        <div v-for="k in keepers" :key="k.id_wsh" class="keeper-item">
          <div class="keeper-header">
            <div class="keeper-avatar">{{ (k.name_wsh || '?')[0] }}</div>
            <div class="keeper-meta">
              <div class="keeper-name">{{ k.name_wsh }}</div>
              <div class="keeper-phone">{{ k.phone_wsh }}</div>
            </div>
          </div>
          <div class="keeper-details">
            <div class="detail-row">
              <span class="detail-label">从业年限</span>
              <span class="detail-value">{{ k.experience_years_wsh || 0 }} 年</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">每日费用</span>
              <span class="detail-value">¥{{ k.price_per_day_wsh || 0 }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">最大接待</span>
              <span class="detail-value">{{ k.max_pets_wsh || 0 }} 只</span>
            </div>
            <div v-if="k.bio_wsh" class="detail-row detail-row--full">
              <span class="detail-label">简介</span>
              <span class="detail-value">{{ k.bio_wsh }}</span>
            </div>
          </div>
          <div v-if="operating === k.id_wsh" class="operating-hint">处理中...</div>
          <div class="keeper-actions">
            <button class="btn-approve" :disabled="operating === k.id_wsh" @click="approve(k.id_wsh)">审核通过</button>
            <button class="btn-reject" :disabled="operating === k.id_wsh" @click="reject(k.id_wsh)">驳回</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import MerchantAttendanceLeavePanel from '@/components/merchant/MerchantAttendanceLeavePanel.vue'
import { getMyMerchant } from '@/api/merchant'
import request from '@/utils/request'

const appStore = useAppStore()

const loading = ref(true)
const keepers = ref([])
const allKeepers = ref([])
const operating = ref(null)

onMounted(async () => {
  try {
    await Promise.all([loadPendingKeepers(), loadAllKeepers()])
  } catch (e) {
    appStore.addToast('获取待审核列表失败', 'error')
  } finally {
    loading.value = false
  }
})

async function loadPendingKeepers() {
  const r = await request.get('/api/keepers/merchant/pending')
  if (r.data.code === 200) keepers.value = r.data.data || []
}

async function loadAllKeepers() {
  const merchantRes = await getMyMerchant()
  const merchantId = merchantRes?.data?.id_wsh
  if (!merchantId) return
  const r = await request.get(`/api/keepers/merchant/${merchantId}`)
  if (r.data.code === 200) allKeepers.value = r.data.data || []
}

async function approve(id) {
  operating.value = id
  try {
    const r = await request.post(`/api/keepers/${id}/merchant-approve`)
    if (r.data.code === 200) {
      keepers.value = keepers.value.filter(k => k.id_wsh !== id)
      await loadAllKeepers()
      appStore.addToast('已审核通过', 'success')
    } else {
      appStore.addToast(r.data.message || '操作失败', 'error')
    }
  } catch (e) {
    appStore.addToast('操作失败', 'error')
  } finally {
    operating.value = null
  }
}

async function reject(id) {
  operating.value = id
  try {
    const r = await request.post(`/api/keepers/${id}/merchant-reject`)
    if (r.data.code === 200) {
      keepers.value = keepers.value.filter(k => k.id_wsh !== id)
      appStore.addToast('已驳回', 'success')
    } else {
      appStore.addToast(r.data.message || '操作失败', 'error')
    }
  } catch (e) {
    appStore.addToast('操作失败', 'error')
  } finally {
    operating.value = null
  }
}
</script>

<style scoped>
.review-container {
  max-width: 1080px;
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
.review-card {
  background: #ffffff;
  border: 1px solid #dee0e3;
  border-radius: var(--radius-inline);
  padding: 24px 32px;
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
.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #8f959e;
  font-size: 14px;
}
.keeper-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.keeper-item {
  border: 1px solid #dee0e3;
  border-radius: var(--radius-inline);
  padding: 16px;
}
.keeper-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.keeper-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #3f51b5;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
}
.keeper-meta {
  flex: 1;
}
.keeper-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}
.keeper-phone {
  font-size: 12px;
  color: #8f959e;
  margin-top: 2px;
}
.keeper-details {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 12px;
}
.detail-row {
  display: flex;
  gap: 6px;
  font-size: 13px;
}
.detail-row--full {
  grid-column: 1 / -1;
}
.detail-label {
  color: #8f959e;
  white-space: nowrap;
}
.detail-value {
  color: #1f2329;
}
.operating-hint {
  text-align: center;
  color: #3f51b5;
  font-size: 13px;
  margin-bottom: 8px;
}
.keeper-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
.btn-approve {
  height: 32px;
  padding: 0 20px;
  background: #3f51b5;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
}
.btn-approve:hover:not(:disabled) {
  background: #303f9f;
}
.btn-reject {
  height: 32px;
  padding: 0 20px;
  background: #ffffff;
  color: #e53935;
  border: 1px solid #e53935;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
}
.btn-reject:hover:not(:disabled) {
  background: #fff1f0;
}
.btn-approve:disabled,
.btn-reject:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
