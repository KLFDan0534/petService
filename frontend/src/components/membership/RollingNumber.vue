<template>
  <span class="rn">
    <span class="rn-sr">{{ value }}</span>
    <template v-for="item in chars" :key="item.key">
      <span v-if="item.digit === null" class="rn-char" aria-hidden="true">{{ item.char }}</span>
      <span v-else class="rn-col" aria-hidden="true">
        <!-- 占位决定宽度：tabular 数字等宽，位与位不会因为数字变化而抖动 -->
        <span class="rn-placeholder">0</span>
        <span class="rn-strip" :style="{ transform: `translateY(${-item.digit * 1.1}em)` }">
          <span v-for="n in DIGITS" :key="n" class="rn-num">{{ n }}</span>
        </span>
      </span>
    </template>
  </span>
</template>

<script setup>
import { computed } from 'vue'

/**
 * 滚动数字：数值变化时每一位单独上下翻到新数字，不变的位保持不动。
 * 位从右往左编号作 key，位数增减时个位、十位不会错位重排。
 * 读屏只读完整的值；减少动态效果时直接替换（见样式里的 media query）。
 */
const props = defineProps({
  value: { type: [String, Number], default: '' },
})

const DIGITS = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]

const chars = computed(() => {
  const text = String(props.value ?? '')
  const list = text.split('')
  return list.map((char, index) => {
    const place = list.length - index
    const digit = /\d/.test(char) ? Number(char) : null
    return { char, digit, key: digit === null ? `s${place}-${char}` : `d${place}` }
  })
})
</script>

<style scoped>
.rn {
  display: inline-flex;
  align-items: flex-end;
  font-variant-numeric: tabular-nums;
}
.rn-sr {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}
.rn-char {
  display: block;
  height: 1.1em;
  line-height: 1.1em;
}
.rn-col {
  position: relative;
  display: block;
  height: 1.1em;
  overflow: hidden;
  line-height: 1.1em;
}
.rn-placeholder {
  display: block;
  visibility: hidden;
}
.rn-strip {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  display: flex;
  flex-direction: column;
  transition: transform 420ms cubic-bezier(0.23, 1, 0.32, 1);
}
.rn-num {
  display: block;
  height: 1.1em;
  line-height: 1.1em;
  text-align: center;
}

@media (prefers-reduced-motion: reduce) {
  .rn-strip { transition: none; }
}
</style>
