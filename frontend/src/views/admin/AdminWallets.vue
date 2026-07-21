<template>
  <div>
    <section class="toolbar-card">
      <div>
        <h2>钱包管理</h2>
        <p>管理员可直接设置、增加或扣减用户余额，所有操作都会进入钱包流水。</p>
      </div>
      <button class="btn btn-outline btn-sm" type="button" @click="loadWallets">刷新</button>
    </section>

    <DataTable
      :columns="[
        { label: 'ID', key: 'id_wsh' },
        { label: '用户ID', key: 'user_id_wsh' },
        { label: '余额', key: 'balance_wsh' },
        { label: '冻结金额', key: 'frozen_amount_wsh' },
        { label: '创建时间', key: 'created_at_wsh' }
      ]"
      :data="wallets"
    >
      <template #default="{ row }">
        <button class="btn btn-sm btn-primary" type="button" @click="openAdjust(row)">调整余额</button>
      </template>
    </DataTable>

    <div v-if="adjusting" class="modal-overlay" @mousedown.self="closeAdjust">
      <div class="modal adjust-modal">
        <h2>调整用户余额</h2>
        <form @submit.prevent="submitAdjust">
          <div class="form-grid">
            <label>
              用户ID
              <input v-model="adjustForm.user_id_wsh" class="form-control" disabled>
            </label>
            <label>
              操作
              <select v-model="adjustForm.mode_wsh" class="form-control">
                <option value="set">设置余额</option>
                <option value="add">增加余额</option>
                <option value="subtract">扣减余额</option>
              </select>
            </label>
            <label>
              金额
              <input v-model="adjustAmount" type="number" min="0" step="0.01" class="form-control" required>
            </label>
            <label class="form-grid__wide">
              备注
              <textarea v-model="adjustForm.remark_wsh" class="form-control" rows="3" placeholder="例如：线下充值、人工修正、活动补贴"></textarea>
            </label>
          </div>
          <div class="modal-actions">
            <button class="btn btn-secondary btn-sm" type="button" @click="closeAdjust">取消</button>
            <button class="btn btn-primary btn-sm" type="submit" :disabled="submitting">确认</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { adjustWallet, getWallets } from '@/api/wallet'
import DataTable from '@/components/common/DataTable.vue'

const appStore = useAppStore()
const wallets = ref([])
const adjusting = ref(false)
const submitting = ref(false)
const adjustAmount = ref('')
const adjustForm = reactive({
  user_id_wsh: '',
  mode_wsh: 'set',
  remark_wsh: '',
})

const currentWallet = computed(() => wallets.value.find(item => Number(item.user_id_wsh) === Number(adjustForm.user_id_wsh)))

onMounted(loadWallets)

async function loadWallets() {
  try {
    const res = await getWallets()
    if (res.code === 200) wallets.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    appStore.addToast(e?.message || '钱包加载失败', 'error')
  }
}

function openAdjust(row) {
  adjustForm.user_id_wsh = row.user_id_wsh
  adjustForm.mode_wsh = 'set'
  adjustForm.remark_wsh = ''
  adjustAmount.value = row.balance_wsh ?? '0.00'
  adjusting.value = true
}

function closeAdjust() {
  adjusting.value = false
}

async function submitAdjust() {
  const amount = Number(adjustAmount.value)
  if (!Number.isFinite(amount) || amount < 0) {
    appStore.addToast('请输入有效金额', 'warning')
    return
  }
  const payload = {
    user_id_wsh: Number(adjustForm.user_id_wsh),
    mode_wsh: adjustForm.mode_wsh,
    remark_wsh: adjustForm.remark_wsh,
  }
  if (adjustForm.mode_wsh === 'set') payload.balance_wsh = amount
  else payload.amount_wsh = amount

  if (adjustForm.mode_wsh === 'set' && currentWallet.value && amount < Number(currentWallet.value.frozen_amount_wsh || 0)) {
    appStore.addToast('余额不能低于冻结金额', 'warning')
    return
  }

  submitting.value = true
  try {
    const res = await adjustWallet(payload)
    if (res.code === 200) {
      appStore.addToast('余额已调整', 'success')
      closeAdjust()
      await loadWallets()
    }
  } catch (e) {
    appStore.addToast(e?.message || '调整失败', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.toolbar-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-card);
}
.toolbar-card h2 {
  margin: 0 0 4px;
  font-size: 20px;
}
.toolbar-card p {
  margin: 0;
  color: var(--color-muted-foreground);
  font-size: 13px;
}
.adjust-modal {
  max-width: 560px;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-top: 16px;
}
.form-grid label {
  display: grid;
  gap: 6px;
  color: var(--color-muted-foreground);
  font-size: 13px;
}
.form-grid__wide {
  grid-column: 1 / -1;
}
</style>
