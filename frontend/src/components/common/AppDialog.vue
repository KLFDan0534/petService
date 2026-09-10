<template>
  <div v-if="visible" class="app-dialog-overlay" @mousedown.self="onOverlayClick">
    <div
      ref="containerRef"
      class="app-dialog"
      :class="className"
      role="dialog"
      :aria-modal="true"
      :aria-label="ariaLabel || undefined"
      :style="containerStyle"
    >
      <header v-if="title || $slots.header || description" class="app-dialog__header">
        <div class="app-dialog__header-text">
          <slot name="header">
            <h2 v-if="title">{{ title }}</h2>
          </slot>
          <p v-if="description" class="app-dialog__description">{{ description }}</p>
        </div>
        <button
          v-if="showClose"
          ref="closeRef"
          type="button"
          class="app-dialog__close"
          aria-label="关闭"
          @click="emit('close')"
        >
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </header>

      <div v-if="$slots.default" class="app-dialog__body">
        <slot />
      </div>

      <footer v-if="$slots.footer" class="app-dialog__footer">
        <slot name="footer" />
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const SIZE_MAP = { sm: 400, md: 440, wide: 520, lg: 640, xl: 960 }

function widthToSize(width) {
  if (width <= 400) return 'sm'
  if (width <= 440) return 'md'
  if (width <= 520) return 'wide'
  if (width <= 640) return 'lg'
  return 'xl'
}

const props = defineProps({
  visible: Boolean,
  /** 标题文案；使用 #header 插槽时可省略 */
  title: { type: String, default: '' },
  /** 标题下方的补充说明 */
  description: String,
  /** 旧 API：内容区最大宽度（px），自动映射到系统尺寸档位 */
  width: { type: Number, default: 500 },
  /** 新 API：直接指定尺寸档位（400/440/520/640/960px），优先于 width */
  size: { type: String, default: '' },
  /** 是否显示右上角关闭按钮 */
  showClose: { type: Boolean, default: true },
  /** 点击蒙版是否关闭 */
  closeOnOverlay: { type: Boolean, default: true },
  /** 按 Esc 是否关闭 */
  closeOnEsc: { type: Boolean, default: true },
  /** 无可见标题时的无障碍名称 */
  ariaLabel: { type: String, default: '' },
  /** 追加到容器的类名 */
  className: { type: String, default: '' },
})

const emit = defineEmits(['close'])

const containerRef = ref(null)
const closeRef = ref(null)

const sizeKey = computed(() => props.size || widthToSize(props.width))
const px = computed(() => SIZE_MAP[sizeKey.value]) // eslint-disable-line no-unused-vars
const containerStyle = computed(() => {
  const w = SIZE_MAP[sizeKey.value]
  return { maxWidth: w + 'px', width: `min(${w}px, 92vw)` }
})

function onOverlayClick() {
  if (props.closeOnOverlay) emit('close')
}

function onKeydown(e) {
  if (!props.visible || e.key !== 'Escape') return
  if (props.closeOnEsc) {
    e.preventDefault()
    emit('close')
  }
}

/* 展示时锁定 body 滚动，并聚焦容器便于键盘操作 */
watch(
  () => props.visible,
  async (val) => {
    if (val) {
      document.body.style.overflow = 'hidden'
      await nextTick()
      ;(containerRef.value || closeRef.value || document.activeElement)?.focus?.({ preventScroll: true })
    } else {
      document.body.style.overflow = ''
    }
  },
  { immediate: true }
)

onMounted(() => document.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = ''
})
</script>

<style scoped>
/* ==== 弹窗基础样式（设计系统 anatomy，统一集中在此） ==== */
.app-dialog-overlay {
  position: fixed;
  inset: 0;
  /* 暖黑 scrim + 2px 模糊，与暖米画布同色系 */
  background: rgba(30, 22, 14, 0.42);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: appDialogFade 150ms ease;
}

.app-dialog {
  position: relative;
  /* bg-surface · 1px line 描边 · rounded-card 18px */
  background: var(--ref-surface);
  border: 1px solid var(--ref-line);
  border-radius: var(--radius-card);
  /* 仅一档 shadow-lift */
  box-shadow: var(--shadow-lift);
  max-height: min(90vh, 720px);
  overflow-y: auto;
  /* 24px 栏距 */
  padding: 24px 24px 28px;
  /* scale(0.98) → 1，200ms 无飞入 */
  animation: appDialogIn 200ms cubic-bezier(0.22, 1, 0.36, 1);
  outline: none;
  scrollbar-width: thin;
}

.app-dialog__header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 8px;
}
.app-dialog__header-text {
  flex: 1;
  min-width: 0;
}
.app-dialog__header h2 {
  margin: 0;
  /* 衬线 20px / 500 */
  font-family: var(--ref-font-display);
  font-size: 20px;
  font-weight: 500;
  line-height: 1.3;
  color: var(--ref-ink);
}
.app-dialog__description {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--ref-muted);
}

.app-dialog__close {
  flex: none;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  margin: -4px -4px 0 0;
  border: none;
  border-radius: 8px; /* rounded-tile */
  background: transparent;
  color: var(--ref-muted);
  cursor: pointer;
  transition: color 150ms ease, background 150ms ease;
}
.app-dialog__close:hover {
  color: var(--ref-ink);
  background: var(--ref-sand);
}

.app-dialog__body {
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ref-ink-soft);
}

.app-dialog__footer {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
}

@keyframes appDialogFade {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes appDialogIn {
  from { opacity: 0; transform: scale(0.98); }
  to { opacity: 1; transform: scale(1); }
}
</style>