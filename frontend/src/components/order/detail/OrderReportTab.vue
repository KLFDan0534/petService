<template>
  <div class="report-tab">
    <div v-if="loading" class="loading compact-loading">加载中...</div>

    <div v-else-if="reports.length === 0" class="empty-state compact-empty">
      <h3>暂无AI报告</h3>
      <p>服务中或完成后的订单可以生成寄养报告。</p>
      <button v-if="canGenerate" class="btn btn-primary btn-sm" type="button" @click="handleGenerate">
        生成AI报告
      </button>
    </div>

    <template v-else>
      <div class="report-actions">
        <button class="btn btn-primary btn-sm" type="button" @click="handleGenerate">重新生成</button>
        <button class="btn btn-outline btn-sm" type="button" @click="handleCopy">复制内容</button>
        <button class="btn btn-outline btn-sm" type="button" @click="handleExport">导出文本</button>
      </div>

      <article v-for="report in reports" :key="report.id_wsh" class="report-card card">
        <header class="report-header">
          <div>
            <span class="section-kicker">{{ report.type_wsh || 'report' }}</span>
            <h3>{{ reportTitle(report) }}</h3>
          </div>
          <time>{{ formatTime(report.created_at_wsh) }}</time>
        </header>
        <pre class="report-content">{{ report.content_wsh || '报告内容为空' }}</pre>
      </article>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAppStore } from '@/stores/app'

const props = defineProps({
  reports: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  order: { type: Object, default: null },
})
const emit = defineEmits(['regenerate'])
const appStore = useAppStore()

const canGenerate = computed(() => props.order && ['completed', 'in_progress'].includes(props.order.status_wsh))

function handleGenerate() {
  emit('regenerate')
}

function reportTitle(report) {
  const map = { final: 'AI寄养报告', care: 'AI照护建议' }
  return map[report.type_wsh] || 'AI报告'
}

function handleCopy() {
  const text = props.reports.map(report => `${reportTitle(report)}\n${report.content_wsh || ''}`).join('\n\n---\n\n')
  if (!text.trim()) {
    appStore.addToast('没有可复制的内容', 'warning')
    return
  }
  navigator.clipboard.writeText(text)
    .then(() => appStore.addToast('已复制到剪贴板', 'success'))
    .catch(() => appStore.addToast('复制失败', 'error'))
}

function handleExport() {
  if (!props.reports.length) return
  const content = props.reports.map(report => {
    return `${reportTitle(report)}\n生成时间：${formatTime(report.created_at_wsh)}\n\n${report.content_wsh || '无'}`
  }).join('\n\n======\n\n')
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `AI报告_${props.order?.order_no_wsh || '寄养报告'}.txt`
  link.click()
  URL.revokeObjectURL(url)
  appStore.addToast('报告已导出', 'success')
}

function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString()
}
</script>

<style scoped>
.report-tab {
  display: grid;
  gap: 16px;
}
.compact-loading,
.compact-empty {
  padding: 36px 20px;
}
.compact-empty .btn {
  margin-top: 10px;
}
.report-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}
.report-card {
  padding: 18px;
}
.report-header {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  margin-bottom: 14px;
}
.section-kicker {
  color: var(--color-muted-foreground);
  font-size: 12px;
}
.report-header h3 {
  margin: 2px 0 0;
  font-size: 18px;
}
.report-header time {
  color: var(--color-muted-foreground);
  font-size: 12px;
  white-space: nowrap;
}
.report-content {
  margin: 0;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background: var(--color-muted);
  white-space: pre-wrap;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.8;
}
</style>
