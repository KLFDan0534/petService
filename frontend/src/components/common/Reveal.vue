<template>
  <component
    :is="tag"
    ref="el"
    class="rvl"
    :class="{ 'rvl-in': visible }"
    :style="{
      transitionDelay: transitionDelay,
      '--rvl-y': `${props.y}px`,
      '--rvl-blur': props.blur ? '6px' : '0px',
    }"
  >
    <slot />
  </component>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  /** 渲染的标签（div / span ...）。 */
  tag: { type: String, default: 'div' },
  /** 秒。配合 index * 0.05 可做分组错落。 */
  delay: { type: Number, default: 0 },
  /** 进入前的垂直偏移（px）。 */
  y: { type: Number, default: 18 },
  /** 进入前是否附加 6px 微模糊。 */
  blur: { type: Boolean, default: true },
  /** 是否只播放一次。 */
  once: { type: Boolean, default: true },
})

const el = ref(null)
const visible = ref(false)

const EASE = 'cubic-bezier(0.23, 1, 0.32, 1)'
const transitionDelay = props.delay ? `${props.delay}s` : undefined

let observer = null

function reduceMotion() {
  return typeof window !== 'undefined'
    && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
}

onMounted(() => {
  if (reduceMotion()) {
    visible.value = true
    return
  }
  if (!('IntersectionObserver' in window)) {
    visible.value = true
    return
  }
  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          visible.value = true
          if (props.once && observer) {
            observer.unobserve(entry.target)
            observer.disconnect()
          }
        } else if (!props.once) {
          visible.value = false
        }
      })
    },
    { rootMargin: '-10% 0px -12% 0px', threshold: 0 },
  )
  if (el.value) observer.observe(el.value)
})

onBeforeUnmount(() => {
  if (observer) observer.disconnect()
})
</script>

<style scoped>
.rvl {
  display: block;
  opacity: 0;
  transform: translateY(var(--rvl-y, 18px));
  filter: blur(var(--rvl-blur, 6px));
  transition:
    opacity 0.3s var(--rvl-ease, cubic-bezier(0.23, 1, 0.32, 1)),
    transform 0.3s var(--rvl-ease, cubic-bezier(0.23, 1, 0.32, 1)),
    filter 0.3s var(--rvl-ease, cubic-bezier(0.23, 1, 0.32, 1));
  will-change: opacity, transform, filter;
}
.rvl-in {
  opacity: 1;
  transform: translateY(0);
  filter: blur(0);
}
</style>