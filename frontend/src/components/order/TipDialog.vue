<template>
  <div v-if="visible" class="modal-overlay" @mousedown.self="$emit('close')">
    <div class="modal mini-modal">
      <h2>打赏服务</h2>
      <input v-model="amount" type="number" min="0.01" step="0.01" class="form-control" placeholder="金额">
      <textarea v-model="message" class="form-control" rows="3" placeholder="留言"></textarea>
      <div class="modal-actions">
        <button class="btn btn-secondary btn-sm" type="button" @click="$emit('close')">取消</button>
        <button class="btn btn-success btn-sm" type="button" @click="submitTip">确认打赏</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ visible: Boolean, order: Object })
const emit = defineEmits(['close', 'tipped'])

const amount = ref('')
const message = ref('')

watch(() => props.visible, (val) => {
  if (val) { amount.value = ''; message.value = '' }
})

function submitTip() {
  if (!amount.value || !props.order) return
  emit('tipped', props.order.id_wsh, Number(amount.value), message.value)
}
</script>

<style scoped>
.mini-modal { max-width: 420px; display: grid; gap: 12px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
</style>
