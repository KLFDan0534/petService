<template>
  <div class="sh-wrap">
    <div class="sh-left">
      <div class="sh-meta">
        <span class="sh-idx">{{ index }}</span>
        <span class="sh-line" aria-hidden="true"></span>
        <span class="sh-lbl" :class="{ 'sh-lbl-dark': tone === 'dark' }">{{ label }}</span>
      </div>
      <h2 class="sh-title" :class="{ 'sh-title-dark': tone === 'dark' }">
        <slot>{{ title }}</slot>
      </h2>
      <p v-if="description" class="sh-desc" :class="{ 'sh-desc-dark': tone === 'dark' }">
        {{ description }}
      </p>
    </div>
    <div v-if="$slots.action" class="sh-action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  index: { type: String, default: '' },
  label: { type: String, default: '' },
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  tone: { type: String, default: 'light' },
})
</script>

<style scoped>
.sh-wrap {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 40px 24px;
  margin-bottom: 48px;
}
.sh-left { max-width: 640px; }
.sh-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}
.sh-idx {
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  color: var(--muted, #7a6e63);
}
.sh-line {
  width: 24px;
  height: 1px;
  background: var(--line, #e7ded2);
}
.sh-lbl {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink-soft, #3a322b);
}
.sh-lbl-dark { color: rgba(255,255,255,0.5); }
.sh-title {
  margin-top: 16px;
  font-family: 'Fraunces', 'Noto Serif SC', Georgia, serif;
  font-size: clamp(28px, 3.4vw, 42px);
  font-weight: 400;
  line-height: 1.15;
  letter-spacing: -0.045em;
  text-wrap: balance;
  color: var(--ink, #17130f);
}
.sh-title-dark { color: var(--cream, #f5efe7); }
.sh-desc {
  margin-top: 16px;
  max-width: 560px;
  font-size: 15px;
  line-height: 1.65;
  color: var(--ink-soft, #3a322b);
  opacity: 0.8;
}
.sh-desc-dark { color: rgba(255,255,255,0.55); }
.sh-action { padding-bottom: 4px; flex-shrink: 0; }

@media (max-width: 720px) {
  .sh-wrap { display: block; margin-bottom: 24px; }
  .sh-title { font-size: 28px; }
  .sh-desc { margin-top: 12px; }
  .sh-action { margin-top: 12px; }
}
</style>
