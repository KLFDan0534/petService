<script setup>
import { computed } from 'vue'
import { getStatusBadge, getStatusLabel } from '@/constants/statusMaps'

/**
 * 统一状态徽章。业务状态到颜色/文案的映射集中在 @/constants/statusMaps。
 * 用法：<StatusBadge :map="OrderStatus" :status="order.status_wsh" />
 */
const props = defineProps({
  status: { type: [String, Number], default: '' },
  map: { type: Object, required: true },
  fallbackLabel: { type: String, default: null },
})

const label = computed(() =>
  props.fallbackLabel ?? getStatusLabel(props.map, String(props.status ?? '').trim()),
)
const cls = computed(() => getStatusBadge(props.map, String(props.status ?? '').trim()))
</script>

<template>
  <span class="badge" :class="cls">{{ label }}</span>
</template>