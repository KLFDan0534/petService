<template>
  <div>
    <DataTable :columns="[
      { key: 'id_wsh', label: 'ID' },
      { key: 'title_wsh', label: '资质类型' },
      { key: 'user_id_wsh', label: '提交用户ID' },
      { key: 'summary_wsh', label: '说明' },
      { key: 'status_wsh', label: '状态' },
      { key: 'created_at_wsh', label: '提交时间' },
    ]" :data="list">
      <template #default="{ row }">
        <div style="display:flex;gap:4px;flex-wrap:wrap">
          <template v-if="row.file_url_wsh">
            <a v-for="(url, i) in row.file_url_wsh.split(',')" :key="i" :href="url" target="_blank" class="btn btn-sm btn-outline" style="text-decoration:none">文件{{ row.file_url_wsh.split(',').length > 1 ? i + 1 : '' }}</a>
          </template>
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-success" @click="approve(row)">通过</button>
          <button v-if="row.status_wsh === 'pending'" class="btn btn-sm btn-danger" @click="showReject(row)">驳回</button>
        </div>
      </template>
    </DataTable>
    <div v-if="!list.length" style="text-align:center;padding:40px;color:var(--color-muted-foreground)">暂无待审核资质</div>
    <Teleport to="body">
      <div v-if="showRejectDialog" class="modal-overlay" @click.self="showRejectDialog = false">
        <div class="modal" style="width:400px">
          <h3>驳回资质</h3>
          <p style="font-size:13px;color:var(--color-muted-foreground);margin-bottom:12px">资质：{{ rejectTarget?.title_wsh }}</p>
          <textarea v-model="rejectRemark" class="form-control" rows="3" placeholder="驳回原因（选填）"></textarea>
          <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:12px">
            <button class="btn btn-outline btn-sm" @click="showRejectDialog = false">取消</button>
            <button class="btn btn-danger btn-sm" @click="doReject">确认驳回</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'
import { getPendingQualifications, approveQualification, rejectQualification } from '@/api/qualification'

const appStore = useAppStore()
const list = ref([])
const showRejectDialog = ref(false)
const rejectTarget = ref(null)
const rejectRemark = ref('')

onMounted(loadList)

async function loadList() {
  try {
    const r = await getPendingQualifications()
    if (r.code === 200) list.value = r.data || []
  } catch (e) {}
}

async function approve(row) {
  if (!confirm(`确定通过该资质？`)) return
  try {
    const userStr = localStorage.getItem('user')
    const user = userStr ? JSON.parse(userStr) : null
    const r = await approveQualification(row.id_wsh, user?.id_wsh || 0)
    if (r.code === 200) { appStore.addToast('已通过', 'success'); await loadList() }
  } catch (e) { appStore.addToast('操作失败', 'error') }
}

function showReject(row) {
  rejectTarget.value = row
  rejectRemark.value = ''
  showRejectDialog.value = true
}

async function doReject() {
  try {
    const userStr = localStorage.getItem('user')
    const user = userStr ? JSON.parse(userStr) : null
    const r = await rejectQualification(rejectTarget.value.id_wsh, user?.id_wsh || 0, rejectRemark.value || undefined)
    if (r.code === 200) { appStore.addToast('已驳回', 'success'); showRejectDialog.value = false; await loadList() }
  } catch (e) { appStore.addToast('操作失败', 'error') }
}
</script>
