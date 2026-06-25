<template>
  <div>
    <PageHero title="我的领养" subtitle="查看领养申请进度" />

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="applications.length === 0" class="empty-state">
      <div class="icon">📋</div>
      <h3>暂无领养申请</h3>
      <p>去宠物领养页面寻找你喜欢的宠物吧</p>
      <button class="btn btn-primary" @click="$router.push('/adoptions')">去领养</button>
    </div>
    <div v-else>
      <div v-for="app in applications" :key="app.id_wsh" class="card" style="margin-bottom:12px">
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">
          <div>
            <strong>宠物 #{{ app.pet_id_wsh }}</strong>
            <span style="margin-left:8px;font-size:13px;color:var(--color-muted-foreground)">{{ app.applicant_name_wsh }}</span>
          </div>
          <span :class="['badge', statusBadge(app.status_wsh)]">{{ statusLabel(app.status_wsh) }}</span>
        </div>
        <div style="margin-top:8px;font-size:13px;color:var(--color-muted-foreground)">
          <div>商家审核：<span :class="['badge', 'badge-sm', reviewBadge(app.merchant_status_wsh)]">{{ reviewLabel(app.merchant_status_wsh) }}</span></div>
          <div style="margin-top:4px">管理员审核：<span :class="['badge', 'badge-sm', reviewBadge(app.admin_status_wsh)]">{{ reviewLabel(app.admin_status_wsh) }}</span></div>
        </div>
        <p v-if="app.merchant_remark_wsh" style="margin-top:8px;font-size:13px;color:var(--color-warning)">商家备注：{{ app.merchant_remark_wsh }}</p>
        <p v-if="app.admin_remark_wsh" style="margin-top:4px;font-size:13px;color:var(--color-info)">管理员备注：{{ app.admin_remark_wsh }}</p>
        <div style="font-size:12px;color:var(--color-muted-foreground);margin-top:4px">提交时间：{{ new Date(app.created_at_wsh).toLocaleString() }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import PageHero from '@/components/common/PageHero.vue'

const authStore = useAuthStore()
const applications = ref([])
const loading = ref(true)

function statusLabel(s) {
  return { pending: '审核中', merchant_reviewing: '商家审核中', admin_reviewing: '管理员审核中', approved: '已通过', rejected: '已驳回' }[normalizeStatus(s)] || s
}

function statusBadge(s) {
  return { pending: 'badge-warning', merchant_reviewing: 'badge-info', admin_reviewing: 'badge-info', approved: 'badge-success', rejected: 'badge-error' }[normalizeStatus(s)] || 'badge-info'
}

function reviewLabel(s) {
  return { pending: '待审核', approved: '已通过', rejected: '已驳回' }[normalizeStatus(s)] || s
}

function reviewBadge(s) {
  return { pending: 'badge-warning', approved: 'badge-success', rejected: 'badge-error' }[normalizeStatus(s)] || 'badge-info'
}

function normalizeStatus(s) { return String(s || '').toLowerCase() }

onMounted(async () => {
  try {
    const r = await authStore.apiGet('/api/adoptions/me')
    if (r.code === 200) applications.value = r.data
  } catch (e) {}
  finally { loading.value = false }
})
</script>
