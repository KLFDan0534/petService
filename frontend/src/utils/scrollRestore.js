/**
 * 全局滚动位置记录与恢复。
 *
 * 背景：
 * - SPA 路由切换本身不会保留滚动位置；
 * - 页面数据是异步加载的，返回时先渲染骨架屏，文档高度会瞬间变矮，
 *   浏览器会把 scrollY 钳制到 0，等数据回来时位置已经丢失。
 *
 * 规则：
 * 1. 路由离开前，按 path 记录当前页面的 window.scrollY；
 * 2. 目标页正好是当前历史记录的上一页（浏览器后退/前进，或页面内的
 *    “返回列表 / 返回个人中心 / 面包屑”这类 push 导航）时，视为“返回”；
 * 3. 返回时等待文档高度足够容纳目标位置，再恢复，避免骨架屏阶段被钳到 0；
 * 4. 其余导航回到顶部；
 * 5. 同一路径的 query/hash 变化不干预，保持页面原有行为。
 */

const positions = new Map()
const MAX_WAIT_MS = 1500
const STABLE_FRAMES = 12

/** 本次导航是否需要恢复滚动位置，由 captureScroll 计算，由 scrollBehavior 消费。 */
let restoreNext = false

function pathOf(value) {
  if (!value) return ''
  return String(value).split(/[?#]/)[0]
}

function samePath(a, b) {
  return Boolean(a) && Boolean(b) && pathOf(a) === pathOf(b)
}

/**
 * 在路由切换前记录当前页面位置。
 * 必须放在 router.beforeEach 中执行：此时 DOM 还是旧页面，scrollY 才是准确的。
 */
export function captureScroll(to, from) {
  if (typeof window === 'undefined') return
  restoreNext = false
  if (!from?.name) return
  if (pathOf(to.fullPath) === pathOf(from.fullPath)) return

  positions.set(from.path, window.scrollY)

  // 当前历史记录（from）的上一页，如果正好是目标页，说明这次导航是“返回”。
  restoreNext = samePath(window.history.state?.back, to.fullPath)
}

/**
 * 等待文档高度增长到能容纳目标位置。
 * 异步页面从骨架屏切换为真实内容后高度才会变大，因此用 rAF 轮询；
 * 高度稳定一段时间或超时后也会结束，避免页面内容确实变短时一直卡住。
 */
function waitForScrollableHeight(targetY) {
  if (typeof window === 'undefined') return Promise.resolve()
  return new Promise((resolve) => {
    const start = performance.now()
    let lastHeight = -1
    let stableFrames = 0
    const tick = () => {
      const scrollable = document.documentElement.scrollHeight - window.innerHeight
      if (scrollable >= targetY) return resolve()
      if (performance.now() - start >= MAX_WAIT_MS) return resolve()
      if (scrollable === lastHeight) {
        stableFrames += 1
      } else {
        stableFrames = 0
        lastHeight = scrollable
      }
      if (stableFrames >= STABLE_FRAMES) return resolve()
      requestAnimationFrame(tick)
    }
    requestAnimationFrame(tick)
  })
}

/** 生成 vue-router 的 scrollBehavior。 */
export function createScrollBehavior() {
  return (to, from, savedPosition) => {
    // 首次进入应用：回顶部。
    if (!from?.name) return { top: 0 }

    // 同一路径的 query/hash 变化不干预（例如列表筛选、页内锚点）。
    if (pathOf(to.fullPath) === pathOf(from.fullPath)) return false

    const isBack = Boolean(savedPosition) || restoreNext
    restoreNext = false

    // hash 定位交给页面自身处理（异步内容需要等数据加载后才存在）。
    if (to.hash) return false

    if (!isBack) return { top: 0 }

    const remembered = positions.get(to.path)
    const targetY = remembered ?? savedPosition?.top ?? 0
    if (targetY <= 0) return { top: 0 }

    return waitForScrollableHeight(targetY).then(() => ({
      left: 0,
      top: targetY,
      behavior: 'auto',
    }))
  }
}
