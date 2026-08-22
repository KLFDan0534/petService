import { test, expect } from '@playwright/test'

const BASE = 'http://localhost:5173'

// 重放约束：正向用例使用重放专用宠物 109（RepeatGuardPet145124，owner=login 用户）。
// 同一宠物在重叠日期窗口只能存在一个有效订单（业务正确行为）。
// 本用例会在下单前动态挑选第一个与既有订单无重叠的未来窗口，因此可连续重跑；
// 若需彻底清理历史 E2E 订单，可执行 SQL：
//   DELETE FROM order_snapshot_wsh WHERE order_id_wsh IN (
//     SELECT id_wsh FROM pet_order_wsh WHERE pet_id_wsh IN (1, 109) AND order_no_wsh LIKE 'ORD2026%');
//   DELETE FROM pet_order_wsh WHERE pet_id_wsh IN (1, 109) AND order_no_wsh LIKE 'ORD2026%';
function isoDate(offsetDays) {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

// 通过 owner 的订单列表计算 pet 109 的既有占用窗口（半开区间 [start, end)），
// 返回最早一个不与任何占用窗口重叠的 (startDate, endDate) 未来窗口。
async function freeBookingWindow(page) {
  const orders = await page.evaluate(async () => {
    const res = await fetch('/api/orders', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    return res.json()
  })
  const blocking = new Set(['pending', 'confirmed', 'paid'])
  const taken = (orders.data || [])
    .filter(o => Number(o.pet_id_wsh) === 109 && blocking.has(String(o.status_wsh)))
    .map(o => ({
      start: new Date(o.start_date_wsh + 'T00:00:00').getTime(),
      end: new Date(o.end_date_wsh + 'T00:00:00').getTime(),
    }))
  // 预约窗口 90 天（后端 booking.max-booking-days 默认 91，含首尾）
  for (let n = 1; n <= 90; n++) {
    const start = new Date(isoDate(n) + 'T00:00:00').getTime()
    const end = new Date(isoDate(n + 1) + 'T00:00:00').getTime()
    const overlaps = taken.some(w => start < w.end && end > w.start)
    if (!overlaps) return { startDate: isoDate(n), endDate: isoDate(n + 1) }
  }
  throw new Error('近 90 天内无可用预约窗口')
}

async function login(page) {
  await page.goto(`${BASE}/login`)
  await page.getByPlaceholder('请输入用户名').fill('owner')
  await page.getByPlaceholder('请输入密码').fill('pet123456')
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL('**/dashboard')
}

test.describe('关店商家未来预约（浏览器端）', () => {
  test('正向：关店(store_status=0)+政策开(future_booking=1)仍可预约并支付', async ({ page }) => {
    await login(page)

    await page.goto(`${BASE}/merchants/104`)
    await page.getByText('Test Pet Shop').first().waitFor()

    const bookButton = page.locator('.service-action button').first()
    await expect(bookButton).toBeEnabled()
    await expect(bookButton).toHaveText('休息中·可预约')
    await bookButton.click()

    // U6 导航契约：商家详情预约按钮收敛到服务详情页并带 book=1 打开下单弹窗
    await page.waitForURL(/\/services\/\d+\?book=1/)
    const dialog = page.locator('.order-modal')
    await expect(dialog).toBeVisible()

    await dialog.locator('select').nth(0).selectOption('109')
    const merchantSelect = dialog.locator('select').nth(1)
    await expect(merchantSelect).toBeDisabled()
    await expect(merchantSelect).toHaveValue('104')
    const keeperSelect = dialog.locator('select').nth(2)
    await expect(keeperSelect).toBeEnabled({ timeout: 15000 })
    await keeperSelect.selectOption('200')

    await dialog.getByRole('button', { name: '商家位置' }).click()

    const { startDate, endDate } = await freeBookingWindow(page)
    const dateInputs = dialog.locator('input[type="date"]')
    const slotSelects = dialog.locator('select')
    await dateInputs.nth(0).fill(startDate)
    await expect(slotSelects.nth(3)).toBeEnabled()
    await slotSelects.nth(3).selectOption({ index: 1 })
    await dateInputs.nth(1).fill(endDate)
    await expect(slotSelects.nth(4)).toBeEnabled()
    await slotSelects.nth(4).selectOption({ index: 1 })

    await dialog.getByPlaceholder('联系人姓名').fill('Tom')
    await dialog.getByPlaceholder('联系人手机号').fill('13800000002')

    await dialog.getByRole('button', { name: '确认下单' }).click()

    await expect(dialog).toBeHidden()
    // U6 导航契约：下单成功后进入订单详情页（不再回落到 /orders 列表）
    await page.waitForURL(/\/orders\/\d+/)
    const payButton = page.getByRole('button', { name: '余额支付' }).first()
    await expect(payButton).toBeVisible()
    await payButton.click()
    await expect(page.locator('.toast', { hasText: '支付成功' }).first()).toBeVisible()
  })

  test('负向：政策关闭(future_booking=0)的商家预约按钮禁用', async ({ page }) => {
    await login(page)

    await page.goto(`${BASE}/merchants/201`)
    await page.getByText('E2E Policy Off Shop').first().waitFor()

    const bookButton = page.locator('.service-action button').first()
    await expect(bookButton).toBeDisabled()
  })

  test('对照：营业中商家(future_booking=1)按钮文案为“预约”且可用', async ({ page }) => {
    await login(page)

    await page.goto(`${BASE}/merchants/1`)
    await page.getByText('Happy Pet Store').first().waitFor()

    const bookButton = page.locator('.service-action button').first()
    await expect(bookButton).toBeEnabled()
    await expect(bookButton).toHaveText('预约')
  })
})
