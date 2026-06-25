<template>
  <div>
    <DataTable :columns="[{key:'id_wsh',label:'ID'},{key:'target_type_wsh',label:'内容类型'},{key:'reporter_id_wsh',label:'提交者'},{key:'status_label_wsh',label:'状态'}]" :data="reviews">
      <template #default="{ row }">
        <div style="display:flex;gap:8px">
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-success" @click="approve(row.id_wsh)">通过</button>
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-danger" @click="reject(row.id_wsh)">拒绝</button>
        </div>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'

const authStore = useAuthStore()
const appStore = useAppStore()
const reviews = ref([])

const reviewStatusMap = { pending: { text: '待审核', cls: 'badge-warning' }, approved: { text: '已通过', cls: 'badge-success' }, rejected: { text: '已拒绝', cls: 'badge-danger' } }

function enrichReview(r) {
  const s = reviewStatusMap[r.status_wsh]
  r.status_label_wsh = s ? `<span class="badge ${s.cls}">${s.text}</span>` : r.status_wsh
  return r
}

onMounted(async () => {
  try { const r = await authStore.apiGet('/api/reviews'); if (r.code === 200) reviews.value = (r.data.list || []).map(enrichReview) }
  catch (e) {}
})

async function approve(id) { try { await authStore.apiPost(`/api/reviews/${id}/approve`, {}); appStore.addToast('已通过', 'success'); location.reload() } catch (e) { appStore.addToast('操作失败', 'error') } }
async function reject(id) { try { await authStore.apiPost(`/api/reviews/${id}/reject`, {}); appStore.addToast('已拒绝', 'success'); location.reload() } catch (e) { appStore.addToast('操作失败', 'error') } }
</script>
