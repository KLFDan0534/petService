<template>
  <div class="form-field">
    <label v-if="label" class="form-field__label" :for="htmlFor">{{ label }}</label>
    <slot />
    <!-- 有错误就先不显示提示语，两行说明叠在一起会挤成一团 -->
    <p v-if="hint && !error" class="form-field__hint">{{ hint }}</p>
    <p v-if="error" class="form-field__error" role="alert">{{ error }}</p>
  </div>
</template>

<script setup>
// htmlFor 要和里面那个输入框的 id 对上，点标题才能跳到输入框里
defineProps({
  label: String,
  htmlFor: String,
  hint: String,
  error: String,
})
</script>

<style scoped>
.form-field {
  display: grid;
  gap: 8px;
}
.form-field__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--ref-ink);
}
.form-field__hint {
  margin: 0;
  font-size: 12px;
  color: var(--ref-muted);
}
.form-field__error {
  margin: 0;
  font-size: 12px;
  color: var(--ref-brand-deep);
}
</style>