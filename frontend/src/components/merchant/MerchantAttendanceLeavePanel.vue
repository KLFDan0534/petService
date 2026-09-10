<template>
  <section class="merchant-attendance">
    <div class="panel-header">
      <div>
        <h2>寄养员考勤</h2>
        <p>今日上班与休假安排</p>
      </div>
      <button class="btn btn-outline btn-sm" :disabled="loading" @click="loadData">刷新</button>
    </div>

    <div class="summary-grid">
      <div class="summary-item">
        <span>今日打卡</span>
        <strong>{{ attendanceRecords.length }}</strong>
      </div>
      <div class="summary-item">
        <span>上班中</span>
        <strong>{{ activeCount }}</strong>
      </div>
      <div class="summary-item">
        <span>休假记录</span>
        <strong>{{ leaves.length }}</strong>
      </div>
    </div>

    <form class="leave-form" @submit.prevent="submitLeave">
      <select v-model="leaveForm.keeper_id_wsh" class="form-control" required>
        <option value="">选择寄养员</option>
        <option v-for="keeper in availableKeepers" :key="keeper.id_wsh" :value="keeper.id_wsh">
          {{ keeper.name_wsh || `#${keeper.id_wsh}` }}
        </option>
      </select>
      <input v-model="leaveForm.start_date_wsh" class="form-control" type="date" required>
      <input v-model="leaveForm.end_date_wsh" class="form-control" type="date" required>
      <input v-model.trim="leaveForm.reason_wsh" class="form-control" placeholder="原因">
      <button class="btn btn-primary btn-sm" :disabled="submitting" type="submit">设置休假</button>
    </form>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>寄养员</th>
            <th>上班</th>
            <th>下班</th>
            <th>距离</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="attendanceRecords.length === 0">
            <td colspan="4" class="empty-cell">今日暂无打卡</td>
          </tr>
          <tr v-for="item in attendanceRecords" :key="item.id_wsh">
            <td>{{ item.keeper_name_wsh || `#${item.keeper_id_wsh}` }}</td>
            <td>{{ formatDateTime(item.check_in_at_wsh) }}</td>
            <td>{{ item.check_out_at_wsh ? formatDateTime(item.check_out_at_wsh) : '上班中' }}</td>
            <td>{{ formatMeters(item.check_in_distance_wsh) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="leave-list">
      <div v-if="leaves.length === 0" class="empty-inline">暂无休假安排</div>
      <div v-for="leave in leaves" :key="leave.id_wsh" class="leave-row">
        <div>
          <strong>{{ leave.keeper_name_wsh || `#${leave.keeper_id_wsh}` }}</strong>
          <span>{{ leave.start_date_wsh }} 至 {{ leave.end_date_wsh }}</span>
          <small v-if="leave.reason_wsh">{{ leave.reason_wsh }}</small>
        </div>
        <button class="btn btn-outline btn-sm" :disabled="deletingId === leave.id_wsh" @click="removeLeave(leave.id_wsh)">删除</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { formatDateTime as utilFormatDateTime } from '@/utils/format'
import { createMerchantLeave, deleteMerchantLeave, getMerchantLeaves, getMerchantTodayAttendance } from '@/api/attendance'

const props = defineProps({
  keepers: {
    type: Array,
    default: () => [],
  },
})

const appStore = useAppStore()
const attendanceRecords = ref([])
const leaves = ref([])
const loading = ref(false)
const submitting = ref(false)
const deletingId = ref(null)

const leaveForm = reactive({
  keeper_id_wsh: '',
  start_date_wsh: '',
  end_date_wsh: '',
  reason_wsh: '',
})

const activeCount = computed(() => attendanceRecords.value.filter(item => !item.check_out_at_wsh).length)
const availableKeepers = computed(() =>
  props.keepers.filter(item => [1, 3, 4].includes(Number(item.status_wsh)))
)

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const [attendanceRes, leaveRes] = await Promise.all([
      getMerchantTodayAttendance(),
      getMerchantLeaves(),
    ])
    if (attendanceRes.code === 200) attendanceRecords.value = attendanceRes.data || []
    if (leaveRes.code === 200) leaves.value = leaveRes.data || []
  } catch (error) {
    appStore.addToast(error.message || '考勤休假数据加载失败', 'warning')
  } finally {
    loading.value = false
  }
}

async function submitLeave() {
  if (submitting.value) return
  submitting.value = true
  try {
    const res = await createMerchantLeave({
      keeper_id_wsh: Number(leaveForm.keeper_id_wsh),
      start_date_wsh: leaveForm.start_date_wsh,
      end_date_wsh: leaveForm.end_date_wsh,
      reason_wsh: leaveForm.reason_wsh,
    })
    if (res.code === 200) {
      appStore.addToast('休假已设置', 'success')
      leaveForm.keeper_id_wsh = ''
      leaveForm.start_date_wsh = ''
      leaveForm.end_date_wsh = ''
      leaveForm.reason_wsh = ''
      await loadData()
    }
  } catch (error) {
    appStore.addToast(error.message || '休假设置失败', 'warning')
  } finally {
    submitting.value = false
  }
}

async function removeLeave(id) {
  if (deletingId.value) return
  deletingId.value = id
  try {
    const res = await deleteMerchantLeave(id)
    if (res.code === 200) {
      appStore.addToast('休假已删除', 'success')
      await loadData()
    }
  } catch (error) {
    appStore.addToast(error.message || '休假删除失败', 'warning')
  } finally {
    deletingId.value = null
  }
}

function formatDateTime(value) {
  return utilFormatDateTime(value, { fallback: '-' })
}

function formatMeters(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) return '-'
  return `${Math.round(number)}m`
}
</script>

<style scoped>
.merchant-attendance {
  display: grid;
  gap: 16px;
  margin-bottom: 24px;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.panel-header h2 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2329;
}
.panel-header p {
  margin: 0;
  font-size: 13px;
  color: #8f959e;
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}
.summary-item {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid #dee0e3;
  border-radius: var(--radius-inline);
  background: #fff;
}
.summary-item span {
  font-size: 12px;
  color: #8f959e;
}
.summary-item strong {
  font-size: 20px;
  color: #1f2329;
}
.leave-form {
  display: grid;
  grid-template-columns: minmax(140px, 1.2fr) 140px 140px minmax(120px, 1fr) auto;
  gap: 8px;
}
.table-wrap {
  overflow-x: auto;
  border: 1px solid #dee0e3;
  border-radius: var(--radius-inline);
  background: #fff;
}
table {
  width: 100%;
  border-collapse: collapse;
  min-width: 560px;
}
th,
td {
  padding: 10px 12px;
  border-bottom: 1px solid #eff0f2;
  text-align: left;
  font-size: 13px;
}
th {
  color: #646a73;
  background: #fafafa;
  font-weight: 600;
}
.empty-cell,
.empty-inline {
  color: #8f959e;
  text-align: center;
}
.leave-list {
  display: grid;
  gap: 8px;
}
.leave-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid #dee0e3;
  border-radius: var(--radius-inline);
  background: #fff;
}
.leave-row div {
  display: grid;
  gap: 3px;
}
.leave-row strong {
  font-size: 14px;
  color: #1f2329;
}
.leave-row span,
.leave-row small {
  font-size: 12px;
  color: #646a73;
}
@media (max-width: 900px) {
  .summary-grid,
  .leave-form {
    grid-template-columns: 1fr;
  }
  .panel-header,
  .leave-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
