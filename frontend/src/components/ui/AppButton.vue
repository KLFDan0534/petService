<template>
  <button
    type="button"
    class="app-btn"
    :class="[`app-btn--${variant}`, `app-btn--${size}`, { 'app-btn--block': block, 'is-arrow': arrow }]"
    :disabled="disabled || loading"
    @click="emit('click', $event)"
  >
    <span v-if="loading" class="app-btn__spinner" aria-hidden="true"></span>
    <slot>{{ label }}</slot>
    <svg
      v-if="arrow && !loading"
      class="app-btn__arrow"
      viewBox="0 0 16 16"
      width="14"
      height="14"
      fill="none"
      stroke="currentColor"
      stroke-width="1.6"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      <path d="M3 8h9m0 0L8.5 4.5M12 8l-3.5 3.5" />
    </svg>
  </button>
</template>

<script setup>
const props = defineProps({
  label: { type: String, default: '' },
  variant: {
    type: String,
    default: 'primary',
    validator: v => ['primary', 'quiet', 'danger', 'outline'].includes(v),
  },
  size: { type: String, default: 'md', validator: v => ['sm', 'md', 'lg'].includes(v) },
  arrow: Boolean,
  loading: Boolean,
  block: Boolean,
  disabled: Boolean,
})
const emit = defineEmits(['click'])
</script>

<style scoped>
.app-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid transparent;
  border-radius: 11px; /* rounded-control */
  font-family: var(--ref-font-sans);
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  transition: background-color 200ms cubic-bezier(0.22, 1, 0.36, 1),
    border-color 200ms ease, color 200ms ease, transform 200ms ease, opacity 200ms ease;
}
.app-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}
.app-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.app-btn:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--ref-brand) 55%, transparent);
  outline-offset: 2px;
}

/* sizes */
.app-btn--sm { height: 34px; padding: 0 14px; font-size: 13px; }
.app-btn--md { height: 42px; padding: 0 20px; font-size: 14px; }
.app-btn--lg { height: 48px; padding: 0 26px; font-size: 15px; }
.app-btn--block { display: flex; width: 100%; }

/* variants */
.app-btn--primary { background: var(--ref-brand); color: #fff; }
.app-btn--primary:hover:not(:disabled) { background: var(--ref-brand-deep); }
.app-btn--quiet { background: transparent; color: var(--ref-muted); }
.app-btn--quiet:hover:not(:disabled) { color: var(--ref-ink); background: var(--ref-sand); }
.app-btn--danger { background: var(--color-danger); color: #fff; }
.app-btn--outline { background: transparent; border-color: var(--ref-line); color: var(--ref-ink); }
.app-btn--outline:hover:not(:disabled) { border-color: var(--ref-brand); color: var(--ref-brand); }

/* arrow nudges 3px on hover */
.app-btn__arrow { transition: transform 200ms cubic-bezier(0.22, 1, 0.36, 1); }
.app-btn.is-arrow:hover:not(:disabled) .app-btn__arrow { transform: translateX(3px); }

/* loading spinner */
.app-btn__spinner {
  width: 14px;
  height: 14px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: appBtnSpin 600ms linear infinite;
}
@keyframes appBtnSpin { to { transform: rotate(360deg); } }
</style>