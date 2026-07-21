<template>
  <div class="table-wrap">
    <table>
      <thead>
        <tr>
          <th v-for="col in columns" :key="col.label || col">{{ col.label || col }}</th>
          <th v-if="$slots.default">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, i) in data" :key="i">
          <td v-for="(col, j) in columns" :key="j">
            <span v-if="isBadgeCell(cellValue(row, col))" :class="['badge', cellValue(row, col).badge]">
              {{ cellValue(row, col).label }}
            </span>
            <span v-else class="cell-text">{{ formatCell(cellValue(row, col)) }}</span>
          </td>
          <td v-if="$slots.default"><slot :row="row" /></td>
        </tr>
        <tr v-if="data.length === 0">
          <td :colspan="emptyColspan" class="empty">暂无数据</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { computed, useSlots } from 'vue'

const props = defineProps({
  columns: { type: Array, default: () => [] },
  data: { type: Array, default: () => [] },
})

const slots = useSlots()
const emptyColspan = computed(() => props.columns.length + (slots.default ? 1 : 0))
const ALLOWED_BADGES = new Set([
  'badge-primary',
  'badge-secondary',
  'badge-success',
  'badge-warning',
  'badge-danger',
  'badge-error',
  'badge-info',
  'badge-disabled',
])

function cellValue(row, col) {
  return row?.[col.key || col]
}

function isBadgeCell(value) {
  return value && typeof value === 'object' && ALLOWED_BADGES.has(value.badge)
}

function formatCell(value) {
  if (value == null || value === '') return '-'
  if (typeof value === 'object') return value.label ?? JSON.stringify(value)
  return value
}
</script>

<style scoped>
.empty {
  color: var(--color-muted-foreground);
  padding: 32px;
  text-align: center;
}

.cell-text {
  overflow-wrap: anywhere;
  word-break: break-word;
}
</style>
