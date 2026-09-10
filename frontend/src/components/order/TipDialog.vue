<template>
  <AppDialog
    :visible="visible"
    title="打赏服务"
    :description="description"
    size="wide"
    @close="$emit('close')"
  >
    <div class="tip-body">
      <p class="tip-meta">
        订单号 <span class="tip-num">{{ orderNo }}</span>
      </p>

      <ChipRail
        label="打赏金额档位"
        :options="presetOptions"
        :model-value="custom ? null : preset"
        :disabled="submitting"
        @update:model-value="onPreset"
      />

      <FormField
        label="自定义金额"
        :hint="`填写后覆盖上方档位，单笔上限 ¥${MAX_TIP}`"
        :error="error"
      >
        <input
          v-model="custom"
          class="form-control tip-input"
          inputmode="decimal"
          placeholder="请输入金额"
          type="text"
          :disabled="submitting"
          @input="onCustomInput"
        >
      </FormField>
    </div>

    <template #footer>
      <AppButton variant="quiet" :disabled="submitting" @click="$emit('close')">取消</AppButton>
      <AppButton variant="primary" :loading="submitting" @click="submitTip">
        {{ submitting ? '提交中…' : `确认打赏 ¥${amount || 0}` }}
      </AppButton>
    </template>
  </AppDialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import AppDialog from '@/components/common/AppDialog.vue'
import AppButton from '@/components/ui/AppButton.vue'
import ChipRail from '@/components/ui/ChipRail.vue'
import FormField from '@/components/ui/FormField.vue'

const TIP_PRESETS = [8, 18, 38, 66, 88]
const MAX_TIP = 2000

const props = defineProps({ visible: Boolean, order: Object })
const emit = defineEmits(['close', 'tipped'])

const preset = ref(TIP_PRESETS[1])
const custom = ref('')
const error = ref('')
const submitting = ref(false)

const description = computed(() => {
  const name = props.order?.keeper_name_wsh || '照护人'
  return `打赏将直接发放给照护人 ${name}，可在订单详情查看记录。`
})
const orderNo = computed(() => props.order?.order_no_wsh || props.order?.id_wsh)

const presetOptions = computed(() => TIP_PRESETS.map(v => ({ value: v, label: `¥${v}` })))
const amount = computed(() => (custom.value ? Number(custom.value) : preset.value || 0))

watch(() => props.visible, (val) => {
  if (val) {
    preset.value = TIP_PRESETS[1]
    custom.value = ''
    error.value = ''
    submitting.value = false
  }
})

function onPreset(v) {
  preset.value = v
  custom.value = ''
  error.value = ''
}

function onCustomInput() {
  custom.value = custom.value.replace(/[^\d]/g, '')
  error.value = ''
}

function submitTip() {
  const amt = amount.value
  if (!amt) { error.value = '请选择或填写打赏金额'; return }
  if (amt > MAX_TIP) { error.value = `单笔打赏不超过 ${MAX_TIP} 元`; return }
  if (!props.order) return
  submitting.value = true
  try { emit('tipped', props.order.id_wsh, amt, '') }
  finally { submitting.value = false }
}
</script>

<style scoped>
.tip-body {
  display: grid;
  gap: 20px;
}
.tip-meta {
  margin: 0;
  font-size: 12px;
  color: var(--ref-muted);
}
.tip-num {
  color: var(--ref-ink-soft);
  font-variant-numeric: tabular-nums;
}
</style>