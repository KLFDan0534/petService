<template>
  <div>
    <div style="display:flex;gap:8px;margin-bottom:16px">
      <button v-for="t in tabs" :key="t.key" :class="['btn', activeTab === t.key ? 'btn-primary' : 'btn-outline', 'btn-sm']"
        @click="activeTab = t.key; loadList()">{{ t.label }}</button>
    </div>
    <div style="margin-bottom:12px">
      <input v-model="keyword" class="form-control" placeholder="搜索用户名/昵称/真实姓名" style="width:280px"
        @keyup.enter="loadList">
    </div>
    <DataTable :columns="[
      { key: 'id_wsh', label: 'ID' },
      { key: 'username_wsh', label: '用户名' },
      { key: 'nickname_wsh', label: '昵称' },
      { key: 'real_name_wsh', label: '真实姓名' },
      { key: 'id_card_no_wsh', label: '身份证号' },
      { key: 'phone_wsh', label: '手机号' },
      { key: 'status_label_wsh', label: '状态' },
      { key: 'reject_reason_wsh', label: '驳回原因' },
      { key: 'created_at_wsh', label: '注册时间' },
    ]" :data="reviews">
      <template #default="{ row }">
        <div style="display:flex;gap:4px;flex-wrap:wrap">
          <button v-if="row.real_name_status_wsh === 1" class="btn btn-sm btn-success" @click="approve(row)">通过</button>
          <button v-if="row.real_name_status_wsh === 1" class="btn btn-sm btn-danger" @click="showReject(row)">驳回</button>
        </div>
      </template>
    </DataTable>
    <div v-if="total > 0" style="display:flex;justify-content:space-between;align-items:center;margin-top:12px">
      <span style="font-size:13px;color:var(--color-muted-foreground)">共 {{ total }} 条</span>
      <div style="display:flex;gap:4px">
        <button :disabled="page <= 1" class="btn btn-sm btn-outline" @click="page--; loadList()">上一页</button>
        <span style="line-height:32px;padding:0 8px;font-size:13px">{{ page }} / {{ pages }}</span>
        <button :disabled="page >= pages" class="btn btn-sm btn-outline" @click="page++; loadList()">下一页</button>
      </div>
    </div>
    <Teleport to="body">
      <AppDialog
        :visible="showRejectDialog"
        :width="400"
        title="驳回实名认证"
        @close="showRejectDialog = false"
      >
        <p style="font-size:13px;color:var(--color-muted-foreground);margin-bottom:12px">用户：{{ rejectTarget?.real_name_wsh || rejectTarget?.username_wsh }}</p>
        <textarea v-model="rejectReason" class="form-control" rows="3" placeholder="驳回原因（选填）"></textarea>
        <template #footer>
          <button class="btn btn-outline btn-sm" @click="showRejectDialog = false">取消</button>
          <button class="btn btn-danger btn-sm" @click="doReject">确认驳回</button>
        </template>
      </AppDialog>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { getRealNameReviews, approveRealNameReview, rejectRealNameReview } from '@/api/realNameReview'
import { RealNameStatus, enrichWithStatus } from '@/constants/statusMaps'
import DataTable from '@/components/common/DataTable.vue'
import AppDialog from '@/components/common/AppDialog.vue'

const appStore = useAppStore()
const reviews = ref([])
const total = ref(0)
const page = ref(1)
const pages = ref(1)
const keyword = ref('')
const activeTab = ref('pending')
const tabs = [
  { key: 'pending', label: '待审核' },
  { key: 'all', label: '全部' },
  { key: 'approved', label: '已通过' },
  { key: 'rejected', label: '未通过' },
]
const showRejectDialog = ref(false)
const rejectTarget = ref(null)
const rejectReason = ref('')

const statusMap = { pending: 1, all: null, approved: 2, rejected: 3 }

onMounted(loadList)

function enrich(r) {
  enrichWithStatus(r, 'real_name_status_wsh', RealNameStatus, 'status_label_wsh')
  return r
}

async function loadList() {
  try {
    const r = await getRealNameReviews({ page: page.value, size: 20, status: statusMap[activeTab.value], q: keyword.value || undefined })
    if (r.code === 200) {
      reviews.value = (r.data.list || []).map(enrich)
      total.value = r.data.total || 0
      page.value = r.data.page || 1
      pages.value = r.data.pages || 1
    }
  } catch (e) {}
}

async function approve(row) {
  if (!confirm(`确定通过 ${row.real_name_wsh || row.username_wsh} 的实名认证？`)) return
  try {
    const r = await approveRealNameReview(row.id_wsh, { remark_wsh: '' })
    if (r.code === 200) { appStore.addToast('已通过', 'success'); await loadList() }
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

function showReject(row) {
  rejectTarget.value = row
  rejectReason.value = ''
  showRejectDialog.value = true
}

async function doReject() {
  try {
    const r = await rejectRealNameReview(rejectTarget.value.id_wsh, { remark_wsh: rejectReason.value || undefined })
    if (r.code === 200) { appStore.addToast('已驳回', 'success'); showRejectDialog.value = false; await loadList() }
  } catch (e) { appStore.addToast('操作失败', 'error') }
}
</script>
