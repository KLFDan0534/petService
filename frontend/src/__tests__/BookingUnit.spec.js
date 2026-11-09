import { describe, it, expect } from 'vitest'
import {
  NIGHT,
  SESSION,
  HOUR,
  normalizeUnit,
  isSupportedUnit,
  unitLabel,
  bookingMode,
  defaultDurationMinutes,
  durationText,
  billingText,
  standardCheckoutDate,
} from '@/domain/BookingUnit'

// 计费单位只允许 night/session/hour：过夜寄养统一按夜，不再有 day/boarding 两套口径
describe('BookingUnit domain', () => {
  it('normalizes canonical + aliases for night/session/hour', () => {
    expect(normalizeUnit('night')).toBe('night')
    expect(normalizeUnit('nights')).toBe('night')
    expect(normalizeUnit('晚')).toBe('night')
    expect(normalizeUnit('按夜')).toBe('night')
    expect(normalizeUnit('过夜')).toBe('night')
    expect(normalizeUnit('session')).toBe('session')
    expect(normalizeUnit('次')).toBe('session')
    expect(normalizeUnit('hour')).toBe('hour')
    expect(normalizeUnit('小时')).toBe('hour')
  })

  // 旧单位一律不再识别：宁可报错也不要静默当成别的单位算钱
  it('rejects legacy and unknown units', () => {
    expect(normalizeUnit('day')).toBeNull()
    expect(normalizeUnit('天')).toBeNull()
    expect(normalizeUnit('boarding')).toBeNull()
    expect(normalizeUnit('week')).toBeNull()
    expect(normalizeUnit('')).toBeNull()
    expect(normalizeUnit(null)).toBeNull()
    expect(normalizeUnit(undefined)).toBeNull()
    expect(isSupportedUnit('boarding')).toBe(false)
    expect(isSupportedUnit('night')).toBe(true)
  })

  it('renders Chinese unit labels', () => {
    expect(unitLabel('night')).toBe('夜')
    expect(unitLabel('session')).toBe('次')
    expect(unitLabel('hour')).toBe('小时')
  })

  it('derives booking mode from unit when absent', () => {
    expect(bookingMode(null, 'night')).toBe('interval')
    expect(bookingMode(null, 'session')).toBe('slot')
    expect(bookingMode(null, 'hour')).toBe('slot')
    expect(bookingMode('slot', 'night')).toBe('slot')
  })

  it('resolves default duration per unit', () => {
    expect(defaultDurationMinutes('night')).toBe(1440)
    expect(defaultDurationMinutes('session')).toBe(60)
    expect(defaultDurationMinutes('hour')).toBe(60)
    // 缺单位不再默认成 day
    expect(defaultDurationMinutes(null)).toBe(60)
  })

  it('formats durations', () => {
    expect(durationText(1440)).toBe('1 天')
    expect(durationText(60)).toBe('1 小时')
    expect(durationText(90)).toBe('90 分钟')
    expect(durationText(45)).toBe('45 分钟')
    expect(durationText(0)).toBe('')
  })

  it('billing text: night quantity uses nights not checkout day', () => {
    expect(billingText({ billing_unit_wsh: 'night', quantity_wsh: 3 })).toBe('3 夜')
    expect(billingText({ billing_unit_wsh: 'night', quantity_wsh: 1 })).toBe('1 夜')
  })

  it('billing text: session with duration', () => {
    expect(billingText({ billing_unit_wsh: 'session', quantity_wsh: 1, duration_minutes_wsh: 60 })).toBe('1 次（1 小时）')
  })

  it('billing text: hour quantity with duration', () => {
    expect(billingText({ billing_unit_wsh: 'hour', quantity_wsh: 4, duration_minutes_wsh: 60 })).toBe('4 小时（1 小时）')
  })

  // 老订单只存了 days_wsh，展示时仍要能读出「2 天」（仅历史读取，不参与计价）
  it('billing text: legacy projection when unit missing', () => {
    expect(billingText({ days_wsh: 2 })).toBe('2 天')
    expect(billingText({})).toBe('')
  })

  it('billing text: keeps null-safe on null order', () => {
    expect(billingText(null)).toBe('')
  })

  it('derives checkout date from checkin date plus nights', () => {
    expect(standardCheckoutDate('2026-06-01', 3)).toBe('2026-06-04')
    expect(standardCheckoutDate('2026-06-01', 1)).toBe('2026-06-02')
    // 非法夜数按 1 夜兜底，空日期返回空串
    expect(standardCheckoutDate('2026-06-01', 0)).toBe('2026-06-02')
    expect(standardCheckoutDate('', 3)).toBe('')
  })
})
