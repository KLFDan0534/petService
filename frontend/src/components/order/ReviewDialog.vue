<template>
  <div v-if="visible" class="modal-overlay" @mousedown.self="$emit('close')">
    <div class="modal mini-modal">
      <h2>评价本次服务</h2>

      <div class="dimension-tabs" role="tablist" aria-label="评价维度">
        <button
          v-for="dim in dimensions"
          :key="dim.type"
          type="button"
          role="tab"
          :class="['dimension-tab', activeDimension === dim.type ? 'active' : '']"
          @click="activeDimension = dim.type"
        >
          {{ dim.label }}
          <span v-if="dim.done" class="dimension-done">已评价</span>
        </button>
      </div>

      <template v-if="activeOrder">
        <div class="dimension-target">{{ currentDimension.targetName }}</div>
        <select v-model="score" class="form-control" aria-label="评分">
          <option :value="5">5 分</option>
          <option :value="4">4 分</option>
          <option :value="3">3 分</option>
          <option :value="2">2 分</option>
          <option :value="1">1 分</option>
        </select>
        <textarea v-model="content" class="form-control" rows="4" placeholder="说说这次服务体验"></textarea>
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" type="button" @click="$emit('close')">取消</button>
          <button
            class="btn btn-primary btn-sm"
            type="button"
            :disabled="submitting || currentDimension.done || !currentDimension.targetId"
            @click="submitReview"
          >
            {{ submitting ? '提交中...' : currentDimension.done ? '该维度已评价' : '提交评价' }}
          </button>
        </div>
        <p v-if="currentDimension.done" class="dimension-note">该维度已评价，感谢你的反馈。</p>
        <p v-else-if="!currentDimension.targetId" class="dimension-note">该订单缺少{{ currentDimension.label }}信息，无法评价。</p>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  visible: Boolean,
  order: Object,
  doneTypes: { type: Array, default: () => [] },
})
const emit = defineEmits(['close', 'reviewed'])

const score = ref(5)
const content = ref('')
const activeDimension = ref('merchant')
const submitting = ref(false)

const dimensions = computed(() => {
  const order = props.order || {}
  return [
    {
      type: 'merchant',
      label: '商家',
      targetId: order.merchant_id_wsh,
      targetName: order.merchant_name_wsh || `商家 #${order.merchant_id_wsh}`,
    },
    {
      type: 'keeper',
      label: '看护人',
      targetId: order.keeper_id_wsh,
      targetName: order.keeper_name_wsh || `看护人 #${order.keeper_id_wsh}`,
    },
    {
      type: 'service',
      label: '服务',
      targetId: order.service_id_wsh,
      targetName: order.service_name_wsh || `服务 #${order.service_id_wsh}`,
    },
  ].map(dim => ({ ...dim, done: props.doneTypes.includes(dim.type) }))
})

const currentDimension = computed(() =>
  dimensions.value.find(dim => dim.type === activeDimension.value) || dimensions.value[0])

const activeOrder = computed(() => props.order)

watch(() => props.visible, (val) => {
  if (val) {
    score.value = 5
    content.value = ''
    submitting.value = false
    const firstUndone = dimensions.value.find(dim => !dim.done && dim.targetId)
    activeDimension.value = firstUndone ? firstUndone.type : 'merchant'
  }
}, { immediate: true })

async function submitReview() {
  const dim = currentDimension.value
  if (!props.order || !dim.targetId || dim.done || submitting.value) return
  submitting.value = true
  try {
    await emit('reviewed', {
      orderId: props.order.id_wsh,
      targetType: dim.type,
      targetId: dim.targetId,
      score: score.value,
      content: content.value,
    })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.mini-modal { max-width: 440px; display: grid; gap: 12px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }

.dimension-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.dimension-tab {
  padding: 6px 12px;
  font-size: 13px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-card);
  color: var(--color-muted-foreground);
  cursor: pointer;
}
.dimension-tab.active {
  color: #fff;
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.dimension-done {
  margin-left: 6px;
  font-size: 11px;
  opacity: 0.85;
}
.dimension-target {
  color: var(--color-foreground);
  font-size: 13px;
  font-weight: 700;
}
.dimension-note {
  color: var(--color-muted-foreground);
  font-size: 12px;
  margin: 0;
}
</style>
