<template>
  <div>
    <div style="display:flex;gap:12px;flex-wrap:wrap;margin-bottom:16px">
      <div><label>目标类型</label><select v-model="targetType" class="form-control"><option value="keeper">寄养师</option><option value="merchant">商家</option></select></div>
      <div><label>目标ID</label><input v-model="targetId" class="form-control" style="width:100px"></div>
      <div style="display:flex;align-items:flex-end"><button class="btn btn-primary btn-sm" @click="loadReputation">查询</button></div>
    </div>
    <div style="margin-bottom:24px">
      <div class="card" style="padding:24px;margin-bottom:16px">
        <h3>评价统计</h3>
        <div class="stat-grid" style="grid-template-columns:repeat(2,1fr)">
          <div class="stat-card"><div class="stat-value">{{ creditInfo.totalRatings || 0 }}</div><div class="stat-label">总评价</div></div>
          <div class="stat-card"><div class="stat-value">{{ creditInfo.avgRating || 0 }}</div><div class="stat-label">平均评分</div></div>
        </div>
      </div>
    </div>

    <h2 style="margin:24px 0 12px">评价记录</h2>
    <div v-if="ratingsLoading" class="loading">加载中...</div>
    <div v-else-if="ratings.length === 0" class="empty-state"><h3>暂无评价</h3></div>
    <div v-else class="card" style="margin-bottom:12px" v-for="r in ratings" :key="r.id_wsh">
      <div style="display:flex;justify-content:space-between">
        <strong>{{ targetType === 'keeper' ? '寄养员' : '商家' }}评价</strong>
        <span :class="'badge badge-success'">评分: {{ r.score_wsh }}/5</span>
      </div>
      <p style="margin-top:8px">{{ r.content_wsh }}</p>
      <div style="font-size:12px;color:var(--color-muted-foreground)">{{ new Date(r.created_at_wsh).toLocaleString() }}</div>
    </div>

    <h2 style="margin:24px 0 12px">投诉记录</h2>
    <div v-if="complaintsLoading" class="loading">加载中...</div>
    <div v-else-if="complaints.length === 0" class="empty-state"><h3>暂无投诉记录</h3></div>
    <div v-else class="card" style="margin-bottom:12px" v-for="c in complaints" :key="c.id_wsh">
      <div style="display:flex;justify-content:space-between">
        <strong>{{ c.title_wsh }}</strong>
        <span :class="['badge', c.status_wsh === 'resolved' ? 'badge-success' : c.status_wsh === 'rejected' ? 'badge-danger' : 'badge-warning']">
          {{ c.status_wsh === 'resolved' ? '已处理' : c.status_wsh === 'rejected' ? '已驳回' : '待处理' }}
        </span>
      </div>
      <p style="margin-top:8px">{{ c.content_wsh }}</p>
      <div v-if="c.result_wsh" style="margin-top:8px;padding:8px;background:var(--color-muted);border-radius:6px">
        <strong>处理结果：</strong>{{ c.result_wsh }}
      </div>
    </div>

    <h2 style="margin:24px 0 12px">绩效评分</h2>
    <div v-if="!creditInfo.totalCompleted" class="empty-state"><h3>暂无绩效数据</h3></div>
    <div v-else class="stat-grid" style="grid-template-columns:repeat(4,1fr)">
      <div class="stat-card"><div class="stat-value">{{ creditInfo.totalCompleted || 0 }}</div><div class="stat-label">完成订单</div></div>
      <div class="stat-card"><div class="stat-value">{{ creditInfo.completionRate || 0 }}%</div><div class="stat-label">完成率</div></div>
      <div class="stat-card"><div class="stat-value">{{ creditInfo.complaintRate || 0 }}%</div><div class="stat-label">投诉率</div></div>
      <div class="stat-card"><div class="stat-value">{{ creditInfo.totalTips || 0 }}</div><div class="stat-label">获得打赏</div></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getRatings } from '@/api/rating'
import { getReputationStatistics } from '@/api/statistics'
import { getMyComplaints } from '@/api/complaint'
const creditInfo = ref({})
const ratings = ref([])
const complaints = ref([])
const ratingsLoading = ref(true)
const complaintsLoading = ref(true)
const targetType = ref('keeper')
const targetId = ref('')

async function loadReputation() {
  if (!targetId.value) return
  ratingsLoading.value = true
  creditInfo.value = {}
  try {
    const [ratingRes, statRes] = await Promise.all([
      getRatings({ targetId: targetId.value, targetType: targetType.value }),
      getReputationStatistics({ targetId: targetId.value, targetType: targetType.value })
    ])
    if (ratingRes.code === 200) ratings.value = ratingRes.data
    if (statRes.code === 200) creditInfo.value = statRes.data
  } catch (e) {}
  finally { ratingsLoading.value = false }
}

onMounted(async () => {
  ratingsLoading.value = false
  try {
    const r = await getMyComplaints()
    if (r.code === 200) complaints.value = r.data
  } catch (e) {}
  finally { complaintsLoading.value = false }
})
</script>
