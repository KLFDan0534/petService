<template>
  <div>
    <span v-if="label" class="chip-rail__label">{{ label }}</span>
    <div class="chip-rail">
      <!-- 选中状态不另存一份，直接拿这一项的 value 跟外面的 modelValue 比，相等就亮起来 -->
      <button
        v-for="opt in options"
        :key="opt.value"
        type="button"
        class="chip"
        :class="{ 'chip--active': opt.value === modelValue, 'is-disabled': disabled }"
        :disabled="disabled"
        @click="emit('update:modelValue', opt.value)"
      >
        {{ opt.label }}
      </button>
    </div>
  </div>
</template>

<script setup>
// options 是一串 { value, label }；谁被选中，只看外面的 modelValue 等于哪个 value
defineProps({
  label: { type: String, default: '' },
  options: { type: Array, required: true }, // [{ value, label }]
  modelValue: { type: [Number, String], default: null },
  disabled: Boolean,
})
// 点了只把新值抛出去，自己不记，改不改由父组件说了算
const emit = defineEmits(['update:modelValue'])
</script>

<style scoped>
.chip-rail__label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--ref-muted);
  margin-bottom: 8px;
}
.chip-rail {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chip {
  height: var(--control-height-sm); /* 34px */
  padding: 0 14px;
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-control);
  background: var(--ref-surface);
  color: var(--ref-ink-soft);
  font-size: 13.5px;
  font-family: var(--ref-font-sans);
  font-weight: 500;
  cursor: pointer;
  transition: border-color 200ms ease, background 200ms ease, color 200ms ease;
}
.chip:hover:not(.is-disabled) {
  border-color: var(--ref-brand);
  color: var(--ref-brand);
}
.chip--active {
  background: var(--ref-brand);
  border-color: var(--ref-brand);
  color: #fff;
}
.chip.is-disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>