<template>
  <div v-if="banners.length" class="page-hero banner-hero">
    <div class="banner-track" :class="{ 'no-transition': !transitioning }" :style="{ transform: `translateX(-${activeSlide * 100}%)` }" @click="goBannerLink">
      <div v-for="(s, i) in slides" :key="i" class="banner-slide">
        <div v-if="s.type === 'welcome'" class="slide-welcome">
          <h1>首页</h1>
          <p>{{ s.text }}</p>
        </div>
        <template v-else>
          <img :src="s.image_url_wsh" :alt="s.title_wsh">
          <div class="banner-label">{{ s.title_wsh }}</div>
        </template>
      </div>
    </div>
    <button v-if="slides.length > 1" class="banner-prev" @click.stop="prevSlide">‹</button>
    <button v-if="slides.length > 1" class="banner-next" @click.stop="nextSlide">›</button>
    <div class="banner-dots">
      <span v-for="(s, i) in slides" :key="i" :class="{ active: activeSlide === i }" @click.stop="goSlide(i)"></span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch, onUnmounted } from 'vue'

const props = defineProps({
  banners: { type: Array, default: () => [] }
})

const activeSlide = ref(0)
const transitioning = ref(true)
let bannerTimer = null

const slides = computed(() => {
  const list = [{ type: 'welcome', text: '欢迎来到宠物寄养平台' }]
  props.banners.forEach(b => list.push({ type: 'banner', ...b }))
  return list
})

function changeSlide(index) {
  index = (index + slides.value.length) % slides.value.length
  const wrap = Math.abs(index - activeSlide.value) > 1
  if (wrap) {
    transitioning.value = false
    activeSlide.value = index
    requestAnimationFrame(() => {
      requestAnimationFrame(() => { transitioning.value = true })
    })
  } else {
    activeSlide.value = index
  }
}

function goBannerLink() {
  const s = slides.value[activeSlide.value]
  if (s?.type === 'banner' && s.link_url_wsh) window.open(s.link_url_wsh, '_blank')
}

function prevSlide() { changeSlide(activeSlide.value - 1) }

function nextSlide() { changeSlide(activeSlide.value + 1) }

function goSlide(i) { changeSlide(i) }

function startAutoSlide() {
  if (bannerTimer) clearInterval(bannerTimer)
  if (slides.value.length > 1) {
    bannerTimer = setInterval(() => changeSlide(activeSlide.value + 1), 6000)
  }
}

watch(() => props.banners, startAutoSlide, { immediate: true })

onUnmounted(() => {
  if (bannerTimer) clearInterval(bannerTimer)
})
</script>

<style scoped>
.banner-hero {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  padding: 0;
  height: 200px;
}

.banner-track {
  display: flex;
  height: 100%;
  transition: transform 0.35s ease;
}

.banner-track.no-transition {
  transition: none;
}

.banner-slide {
  min-width: 100%;
  height: 100%;
  position: relative;
}

.banner-slide img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.slide-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: var(--color-primary);
  color: #fff;
}

.slide-welcome h1 {
  font-size: 28px;
  margin-bottom: 4px;
}

.slide-welcome p {
  color: var(--color-muted-foreground);
}

.banner-label {
  position: absolute;
  right: 16px;
  bottom: 40px;
  background: linear-gradient(135deg, rgba(0,0,0,0.6), rgba(0,0,0,0.3));
  color: #fff;
  padding: 8px 18px;
  border-radius: 6px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.5px;
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255,255,255,0.15);
}

.banner-prev,
.banner-next {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(0,0,0,0.3);
  color: #fff;
  border: none;
  font-size: 24px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  line-height: 36px;
  text-align: center;
  z-index: 2;
}

.banner-prev { left: 12px; }
.banner-next { right: 12px; }

.banner-prev:hover,
.banner-next:hover {
  background: rgba(0,0,0,0.6);
}

.banner-dots {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
}

.banner-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255,255,255,0.5);
  cursor: pointer;
}

.banner-dots span.active {
  background: #fff;
}
</style>
