<template>
  <article
    ref="cardRef"
    class="mi"
    :style="cardStyle"
    :aria-label="previewing ? `预览${name}身份卡` : active ? `${name}身份卡` : '尚未开通会员'"
    @pointermove="light.onPointerMove"
    @pointerleave="light.onPointerLeave"
  >
    <!-- 材质 + 光照整层按等级交叉淡入：旧一档淡出、新一档淡入，不跳变 -->
    <Transition name="mi-fade">
      <div :key="tier.key" class="mi-material" aria-hidden="true">
        <div class="mi-base" />
        <div class="mi-grain" />
        <TierEmblem :tier="tier" outline :size="240" class="mi-watermark" />
        <TierLight :tier="tier" scale="hero" />
      </div>
    </Transition>

    <!-- 开通成功：高光从左到右扫过一次，只播一次 -->
    <span v-if="sweep > 0" :key="`sweep-${sweep}`" class="mi-sweep" aria-hidden="true">
      <span class="mi-sweep-overlay" />
      <span class="mi-sweep-screen" />
    </span>

    <header class="mi-head">
      <div class="mi-head-copy">
        <p class="mi-kicker">
          {{ previewing ? `预览 · ${tier.en} ${tier.lv}` : active ? `${tier.en} ${tier.lv}` : '未开通' }}
        </p>
        <Transition name="mi-rise" mode="out-in">
          <h3 :key="name" class="mi-name">{{ name }}</h3>
        </Transition>
        <Transition name="mi-fade" mode="out-in">
          <p :key="`${name}-voice`" class="mi-tagline">{{ tagline }}</p>
        </Transition>
        <ul v-if="perks.length" class="mi-perks" aria-label="本档权益">
          <li v-for="(perk, index) in perks" :key="perk" :style="{ animationDelay: `${60 + index * 40}ms` }">
            <span class="mi-perk-dot" aria-hidden="true" />
            {{ perk }}
          </li>
        </ul>
      </div>

      <div class="mi-head-side">
        <Transition name="mi-fade" mode="out-in">
          <span v-if="soonDays !== null" :key="soonDays" class="mi-soon">
            {{ soonDays === 0 ? '今天到期' : `${soonDays} 天后到期` }}
          </span>
        </Transition>
        <TierEmblem
          :key="`emblem-${sweep}-${tier.key}`"
          :tier="tier"
          :size="EMBLEM.hero"
          :heartbeat="sweep > 0"
          hoverable
          class="mi-emblem"
        />
      </div>
    </header>

    <div class="mi-foot">
      <dl class="mi-facts">
        <div v-for="fact in facts" :key="fact.key">
          <dt>{{ fact.label }}</dt>
          <dd>
            <RollingNumber v-if="fact.rolling" :value="fact.value" />
            <template v-else>{{ fact.value }}</template>
          </dd>
        </div>
      </dl>

      <div v-if="previewing" class="mi-actions">
        <button type="button" class="mi-quiet" @click="emit('clear-preview')">
          {{ active ? '回到当前等级' : '收起预览' }}
        </button>
        <button type="button" class="mi-cta" @click="emit('open-preview')">开通</button>
      </div>
      <div v-else-if="!active" class="mi-actions">
        <button type="button" class="mi-cta" @click="emit('browse')">查看套餐</button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useCardLight } from '@/composables/useCardLight'
import {
  EMBLEM,
  discountLabel,
  fmtDate,
  getValidity,
  isActiveMembership,
  memberName,
  perDay,
  planExtras,
  tierOf,
  tierVars,
} from '@/domain/MembershipTier'
import RollingNumber from './RollingNumber.vue'
import TierEmblem from './TierEmblem.vue'
import TierLight from './TierLight.vue'

/**
 * 会员身份卡：一张实体卡。
 * 自下而上：卡面底色 → 拉丝纹理 → 压印徽记 → 光照（环境反光 · 拉丝条带 · 主高光 · 边缘光 · 颗粒）→ 内容。
 * 光照随指针走，倾斜角与高光强度取本档参数；整层材质按等级 key 交叉淡入，换档不跳变。
 * 点选一张未开通的套餐卡时预览那一档：徽记形变、底色、名称、tagline、权益与价格滚动一起联动。
 */
const props = defineProps({
  membership: { type: Object, default: null },
  /** 当前生效套餐（按 plan_id_wsh 匹配），用于取会员号与权益 */
  plan: { type: Object, default: null },
  /** 正在预览的套餐（点选未开通的那一档） */
  preview: { type: Object, default: null },
  /** 每次开通成功 +1，卡面高光扫过一次 */
  sweep: { type: Number, default: 0 },
})

const emit = defineEmits(['browse', 'open-preview', 'clear-preview'])

const cardRef = ref(null)
const light = useCardLight()

const validity = computed(() => getValidity(props.membership))
const active = computed(() => isActiveMembership(props.membership) && !validity.value.expired)
const previewing = computed(() => Boolean(props.preview))
const level = computed(() => (previewing.value
  ? props.preview?.level_wsh
  : active.value ? props.membership?.level_wsh : null))
const tier = computed(() => tierOf(level.value))

const name = computed(() => {
  if (previewing.value) return props.preview?.name_wsh || '会员'
  if (active.value) return memberName(props.membership, props.plan) || '会员'
  return '尚未开通会员'
})

const tagline = computed(() => tier.value.tagline
  || '开通后全部照护服务按折扣价结算，到期不自动续费。')

const discount = computed(() => discountLabel(
  previewing.value
    ? props.preview?.discount_rate_wsh
    : active.value ? (props.membership?.discount_rate_wsh ?? props.plan?.discount_rate_wsh) : null,
))

const shownPlan = computed(() => (previewing.value ? props.preview : active.value ? props.plan : null))
const perks = computed(() => (shownPlan.value ? planExtras(shownPlan.value) : []))

/** 剩余 7 天内才出现到期角标 */
const soonDays = computed(() => {
  if (!active.value || previewing.value) return null
  const days = validity.value.remainingDays
  return days != null && days <= 7 ? days : null
})

const facts = computed(() => {
  if (previewing.value) {
    const daily = props.preview ? perDay(props.preview) : null
    return [
      { key: 'price', label: '价格', value: `¥${Number(props.preview?.price_wsh || 0).toLocaleString('zh-CN')}`, rolling: true },
      { key: 'days', label: '有效期', value: `${props.preview?.duration_days_wsh ?? '—'} 天`, rolling: true },
      { key: 'daily', label: '日均', value: daily != null ? `¥${daily.toFixed(2)}/天` : '—', rolling: true },
      { key: 'discount', label: '折扣', value: discount.value, rolling: false },
    ]
  }
  if (!active.value) return []
  return [
    { key: 'no', label: '会员号', value: `MS-${String(props.membership?.id_wsh ?? 0).padStart(6, '0')}`, rolling: false },
    { key: 'expires', label: '有效期至', value: fmtDate(validity.value.expiresAt), rolling: false },
    { key: 'discount', label: '折扣', value: discount.value, rolling: false },
  ]
})

const cardStyle = computed(() => ({
  ...tierVars(tier.value),
  boxShadow: 'inset 0 0 0 1px var(--t-card-line), 0 28px 56px -34px var(--t-drop)',
}))

watch(tier, (next) => {
  light.configure({ tilt: next.light.tilt, glow: next.light.glowR })
  light.attach(cardRef.value)
}, { immediate: true })

// 组件挂载后再绑一次：immediate 时模板还没渲染，拿不到元素
watch(cardRef, (node) => {
  if (node) {
    light.configure({ tilt: tier.value.light.tilt, glow: tier.value.light.glowR })
    light.attach(node)
  }
}, { immediate: true })
</script>

<style scoped>
.mi {
  position: relative;
  isolation: isolate;
  display: flex;
  min-height: 248px;
  flex-direction: column;
  justify-content: space-between;
  gap: 28px;
  overflow: hidden;
  border-radius: var(--radius-card);
  padding: 22px;
  will-change: transform;
  transition: box-shadow 240ms ease;
}

/* ── 材质层 ── */
.mi-material {
  position: absolute;
  inset: 0;
  z-index: -1;
  border-radius: inherit;
  overflow: hidden;
}
.mi-base {
  position: absolute;
  inset: 0;
  background: var(--t-card);
}
.mi-grain {
  position: absolute;
  inset: 0;
  background-image: var(--t-grain);
}
.mi-watermark {
  position: absolute;
  right: -40px;
  bottom: -64px;
  color: var(--t-card-ink);
  opacity: 0.09;
}

/* ── 内容 ── */
.mi-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.mi-head-copy { min-width: 0; }
.mi-kicker {
  margin: 0;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--t-card-sub);
  transition: color 360ms cubic-bezier(0.23, 1, 0.32, 1);
}
.mi-name {
  margin: 8px 0 0;
  font-family: var(--ref-font-display);
  font-size: clamp(24px, 3vw, 30px);
  font-weight: 600;
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--t-card-ink);
  overflow-wrap: anywhere;
  transition: color 360ms cubic-bezier(0.23, 1, 0.32, 1);
}
.mi-tagline {
  margin: 8px 0 0;
  max-width: 30ch;
  font-size: 13px;
  line-height: 1.65;
  color: var(--t-card-sub);
  transition: color 360ms cubic-bezier(0.23, 1, 0.32, 1);
}
.mi-perks {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}
.mi-perks li {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--t-card-ink);
  animation: mi-perk-in 240ms cubic-bezier(0.23, 1, 0.32, 1) both;
}
.mi-perk-dot {
  width: 4px;
  height: 4px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--t-card-sub);
}

.mi-head-side {
  display: flex;
  flex: 0 0 auto;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}
.mi-soon {
  padding: 2px 6px;
  border-radius: var(--radius-inline, 6px);
  background: var(--t-card-glass);
  color: var(--t-card-sub);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.mi-emblem { margin: 4px 4px 0 0; }

.mi-foot {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}
.mi-facts {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px 24px;
  margin: 0;
}
.mi-facts dt {
  font-size: 11px;
  color: var(--t-card-sub);
  transition: color 360ms cubic-bezier(0.23, 1, 0.32, 1);
}
.mi-facts dd {
  margin: 4px 0 0;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 15px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  color: var(--t-card-ink);
  transition: color 360ms cubic-bezier(0.23, 1, 0.32, 1);
}

.mi-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 4px;
}
.mi-quiet {
  height: 36px;
  padding: 0 12px;
  border: 0;
  border-radius: var(--radius-control, 10px);
  background: transparent;
  color: var(--t-card-sub);
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  text-decoration: underline;
  text-decoration-color: transparent;
  text-underline-offset: 4px;
  transition: color 200ms ease, text-decoration-color 200ms ease;
}
.mi-quiet:hover { color: var(--t-card-ink); text-decoration-color: currentColor; }
.mi-cta {
  display: inline-flex;
  height: 36px;
  align-items: center;
  justify-content: center;
  padding: 0 14px;
  border: 0;
  border-radius: var(--radius-control, 10px);
  background: var(--t-cta);
  color: var(--t-cta-ink);
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: background 150ms ease, transform 150ms cubic-bezier(0.23, 1, 0.32, 1);
}
.mi-cta:hover { background: var(--t-cta-hover); }
.mi-cta:active { transform: scale(0.98); }

/* ── 开通成功的高光扫过：overlay 白光负责浅色卡，screen 用本档光色负责深色卡 ── */
.mi-sweep {
  position: absolute;
  top: -20%;
  bottom: -20%;
  left: 0;
  width: 34%;
  pointer-events: none;
  animation: mi-sweep 480ms cubic-bezier(0.65, 0, 0.35, 1) 180ms both;
}
.mi-sweep-overlay,
.mi-sweep-screen {
  position: absolute;
  inset: 0;
}
.mi-sweep-overlay {
  background-image: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.9), transparent);
  mix-blend-mode: overlay;
}
.mi-sweep-screen {
  background-image: linear-gradient(100deg, transparent, rgba(var(--l-rgb), 0.6), transparent);
  mix-blend-mode: screen;
}

@keyframes mi-sweep {
  from { transform: translateX(-120%); }
  to { transform: translateX(320%); }
}
@keyframes mi-perk-in {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: none; }
}

/* ── 过渡 ── */
.mi-fade-enter-active,
.mi-fade-leave-active { transition: opacity 340ms cubic-bezier(0.23, 1, 0.32, 1); }
.mi-fade-enter-from,
.mi-fade-leave-to { opacity: 0; }
.mi-rise-enter-active,
.mi-rise-leave-active { transition: opacity 280ms cubic-bezier(0.23, 1, 0.32, 1), transform 280ms cubic-bezier(0.23, 1, 0.32, 1); }
.mi-rise-enter-from,
.mi-rise-leave-to { opacity: 0; transform: translateY(6px); }

@media (max-width: 560px) {
  .mi { padding: 20px; }
  .mi-watermark { right: -70px; }
}

@media (prefers-reduced-motion: reduce) {
  .mi, .mi-cta { transition: none; }
  .mi-sweep { display: none; }
  .mi-perks li { animation: none; }
  .mi-fade-enter-active,
  .mi-fade-leave-active,
  .mi-rise-enter-active,
  .mi-rise-leave-active { transition: opacity 200ms ease; }
  .mi-rise-enter-from,
  .mi-rise-leave-to { transform: none; }
}
</style>
