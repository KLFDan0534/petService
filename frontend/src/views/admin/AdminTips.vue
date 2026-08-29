<template>
  <div>
    <div class="admin-filters">
      <button class="btn btn-sm btn-primary" type="button" @click="reload">刷新</button>
    </div>

    <DataTable :columns="[
      {label:'ID',key:'id_wsh'},
      {label:'金额',key:'amount_label_wsh'},
      {label:'打赏人',key:'from_user_name_wsh'},
      {label:'收款人',key:'to_user_name_wsh'},
      {label:'关联订单',key:'order_id_wsh'},
      {label:'留言',key:'message_wsh'},
      {label:'时间',key:'created_at_wsh'}
    ]" :data="tips" />

    <div v-if="total > size" style="display:flex;justify-content:center;margin-top:16px;gap:8px;align-items:center">
      <button :disabled="currentPage <= 1" @click="changePage(currentPage - 1)" class="btn btn-sm">上一页</button>
      <span style="padding:6px 12px;font-size:14px">第 {{ currentPage }}/{{ pageCount }} 页 (共 {{ total }} 条)</span>
      <button :disabled="currentPage >= pageCount" @click="changePage(currentPage + 1)" class="btn btn-sm">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import DataTable from '@/components/common/DataTable.vue'
import { getAdminTips } from '@/api/wallet'

const tips = ref([])
const currentPage = ref(1)
const size = ref(20)
const total = ref(0)
const pageCount = computed(() => Math.ceil(total.value / size.value) || 1)

function enrich(t) {
  return {
    ...t,
    amount_label_wsh: t.amount_wsh == null ? '-' : '¥' + Number(t.amount_wsh).toFixed(2),
    from_user_name_wsh: t.from_user_name_wsh || '-',
    to_user_name_wsh: t.to_user_name_wsh || '-',
  }
}

async function load() {
  try {
    const r = await getAdminTips({ page: currentPage.value, size: size.value })
    if (r.code === 200) {
      tips.value = (r.data.list || []).map(enrich)
      total.value = r.data.total || 0
    }
  } catch (e) {}
}

function reload() { load() }
function changePage(page) { currentPage.value = page; load() }

onMounted(load)
</script>

<style scoped>
.admin-filters { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin-bottom: 14px; }
.admin-filters .form-control { width: auto; min-width: 220px; }
</style>