<template>
  <div>
    <PageHero title="领养管理" subtitle="管理员终审领养申请" />

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="applications.length === 0" class="empty-state">
      <div class="icon">📋</div>
      <h3>暂无领养申请</h3>
    </div>
    <div v-else>
      <div v-for="app in applications" :key="app.id_wsh" class="card" style="margin-bottom:12px">
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">
          <div>
            <strong>{{ app.applicant_name_wsh }}</strong>
            <span style="margin-left:8px;font-size:13px;color:var(--color-muted-foreground)">{{ app.applicant_phone_wsh }}</span>
          </div>
          <span :class="['badge', statusBadge(app.status_wsh)]">{{ statusLabel(app.status_wsh) }}</span>
        </div>

        <div style="margin-top:12px;font-size:14px">
          <div><strong>申请宠物：</strong>#{{ app.pet_id_wsh }}</div>
          <div><strong>申请用户：</strong>#{{ app.user_id_wsh }}</div>
          <div><strong>商家：</strong>#{{ app.merchant_id_wsh }}</div>
          <div><strong>领养理由：</strong>{{ app.reason_wsh }}</div>
          <div><strong>商家审核：</strong><span :class="['badge', reviewBadge(app.merchant_status_wsh)]">{{ reviewLabel(app.merchant_status_wsh) }}</span></div>
          <div v-if="app.merchant_remark_wsh">商家备注：{{ app.merchant_remark_wsh }}</div>
        </div>

        <div v-if="isApproved(app.merchant_status_wsh) && isPending(app.admin_status_wsh)" style="margin-top:12px;display:flex;gap:8px">
          <button class="btn btn-success btn-sm" @click="adminReview(app.id_wsh, true)">终审通过</button>
          <button class="btn btn-error btn-sm" @click="adminReview(app.id_wsh, false)">驳回</button>
        </div>

        <div v-if="app.admin_remark_wsh" style="margin-top:8px;font-size:13px;color:var(--color-info)">管理员备注：{{ app.admin_remark_wsh }}</div>
        <div style="font-size:12px;color:var(--color-muted-foreground);margin-top:4px">{{ new Date(app.created_at_wsh).toLocaleString() }}</div>
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
const applications = ref([])
const loading = ref(true)

function normalizeStatus(s) { return String(s || '').toLowerCase() }
function isPending(s) { return normalizeStatus(s) === 'pending' || !s }
function isApproved(s) { return normalizeStatus(s) === 'approved' }
function statusLabel(s) { return { pending: '待审核', merchant_reviewing: '商家已审核', admin_reviewing: '管理员审核中', approved: '已通过', rejected: '已驳回' }[normalizeStatus(s)] || s }
function statusBadge(s) { return { pending: 'badge-warning', merchant_reviewing: 'badge-info', admin_reviewing: 'badge-info', approved: 'badge-success', rejected: 'badge-error' }[normalizeStatus(s)] || 'badge-info' }
function reviewLabel(s) { return { pending: '待审核', approved: '已通过', rejected: '已驳回' }[normalizeStatus(s)] || s }
function reviewBadge(s) { return { pending: 'badge-warning', approved: 'badge-success', rejected: 'badge-error' }[normalizeStatus(s)] || 'badge-info' }

onMounted(async () => {
  try {
    const r = await authStore.apiGet('/api/adoptions')
    if (r.code === 200) applications.value = r.data
  } catch (e) {}
  finally { loading.value = false }
})

async function adminReview(id, approved) {
  const remark = prompt(approved ? '请输入审核备注（可选）：' : '请输入驳回原因：')
  try {
    const r = await authStore.apiPost(`/api/adoptions/${id}/admin-review`, { approved, remark: remark || '' })
    if (r.code === 200) {
      appStore.addToast(approved ? '终审通过' : '已驳回', 'success')
      const idx = applications.value.findIndex(a => a.id_wsh === id)
      if (idx !== -1) applications.value[idx] = r.data
    }
  } catch (e) { appStore.addToast('操作失败', 'error') }
}
</script>
