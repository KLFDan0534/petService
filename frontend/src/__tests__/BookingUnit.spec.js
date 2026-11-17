import { describe, it, expect } from 'vitest'
import {
  NIGHT,
  SESSION,
  HOUR,
  unitLabel,
  durationText,
  billingText,
  standardCheckoutDate,
} from '@/domain/BookingUnit'

// 全项目只允许 night/session/hour：唯一注册表在后端，前端只做展示、不校验、不归一、不算金额。
describe('BookingUnit domain', () => {
  it('renders Chinese labels for the three standard units', () => {
    expect(unitLabel(NIGHT)).toBe('按夜')
    expect(unitLabel(SESSION)).toBe('按次')
    expect(unitLabel(HOUR)).toBe('按小时')
  })

  it('returns empty string for missing unit', () => {
    expect(unitLabel('')).toBe('')
    expect(unitLabel(null)).toBe('')
    expect(unitLabel(undefined)).toBe('')
  })

  it('marks legacy units as historical without interpreting them as 24 hours or mapping to standard units', () => {
    expect(unitLabel('day')).toBe('day（历史计价单位）')
    expect(unitLabel('boarding')).toBe('boarding（历史计价单位）')
    expect(unitLabel('time')).toBe('time（历史计价单位）')
    expect(unitLabel('boarding')).not.toContain('24')
    expect(unitLabel('boarding')).not.toContain('按夜')
    expect(unitLabel('day')).not.toContain('按夜')
  })

  it('formats durations without the legacy 天 wording', () => {
    expect(durationText(1440)).toBe('24 小时')
    expect(durationText(60)).toBe('1 小时')
    expect(durationText(90)).toBe('90 分钟')
    expect(durationText(45)).toBe('45 分钟')
    expect(durationText(0)).toBe('')
  })

  it('billing text uses the order unit snapshot, never the days_wsh fallback', () => {
    expect(billingText({ billing_unit_wsh: 'night', quantity_wsh: 3 })).toBe('3 按夜')
    expect(billingText({ billing_unit_wsh: 'session', quantity_wsh: 1, duration_minutes_wsh: 60 })).toBe('1 按次（1 小时）')
    expect(billingText({ billing_unit_wsh: 'hour', quantity_wsh: 4 })).toBe('4 按小时')
    // 历史单位按原始值标注，绝不解释成按24小时
    expect(billingText({ billing_unit_wsh: 'day', days_wsh: 2 })).toBe('day（历史计价单位）')
    // 缺单位不再回退到 days_wsh 的「N 天」
    expect(billingText({ days_wsh: 2 })).toBe('历史计价单位缺失')
    expect(billingText({})).toBe('历史计价单位缺失')
    expect(billingText(null)).toBe('')
  })

  it('derives checkout date from checkin date plus nights', () => {
    expect(standardCheckoutDate('2026-06-01', 3)).toBe('2026-06-04')
    expect(standardCheckoutDate('2026-06-01', 1)).toBe('2026-06-02')
    expect(standardCheckoutDate('2026-06-01', 0)).toBe('2026-06-02')
    expect(standardCheckoutDate('', 3)).toBe('')
  })
})
