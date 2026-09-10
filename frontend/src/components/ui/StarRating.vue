<template>
  <div class="star-rating">
    <div
      role="radiogroup"
      :aria-label="label"
      class="star-rating__group"
      tabindex="0"
      @keydown="onKeydown"
    >
      <button
        v-for="star in max"
        :key="star"
        type="button"
        role="radio"
        :aria-checked="modelValue === star"
        :aria-label="`${star} 星`"
        class="star"
        :class="{ 'star--filled': star <= modelValue, 'is-disabled': disabled }"
        :disabled="disabled"
        :tabindex="modelValue === star || (modelValue === 0 && star === 1) ? 0 : -1"
        @click="select(star)"
      >
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" aria-hidden="true">
          <polygon
            :fill="star <= modelValue ? 'currentColor' : 'none'"
            stroke="currentColor"
            points="12 3.5 14.8 9.2 21 10 16.6 14.4 17.6 20.5 12 17.4 6.4 20.5 7.4 14.4 3 10 9.2 9.2"
          />
        </svg>
      </button>
    </div>
    <span class="star-rating__hint">{{ modelValue ? `${modelValue}.0 分` : '未评分' }}</span>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  modelValue: { type: Number, default: 0 },
  max: { type: Number, default: 5 },
  disabled: Boolean,
})
const emit = defineEmits(['update:modelValue'])

function select(v) {
  if (props.disabled) return
  emit('update:modelValue', v)
}

function onKeydown(e) {
  if (props.disabled) return
  if (e.key === 'ArrowRight' || e.key === 'ArrowUp') {
    e.preventDefault()
    emit('update:modelValue', Math.min(props.max, (props.modelValue || 0) + 1))
  } else if (e.key === 'ArrowLeft' || e.key === 'ArrowDown') {
    e.preventDefault()
    emit('update:modelValue', Math.max(1, (props.modelValue || 1) - 1))
  }
}
</script>

<style scoped>
.star-rating {
  display: flex;
  align-items: center;
  gap: 12px;
}
.star-rating__group {
  display: flex;
  align-items: center;
  gap: 2px;
  outline: none;
}
.star {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 8px; /* rounded-tile */
  background: transparent;
  color: color-mix(in srgb, var(--ref-ink) 25%, transparent);
  cursor: pointer;
  transition: background 200ms ease, color 200ms ease;
}
.star--filled {
  color: var(--ref-brand);
}
.star:hover:not(.is-disabled) {
  background: var(--ref-sand);
  color: var(--ref-brand);
}
.star.is-disabled {
  cursor: not-allowed;
  opacity: 0.4;
}
.star-rating__hint {
  font-size: 12px;
  color: var(--ref-muted);
}
</style>