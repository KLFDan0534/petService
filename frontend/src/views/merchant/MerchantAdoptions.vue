<template>
  <div>
    <PageHero title="领养审核" subtitle="审核本店宠物的领养申请" />

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
          <div><strong>居住地址：</strong>{{ app.applicant_address_wsh }}</div>
          <div><strong>住房类型：</strong>{{ app.housing_type_wsh }} <span v-if="app.has_yard_wsh">· 有院子</span></div>
          <div v-if="app.family_members_wsh"><strong>家庭成员：</strong>{{ app.family_members_wsh }}</div>
          <div v-if="app.pet_experience_wsh"><strong>养宠经验：</strong>{{ app.pet_experience_wsh }}</div>
          <div><strong>领养理由：</strong>{{ app.reason_wsh }}</div>
          <div v-if="app.economic_condition_wsh"><strong>经济状况：</strong>{{ app.economic_condition_wsh }}</div>
          <div><strong>同意回访：</strong>{{ app.agree_visit_wsh ? '是' : '否' }}</div>
        </div>

        <div v-if="isPending(app.merchant_status_wsh)" style="margin-top:12px;display:flex;gap:8px">
          <button class="btn btn-success btn-sm" @click="review(app.id_wsh, true)">通过</button>
          <button class="btn btn-error btn-sm" @click="review(app.id_wsh, false)">驳回</button>
        </div>

        <div v-if="app.merchant_remark_wsh" style="margin-top:8px;font-size:13px;color:var(--color-warning)">审核备注：{{ app.merchant_remark_wsh }}</div>
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
function statusLabel(s) { return { pending: '待审核', merchant_reviewing: '商家已审核', admin_reviewing: '管理员审核中', approved: '已通过', rejected: '已驳回' }[normalizeStatus(s)] || s }
function statusBadge(s) { return { pending: 'badge-warning', merchant_reviewing: 'badge-info', admin_reviewing: 'badge-info', approved: 'badge-success', rejected: 'badge-error' }[normalizeStatus(s)] || 'badge-info' }

onMounted(async () => {
  try {
    const r = await authStore.apiGet('/api/adoptions/merchant')
    if (r.code === 200) applications.value = r.data
  } catch (e) {}
  finally { loading.value = false }
})

async function review(id, approved) {
  const remark = prompt(approved ? '请输入审核备注（可选）：' : '请输入驳回原因：')
  try {
    const r = await authStore.apiPost(`/api/adoptions/${id}/merchant-review`, { approved_wsh: approved, remark_wsh: remark || '' })
    if (r.code === 200) {
      appStore.addToast(approved ? '已通过' : '已驳回', 'success')
      const idx = applications.value.findIndex(a => a.id_wsh === id)
      if (idx !== -1) applications.value[idx] = r.data
    }
  } catch (e) { appStore.addToast('操作失败', 'error') }
}
</script>
