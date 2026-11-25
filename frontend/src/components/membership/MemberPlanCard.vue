<template>
  <div
    ref="cardRef"
    class="mp"
    :class="{ 'is-selected': selected, 'is-current': isCurrent, 'is-featured': featured }"
    :style="cardStyle"
    role="radio"
    :aria-checked="selected"
    tabindex="0"
    @click="emit('select')"
    @keydown.enter.prevent="emit('select')"
    @keydown.space.prevent="emit('select')"
    @pointermove="light.onPointerMove"
    @pointerleave="light.onPointerLeave"
  >
    <!-- 底色与光照同一层：带 z-index 的层自成混合组，overlay / screen 要和底色在同一组里才看得见 -->
    <div class="mp-material" aria-hidden="true">
      <div class="mp-base" />
      <TierLight :tier="tier" scale="plan" />
    </div>

    <!-- 选中：卡内左缘一条 2px 指示线 -->
    <span v-if="selected" class="mp-rail" aria-hidden="true" />

    <header class="mp-head">
      <div class="mp-head-copy">
        <p class="mp-kicker">
          <span v-if="featured" class="mp-badge">主推</span>
          {{ tier.en }} · {{ tier.lv }}
        </p>
        <h3 class="mp-name">{{ plan.name_wsh }}</h3>
        <p v-if="tier.tagline" class="mp-tagline">{{ tier.tagline }}</p>
      </div>
      <TierEmblem :tier="tier" :size="emblemSize" ring hoverable class="mp-emblem" />
    </header>

    <div class="mp-price-row">
      <RollingNumber :value="priceText" :class="featured ? 'mp-price is-featured' : 'mp-price'" />
      <span class="mp-days">/ {{ plan.duration_days_wsh ?? '—' }} 天</span>
    </div>
    <p class="mp-perday">{{ dayText }}</p>

    <ul class="mp-perks">
      <li v-for="perk in perks" :key="perk">
        <svg class="mp-check" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M20 6 9 17l-5-5" />
        </svg>
        {{ perk }}
      </li>
      <li v-if="!perks.length" class="mp-perk-empty">
        <svg class="mp-check" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M20 6 9 17l-5-5" />
        </svg>
        暂无附加权益
      </li>
    </ul>

    <div class="mp-foot">
      <button type="button" class="mp-cta" :disabled="busy" @click.stop="emit('open')">{{ ctaText }}</button>
      <p class="mp-note">{{ noteText }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useCardLight } from '@/composables/useCardLight'
import { EMBLEM, discountLabel, perDay, planExtras, tierOf, tierVars } from '@/domain/MembershipTier'
import RollingNumber from './RollingNumber.vue'
import TierEmblem from './TierEmblem.vue'
import TierLight from './TierLight.vue'

/**
 * 套餐卡：等级徽记 + 名称 + 一句话定位 + 价格 + 日均 + 权益（月券 + 下单折扣，全部来自 planExtras）。
 * 底色、正文、按钮全部取自本档的 --t-*：挚爱档深底浅字、相伴守护浅底深字，不跟随页面主题变化。
 * 材质：缎面金属底 → 光照（参数取本档 plan 档：overlay 对比 · screen 提亮 · conic 边缘光 · 噪点）。
 * 悬停时光照强度升到 1.08 倍，离开时同一插值回落；减少动态效果时不倾斜、不跟随。
 */
const props = defineProps({
  plan: { type: Object, required: true },
  featured: { type: Boolean, default: false },
  current: { type: Object, default: null },
  selected: { type: Boolean, default: false },
  hasPending: { type: Boolean, default: false },
  busy: { type: Boolean, default: false },
})

const emit = defineEmits(['select', 'open'])

const cardRef = ref(null)
const light = useCardLight()
const tier = computed(() => tierOf(props.plan?.level_wsh))
const isCurrent = computed(() => Boolean(props.current) && props.current.id_wsh === props.plan?.id_wsh)
const perks = computed(() => planExtras(props.plan))
const emblemSize = computed(() => (props.featured ? Math.round(EMBLEM.card * 1.2) : EMBLEM.card))

const priceText = computed(() => {
  const price = Number(props.plan?.price_wsh)
  return Number.isFinite(price) ? `¥${price.toLocaleString('zh-CN')}` : '—'
})
const dayText = computed(() => {
  const day = perDay(props.plan)
  if (day == null) return `折扣 ${discountLabel(props.plan?.discount_rate_wsh)}`
  return `约 ¥${day.toFixed(2)} / 天 · ${discountLabel(props.plan?.discount_rate_wsh)}`
})

const ctaText = computed(() => {
  if (isCurrent.value) return '续费本套餐'
  if (props.hasPending) return '继续支付'
  return '立即开通'
})
const noteText = computed(() => {
  if (isCurrent.value) return '当前生效中的套餐'
  if (props.hasPending) return '有一笔待支付订单'
  return ''
})

const cardStyle = computed(() => ({
  ...tierVars(tier.value),
  color: 'var(--t-ink)',
  borderColor: props.selected ? 'var(--t-border-hover)' : 'var(--t-border)',
  boxShadow: props.selected
    ? '0 0 0 3px color-mix(in srgb, var(--t-border-hover) 24%, transparent), 0 22px 44px -30px var(--t-drop)'
    : '0 18px 40px -34px var(--t-drop)',
}))

watch(cardRef, (node) => {
  if (!node) return
  light.configure({ tilt: tier.value.light.tilt, glow: 1.2 })
  light.attach(node)
}, { immediate: true })
</script>

<style scoped>
.mp {
  position: relative;
  isolation: isolate;
  display: flex;
  height: 100%;
  cursor: pointer;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--t-border);
  border-radius: var(--radius-card);
  padding: 20px;
  outline: none;
  will-change: transform;
  transition:
    border-color 360ms cubic-bezier(0.23, 1, 0.32, 1),
    box-shadow 360ms cubic-bezier(0.23, 1, 0.32, 1),
    transform 200ms cubic-bezier(0.23, 1, 0.32, 1);
}
.mp:hover { border-color: var(--t-border-hover); transform: translateY(-4px); }
.mp.is-selected { transform: translateY(-3px); }
.mp:focus-visible { box-shadow: 0 0 0 2px var(--t-border-hover); }

.mp-material {
  position: absolute;
  inset: 0;
  z-index: -1;
  overflow: hidden;
  border-radius: inherit;
}
.mp-base {
  position: absolute;
  inset: 0;
  background-image: var(--t-brushed, none);
  background-color: var(--t-surface);
}

.mp-rail {
  position: absolute;
  top: 24px;
  bottom: 24px;
  left: -1px;
  width: 2px;
  border-radius: 999px;
  background: var(--t-border-hover);
  animation: mp-rail-in 280ms cubic-bezier(0.65, 0, 0.35, 1) both;
}

.mp-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.mp-head-copy { min-width: 0; }
.mp-kicker {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--t-sub);
}
.mp-badge {
  padding: 1px 6px;
  border: 1px solid var(--t-border-hover);
  border-radius: 4px;
  color: var(--t-ink);
  font-family: var(--ref-font-sans);
  font-size: 11px;
  font-weight: 500;
  letter-spacing: normal;
  line-height: 16px;
  text-transform: none;
}
.mp-name {
  margin: 8px 0 0;
  font-family: var(--ref-font-display);
  font-size: 17px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--t-ink);
  overflow-wrap: anywhere;
}
.mp-tagline {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.4;
  color: var(--t-sub);
}
.mp-emblem { flex: 0 0 auto; margin: 6px 6px 0 0; }

.mp-price-row {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px;
  margin-top: 22px;
}
.mp-price {
  font-family: var(--ref-font-display);
  font-size: 40px;
  line-height: 1;
  letter-spacing: -0.02em;
  color: var(--t-ink);
}
.mp-price.is-featured { font-size: 48px; }
.mp-days {
  font-size: 13px;
  color: var(--t-sub);
}
.mp-perday {
  margin: 8px 0 0;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  color: var(--t-sub);
}

.mp-perks {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  margin: 20px 0 0;
  padding: 18px 0 0;
  list-style: none;
  border-top: 1px solid color-mix(in srgb, var(--t-ink) 18%, transparent);
}
.mp-perks li {
  display: flex;
  flex: 1;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  line-height: 1.6;
}
.mp-check {
  flex: 0 0 auto;
  margin-top: 3px;
  color: var(--t-accent);
}
.mp-perk-empty { color: var(--t-sub); }

.mp-foot {
  margin-top: auto;
  padding-top: 22px;
}
.mp-cta {
  display: inline-flex;
  width: 100%;
  height: 44px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  border-radius: var(--radius-control, 10px);
  background: var(--t-cta);
  color: var(--t-cta-ink);
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: background 150ms ease, transform 150ms cubic-bezier(0.23, 1, 0.32, 1);
}
.mp-cta:hover:not(:disabled) { background: var(--t-cta-hover); }
.mp-cta:active:not(:disabled) { transform: scale(0.98); }
.mp-cta:disabled { opacity: 0.6; cursor: not-allowed; }
.mp-note {
  height: 16px;
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 16px;
  text-align: center;
  color: var(--t-sub);
}

@keyframes mp-rail-in {
  from { transform: scaleY(0.4); opacity: 0; }
  to { transform: scaleY(1); opacity: 1; }
}

@media (prefers-reduced-motion: reduce) {
  .mp, .mp-cta { transition: none; }
  .mp:hover, .mp.is-selected { transform: none; }
  .mp-rail { animation: none; }
}
</style>
