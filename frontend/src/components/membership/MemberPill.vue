<template>
  <router-link
    class="mpill"
    :class="`is-${tier.key}`"
    :style="pillStyle"
    to="/membership"
    :aria-label="`${name}，查看会员中心`"
    :title="`${name} · 服务折扣 ${discount}`"
  >
    <TierEmblem :tier="tier" :size="14" hoverable class="mpill-emblem" />
    <span class="mpill-name">{{ name }}</span>
    <span class="mpill-shine" aria-hidden="true" />
  </router-link>
</template>

<script setup>
import { computed } from 'vue'
import { discountLabel, tierOf, tierVars } from '@/domain/MembershipTier'
import TierEmblem from './TierEmblem.vue'

/**
 * 会员身份胶囊：跟在「已实名」旁边，一眼看出是哪一档。
 * 底色取本档材质（相伴银 / 守护金 / 挚爱藏蓝），进页面时一道高光扫过、之后悬停轻微抬升。
 * 只在有生效会员时渲染 —— 没有会员就不显示，不占位、不写「未开通」。
 */
const props = defineProps({
  name: { type: String, required: true },
  level: { type: [Number, String], default: 1 },
  discountRate: { type: [Number, String], default: null },
})

const tier = computed(() => tierOf(props.level))
const discount = computed(() => discountLabel(props.discountRate))
const pillStyle = computed(() => tierVars(tier.value))
</script>

<style scoped>
.mpill {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  overflow: hidden;
  padding: 3px 9px 3px 7px;
  border: 1px solid var(--t-card-line);
  border-radius: var(--r-tag, 6px);
  background: var(--t-card);
  color: var(--t-card-ink);
  font-size: 11px;
  font-weight: 600;
  line-height: 1.4;
  text-decoration: none;
  white-space: nowrap;
  isolation: isolate;
  animation: mpill-in 420ms cubic-bezier(0.23, 1, 0.32, 1) both;
  transition: transform 200ms cubic-bezier(0.23, 1, 0.32, 1), box-shadow 200ms ease;
}
.mpill:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px -12px var(--t-drop);
}
.mpill:focus-visible {
  outline: 2px solid var(--t-card-ink);
  outline-offset: 2px;
}

.mpill-name {
  position: relative;
  z-index: 1;
  max-width: 9em;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mpill-emblem {
  position: relative;
  z-index: 1;
  flex: 0 0 auto;
}

/* 一道高光扫过：只播一次，不循环；减少动态效果时不出现 */
.mpill-shine {
  position: absolute;
  top: -60%;
  bottom: -60%;
  left: 0;
  width: 42%;
  background-image: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.85), transparent);
  mix-blend-mode: overlay;
  pointer-events: none;
  animation: mpill-shine 720ms cubic-bezier(0.65, 0, 0.35, 1) 260ms both;
}

/* 挚爱档深底：overlay 的白光在深色上几乎看不见，补一层 screen 提亮 */
.mpill.is-beloved .mpill-shine {
  background-image:
    linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.85), transparent),
    linear-gradient(100deg, transparent, rgba(200, 228, 255, 0.55), transparent);
  background-blend-mode: overlay, screen;
}

@keyframes mpill-in {
  from { opacity: 0; transform: translateY(4px) scale(0.94); }
  to { opacity: 1; transform: none; }
}
@keyframes mpill-shine {
  from { transform: translateX(-140%) skewX(-12deg); }
  to { transform: translateX(340%) skewX(-12deg); }
}

@media (prefers-reduced-motion: reduce) {
  .mpill { animation: none; transition: none; }
  .mpill-shine { display: none; }
}
</style>
