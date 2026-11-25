/**
 * 徽记实例 id 发号器。
 *
 * 必须放在模块作用域：<script setup> 里的顶层语句会随组件实例重跑，
 * 计数器写在组件里会让同页多张卡拿到同一个 id，SVG 渐变与爪印几何就会互相覆盖。
 */
let seq = 0

export function nextEmblemId() {
  seq += 1
  return `te${seq}`
}
