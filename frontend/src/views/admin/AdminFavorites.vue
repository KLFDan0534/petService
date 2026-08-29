<template>
  <div>
    <div class="admin-filters">
      <select v-model="filters.target_type_wsh" class="form-control" @change="applyFilters">
        <option value="">全部类型</option>
        <option value="merchant">商家</option>
        <option value="keeper">寄养员</option>
        <option value="service">服务</option>
      </select>
      <button class="btn btn-sm btn-primary" type="button" @click="applyFilters">筛选</button>
      <button class="btn btn-sm" type="button" @click="resetFilters">重置</button>
    </div>

    <DataTable :columns="[
      {label:'ID',key:'id_wsh'},
      {label:'用户',key:'user_name_wsh'},
      {label:'收藏类型',key:'type_label_wsh'},
      {label:'目标ID',key:'target_id_wsh'},
      {label:'收藏时间',key:'created_at_wsh'}
    ]" :data="favorites" />

    <div v-if="total > size" style="display:flex;justify-content:center;margin-top:16px;gap:8px;align-items:center">
      <button :disabled="currentPage <= 1" @click="changePage(currentPage - 1)" class="btn btn-sm">上一页</button>
      <span style="padding:6px 12px;font-size:14px">第 {{ currentPage }}/{{ pageCount }} 页 (共 {{ total }} 条)</span>
      <button :disabled="currentPage >= pageCount" @click="changePage(currentPage + 1)" class="btn btn-sm">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import DataTable from '@/components/common/DataTable.vue'
import { getAdminFavorites } from '@/api/favorite'
import { FavoriteTargetTypeMap, enrichWithStatus } from '@/constants/statusMaps'

const favorites = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)
const filters = reactive({ target_type_wsh: '' })
const pageCount = computed(() => Math.ceil(total.value / size.value) || 1)

function enrich(f) {
  const e = enrichWithStatus(f, 'target_type_wsh', FavoriteTargetTypeMap, 'type_label_wsh')
  e.user_name_wsh = e.user_name_wsh || '-'
  return e
}

function buildParams() {
  const params = { page: currentPage.value, size: size.value }
  if (filters.target_type_wsh) params.target_type_wsh = filters.target_type_wsh
  return params
}

async function load() {
  try {
    const r = await getAdminFavorites(buildParams())
    if (r.code === 200) {
      favorites.value = (r.data.list || []).map(enrich)
      total.value = r.data.total || 0
    }
  } catch (e) {}
}

function applyFilters() { currentPage.value = 1; load() }
function resetFilters() { filters.target_type_wsh = ''; applyFilters() }
function changePage(page) { currentPage.value = page; load() }

onMounted(load)
</script>

<style scoped>
.admin-filters { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin-bottom: 14px; }
.admin-filters .form-control { width: auto; min-width: 120px; }
</style>