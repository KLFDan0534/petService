import { onBeforeUnmount } from 'vue'

/**
 * 卡片指针光照：把指针位置弹簧式地写进宿主元素的 CSS 变量，由各光照层读取。
 *
 * 写入的变量（与 tier-fx 里的层一一对应）：
 *   --ms-mx / --ms-my  主高光圆心（百分比，已含视差偏移）
 *   --ms-px            指针横向位置 0–1，驱动拉丝条带的角度与位置
 *   --ms-ra / --ms-rs  边缘光朝向（度，0° 朝上、顺时针）与强度系数；静止时 315° / 0.45
 *   --ms-e             指针在卡上的程度 0–1，驱动悬停时的光照强度
 *   --l-r              提亮半径（px）= 卡片短边 × 本档 glowR
 *
 * 只响应鼠标 / 触控笔；系统开启「减少动态效果」时不倾斜、不跟随。
 * 用 rAF 自己插值而不是 CSS transition：指针连续移动时 CSS transition 会互相打断，看起来发粘。
 */
const REST = { x: 0.72, y: 0.24 }
/** 卡片倾斜到底时高光反向偏移 7%，读作「卡在转」而不是「图在动」 */
const PARALLAX = 7
/** 固定光向：左上（315°） */
const LIGHT_DIR = { x: -Math.SQRT1_2, y: -Math.SQRT1_2 }
/** 跟手但不粘：每帧向目标靠 16% */
const FOLLOW = 0.16
const EPS = 0.0006

export function useCardLight() {
  let el = null
  let raf = 0
  let observer = null
  let tilt = 0
  let glowRatio = 0.72
  let reduce = typeof window !== 'undefined' && window.matchMedia
    ? window.matchMedia('(prefers-reduced-motion: reduce)').matches
    : false
  const target = { x: REST.x, y: REST.y, e: 0 }
  const cur = { x: REST.x, y: REST.y, e: 0 }

  function paint() {
    if (!el) return
    const tx = (cur.x - 0.5) * 2 * cur.e
    const ty = (cur.y - 0.5) * 2 * cur.e
    const engage = Math.min(1, Math.max(0, cur.e))
    const m = Math.min(1, Math.hypot(tx, ty))
    // 边缘光：卡面法线 = 固定光向与倾斜方向的合成，倾斜越大越偏向指针一侧
    const vx = LIGHT_DIR.x * (1 - m) + tx
    const vy = LIGHT_DIR.y * (1 - m) + ty
    el.style.setProperty('--ms-mx', `${(cur.x * 100 - tx * PARALLAX).toFixed(2)}%`)
    el.style.setProperty('--ms-my', `${(cur.y * 100 - ty * PARALLAX).toFixed(2)}%`)
    el.style.setProperty('--ms-px', cur.x.toFixed(3))
    el.style.setProperty('--ms-e', engage.toFixed(3))
    el.style.setProperty('--ms-ra', ((Math.atan2(vx, -vy) * 180) / Math.PI).toFixed(1))
    el.style.setProperty('--ms-rs', (0.45 + 0.55 * m).toFixed(3))
    el.style.transform = reduce || !tilt
      ? ''
      : `perspective(900px) rotateX(${(-ty * tilt).toFixed(2)}deg) rotateY(${(tx * tilt).toFixed(2)}deg)`
  }

  function tick() {
    raf = 0
    cur.x += (target.x - cur.x) * FOLLOW
    cur.y += (target.y - cur.y) * FOLLOW
    cur.e += (target.e - cur.e) * FOLLOW
    paint()
    const settled = Math.abs(target.x - cur.x) < EPS
      && Math.abs(target.y - cur.y) < EPS
      && Math.abs(target.e - cur.e) < EPS
    if (!settled) raf = requestAnimationFrame(tick)
  }

  function kick() {
    if (!raf) raf = requestAnimationFrame(tick)
  }

  function measure() {
    if (!el) return
    const rect = el.getBoundingClientRect()
    const short = Math.min(rect.width, rect.height)
    if (short > 0) el.style.setProperty('--l-r', `${Math.round(short * glowRatio)}px`)
  }

  /** 绑定到卡片根元素：登记尺寸观察，把提亮半径落到 px */
  function attach(node) {
    if (!node || node === el) return
    detach()
    el = node
    measure()
    if (typeof ResizeObserver !== 'undefined') {
      observer = new ResizeObserver(measure)
      observer.observe(el)
    }
    paint()
  }

  function detach() {
    if (observer) {
      observer.disconnect()
      observer = null
    }
    if (raf) {
      cancelAnimationFrame(raf)
      raf = 0
    }
    el = null
  }

  /** 换档时更新倾斜角与提亮比例（身份卡 0.72–0.9，套餐卡 1.2） */
  function configure({ tilt: nextTilt = 0, glow = 0.72 } = {}) {
    tilt = reduce ? 0 : nextTilt
    if (glow !== glowRatio) {
      glowRatio = glow
      measure()
    }
    paint()
  }

  function onPointerMove(event) {
    if (reduce || event.pointerType === 'touch' || !el) return
    const rect = el.getBoundingClientRect()
    target.x = clamp((event.clientX - rect.left) / rect.width)
    target.y = clamp((event.clientY - rect.top) / rect.height)
    target.e = 1
    kick()
  }

  function onPointerLeave() {
    target.x = REST.x
    target.y = REST.y
    target.e = 0
    kick()
  }

  onBeforeUnmount(detach)

  return { attach, detach, configure, onPointerMove, onPointerLeave }
}

function clamp(value) {
  return Math.min(1, Math.max(0, value))
}
