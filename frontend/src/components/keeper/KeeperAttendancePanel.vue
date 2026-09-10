<template>
  <section class="attendance-panel">
    <div class="attendance-main">
      <div>
        <h3>今日考勤</h3>
        <p v-if="currentAttendance" class="attendance-meta">
          已上班 {{ formatDateTime(currentAttendance.check_in_at_wsh) }}
          <span v-if="currentAttendance.check_in_distance_wsh != null">
            · 距店 {{ formatMeters(currentAttendance.check_in_distance_wsh) }}
          </span>
        </p>
        <p v-else class="attendance-meta">当前未上班</p>
      </div>
      <button
        class="btn btn-primary btn-sm"
        :disabled="submitting || loading"
        @click="currentAttendance ? submitCheckOut() : submitCheckIn()"
      >
        <span>{{ currentAttendance ? '下班打卡' : '上班打卡' }}</span>
      </button>
    </div>

    <div v-if="currentAttendance?.check_in_address_wsh" class="attendance-address">
      {{ currentAttendance.check_in_address_wsh }}
    </div>

    <div v-if="records.length" class="attendance-records">
      <div v-for="item in records" :key="item.id_wsh" class="attendance-record">
        <span>{{ formatDateTime(item.check_in_at_wsh) }}</span>
        <span>{{ item.check_out_at_wsh ? formatDateTime(item.check_out_at_wsh) : '上班中' }}</span>
        <span>{{ formatMeters(item.check_in_distance_wsh) }}</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { getCurrentAddress } from '@/composables/useAmapLocation'
import { formatDateTime as utilFormatDateTime } from '@/utils/format'
import { checkIn, checkOut, getCurrentAttendance, getTodayAttendance } from '@/api/attendance'

const appStore = useAppStore()

const currentAttendance = ref(null)
const records = ref([])
const loading = ref(false)
const submitting = ref(false)

onMounted(loadAttendance)

async function loadAttendance() {
  loading.value = true
  try {
    const [currentRes, todayRes] = await Promise.all([
      getCurrentAttendance(),
      getTodayAttendance(),
    ])
    if (currentRes.code === 200) currentAttendance.value = currentRes.data || null
    if (todayRes.code === 200) records.value = todayRes.data || []
  } catch (error) {
    appStore.addToast(error.message || '考勤记录加载失败', 'warning')
  } finally {
    loading.value = false
  }
}

async function submitCheckIn() {
  await submitAttendance(checkIn, '上班打卡成功')
}

async function submitCheckOut() {
  await submitAttendance(checkOut, '下班打卡成功')
}

async function submitAttendance(action, successText) {
  if (submitting.value) return
  submitting.value = true
  try {
    const location = await getCurrentAddress()
    const res = await action({
      latitude_wsh: location.latitude_wsh,
      longitude_wsh: location.longitude_wsh,
      address_wsh: location.address_wsh,
      accuracy_wsh: location.accuracy_wsh,
      location_source_wsh: location.location_source_wsh,
    })
    if (res.code === 200) {
      appStore.addToast(successText, 'success')
      await loadAttendance()
    }
  } catch (error) {
    appStore.addToast(error.message || '打卡失败，请确认已允许定位', 'warning')
  } finally {
    submitting.value = false
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
.attendance-panel {
  display: grid;
  gap: 10px;
  padding: 14px 16px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #dee0e3;
  border-radius: var(--radius-inline);
}
.attendance-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.attendance-main h3 {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}
.attendance-meta,
.attendance-address {
  margin: 0;
  font-size: 12px;
  color: #646a73;
}
.attendance-records {
  display: grid;
  gap: 6px;
  padding-top: 8px;
  border-top: 1px solid #eff0f2;
}
.attendance-record {
  display: grid;
  grid-template-columns: 1fr 1fr 72px;
  gap: 8px;
  font-size: 12px;
  color: #646a73;
}
@media (max-width: 640px) {
  .attendance-main {
    align-items: flex-start;
    flex-direction: column;
  }
  .attendance-record {
    grid-template-columns: 1fr;
  }
}
</style>
