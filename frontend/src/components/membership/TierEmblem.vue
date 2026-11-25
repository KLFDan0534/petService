<template>
  <span
    class="te"
    :class="{ 'is-hot': hot, 'is-beat': beat, 'is-outline': outline }"
    :style="{ width: `${size}px`, height: `${size}px` }"
    @mouseenter="hoverable && (hot = true)"
    @mouseleave="hoverable && (hot = false)"
  >
    <!-- 守护档外圈刻度环：静止不自转，没有事件驱动的循环运动不做 -->
    <span v-if="ring && tier.key === 'guardian'" class="te-ring" :style="{ color: tier.metal[2] }" aria-hidden="true">
      <svg viewBox="0 0 120 120">
        <circle cx="60" cy="60" r="58.5" fill="none" stroke="currentColor" stroke-opacity="0.35" stroke-width="0.6" />
        <line
          v-for="tick in RING_TICKS"
          :key="tick.a"
          :x1="tick.x1"
          :y1="tick.y1"
          :x2="tick.x2"
          :y2="tick.y2"
          stroke="currentColor"
          :stroke-opacity="tick.major ? 0.8 : 0.45"
          :stroke-width="tick.major ? 1.1 : 0.7"
          stroke-linecap="round"
        />
      </svg>
    </span>

    <svg class="te-svg" viewBox="0 0 100 100" aria-hidden="true">
      <defs>
        <linearGradient v-if="!outline" :id="metalId" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" :stop-color="tier.metal[0]" />
          <stop offset="55%" :stop-color="tier.metal[1]" />
          <stop offset="100%" :stop-color="tier.metal[2]" />
        </linearGradient>
        <!-- 同一组金属色、反向受光：爪印读作压进金属里的浮雕 -->
        <linearGradient v-if="!outline" :id="pawId" x1="1" y1="1" x2="0" y2="0">
          <stop offset="0%" :stop-color="tier.metal[0]" />
          <stop offset="50%" :stop-color="tier.metal[1]" />
          <stop offset="100%" :stop-color="tier.metal[2]" />
        </linearGradient>
        <!-- 一枚爪印的几何只定义一次，两处用 <use> 复用，填充/描边由引用处继承 -->
        <g :id="pawShapeId">
          <path d="M0 1.5 C-6.5 1.5 -10.5 7 -9.5 10.8 C-8.6 14 -4.6 14.2 0 12.9 C4.6 14.2 8.6 14 9.5 10.8 C10.5 7 6.5 1.5 0 1.5 Z" />
          <ellipse cx="-9.6" cy="-3.2" rx="3.1" ry="4.1" transform="rotate(-24 -9.6 -3.2)" />
          <ellipse cx="-3.6" cy="-8.6" rx="3.3" ry="4.4" transform="rotate(-8 -3.6 -8.6)" />
          <ellipse cx="3.6" cy="-8.6" rx="3.3" ry="4.4" transform="rotate(8 3.6 -8.6)" />
          <ellipse cx="9.6" cy="-3.2" rx="3.1" ry="4.1" transform="rotate(24 9.6 -3.2)" />
        </g>
      </defs>

      <path
        :d="pathD"
        :fill="outline ? 'none' : `url(#${metalId})`"
        :stroke="line"
        :stroke-width="outline ? Math.max(0.8, 40 / size) : 1.2"
        stroke-linejoin="round"
      />

      <g :key="shape.key">
        <g :stroke="line" :stroke-width="outline ? Math.max(0.6, 30 / size) : 1" fill="none" stroke-linejoin="round">
          <path v-for="d in shape.facets" :key="d" :d="d" />
        </g>
        <use
          v-for="(paw, index) in shape.paws"
          :key="`paw-${index}`"
          :href="`#${pawShapeId}`"
          :transform="pawTransform(paw)"
          :fill="outline ? 'none' : `url(#${pawId})`"
          :stroke="line"
          :stroke-width="(outline ? Math.max(0.6, 30 / size) : 0.9) / paw.s"
          stroke-linejoin="round"
        />
        <!-- 悬停时爪印按 1 → 2 → 3 依次亮起，每枚间隔 80ms -->
        <use
          v-for="(paw, index) in shape.paws"
          :key="`lit-${index}`"
          class="te-lit"
          :href="`#${pawShapeId}`"
          :transform="pawTransform(paw)"
          :fill="tier.metal[0]"
          stroke="none"
          :style="{ opacity: hot && !outline ? 0.8 : 0, transitionDelay: hot ? `${index * 80}ms` : '0ms' }"
        />
      </g>
    </svg>
  </span>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { nextEmblemId } from './emblemId'
import { RING_TICKS, SHAPES, shapeKeyOf } from './emblemShapes'

/**
 * 等级徽记：相伴六边、守护八边、挚爱十二边明亮式切割。
 * 边数随等级增加，内部压一到三枚爪印 —— 爪印数量就是等级，
 * 与多边形同一套金属渐变、方向相反，读作压印浮雕。
 * 三档轮廓统一重采样成 24 个点，换档时外形真实形变；切面与爪印交叉淡入。
 */
const props = defineProps({
  tier: { type: Object, required: true },
  size: { type: Number, default: 40 },
  outline: { type: Boolean, default: false },
  ring: { type: Boolean, default: false },
  hoverable: { type: Boolean, default: false },
  /** 开通成功那一刻做一次心跳脉冲，只播一次 */
  heartbeat: { type: Boolean, default: false },
})

// 每个实例一套独立 id：渐变与爪印几何要被同页多张卡同时引用，不能撞名
const uid = nextEmblemId()
const metalId = `te-m-${uid}`
const pawId = `te-p-${uid}`
const pawShapeId = `te-paw-${uid}`

const hot = ref(false)
const reduce = typeof window !== 'undefined' && window.matchMedia
  ? window.matchMedia('(prefers-reduced-motion: reduce)').matches
  : false
const beat = computed(() => props.heartbeat && !reduce)

const shape = computed(() => SHAPES[shapeKeyOf(props.tier.key)])
const line = computed(() => (props.outline ? 'currentColor' : props.tier.facet))

/* ── 轮廓形变：24 点线性插值，360ms ── */
const points = ref(SHAPES[shapeKeyOf(props.tier.key)].points.map((p) => [...p]))
let raf = 0

function animateTo(target) {
  if (raf) cancelAnimationFrame(raf)
  const from = points.value.map((p) => [...p])
  const started = performance.now()
  const DURATION = 360
  const step = (now) => {
    const raw = Math.min(1, (now - started) / DURATION)
    const t = 1 - Math.pow(1 - raw, 3)
    points.value = from.map((p, i) => [
      p[0] + (target[i][0] - p[0]) * t,
      p[1] + (target[i][1] - p[1]) * t,
    ])
    raf = raw < 1 ? requestAnimationFrame(step) : 0
  }
  raf = requestAnimationFrame(step)
}

watch(() => props.tier.key, (key) => {
  const target = SHAPES[shapeKeyOf(key)].points
  if (reduce) {
    points.value = target.map((p) => [...p])
    return
  }
  animateTo(target)
}, { immediate: true })

onBeforeUnmount(() => {
  if (raf) cancelAnimationFrame(raf)
})

const pathD = computed(() => `M ${points.value.map(([x, y]) => `${x.toFixed(2)} ${y.toFixed(2)}`).join(' L ')} Z`)

function pawTransform(paw) {
  return `translate(${paw.x} ${paw.y}) rotate(${paw.r}) scale(${paw.s})`
}

</script>

<style scoped>
.te {
  position: relative;
  display: inline-block;
  vertical-align: middle;
  transition: transform 160ms cubic-bezier(0.23, 1, 0.32, 1);
}
.te.is-hot { transform: scale(1.03); }
.te.is-hot .te-lit { transition-property: opacity; transition-duration: 160ms; }
.te-lit { transition-property: opacity; transition-duration: 160ms; }

.te-ring {
  position: absolute;
  inset: -24%;
  display: block;
  pointer-events: none;
  animation: te-fade-in 340ms cubic-bezier(0.23, 1, 0.32, 1) both;
}
.te-ring svg { display: block; width: 100%; height: 100%; }

.te-svg { display: block; width: 100%; height: 100%; overflow: visible; }

/* 开通成功：1 → 1.06 → 1，只播一次、不循环 */
.te.is-beat .te-svg {
  animation: te-beat 360ms cubic-bezier(0.23, 1, 0.32, 1) 260ms 1 both;
}

@keyframes te-beat {
  0% { transform: scale(1); }
  38% { transform: scale(1.06); }
  100% { transform: scale(1); }
}
@keyframes te-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

@media (prefers-reduced-motion: reduce) {
  .te, .te.is-hot { transition: none; transform: none; }
  .te.is-beat .te-svg { animation: none; }
  .te-lit { transition-property: opacity !important; }
}
</style>
