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

    <div style="display:flex;align-items:center;justify-content:flex-end;gap:8px;flex-wrap:wrap;margin-top:12px;font-size:13px">
      <div style="display:inline-flex;align-items:center;gap:6px;margin-right:auto">
        <span>每页</span>
        <select v-model.number="size" :disabled="loading" style="width:auto;min-width:84px" @change="changeSize">
          <option v-for="n in pageSizes" :key="n" :value="n">{{ n }} 条</option>
        </select>
      </div>
      <span>共 {{ total }} 条 · 第 {{ page }} / {{ pages || 1 }} 页</span>
      <button class="btn btn-sm btn-outline" type="button" :disabled="loading || page <= 1" @click="goPage(page - 1)">上一页</button>
      <button class="btn btn-sm btn-outline" type="button" :disabled="loading || pages === 0 || page >= pages" @click="goPage(page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'
import { getReviews, approveReview, rejectReview } from '@/api/rating'
import { ReviewStatus, enrichWithStatus } from '@/constants/statusMaps'


const appStore = useAppStore()
// 审核列表：后端 /api/reviews 已支持分页，这里补上翻页
const reviews = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const pages = ref(0)
const loading = ref(false)
const pageSizes = [20, 50, 100]

function enrichReview(r) {
  return enrichWithStatus(r, 'status_wsh', ReviewStatus)
}

async function loadReviews() {
  loading.value = true
  try {
    const r = await getReviews({ page: page.value, size: size.value })
    if (r.code === 200) {
      const data = r.data || {}
      reviews.value = (data.list || []).map(enrichReview)
      total.value = data.total || 0
      pages.value = data.pages || 0
      if (data.page) page.value = data.page
    }
  } catch (e) {
    // 失败保留上一次结果
  } finally {
    loading.value = false
  }
}

async function changeSize() {
  page.value = 1
  await loadReviews()
}

async function goPage(target) {
  if (target < 1 || target === page.value) return
  if (pages.value && target > pages.value) return
  page.value = target
  await loadReviews()
}
onMounted(loadReviews)

// 通过和拒绝都只是改状态，改完重新拉一遍列表
async function approve(id) { try { await approveReview(id); appStore.addToast('已通过', 'success'); await loadReviews() } catch (e) { appStore.addToast('操作失败', 'error') } }
async function reject(id) { try { await rejectReview(id); appStore.addToast('已拒绝', 'success'); await loadReviews() } catch (e) { appStore.addToast('操作失败', 'error') } }
</script>
