<template>
  <div class="business-hours-page">
    <div class="page-head">
      <div>
        <h2>营业时间</h2>
        <p>设置店铺营业模式和每周营业时间，自动模式会按这里的时间判断营业状态。</p>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="!merchant" class="empty-state">
      当前账号尚未绑定商家资料，暂不能设置营业时间。
    </div>

    <div v-else class="settings-grid">
      <section class="panel">
        <div class="panel-head">
          <div>
            <h3>营业模式</h3>
            <p>自动模式按每周营业时间判断；手动开店/关店会直接覆盖当前状态。</p>
          </div>
          <span :class="['badge', storeStatusBadge]">{{ storeStatusLabel }}</span>
        </div>

        <div class="mode-actions">
          <button
            class="btn btn-outline btn-sm"
            :class="merchant.store_mode_wsh === 0 ? 'btn-primary' : ''"
            :disabled="modeSaving"
            @click="changeStoreMode(0)"
          >
            自动
          </button>
          <button
            class="btn btn-outline btn-sm"
            :class="merchant.store_mode_wsh === 1 ? 'btn-primary' : ''"
            :disabled="modeSaving"
            @click="changeStoreMode(1)"
          >
            开店
          </button>
          <button
            class="btn btn-outline btn-sm"
            :class="merchant.store_mode_wsh === 2 ? 'btn-primary' : ''"
            :disabled="modeSaving"
            @click="changeStoreMode(2)"
          >
            关店
          </button>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div>
            <h3>每周营业时间</h3>
            <p>勾选休息后，该日不会参与自动营业判断。</p>
          </div>
        </div>

        <div class="hours-list">
          <div v-for="row in scheduleRows" :key="row.day" class="hour-row">
            <div class="hour-day">{{ row.label }}</div>
            <label class="closed-toggle">
              <input v-model="row.form.is_closed_wsh" type="checkbox" :true-value="1" :false-value="0">
              <span>休息</span>
            </label>
            <input
              v-model="row.form.open_time_wsh"
              class="time-input"
              type="time"
              :disabled="Number(row.form.is_closed_wsh) === 1"
            >
            <span class="dash">-</span>
            <input
              v-model="row.form.close_time_wsh"
              class="time-input"
              type="time"
              :disabled="Number(row.form.is_closed_wsh) === 1"
            >
            <button
              class="btn btn-primary btn-sm"
              :disabled="savingDay === row.day"
              @click="saveDay(row.day)"
            >
              {{ savingDay === row.day ? '保存中...' : '保存' }}
            </button>
            <button
              v-if="row.form.id_wsh"
              class="btn btn-outline btn-sm"
              :disabled="deletingDay === row.day"
              @click="deleteDay(row.day)"
            >
              {{ deletingDay === row.day ? '删除中...' : '删除' }}
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { getMy, updateStoreMode, getBusinessHours, upsertBusinessHours, deleteBusinessHour } from '@/services/merchantService'

const appStore = useAppStore()
const loading = ref(true)
const modeSaving = ref(false)
const savingDay = ref(null)
const deletingDay = ref(null)
const merchant = ref(null)

const weekDays = [1, 2, 3, 4, 5, 6, 7]
const dayLabels = {
  1: '周一',
  2: '周二',
  3: '周三',
  4: '周四',
  5: '周五',
  6: '周六',
  7: '周日',
}

const scheduleMap = reactive({})

const storeStatusLabel = computed(() => Number(merchant.value?.store_status_wsh) === 1 ? '营业中' : '休息中')
const storeStatusBadge = computed(() => Number(merchant.value?.store_status_wsh) === 1 ? 'badge-success' : 'badge-secondary')

function createDefaultDay(day) {
  return {
    id_wsh: null,
    merchant_id_wsh: merchant.value?.id_wsh ?? null,
    day_of_week_wsh: day,
    open_time_wsh: '09:00',
    close_time_wsh: '18:00',
    is_closed_wsh: 0,
  }
}

weekDays.forEach(day => {
  scheduleMap[day] = createDefaultDay(day)
})

const scheduleRows = computed(() => weekDays.map(day => ({
  day,
  label: dayLabels[day],
  form: scheduleMap[day],
})))

function normalizeTime(value) {
  if (!value) return '09:00'
  const text = String(value)
  return text.length >= 5 ? text.slice(0, 5) : text
}

function toPayloadTime(value) {
  if (!value) return null
  const text = String(value)
  return text.length === 5 ? `${text}:00` : text
}

function resetSchedule() {
  weekDays.forEach(day => {
    scheduleMap[day] = createDefaultDay(day)
  })
}

function applyHours(list) {
  resetSchedule()
  ;(list || []).forEach(item => {
    const day = Number(item.day_of_week_wsh)
    if (!scheduleMap[day]) return
    scheduleMap[day] = {
      ...createDefaultDay(day),
      ...item,
      open_time_wsh: normalizeTime(item.open_time_wsh),
      close_time_wsh: normalizeTime(item.close_time_wsh),
      is_closed_wsh: Number(item.is_closed_wsh ?? 0),
    }
  })
}

async function reloadPage() {
  const current = await getMy()
  merchant.value = current || null
  if (!current?.id_wsh) {
    resetSchedule()
    return
  }
  const list = await getBusinessHours(current.id_wsh)
  applyHours(list)
}

async function loadPage() {
  loading.value = true
  try {
    await reloadPage()
  } catch (error) {
    appStore.addToast('加载营业时间失败', 'error')
  } finally {
    loading.value = false
  }
}

async function changeStoreMode(mode) {
  if (!merchant.value?.id_wsh) return
  modeSaving.value = true
  try {
    await updateStoreMode(merchant.value.id_wsh, mode)
    await reloadPage()
    appStore.addToast('营业模式已更新', 'success')
  } catch (error) {
    appStore.addToast('更新营业模式失败', 'error')
  } finally {
    modeSaving.value = false
  }
}

async function saveDay(day) {
  if (!merchant.value?.id_wsh) return
  const form = scheduleMap[day]
  if (!form) return
  savingDay.value = day
  try {
    await upsertBusinessHours(merchant.value.id_wsh, {
      day_of_week_wsh: day,
      open_time_wsh: toPayloadTime(form.open_time_wsh),
      close_time_wsh: toPayloadTime(form.close_time_wsh),
      is_closed_wsh: Number(form.is_closed_wsh ?? 0),
    })
    await reloadPage()
    appStore.addToast(`${dayLabels[day]}营业时间已保存`, 'success')
  } catch (error) {
    appStore.addToast('保存营业时间失败', 'error')
  } finally {
    savingDay.value = null
  }
}

async function deleteDay(day) {
  if (!merchant.value?.id_wsh) return
  const form = scheduleMap[day]
  if (!form?.id_wsh) return
  deletingDay.value = day
  try {
    await deleteBusinessHour(merchant.value.id_wsh, form.id_wsh)
    await reloadPage()
    appStore.addToast(`${dayLabels[day]}营业时间已删除`, 'success')
  } catch (error) {
    appStore.addToast('删除营业时间失败', 'error')
  } finally {
    deletingDay.value = null
  }
}

onMounted(loadPage)
</script>

<style scoped>
.business-hours-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-head,
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.page-head h2,
.panel h3 {
  margin: 0;
}

.page-head p,
.panel-head p {
  margin: 6px 0 0;
  color: var(--color-muted-foreground);
  line-height: 1.6;
}

.loading,
.empty-state {
  padding: 28px;
  text-align: center;
  color: var(--color-muted-foreground);
}

.settings-grid {
  display: grid;
  gap: 20px;
}

.panel {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-card);
  padding: 20px;
}

.mode-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.hours-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.hour-row {
  display: grid;
  grid-template-columns: 70px 80px 140px 12px 140px min-content min-content;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
}

.hour-day {
  font-weight: 600;
}

.closed-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-muted-foreground);
}

.time-input {
  width: 100%;
  min-width: 0;
}

.dash {
  color: var(--color-muted-foreground);
  text-align: center;
}

@media (max-width: 900px) {
  .hour-row {
    grid-template-columns: 1fr;
    align-items: stretch;
  }
}
</style>
