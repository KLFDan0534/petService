<template>
  <div class="media-frame" :class="{ 'is-fallback': showFallback }">
    <img
      v-if="!showFallback"
      class="media-image"
      :src="displaySrc"
      :alt="alt"
      :loading="loading"
      decoding="async"
      @error="failed = true"
    >
    <div
      v-else
      class="media-placeholder"
      :role="alt ? 'img' : undefined"
      :aria-label="alt || undefined"
    >
      <el-icon aria-hidden="true"><Picture /></el-icon>
      <span v-if="placeholder">{{ placeholder }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Picture } from '@element-plus/icons-vue'

const props = defineProps({
  src: { type: String, default: '' },
  alt: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  loading: { type: String, default: 'lazy' },
})

const failed = ref(false)

const displaySrc = computed(() => normalizeMediaUrl(props.src))
const showFallback = computed(() => !displaySrc.value || failed.value)

watch(() => props.src, () => {
  failed.value = false
})

function normalizeMediaUrl(value) {
  const rawUrl = String(value || '').trim()
  if (!rawUrl || typeof window === 'undefined') return rawUrl

  try {
    const parsedUrl = new URL(rawUrl, window.location.origin)
    const isLocalMinioUrl = ['localhost', '127.0.0.1'].includes(parsedUrl.hostname)
      && parsedUrl.pathname.startsWith('/minio/')

    if (isLocalMinioUrl) {
      return `${parsedUrl.pathname}${parsedUrl.search}${parsedUrl.hash}`
    }
  } catch (_) {
    return rawUrl
  }

  return rawUrl
}
</script>

<style scoped>
.media-frame {
  position: relative;
  width: 100%;
  height: 100%;
  min-width: 0;
  overflow: hidden;
  background: var(--color-muted);
}

.media-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-placeholder {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: inherit;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8px;
  padding: 24px;
  color: var(--color-muted-foreground);
  background: color-mix(in srgb, var(--color-primary) 10%, var(--color-card));
  text-align: center;
}

.media-placeholder .el-icon {
  font-size: 36px;
  color: var(--color-primary);
}

.media-placeholder span {
  font-size: 13px;
  font-weight: 700;
}
</style>
