<template>
  <AppDialog
    :visible="visible"
    :width="400"
    :show-close="false"
    :close-on-overlay="false"
    title="取消这笔订单？"
    description="取消后档期会立即释放，需要重新预约才能恢复。"
    @close="$emit('close')"
  >
    <!-- 确认取消之前先把退多少钱、多久到账摆出来，用户心里有数再决定 -->
    <dl class="cancel-body">
      <div class="cancel-row">
        <dt>订单号</dt>
        <dd class="cancel-num">{{ orderNo }}</dd>
      </div>
      <div class="cancel-row">
        <dt>预计退款</dt>
        <dd class="cancel-refund">¥{{ formatMoney(refundAmount) }}</dd>
      </div>
      <div class="cancel-row">
        <dt>到账时间</dt>
        <dd class="cancel-note">1-3 个工作日退回原支付方式</dd>
      </div>
    </dl>

    <!-- 为什么左边按钮写"保留订单"而不是"取消"？这里的取消指的是取消订单，写清楚才不会被误点 -->
    <template #footer>
      <AppButton variant="quiet" :disabled="loading" @click="$emit('close')">保留订单</AppButton>
      <AppButton variant="danger" :loading="loading" @click="$emit('cancel', order)">
        {{ loading ? '处理中…' : '确认取消订单' }}
      </AppButton>
    </template>
  </AppDialog>
</template>

<script setup>
import { computed } from 'vue'
import AppDialog from '@/components/common/AppDialog.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { formatMoney } from '@/utils/format'

const props = defineProps({
  visible: Boolean,
  order: Object,
  loading: { type: Boolean, default: false },
})
defineEmits(['close', 'cancel'])

const orderNo = computed(() => props.order?.order_no_wsh || props.order?.id_wsh || '—')
// 退的是实付金额；实付没记录就依次退到订单总额、结算金额
const refundAmount = computed(() =>
  Number(props.order?.final_amount_wsh || props.order?.total_amount_wsh || props.order?.settlement_amount_wsh || 0)
)
</script>

<style scoped>
.cancel-body {
  margin: 0;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-card);
  overflow: hidden;
}
.cancel-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
}
.cancel-row + .cancel-row {
  border-top: 1px solid var(--ref-line);
}
.cancel-row dt {
  font-size: 12.5px;
  color: var(--ref-muted);
}
.cancel-row dd {
  margin: 0;
  font-size: 13px;
}
.cancel-num {
  color: var(--ref-ink-soft);
  font-variant-numeric: tabular-nums;
}
.cancel-refund {
  font-weight: 500;
  color: var(--ref-ink);
  font-variant-numeric: tabular-nums;
}
.cancel-note {
  color: var(--ref-ink-soft);
}
</style>