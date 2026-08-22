<template>
  <div>
    <section class="toolbar-card">
      <div>
        <h2>钱包管理</h2>
        <p>管理员可直接设置、增加或扣减用户余额，所有操作都会进入钱包流水。</p>
      </div>
      <button class="btn btn-outline btn-sm" type="button" @click="loadWallets">刷新</button>
    </section>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th v-for="col in sortableColumns" :key="col.key" @click="toggleSort(col.key)">
              <span class="th-content">
                {{ col.label }}
                <span class="sort-indicator" :class="sortIndicatorClass(col.key)" aria-hidden="true"></span>
              </span>
            </th>
            <th>冻结金额</th>
            <th>用户名</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, i) in wallets" :key="i">
            <td>{{ row.id_wsh }}</td>
            <td>{{ row.user_id_wsh }}</td>
            <td>{{ formatMoney(row.balance_wsh) }}</td>
            <td>{{ formatMoney(row.frozen_amount_wsh) }}</td>
            <td>{{ formatTime(row.created_at_wsh) }}</td>
            <td>{{ row.username_wsh || '-' }}</td>
            <td>
              <button class="btn btn-sm btn-primary" type="button" @click="openAdjust(row)">调整余额</button>
            </td>
          </tr>
          <tr v-if="wallets.length === 0">
            <td :colspan="sortableColumns.length + 3" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pagination-bar">
      <span style="font-size:13px;color:var(--color-muted-foreground)">共 {{ total }} 条</span>
      <div style="display:flex;gap:8px">
        <button class="btn btn-sm" :disabled="page <= 1" @click="page--; loadWallets()">上一页</button>
        <span style="line-height:32px">第 {{ page }} / {{ pages }} 页</span>
        <button class="btn btn-sm" :disabled="page >= pages" @click="page++; loadWallets()">下一页</button>
      </div>
    </div>

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

const appStore = useAppStore()
const wallets = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const pages = ref(0)
const sortKey = ref('')
const sortOrder = ref('')
const adjusting = ref(false)
const submitting = ref(false)
const adjustAmount = ref('')
const adjustForm = reactive({
  user_id_wsh: '',
  mode_wsh: 'set',
  remark_wsh: '',
})

const sortableColumns = [
  { label: 'ID', key: 'id' },
  { label: '用户ID', key: 'user_id' },
  { label: '余额', key: 'balance' },
  { label: '创建时间', key: 'created' },
]

const currentWallet = computed(() => wallets.value.find(item => Number(item.user_id_wsh) === Number(adjustForm.user_id_wsh)))

onMounted(loadWallets)

async function loadWallets() {
  try {
    const params = { page: page.value, size: size.value }
    if (sortKey.value) {
      params.sort_by_wsh = sortKey.value
      params.order_wsh = sortOrder.value
    }
    const res = await getWallets(params)
    if (res.code === 200) {
      wallets.value = Array.isArray(res.data?.list) ? res.data.list : []
      total.value = res.data?.total || 0
      pages.value = res.data?.pages || 0
    }
  } catch (e) {
    appStore.addToast(e?.message || '钱包加载失败', 'error')
  }
}

function toggleSort(key) {
  if (sortKey.value === key) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortOrder.value = key === 'balance' ? 'desc' : 'asc'
  }
  page.value = 1
  loadWallets()
}

function sortIndicatorClass(key) {
  if (sortKey.value !== key) return 'sort-idle'
  return sortOrder.value === 'asc' ? 'sort-asc' : 'sort-desc'
}

function formatMoney(v) {
  if (v == null) return '0.00'
  return Number(v).toFixed(2)
}

function formatTime(v) {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
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
.table-wrap {
  overflow-x: auto;
}
.table-wrap table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.table-wrap th {
  text-align: left;
  padding: 8px 12px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-muted);
  color: var(--color-foreground);
  font-size: 12px;
  white-space: nowrap;
}
.table-wrap th {
  user-select: none;
}
.th-content {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}
.sort-indicator {
  display: inline-block;
  width: 0;
  height: 0;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  opacity: 0.35;
}
.sort-indicator.sort-asc {
  border-bottom: 5px solid var(--color-primary);
  opacity: 1;
}
.sort-indicator.sort-desc {
  border-top: 5px solid var(--color-primary);
  opacity: 1;
}
.sort-indicator.sort-idle {
  border-top: 5px solid var(--color-muted-foreground);
}
.table-wrap td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--color-border);
}
.table-wrap tr:hover {
  background: var(--color-muted);
}
.empty {
  color: var(--color-muted-foreground);
  padding: 32px;
  text-align: center;
}
.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
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