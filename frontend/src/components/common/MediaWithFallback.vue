<template>
  <div class="media-frame" :class="{ 'is-fallback': showFallback }">
    <img
      v-if="!showFallback"
      class="media-image"
      :src="displaySrc"
      :alt="alt"
      :loading="loading"
      decoding="async"
      @error="handleError"
    >
    <div
      v-else
      class="media-placeholder"
      :role="alt ? 'img' : undefined"
      :aria-label="alt || undefined"
    >
      <AppIcon aria-hidden="true"><Picture /></AppIcon>
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
  /** 主图加载失败时改用的备用图（如本地素材图）；备用图也失败才落到占位块。 */
  fallbackSrc: { type: String, default: '' },
})

// stage: 0 = 显示 src，1 = 显示 fallbackSrc，2 = 都失败，显示占位块
const stage = ref(0)

const primarySrc = computed(() => normalizeMediaUrl(props.src))
const backupSrc = computed(() => (props.fallbackSrc ? normalizeMediaUrl(props.fallbackSrc) : ''))

const displaySrc = computed(() => (stage.value === 1 ? backupSrc.value : primarySrc.value))
const showFallback = computed(() => !primarySrc.value || stage.value === 2)

watch(() => props.src, () => {
  stage.value = 0
})

function handleError() {
  if (stage.value === 0 && backupSrc.value) {
    stage.value = 1
    return
  }
  stage.value = 2
}

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
