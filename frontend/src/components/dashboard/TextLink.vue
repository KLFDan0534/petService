<template>
  <component
    :is="to ? 'router-link' : 'button'"
    :to="to"
    :href="href"
    class="text-link"
    @click="$emit('click', $event)"
  >
    <span class="tl-text"><slot /></span>
    <span class="tl-arrow" aria-hidden="true">→</span>
  </component>
</template>

<script setup>
defineProps({
  to: { type: String, default: '' },
  href: { type: String, default: '' },
})
defineEmits(['click'])
</script>

<style scoped>
.text-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--ink, #17130f);
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  text-decoration: none;
  font-family: inherit;
  transition: color 0.15s;
}
.text-link:hover { color: var(--brand, #c25a2a); }
.tl-text {
  border-bottom: 1px solid rgba(23,19,15,0.25);
  padding-bottom: 2px;
  transition: border-color 0.15s;
}
.text-link:hover .tl-text { border-color: var(--brand, #c25a2a); }
.tl-arrow {
  font-size: 14px;
  transition: transform 0.15s;
}
.text-link:hover .tl-arrow { transform: translateX(4px); }
</style>
