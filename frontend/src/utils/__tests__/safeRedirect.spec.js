import { describe, expect, it } from 'vitest'
import { safeRedirect } from '@/utils/safeRedirect'

describe('safeRedirect', () => {
  it('accepts internal absolute paths', () => {
    expect(safeRedirect('/dashboard')).toBe('/dashboard')
    expect(safeRedirect('/services/12')).toBe('/services/12')
    expect(safeRedirect('/services/12?book=1')).toBe('/services/12?book=1')
    expect(safeRedirect('/orders?tab=pending')).toBe('/orders?tab=pending')
  })

  it('rejects external and protocol-relative targets', () => {
    expect(safeRedirect('https://evil.example/x')).toBeNull()
    expect(safeRedirect('http://evil.example')).toBeNull()
    expect(safeRedirect('//evil.example')).toBeNull()
    expect(safeRedirect('/\\evil.example')).toBeNull()
    expect(safeRedirect('\\\\evil.example')).toBeNull()
    expect(safeRedirect('javascript:alert(1)')).toBeNull()
    expect(safeRedirect('data:text/html,<script>1</script>')).toBeNull()
    expect(safeRedirect('mailto:attacker@example.com')).toBeNull()
  })

  it('rejects encoded-control targets', () => {
    expect(safeRedirect('/services/1?book=1%0Aevil')).toBeNull()
    expect(safeRedirect('/services/1%0A%00')).toBeNull()
    expect(safeRedirect('/services/1?book=1%250Aevil')).toBeNull()
    expect(safeRedirect('/services/1?book=1\u0000')).toBeNull()
  })

  it('rejects fragment targets', () => {
    expect(safeRedirect('/services/1#top')).toBeNull()
    expect(safeRedirect('/services/1?book=1#section')).toBeNull()
    expect(safeRedirect('/services/1%23section')).toBeNull()
  })

  it('rejects nested redirect targets', () => {
    expect(safeRedirect('/orders?redirect=/dashboard')).toBeNull()
    expect(safeRedirect('/login?redirect=%2Fdashboard')).toBeNull()
    expect(safeRedirect('/services/1?book=1&redirect=x')).toBeNull()
  })

  it('rejects non-string, empty, oversized and untrimmed values', () => {
    expect(safeRedirect(undefined)).toBeNull()
    expect(safeRedirect(null)).toBeNull()
    expect(safeRedirect(123)).toBeNull()
    expect(safeRedirect('')).toBeNull()
    expect(safeRedirect(' /dashboard')).toBeNull()
    expect(safeRedirect('/dashboard '.repeat(500))).toBeNull()
  })

  it('rejects scheme smuggling inside the first path segment', () => {
    expect(safeRedirect('/javascript:alert(1)')).toBeNull()
    expect(safeRedirect('/%6aavascript:alert(1)')).toBeNull()
  })
})
