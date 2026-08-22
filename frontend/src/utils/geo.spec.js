import { describe, it, expect } from 'vitest'
import { haversineMeters, formatDistanceMeters } from './geo'

describe('geo utils', () => {
  it('haversineMeters: 纬度每 0.01 度约为 1.1 公里', () => {
    const meters = haversineMeters(31.23, 121.47, 31.24, 121.47)
    expect(meters).toBeGreaterThan(1000)
    expect(meters).toBeLessThan(1200)
  })

  it('haversineMeters: 相同坐标距离为 0', () => {
    expect(haversineMeters(31.23, 121.47, 31.23, 121.47)).toBe(0)
  })

  it('formatDistanceMeters: 1 公里内显示米，以上显示公里', () => {
    expect(formatDistanceMeters(850)).toBe('850 米')
    expect(formatDistanceMeters(999)).toBe('999 米')
    expect(formatDistanceMeters(2460)).toBe('2.5 公里')
  })

  it('formatDistanceMeters: 非法值返回空字符串', () => {
    expect(formatDistanceMeters(null)).toBe('')
    expect(formatDistanceMeters('abc')).toBe('')
  })
})
