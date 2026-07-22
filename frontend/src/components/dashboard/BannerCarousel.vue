<template>
  <section
    v-if="slides.length"
    class="campaign-banner"
    aria-label="平台广告"
    @mouseenter="pause"
    @mouseleave="resume"
    @focusin="pause"
    @focusout="resume"
  >
    <Transition name="campaign-fade" mode="out-in">
      <a
        :key="currentBanner.id_wsh"
        class="campaign-slide"
        :class="{ 'is-linked': safeCurrentLink }"
        :href="safeCurrentLink || undefined"
        :target="safeCurrentLink ? '_blank' : undefined"
        :rel="safeCurrentLink ? 'noopener noreferrer' : undefined"
        :aria-label="safeCurrentLink ? `查看广告：${currentBanner.title_wsh}` : undefined"
      >
        <MediaWithFallback
          class="campaign-media"
          :src="currentBanner.image_url_wsh"
          :alt="currentBanner.title_wsh || '平台广告图片'"
          :placeholder="currentBanner.title_wsh || '平台广告'"
          loading="eager"
        />
        <span class="campaign-copy">
          <span class="campaign-label">广告 · 平台推荐</span>
          <strong>{{ currentBanner.title_wsh || '平台精选活动' }}</strong>
          <span v-if="safeCurrentLink" class="campaign-action">
            查看活动
            <el-icon aria-hidden="true"><ArrowRight /></el-icon>
          </span>
        </span>
      </a>
    </Transition>

    <template v-if="slides.length > 1">
      <button class="campaign-arrow campaign-prev" type="button" aria-label="上一条广告" @click="showPrevious">
        <el-icon aria-hidden="true"><ArrowLeft /></el-icon>
      </button>
      <button class="campaign-arrow campaign-next" type="button" aria-label="下一条广告" @click="showNext">
        <el-icon aria-hidden="true"><ArrowRight /></el-icon>
      </button>
      <div class="campaign-dots" aria-label="广告切换">
        <button
          v-for="(banner, index) in slides"
          :key="banner.id_wsh"
          type="button"
          :class="{ active: activeIndex === index }"
          :aria-label="`查看第 ${index + 1} 条广告`"
          :aria-current="activeIndex === index ? 'true' : undefined"
          @click="showSlide(index)"
        />
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import MediaWithFallback from '@/components/common/MediaWithFallback.vue'

const props = defineProps({
  banners: { type: Array, default: () => [] },
})

const activeIndex = ref(0)
let timer = null

const slides = computed(() => props.banners.filter(banner => banner?.id_wsh && banner.status_wsh !== 0))
const currentBanner = computed(() => slides.value[activeIndex.value] || {})
const safeCurrentLink = computed(() => toSafeUrl(currentBanner.value.link_url_wsh))

function toSafeUrl(value) {
  const rawUrl = String(value || '').trim()
  if (!rawUrl || typeof window === 'undefined') return ''

  try {
    const parsedUrl = new URL(rawUrl, window.location.origin)
    return ['http:', 'https:'].includes(parsedUrl.protocol) ? parsedUrl.href : ''
  } catch (_) {
    return ''
  }
}

function showSlide(index) {
  if (!slides.value.length) return
  activeIndex.value = (index + slides.value.length) % slides.value.length
  resume()
}

function showPrevious() {
  showSlide(activeIndex.value - 1)
}

function showNext() {
  showSlide(activeIndex.value + 1)
}

function prefersReducedMotion() {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
}

function pause() {
  if (timer) window.clearInterval(timer)
  timer = null
}

function resume() {
  pause()
  if (slides.value.length > 1 && !prefersReducedMotion()) {
    timer = window.setInterval(() => {
      activeIndex.value = (activeIndex.value + 1) % slides.value.length
    }, 6000)
  }
}

watch(slides, () => {
  activeIndex.value = 0
  resume()
})

onMounted(resume)
onUnmounted(pause)
</script>

<style scoped>
.campaign-banner {
  position: relative;
  width: 100%;
  min-height: 220px;
  aspect-ratio: 4 / 1;
  overflow: hidden;
  background: var(--color-muted);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  box-shadow: var(--shadow-md);
}

.campaign-slide,
.campaign-media {
  display: block;
  width: 100%;
  height: 100%;
}

.campaign-slide {
  position: absolute;
  inset: 0;
  color: #fff;
  cursor: default;
}

.campaign-slide.is-linked {
  cursor: pointer;
}

.campaign-slide::after {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(20, 25, 28, 0.78) 0%, rgba(20, 25, 28, 0.36) 48%, rgba(20, 25, 28, 0.08) 75%);
  content: '';
  pointer-events: none;
}

.campaign-copy {
  position: absolute;
  z-index: 1;
  bottom: 28px;
  left: clamp(24px, 5vw, 64px);
  display: flex;
  max-width: min(560px, calc(100% - 120px));
  align-items: flex-start;
  flex-direction: column;
  gap: 9px;
}

.campaign-label {
  display: inline-flex;
  padding: 5px 9px;
  color: #17202a;
  background: #fff;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 800;
}

.campaign-copy strong {
  font-family: Fredoka, 'Nunito', 'Microsoft YaHei', sans-serif;
  font-size: clamp(24px, 3vw, 42px);
  line-height: 1.15;
  text-wrap: balance;
}

.campaign-action {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  gap: 7px;
  color: #fff;
  font-size: 14px;
  font-weight: 800;
}

.campaign-arrow {
  position: absolute;
  z-index: 2;
  top: 50%;
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  color: #17202a;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.82);
  border-radius: 8px;
  box-shadow: 0 5px 16px rgba(0, 0, 0, 0.18);
  transform: translateY(-50%);
  transition: background 180ms ease-out, transform 180ms ease-out;
}

.campaign-arrow:hover {
  background: #fff;
  transform: translateY(-50%) scale(1.04);
}

.campaign-prev { left: 14px; }
.campaign-next { right: 14px; }

.campaign-dots {
  position: absolute;
  z-index: 2;
  right: 18px;
  bottom: 16px;
  display: flex;
  gap: 8px;
}

.campaign-dots button {
  width: 24px;
  height: 8px;
  padding: 0;
  background: rgba(255, 255, 255, 0.58);
  border: 0;
  border-radius: 4px;
  transition: background 180ms ease-out, width 180ms ease-out;
}

.campaign-dots button.active {
  width: 36px;
  background: #fff;
}

.campaign-fade-enter-active,
.campaign-fade-leave-active {
  transition: opacity 220ms ease-out;
}

.campaign-fade-enter-from,
.campaign-fade-leave-to {
  opacity: 0;
}

@media (max-width: 720px) {
  .campaign-banner {
    min-height: 260px;
    aspect-ratio: 16 / 11;
  }

  .campaign-slide::after {
    background: linear-gradient(0deg, rgba(20, 25, 28, 0.82) 0%, rgba(20, 25, 28, 0.18) 75%);
  }

  .campaign-copy {
    right: 20px;
    bottom: 32px;
    left: 20px;
    max-width: none;
  }

  .campaign-copy strong {
    font-size: 26px;
  }

  .campaign-arrow {
    top: 18px;
    width: 40px;
    height: 40px;
    transform: none;
  }

  .campaign-arrow:hover {
    transform: scale(1.04);
  }

  .campaign-prev { left: auto; right: 62px; }
  .campaign-next { right: 14px; }
}

@media (prefers-reduced-motion: reduce) {
  .campaign-arrow,
  .campaign-dots button,
  .campaign-fade-enter-active,
  .campaign-fade-leave-active {
    transition: none;
  }
}
</style>
