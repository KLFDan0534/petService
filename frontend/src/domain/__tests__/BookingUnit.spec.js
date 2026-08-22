import { describe, it, expect } from 'vitest'
import {
  DAY,
  SESSION,
  HOUR,
  normalizeUnit,
  isSupportedUnit,
  unitLabel,
  bookingMode,
  defaultDurationMinutes,
  durationText,
  billingText,
} from '@/domain/BookingUnit'

describe('BookingUnit domain', () => {
  it('normalizes canonical + aliases for day/session/hour', () => {
    expect(normalizeUnit('day')).toBe('day')
    expect(normalizeUnit('days')).toBe('day')
    expect(normalizeUnit('天')).toBe('day')
    expect(normalizeUnit('session')).toBe('session')
    expect(normalizeUnit('sessions')).toBe('session')
    expect(normalizeUnit('次')).toBe('session')
    expect(normalizeUnit('hour')).toBe('hour')
    expect(normalizeUnit('hours')).toBe('hour')
    expect(normalizeUnit('小时')).toBe('hour')
  })

  it('rejects unknown or blank units', () => {
    expect(normalizeUnit('week')).toBeNull()
    expect(normalizeUnit('')).toBeNull()
    expect(normalizeUnit(null)).toBeNull()
    expect(normalizeUnit(undefined)).toBeNull()
    expect(isSupportedUnit('week')).toBe(false)
    expect(isSupportedUnit('hour')).toBe(true)
  })

  it('renders Chinese unit labels', () => {
    expect(unitLabel('day')).toBe('天')
    expect(unitLabel('session')).toBe('次')
    expect(unitLabel('hour')).toBe('小时')
  })

  it('derives booking mode from unit when absent', () => {
    expect(bookingMode(null, 'day')).toBe('date_range')
    expect(bookingMode(null, 'session')).toBe('slot')
    expect(bookingMode(null, 'hour')).toBe('slot')
    expect(bookingMode('slot', 'day')).toBe('slot')
  })

  it('resolves default duration per unit', () => {
    expect(defaultDurationMinutes('day')).toBe(1440)
    expect(defaultDurationMinutes('session')).toBe(60)
    expect(defaultDurationMinutes('hour')).toBe(60)
    expect(defaultDurationMinutes(null)).toBe(1440)
  })

  it('formats durations', () => {
    expect(durationText(1440)).toBe('1 天')
    expect(durationText(60)).toBe('1 小时')
    expect(durationText(90)).toBe('90 分钟')
    expect(durationText(45)).toBe('45 分钟')
    expect(durationText(0)).toBe('')
  })

  it('billing text: day quantity', () => {
    expect(billingText({ billing_unit_wsh: 'day', quantity_wsh: 3 })).toBe('3 天')
  })

  it('billing text: session with duration', () => {
    expect(billingText({ billing_unit_wsh: 'session', quantity_wsh: 1, duration_minutes_wsh: 60 })).toBe('1 次（1 小时）')
  })

  it('billing text: hour quantity with duration', () => {
    expect(billingText({ billing_unit_wsh: 'hour', quantity_wsh: 4, duration_minutes_wsh: 60 })).toBe('4 小时（1 小时）')
  })

  it('billing text: legacy day projection when new fields missing', () => {
    expect(billingText({ days_wsh: 2 })).toBe('2 天')
    expect(billingText({})).toBe('')
  })

  it('billing text: keeps null-safe on null order', () => {
    expect(billingText(null)).toBe('')
  })
})