import { test, expect } from '@playwright/test'

const BASE = 'http://localhost:5173'

// 多宠物连续下单：owner 用户（id=2）拥有宠物 1(Fluffy)、2(Buddy)；
// 服务 110（day 计费）归属商家 104，看护人 200 容量 5。
// 每次运行会创建 2 个新订单（每只宠物 1 个），通过“下单前后订单数增量”断言可重跑。

function isoDate(offsetDays) {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

// 计算指定宠物最早一个不与既有订单重叠的未来窗口（1..90 天）
async function freeBookingWindow(page, petId) {
  const orders = await page.evaluate(async () => {
    const res = await fetch('/api/orders', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    return res.json()
  })
  const blocking = new Set(['pending', 'paid', 'confirmed'])
  const taken = (orders.data || [])
    .filter(o => Number(o.pet_id_wsh) === petId && blocking.has(String(o.status_wsh)))
    .map(o => ({
      start: new Date(o.start_date_wsh + 'T00:00:00').getTime(),
      end: new Date(o.end_date_wsh + 'T00:00:00').getTime(),
    }))
  for (let n = 1; n <= 90; n++) {
    const start = new Date(isoDate(n) + 'T00:00:00').getTime()
    const end = new Date(isoDate(n + 1) + 'T00:00:00').getTime()
    const overlaps = taken.some(w => start < w.end && end > w.start)
    if (!overlaps) return { startDate: isoDate(n), endDate: isoDate(n + 1) }
  }
  throw new Error('近 90 天内无可用预约窗口')
}

async function countPetsOrders(page) {
  const orders = await page.evaluate(async () => {
    const res = await fetch('/api/orders', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
    })
    return res.json()
  })
  const active = new Set(['pending', 'paid', 'confirmed'])
  return (orders.data || [])
    .filter(o => [1, 2].includes(Number(o.pet_id_wsh)) && active.has(String(o.status_wsh)))
    .reduce((acc, o) => {
      acc[Number(o.pet_id_wsh)] = (acc[Number(o.pet_id_wsh)] || 0) + 1
      return acc
    }, {})
}

async function login(page) {
  await page.goto(`${BASE}/login`)
  await page.getByPlaceholder('请输入用户名').fill('owner')
  await page.getByPlaceholder('请输入密码').fill('pet123456')
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL('**/dashboard')
}

test.describe('多宠物连续下单（浏览器端）', () => {
  test('正向：宠物A 5天 + 宠物B 3天一次提交，生成两个独立订单', async ({ page }) => {
    await login(page)

    const before = await countPetsOrders(page)
    const windowA = await freeBookingWindow(page, 1)
    // 宠物B 用另一段不重叠窗口（偏后 2 天起找），体现“各自日期区间”
    const windowB = await freeBookingWindow(page, 2)

    await page.goto(`${BASE}/services/110?book=1`)
    const dialog = page.locator('.order-modal')
    await expect(dialog).toBeVisible()

    // 批量行 1：宠物 1
    const selects = dialog.locator('select')
    await selects.nth(0).selectOption('1')
    // 看护人（批量行 3 个 select 之后）
    await selects.nth(3).selectOption('200')
    await expect(selects.nth(3)).toBeEnabled({ timeout: 15000 })

    const dateInputs = dialog.locator('input[type="date"]')
    await dateInputs.nth(0).fill(windowA.startDate)
    await expect(selects.nth(1)).toBeEnabled()
    await selects.nth(1).selectOption({ index: 1 })
    await dateInputs.nth(1).fill(windowA.endDate)
    await expect(selects.nth(2)).toBeEnabled()
    await selects.nth(2).selectOption({ index: 1 })

    // 添加批量行 2：宠物 2
    await dialog.getByRole('button', { name: '+ 添加宠物' }).click()
    await selects.nth(3).selectOption('2')
    await dateInputs.nth(2).fill(windowB.startDate)
    await expect(selects.nth(4)).toBeEnabled()
    await selects.nth(4).selectOption({ index: 1 })
    await dateInputs.nth(3).fill(windowB.endDate)
    await expect(selects.nth(5)).toBeEnabled()
    await selects.nth(5).selectOption({ index: 1 })

    await dialog.getByPlaceholder('联系人姓名').fill('Tom')
    await dialog.getByPlaceholder('联系人手机号').fill('13800000002')

    await dialog.getByRole('button', { name: '确认下单' }).click()
    await expect(dialog).toBeHidden()
    // 批量下单成功后进入订单列表（逐单支付）
    await page.waitForURL(/\/orders\/?$/)

    const after = await countPetsOrders(page)
    expect(after[1] - (before[1] || 0)).toBeGreaterThanOrEqual(1)
    expect(after[2] - (before[2] || 0)).toBeGreaterThanOrEqual(1)
  })
})
