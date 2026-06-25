<template>
  <div>
    <PageHero title="退款管理" subtitle="查看退款申请和进度" />
    <button class="btn btn-primary" style="margin-bottom:24px" @click="openForm">+ 申请退款</button>
    <div v-if="loading" class="loading">加载中...</div>
    <EmptyState v-else-if="refunds.length === 0" title="暂无退款申请" icon="💳">
      <router-link to="/orders" class="btn btn-primary">查看订单</router-link>
    </EmptyState>
    <div v-else v-for="r in refunds" :key="r.id_wsh" class="card" style="margin-bottom:12px">
      <div style="display:flex;justify-content:space-between">
        <div><strong>¥{{ r.amount_wsh }}</strong> · {{ r.reason_wsh }}</div>
        <span :class="['badge', r.status_wsh === 'approved' ? 'badge-success' : r.status_wsh === 'rejected' ? 'badge-danger' : 'badge-warning']">
          {{ statusMap[r.status_wsh] || r.status_wsh }}
        </span>
      </div>
      <div style="font-size:13px;color:var(--color-muted-foreground);margin-top:4px">订单 #{{ r.order_id_wsh }} · {{ new Date(r.created_at_wsh).toLocaleDateString() }}</div>
    </div>

    <div v-if="showForm" class="modal-overlay" @mousedown.self="showForm = false">
      <div class="modal">
        <h2>申请退款</h2>
        <form @submit.prevent="submitRefund">
          <div class="form-group">
            <label>订单号</label>
            <input v-model="form.order_id_wsh" type="number" required placeholder="请输入订单号">
          </div>
          <div class="form-group">
            <label>退款原因</label>
            <textarea v-model="form.reason_wsh" rows="3" required placeholder="请说明退款原因"></textarea>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" @click="showForm = false">取消</button>
            <button type="submit" class="btn btn-primary btn-sm">提交申请</button>
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
const refunds = ref([])
const loading = ref(true)
const showForm = ref(false)
const form = reactive({ order_id_wsh: '', reason_wsh: '' })
const statusMap = { pending: '审核中', approved: '已通过', rejected: '已拒绝', completed: '已完成' }

function openForm() { form.order_id_wsh = ''; form.reason_wsh = ''; showForm.value = true }

onMounted(async () => {
  try { const r = await authStore.apiGet('/api/refunds'); if (r.code === 200) refunds.value = r.data }
  catch (e) {}
  finally { loading.value = false }
})

async function submitRefund() {
  try {
    const r = await authStore.apiPost('/api/refunds', form)
    if (r.code === 200) {
      appStore.addToast('退款申请已提交', 'success')
      showForm.value = false
      refunds.value.unshift(r.data)
    }
  } catch (e) { appStore.addToast('提交失败', 'error') }
}
</script>
