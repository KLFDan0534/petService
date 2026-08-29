<template>
  <div class="cs-apply-page">
    <section class="panel">
      <div class="section-header">
        <div>
          <h1>申请成为客服</h1>
          <p>选择要服务的商家，提交后由商家负责人审核。</p>
        </div>
      </div>

      <form class="apply-form" @submit.prevent="submitApplication">
        <div class="form-group">
          <label>服务商家</label>
          <select v-model="form.merchant_id_wsh" required>
            <option value="" disabled>请选择商家</option>
            <option v-for="merchant in merchants" :key="merchant.id_wsh" :value="merchant.id_wsh">
              {{ merchant.name_wsh || merchant.username_wsh || `商家 #${merchant.id_wsh}` }}
            </option>
          </select>
        </div>

        <div class="form-group">
          <label>申请说明</label>
          <textarea
            v-model="form.applicant_note_wsh"
            rows="4"
            maxlength="500"
            placeholder="简单说明你的客服经验、可服务时间或沟通优势"
          />
        </div>

        <div class="actions">
          <button class="btn btn-primary" type="submit" :disabled="submitting || !form.merchant_id_wsh">
            {{ submitting ? '提交中...' : '提交申请' }}
          </button>
        </div>
      </form>
    </section>

    <section class="panel">
      <div class="section-header">
        <div>
          <h2>我的申请</h2>
          <p>客服权限由通过审核的商家关系决定。</p>
        </div>
        <button class="btn btn-outline btn-sm" type="button" @click="loadApplications">刷新</button>
      </div>

      <DataTable :columns="columns" :data="applications">
        <template #default="{ row }">
          <span :class="['badge', statusBadge(row.status_wsh)]">{{ statusLabel(row.status_wsh) }}</span>
        </template>
      </DataTable>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import DataTable from '@/components/common/DataTable.vue'
import { getMerchants } from '@/api/merchant'
import {
  applyMerchantCustomerService,
  getMyCustomerServiceApplications,
} from '@/api/merchantCustomerService'

const appStore = useAppStore()
const merchants = ref([])
const applications = ref([])
const submitting = ref(false)

const form = reactive({
  merchant_id_wsh: '',
  applicant_note_wsh: '',
})

const columns = [
  { label: '商家', key: 'merchant_name_wsh' },
  { label: '申请说明', key: 'applicant_note_wsh' },
  { label: '审核意见', key: 'review_note_wsh' },
  { label: '申请时间', key: 'created_at_wsh' },
]

async function loadMerchants() {
  const response = await getMerchants()
  if (response.code === 200) {
    merchants.value = response.data || []
  }
}

async function loadApplications() {
  const response = await getMyCustomerServiceApplications()
  if (response.code === 200) {
    applications.value = (response.data || []).map(item => ({
      ...item,
      merchant_name_wsh: item.merchant_name_wsh || `商家 #${item.merchant_id_wsh}`,
      applicant_note_wsh: item.applicant_note_wsh || '-',
      review_note_wsh: item.review_note_wsh || '-',
    }))
  }
}

async function submitApplication() {
  if (submitting.value) return
  submitting.value = true
  try {
    const response = await applyMerchantCustomerService({
      merchant_id_wsh: Number(form.merchant_id_wsh),
      applicant_note_wsh: form.applicant_note_wsh.trim(),
    })
    if (response.code === 200) {
      appStore.addToast('申请已提交，请等待商家审核', 'success')
      form.merchant_id_wsh = ''
      form.applicant_note_wsh = ''
      await loadApplications()
    }
  } catch {
    appStore.addToast('提交申请失败', 'error')
  } finally {
    submitting.value = false
  }
}

function statusLabel(status) {
  return {
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝',
  }[status] || status || '-'
}

function statusBadge(status) {
  return {
    pending: 'badge-warning',
    approved: 'badge-success',
    rejected: 'badge-danger',
  }[status] || 'badge-info'
}

onMounted(async () => {
  await Promise.all([loadMerchants(), loadApplications()])
})
</script>

<style scoped>
.cs-apply-page {
  display: grid;
  gap: 20px;
}
.panel {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 24px;
  box-shadow: var(--shadow-sm);
}
.section-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}
.section-header h1,
.section-header h2 {
  font-size: 20px;
  line-height: 1.3;
  margin: 0 0 4px;
}
.section-header p {
  color: var(--color-muted-foreground);
  font-size: 13px;
  margin: 0;
}
.apply-form {
  display: grid;
  gap: 16px;
  max-width: 680px;
}
.actions {
  display: flex;
  justify-content: flex-end;
}
@media (max-width: 700px) {
  .section-header {
    flex-direction: column;
  }
  .panel {
    padding: 18px;
  }
}
</style>
