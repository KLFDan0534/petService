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
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'
import { getReviews, approveReview, rejectReview } from '@/api/rating'
import { ReviewStatus, enrichWithStatus } from '@/constants/statusMaps'


const appStore = useAppStore()
const reviews = ref([])

function enrichReview(r) {
  return enrichWithStatus(r, 'status_wsh', ReviewStatus)
}

async function loadReviews() {
  try { const r = await getReviews(); if (r.code === 200) reviews.value = (r.data.list || []).map(enrichReview) }
  catch (e) {}
}
onMounted(loadReviews)

async function approve(id) { try { await approveReview(id); appStore.addToast('已通过', 'success'); await loadReviews() } catch (e) { appStore.addToast('操作失败', 'error') } }
async function reject(id) { try { await rejectReview(id); appStore.addToast('已拒绝', 'success'); await loadReviews() } catch (e) { appStore.addToast('操作失败', 'error') } }
</script>
