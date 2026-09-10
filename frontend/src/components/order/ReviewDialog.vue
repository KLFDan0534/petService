<template>
  <AppDialog
    :visible="visible"
    title="评价这次服务"
    :description="description"
    size="lg"
    @close="$emit('close')"
  >
    <div class="review-body">
      <p class="review-meta">
        订单号 <span class="review-num">{{ orderNo }}</span> ·
        已评 <span class="review-num">{{ ratedCount }}/{{ dimensions.length }}</span> 个维度
      </p>

      <div class="review-rows">
        <div
          v-for="(dim, i) in dimensions"
          :key="dim.type"
          class="review-row"
          :class="{ 'review-row--border': i > 0, 'is-disabled': dim.done || !dim.targetId }"
        >
          <div class="review-row__text">
            <p class="review-row__label">
              {{ dim.label }}
              <span v-if="dim.done" class="review-row__done">已评价</span>
              <span v-else-if="!dim.targetId" class="review-row__note">该订单缺少信息</span>
            </p>
            <p class="review-row__hint">{{ dim.targetName }}</p>
          </div>
          <StarRating
            v-model="ratings[dim.type]"
            :label="`评价${dim.label}`"
            :disabled="submitting || dim.done || !dim.targetId"
          />
        </div>
      </div>

      <p v-if="error" class="review-error" role="alert">{{ error }}</p>

      <FormField
        label="补充说明"
        :hint="`${comment.length}/${MAX_COMMENT} · 选填，写下有帮助的细节`"
      >
        <textarea
          v-model="comment"
          class="form-control review-textarea"
          :maxlength="MAX_COMMENT"
          rows="4"
          placeholder="例如：上门准时，遛狗后发来了照片和饮水记录。"
          :disabled="submitting"
        ></textarea>
      </FormField>
    </div>

    <template #footer>
      <AppButton variant="quiet" :disabled="submitting" @click="$emit('close')">稍后再说</AppButton>
      <AppButton variant="primary" :loading="submitting" @click="submitReviews">
        {{ submitting ? '提交中…' : '提交评价' }}
      </AppButton>
    </template>
  </AppDialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import AppDialog from '@/components/common/AppDialog.vue'
import AppButton from '@/components/ui/AppButton.vue'
import StarRating from '@/components/ui/StarRating.vue'
import FormField from '@/components/ui/FormField.vue'

const MAX_COMMENT = 200

const props = defineProps({
  visible: Boolean,
  order: Object,
  doneTypes: { type: Array, default: () => [] },
})
const emit = defineEmits(['close', 'reviewed'])

const comment = ref('')
const error = ref('')
const submitting = ref(false)
const ratings = reactive({ merchant: 0, keeper: 0, service: 0 })

const dimensions = computed(() => {
  const order = props.order || {}
  return [
    {
      type: 'merchant',
      label: '商家',
      targetId: order.merchant_id_wsh,
      targetName: order.merchant_name_wsh || '门店服务',
    },
    {
      type: 'keeper',
      label: '看护人',
      targetId: order.keeper_id_wsh,
      targetName: order.keeper_name_wsh || '照护人',
    },
    {
      type: 'service',
      label: '服务',
      targetId: order.service_id_wsh,
      targetName: order.service_name_wsh || '服务项目',
    },
  ].map(dim => ({ ...dim, done: props.doneTypes.includes(dim.type) }))
})

const ratedCount = computed(() => dimensions.value.filter(d => ratings[d.type] > 0).length)
const description = computed(() => {
  const service = props.order?.service_name_wsh || '本次服务'
  return `${service} · 评价提交后公开展示，可在 24 小时内修改一次。`
})
const orderNo = computed(() => props.order?.order_no_wsh || props.order?.id_wsh)

watch(() => props.visible, (val) => {
  if (val) {
    ratings.merchant = 0
    ratings.keeper = 0
    ratings.service = 0
    comment.value = ''
    error.value = ''
    submitting.value = false
  }
})

async function submitReviews() {
  const pending = dimensions.value.filter(d => d.targetId && !d.done && ratings[d.type] > 0)
  if (!pending.length) {
    error.value = '请为可评价的维度评分后再提交'
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    for (const dim of pending) {
      emit('reviewed', {
        orderId: props.order?.id_wsh,
        targetType: dim.type,
        targetId: dim.targetId,
        score: ratings[dim.type],
        content: comment.value,
      })
    }
    emit('close')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.review-body {
  display: grid;
  gap: 20px;
}
.review-meta {
  margin: 0;
  font-size: 12px;
  color: var(--ref-muted);
}
.review-num {
  color: var(--ref-ink-soft);
  font-variant-numeric: tabular-nums;
}
.review-rows {
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-card);
  overflow: hidden;
}
.review-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
}
.review-row--border {
  border-top: 1px solid var(--ref-line);
}
.review-row__text {
  min-width: 0;
}
.review-row__label {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--ref-ink);
}
.review-row__done {
  margin-left: 8px;
  font-size: 11px;
  color: var(--ref-brand);
}
.review-row__note {
  margin-left: 8px;
  font-size: 11px;
  color: var(--ref-muted);
}
.review-row__hint {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--ref-muted);
}
.review-row.is-disabled {
  opacity: 0.6;
}
.review-error {
  margin: 0;
  font-size: 12px;
  color: var(--ref-brand-deep);
}
.review-textarea {
  resize: vertical;
}
</style>