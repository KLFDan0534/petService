<template>
  <div>
    <PageHero title="退款管理" subtitle="查看退款申请和进度" />
    <div class="refund-actions">
      <button class="btn btn-primary" type="button" @click="openForm">+ 申请退款</button>
    </div>
    <div v-if="loading" class="loading">加载中...</div>
    <EmptyState v-else-if="refunds.length === 0" title="暂无退款申请" icon="💳">
      <router-link to="/orders" class="btn btn-primary">查看订单</router-link>
    </EmptyState>
    <div v-else class="refund-list">
      <article v-for="r in refunds" :key="r.id_wsh" class="card refund-card">
        <div class="refund-card-head">
          <div class="refund-main">
            <strong>¥{{ r.amount_wsh }}</strong>
            <span class="refund-reason">{{ r.reason_wsh }}</span>
          </div>
          <span :class="['badge', r.status_wsh === 'approved' ? 'badge-success' : r.status_wsh === 'rejected' ? 'badge-danger' : 'badge-warning']">
            {{ statusMap[r.status_wsh] || r.status_wsh }}
          </span>
        </div>
        <div class="refund-meta">订单 #{{ r.order_id_wsh }} · {{ new Date(r.created_at_wsh).toLocaleDateString() }}</div>
      </article>
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
import { getMyRefunds, createRefund } from '@/api/refund'
import { RefundStatus, getStatusLabel } from '@/constants/statusMaps'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const appStore = useAppStore()
const refunds = ref([])
const loading = ref(true)
const showForm = ref(false)
const form = reactive({ order_id_wsh: '', reason_wsh: '' })
const statusMap = Object.fromEntries(Object.entries(RefundStatus).map(([k, v]) => [k, v.label]))
function statusLabel(s) { return getStatusLabel(RefundStatus, s) }

function openForm() { form.order_id_wsh = ''; form.reason_wsh = ''; showForm.value = true }

onMounted(async () => {
  try { const r = await getMyRefunds(); if (r.code === 200) refunds.value = r.data }
  catch (e) {}
  finally { loading.value = false }
})

async function submitRefund() {
  try {
    const r = await createRefund(form)
    if (r.code === 200) {
      appStore.addToast('退款申请已提交', 'success')
      showForm.value = false
      refunds.value.unshift(r.data)
    }
  } catch (e) { appStore.addToast('提交失败', 'error') }
}
</script>

<style scoped>
.refund-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
}

.refund-list {
  display: grid;
  gap: 12px;
}

.refund-card {
  min-width: 0;
}

.refund-card-head {
  align-items: flex-start;
  display: flex;
  gap: 12px;
  justify-content: space-between;
  min-width: 0;
}

.refund-main {
  align-items: baseline;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.refund-reason {
  overflow-wrap: anywhere;
}

.refund-meta {
  color: var(--color-muted-foreground);
  font-size: 13px;
  margin-top: 6px;
  overflow-wrap: anywhere;
}

@media (max-width: 520px) {
  .refund-actions .btn {
    width: 100%;
  }

  .refund-card-head {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
