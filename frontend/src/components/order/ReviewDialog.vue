<template>
  <div v-if="visible" class="modal-overlay" @mousedown.self="$emit('close')">
    <div class="modal mini-modal">
      <h2>评价服务</h2>
      <select v-model="score" class="form-control">
        <option :value="5">5 分</option>
        <option :value="4">4 分</option>
        <option :value="3">3 分</option>
        <option :value="2">2 分</option>
        <option :value="1">1 分</option>
      </select>
      <textarea v-model="content" class="form-control" rows="4" placeholder="说说这次服务体验"></textarea>
      <div class="modal-actions">
        <button class="btn btn-secondary btn-sm" type="button" @click="$emit('close')">取消</button>
        <button class="btn btn-primary btn-sm" type="button" @click="submitReview">提交评价</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ visible: Boolean, order: Object })
const emit = defineEmits(['close', 'reviewed'])

const score = ref(5)
const content = ref('')

watch(() => props.visible, (val) => {
  if (val) { score.value = 5; content.value = '' }
})

function submitReview() {
  if (!props.order) return
  emit('reviewed', props.order.id_wsh, score.value, content.value)
}
</script>

<style scoped>
.mini-modal { max-width: 420px; display: grid; gap: 12px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
</style>
