<template>
  <div class="timeline-tab">
    <section v-if="dailyStatus" class="card daily-card">
      <div class="card-title">
        <span>每日照护上传</span>
        <strong>{{ uploadedCount }}/{{ requiredCount }} 天</strong>
      </div>
      <div class="progress-track">
        <div class="progress-bar" :style="{ width: `${uploadPercent}%` }"></div>
      </div>
      <div v-if="missingDays.length" class="missing-days">
        <span>缺少</span>
        <em v-for="day in missingDays" :key="day">{{ day }}</em>
      </div>
    </section>

    <div v-if="items.length === 0 && !loading" class="empty-state compact-empty">
      <h3>暂无服务动态</h3>
      <p>看护人上传照护记录后，会显示在这里。</p>
    </div>

    <div v-else class="timeline-list">
      <article v-for="item in items" :key="item.id_wsh" class="timeline-item card">
        <div class="timeline-dot"></div>
        <div class="timeline-content">
          <div class="timeline-header">
            <span class="badge badge-info">{{ recordTypeLabel(item.type_wsh) }}</span>
            <time>{{ formatTime(item.record_time_wsh || item.created_at_wsh) }}</time>
          </div>
          <p v-if="item.content_wsh">{{ item.content_wsh }}</p>
          <div v-if="parseImages(item.images_wsh).length" class="photo-grid">
            <img
              v-for="(url, index) in parseImages(item.images_wsh)"
              :key="`${url}-${index}`"
              :src="url"
              alt="照护照片"
            >
          </div>
        </div>
      </article>
    </div>

    <div v-if="hasMore" class="load-more">
      <button class="btn btn-outline btn-sm" type="button" :disabled="loading" @click="$emit('load-more')">
        {{ loading ? '加载中...' : '加载更多' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  dailyStatus: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  hasMore: { type: Boolean, default: false },
})
defineEmits(['load-more'])

const uploadedCount = computed(() => props.dailyStatus?.uploaded_days_wsh?.length || 0)
const requiredCount = computed(() => props.dailyStatus?.required_days_wsh?.length || 0)
const uploadPercent = computed(() => requiredCount.value > 0 ? Math.round((uploadedCount.value / requiredCount.value) * 100) : 0)
const missingDays = computed(() => props.dailyStatus?.missing_days_wsh || [])

const recordTypeMap = {
  feed: '喂食',
  activity: '活动',
  medication: '用药',
  health: '健康',
  note: '备注',
  check_in: '签到',
  photo: '照片',
}

function recordTypeLabel(type) {
  return recordTypeMap[type] || type || '动态'
}

function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString()
}

function parseImages(value) {
  return String(value || '').split(',').map(url => url.trim()).filter(Boolean)
}
</script>

<style scoped>
.timeline-tab {
  display: grid;
  gap: 16px;
}
.card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}
.daily-card {
  padding: 18px;
}
.card-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
  margin-bottom: 12px;
}
.card-title span {
  color: var(--color-muted-foreground);
  font-size: 12px;
}
.card-title strong {
  color: var(--color-primary);
}
.progress-track {
  height: 10px;
  border-radius: 999px;
  background: var(--color-muted);
  overflow: hidden;
}
.progress-bar {
  height: 100%;
  border-radius: inherit;
  background: var(--color-primary);
  transition: width var(--transition-normal);
}
.missing-days {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
  font-size: 12px;
  color: var(--color-muted-foreground);
}
.missing-days em {
  font-style: normal;
  color: var(--color-destructive);
  background: #fee2e2;
  border-radius: var(--radius-full);
  padding: 2px 8px;
}
.compact-empty {
  padding: 36px 20px;
}
.timeline-list {
  display: grid;
  gap: 12px;
}
.timeline-item {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 12px;
  padding: 16px;
}
.timeline-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--color-primary);
  margin-top: 7px;
  box-shadow: 0 0 0 4px rgba(249,115,22,.16);
}
.timeline-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
}
.timeline-header time {
  color: var(--color-muted-foreground);
  font-size: 12px;
}
.timeline-content p {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}
.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(88px, 1fr));
  gap: 8px;
  margin-top: 10px;
  max-width: 460px;
}
.photo-grid img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-muted);
}
.load-more {
  text-align: center;
}
</style>
