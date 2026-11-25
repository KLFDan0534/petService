<template>
  <div class="tl" :style="vars" aria-hidden="true">
    <!-- 底部环境反光：卡立在桌面上，下沿收一点环境光 -->
    <div class="tl-ambient" />
    <!-- 拉丝条带：角度与位置都跟指针横向位置走 -->
    <div class="tl-streak" />
    <!-- 主高光 · 对比层（overlay）：亮处更亮、暗处更暗，白芯才有可压的区间 -->
    <div class="tl-contrast" />
    <!-- 主高光 · 提亮层（screen）：只有这一种混合，浅色金属上不会顶到 255 -->
    <div class="tl-glow" />
    <!-- 边缘光：conic 方向性亮边，mask 只留 1.5px 内 -->
    <div class="tl-rim" />
    <!-- 颗粒：去掉渐变过度干净的塑料感 -->
    <div class="tl-grain" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { GRAIN_URL, lightVars } from '@/domain/MembershipTier'

/**
 * 卡片光照层，叠在卡面材质之上、内容之下：
 * 环境反光 · 拉丝条带 · 主高光（overlay 对比 + screen 提亮）· 边缘光（conic，1.5px 内）· 颗粒。
 *
 * 所有参数来自 --l-* 变量（见 MembershipTier.lightVars），指针坐标来自宿主上的 --ms-* 变量
 * （见 useCardLight）。宿主需要 isolate，混合只发生在卡内。
 * scale=hero 用于身份卡（提亮半径 = 短边 × glowR），scale=plan 用于套餐卡（半径 = 短边 × 1.2，钟形包络、无硬核）。
 */
const props = defineProps({
  tier: { type: Object, required: true },
  scale: { type: String, default: 'hero' },
})

const vars = computed(() => ({
  ...lightVars(props.tier, props.scale),
  '--tl-grain': GRAIN_URL,
}))
</script>

<style scoped>
@property --ms-px {
  syntax: '<number>';
  inherits: true;
  initial-value: 0.72;
}

.tl {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  /* 光照强度：未悬停时为 rest，指针进入后随 --ms-e 升到 peak */
  --l-int: calc(var(--l-rest, 1) + var(--ms-e, 0) * (var(--l-peak, 1) - var(--l-rest, 1)));
  overflow: hidden;
}

.tl-ambient,
.tl-streak,
.tl-contrast,
.tl-glow,
.tl-rim,
.tl-grain {
  position: absolute;
  inset: 0;
  border-radius: inherit;
}

.tl-ambient {
  background-image: linear-gradient(
    to top,
    rgba(var(--l-rgb), var(--l-ambient, 0.08)) 0%,
    rgba(var(--l-rgb), 0) 44%
  );
}

/*
 * 拉丝条带：角度 = a0 + px × (a1 − a0)，位置随 px 从左扫到右；亮芯半宽 --l-band。
 * 两侧各一道极淡的暗肩：浅色金属上白芯本身看不出来，靠暗肩衬出拉丝；深色卡几乎为 0。
 */
.tl-streak {
  --ms-angle: calc(var(--l-a0) * 1deg + var(--ms-px) * (var(--l-a1) - var(--l-a0)) * 1deg);
  --ms-sp: calc(12% + var(--ms-px) * 76%);
  background-image: linear-gradient(
    var(--ms-angle),
    transparent calc(var(--ms-sp) - var(--l-band, 9%) * 1.8),
    rgba(0, 0, 0, var(--l-shoulder, 0)) calc(var(--ms-sp) - var(--l-band, 9%) * 1.1),
    rgba(var(--l-rgb), var(--l-streak)) var(--ms-sp),
    rgba(0, 0, 0, var(--l-shoulder, 0)) calc(var(--ms-sp) + var(--l-band, 9%) * 1.1),
    transparent calc(var(--ms-sp) + var(--l-band, 9%) * 1.8)
  );
  opacity: var(--l-int);
}

.tl-contrast {
  background-image: radial-gradient(
    ellipse var(--l-ellipse, 56% 72%) at var(--ms-mx, 72%) var(--ms-my, 24%),
    rgba(255, 255, 255, 0.95) 0%,
    rgba(255, 255, 255, 0.42) 28%,
    rgba(255, 255, 255, 0) 50%,
    rgba(0, 0, 0, 0) 54%,
    rgba(0, 0, 0, var(--l-dark, 0.6)) 100%
  );
  mix-blend-mode: overlay;
  opacity: calc(var(--l-contrast, 1) * var(--l-int));
}

.tl-glow {
  background-image: var(--l-glow-image);
  mix-blend-mode: screen;
  opacity: calc(var(--l-glow, 0) * var(--l-int));
}

/*
 * 边缘光：conic 造一圈方向性亮边，最亮处朝 --ms-ra，±110° 外归零。
 * mask 只留边缘 1.5px：padding 1.5px + content-box 与整块做 exclude，跟着圆角走。
 */
.tl-rim {
  --rim-a: min(var(--l-rim-max, 0.35), calc(var(--l-rim, 0.3) * var(--ms-rs, 0.45) * var(--l-int)));
  padding: 1.5px;
  background: conic-gradient(
    from calc(var(--ms-ra, 315) * 1deg),
    rgba(var(--l-rgb), var(--rim-a)) 0deg,
    rgba(var(--l-rgb), calc(var(--rim-a) * 0.35)) 60deg,
    rgba(var(--l-rgb), 0) 110deg,
    rgba(var(--l-rgb), 0) 250deg,
    rgba(var(--l-rgb), calc(var(--rim-a) * 0.35)) 300deg,
    rgba(var(--l-rgb), var(--rim-a)) 360deg
  );
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask: linear-gradient(#000 0 0) content-box exclude, linear-gradient(#000 0 0);
}

.tl-grain {
  background-image: var(--tl-grain);
  opacity: var(--l-grain, 0.04);
  mix-blend-mode: overlay;
}

/* 减少动态效果：条带不出现，光照停在默认位置 */
@media (prefers-reduced-motion: reduce) {
  .tl-streak { display: none; }
}
</style>
