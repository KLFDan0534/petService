/**
 * 等级徽记的几何：三档轮廓 + 切面 + 爪印，以及守护档的刻度环。
 *
 * 单独成模块的原因：<script setup> 的顶层语句随实例执行，
 * 组件里先用后声明会踩 TDZ；import 是提升的，放到模块里就没有顺序问题。
 *
 * 三档轮廓统一沿边等分重采样成 24 个点，点数一致才能直接插值形变。
 */

/** 正 n 边形顶点，从正上方（或偏转 rot 度）顺时针 */
function ngon(n, radius, rot = 0) {
  return Array.from({ length: n }, (_, k) => {
    const a = ((-90 + rot + (360 / n) * k) * Math.PI) / 180
    return [50 + radius * Math.cos(a), 50 + radius * Math.sin(a)]
  })
}

/** 沿边等分重采样到 24 个点 */
function toMorphPoints(list) {
  const per = 24 / list.length
  const out = []
  list.forEach((p, i) => {
    const q = list[(i + 1) % list.length]
    for (let j = 0; j < per; j++) {
      const t = j / per
      out.push([p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t])
    }
  })
  return out
}

const HEX = [[50, 6], [88, 28], [88, 72], [50, 94], [12, 72], [12, 28]]
const OCT = [[66, 6], [94, 34], [94, 66], [66, 94], [34, 94], [6, 66], [6, 34], [34, 6]]
const DODEC = ngon(12, 45)
const DODEC_TABLE = ngon(12, 31, 15)

const fmt = (p) => `${p[0].toFixed(2)} ${p[1].toFixed(2)}`

export const SHAPES = {
  companion: {
    key: 'companion',
    points: toMorphPoints(HEX),
    facets: [
      'M50 26 L69 37 L69 63 L50 74 L31 63 L31 37 Z',
      'M50 6 L50 26', 'M88 28 L69 37', 'M88 72 L69 63',
      'M50 94 L50 74', 'M12 72 L31 63', 'M12 28 L31 37',
    ],
    paws: [{ x: 50, y: 50, s: 1.1, r: 0 }],
  },
  guardian: {
    key: 'guardian',
    points: toMorphPoints(OCT),
    facets: [
      'M40 24 L60 24 L76 40 L76 60 L60 76 L40 76 L24 60 L24 40 Z',
      'M34 6 L40 24', 'M66 6 L60 24', 'M94 34 L76 40', 'M94 66 L76 60',
      'M66 94 L60 76', 'M34 94 L40 76', 'M6 66 L24 60', 'M6 34 L24 40',
    ],
    paws: [
      { x: 39.5, y: 59.5, s: 0.8, r: -14 },
      { x: 60.5, y: 40.5, s: 0.8, r: 14 },
    ],
  },
  beloved: {
    key: 'beloved',
    points: toMorphPoints(DODEC),
    facets: [
      // 台面十二边 + 冠部星形切面，俯视的明亮式切割
      `M ${DODEC_TABLE.map(fmt).join(' L ')} Z`,
      `M ${DODEC.flatMap((p, i) => [fmt(p), fmt(DODEC_TABLE[i])]).join(' L ')} Z`,
    ],
    paws: [
      { x: 50, y: 37, s: 0.6, r: 0 },
      { x: 36.5, y: 59, s: 0.6, r: -12 },
      { x: 63.5, y: 59, s: 0.6, r: 12 },
    ],
  },
  plain: {
    key: 'plain',
    points: toMorphPoints(HEX),
    facets: [],
    paws: [],
  },
}

export function shapeKeyOf(key) {
  return SHAPES[key] ? key : 'plain'
}

/** 守护刻度环：72 格，每 6 格一根长刻度 */
export const RING_TICKS = Array.from({ length: 72 }, (_, i) => {
  const a = ((i * 5 - 90) * Math.PI) / 180
  const major = i % 6 === 0
  const r1 = major ? 51.5 : 54
  const r2 = 56.5
  return {
    a: i,
    major,
    x1: 60 + r1 * Math.cos(a),
    y1: 60 + r1 * Math.sin(a),
    x2: 60 + r2 * Math.cos(a),
    y2: 60 + r2 * Math.sin(a),
  }
})
