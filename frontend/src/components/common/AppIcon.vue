<script setup>
// 自封装图标包裹组件，替代 Element Plus 的 <el-icon>。
// 它仅负责图标盒模型与尺寸；字形由插槽内的图标组件提供（@element-plus/icons-vue）。
// 复用 .el-icon 类名以兼容页面内已有的图标尺寸/对齐 CSS 规则。
import { computed } from 'vue'

const props = defineProps({
  size: { type: [Number, String], default: undefined },
})

const sizeStyle = computed(() => {
  if (props.size == null) return undefined
  const value = typeof props.size === 'number' ? `${props.size}px` : props.size
  return { fontSize: value }
})
</script>

<template>
  <span class="el-icon app-icon" :style="sizeStyle" role="img" aria-hidden="true">
    <slot />
  </span>
</template>