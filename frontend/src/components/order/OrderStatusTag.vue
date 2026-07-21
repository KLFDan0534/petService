<template>
  <span :class="['badge', badgeClass]">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'
import { OrderStatus, getStatusLabel, getStatusBadge } from '@/constants/statusMaps'

const props = defineProps({ status: { type: String, default: '' } })
const label = computed(() => getStatusLabel(OrderStatus, props.status))
const badgeClass = computed(() => getStatusBadge(OrderStatus, props.status))
</script>
