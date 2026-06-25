<template>
  <div>
    <PageHero title="投诉建议" subtitle="向我们反馈问题" />
    <div style="display:flex;gap:12px;margin-bottom:24px">
      <button class="btn btn-primary" @click="showForm = true">+ 提交投诉</button>
      <button class="btn btn-outline" @click="showReportForm = !showReportForm">举报内容</button>
    </div>

    <div v-if="showReportForm" class="card" style="margin-bottom:16px;padding:20px">
      <h3 style="margin-bottom:16px">举报内容</h3>
      <form @submit.prevent="submitReport">
        <div class="form-group"><label>目标ID</label><input v-model="reportForm.target_id_wsh" required></div>
        <div class="form-group"><label>目标类型</label>
          <select v-model="reportForm.target_type_wsh">
            <option value="keeper">寄养师</option>
            <option value="merchant">商家</option>
            <option value="pet">宠物</option>
            <option value="content">内容</option>
          </select>
        </div>
        <div class="form-group"><label>原因</label><textarea v-model="reportForm.reason_wsh" rows="3" required></textarea></div>
        <div class="modal-actions">
          <button type="button" class="btn btn-secondary btn-sm" @click="showReportForm = false">取消</button>
          <button type="submit" class="btn btn-primary btn-sm">提交</button>
        </div>
      </form>
    </div>
    <div v-for="c in complaints" :key="c.id_wsh" class="card" style="margin-bottom:12px">
      <div style="display:flex;justify-content:space-between">
        <strong>{{ c.title_wsh }}</strong>
        <span :class="['badge', c.status_wsh === 'resolved' ? 'badge-success' : c.status_wsh === 'rejected' ? 'badge-danger' : 'badge-warning']">{{ c.status_wsh === 'resolved' ? '已处理' : c.status_wsh === 'rejected' ? '已驳回' : '待处理' }}</span>
      </div>
      <p style="margin-top:8px;font-size:14px;color:var(--color-muted-foreground)">{{ c.content_wsh }}</p>
    </div>
    <EmptyState v-if="!loading && complaints.length === 0" title="暂无投诉" icon="📢" />

    <div v-if="showForm" class="modal-overlay" @mousedown.self="showForm = false">
      <div class="modal">
        <h2>提交投诉</h2>
        <form @submit.prevent="createComplaint">
          <div class="form-group"><label>标题</label><input v-model="form.title_wsh" required></div>
          <div class="form-group"><label>内容</label><textarea v-model="form.content_wsh" rows="4" required></textarea></div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" @click="showForm = false">取消</button>
            <button type="submit" class="btn btn-primary btn-sm">提交</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const authStore = useAuthStore()
const appStore = useAppStore()
const complaints = ref([])
const loading = ref(true)
const showForm = ref(false)
const showReportForm = ref(false)
const form = reactive({ title_wsh: '', content_wsh: '' })
const reportForm = reactive({ target_id_wsh: '', target_type_wsh: 'pet', reason_wsh: '' })

onMounted(async () => {
  try { const r = await authStore.apiGet('/api/complaints'); if (r.code === 200) complaints.value = r.data }
  catch (e) {}
  finally { loading.value = false }
})

async function createComplaint() {
  try {
    const r = await authStore.apiPost('/api/complaints', form)
    if (r.code === 200) { appStore.addToast('提交成功', 'success'); showForm.value = false; complaints.value.push(r.data) }
  } catch (e) { appStore.addToast('提交失败', 'error') }
}

async function submitReport() {
  try {
    const r = await authStore.apiPost('/api/reviews', { ...reportForm })
    if (r.code === 200) {
      appStore.addToast('举报成功', 'success')
      showReportForm.value = false
      Object.assign(reportForm, { target_id_wsh: '', target_type_wsh: 'pet', reason_wsh: '' })
    }
  } catch (e) { appStore.addToast('举报失败', 'error') }
}
</script>
