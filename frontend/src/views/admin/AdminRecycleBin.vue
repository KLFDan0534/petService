<template>
  <div>
    <div style="display:flex;gap:12px;margin-bottom:16px;align-items:center">
      <select v-model="selectedTable" class="input" style="width:200px">
        <option value="">请选择表</option>
        <option v-for="t in tables" :key="t" :value="t">{{ t }}</option>
      </select>
      <button class="btn btn-primary btn-sm" :disabled="!selectedTable" @click="loadDeleted">查询已删除记录</button>
    </div>

    <div v-if="records.length > 0" style="overflow-x:auto">
      <table class="table">
        <thead>
          <tr>
            <th>编号</th>
            <th v-for="(v,k) in sample" :key="k">{{ k }}</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in records" :key="r.id_wsh || r.id_wsh">
            <td>{{ r.id_wsh || r.id_wsh }}</td>
            <td v-for="(v,k) in sample" :key="k" style="max-width:150px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ formatValue(r[k]) }}</td>
            <td>
              <button class="btn btn-sm btn-success" @click="restore(r.id_wsh || r.id_wsh)">恢复</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-else-if="searched" style="text-align:center;padding:32px;color:var(--color-muted-foreground)">没有已删除的记录</div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useAppStore } from '@/stores/app'
import { getRecycleBinTables, getRecycleBin, restoreFromRecycleBin } from '@/api/admin'


const appStore = useAppStore()
const tables = ref([])
const selectedTable = ref('')
const records = ref([])
const searched = ref(false)

const sample = computed(() => {
  if (records.value.length === 0) return {}
  const keys = Object.keys(records.value[0])
  return keys.slice(0, 8).reduce((acc, k) => { acc[k] = records.value[0][k]; return acc }, {})
})

onMounted(async () => {
  try { const r = await getRecycleBinTables(); if (r.code === 200) tables.value = r.data || [] }
  catch (e) {}
})

async function loadDeleted() {
  if (!selectedTable.value) return
  searched.value = true
  try { const r = await getRecycleBin({ table: selectedTable.value }); if (r.code === 200) records.value = r.data || [] }
  catch (e) { records.value = [] }
}

async function restore(id) {
  try { await restoreFromRecycleBin(selectedTable.value, id); appStore.addToast('恢复成功', 'success'); loadDeleted() }
  catch (e) { appStore.addToast('恢复失败', 'error') }
}

function formatValue(v) {
  if (v === null || v === undefined) return '-'
  if (typeof v === 'string' && v.length > 40) return v.slice(0, 40) + '...'
  return String(v)
}
</script>

<style scoped>
.table { width:100%; border-collapse:collapse; font-size:13px }
.table th, .table td { padding:8px 10px; border-bottom:1px solid var(--color-border); text-align:left }
.table th { background:var(--color-muted); font-weight:600; color:var(--color-muted-foreground) }
.input { padding:6px 10px; border:1px solid var(--color-border); border-radius:var(--radius-inline); background:var(--color-background); color:var(--color-foreground); font-size:13px }
</style>
