import { test, expect } from '@playwright/test'

const BASE = 'http://localhost:5173'

async function login(page) {
  await page.goto(`${BASE}/login`)
  await page.getByPlaceholder('请输入用户名').fill('owner')
  await page.getByPlaceholder('请输入密码').fill('pet123456')
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL('**/dashboard')
}

// U6 入口收敛（F-NAV）：所有下单入口统一收敛到服务详情页 /services/{id}[?book=1]，
// 不再出现 /orders?create=true 创建流；看护人入口收敛到商家详情 /merchants/{id}。
test.describe('U6 入口收敛导航契约（浏览器端）', () => {
  test('F-NAV-001: 服务列表卡片进入 /services/{id} 详情页', async ({ page }) => {
    await login(page)
    await page.goto(`${BASE}/services`)

    const cardButton = page.locator('.service-card .service-media-button').first()
    await expect(cardButton).toBeVisible()
    await cardButton.click()

    await page.waitForURL(/\/services\/\d+$/)
    await expect(page.locator('.service-hero')).toBeVisible()
  })

  test('F-NAV-002: 首页 hero 立即预约 → /services/{id}?book=1', async ({ page }) => {
    await login(page)
    await page.goto(`${BASE}/dashboard`)

    const hero = page.locator('.hero-primary')
    await expect(hero).toBeEnabled()
    await hero.click()

    await page.waitForURL(/\/services\/\d+\?book=1$/)
  })

  test('F-NAV-003: 首页服务卡预约按钮 → /services/{id}?book=1；不可预约服务显示原因提示', async ({ page }) => {
    await login(page)
    await page.goto(`${BASE}/dashboard`)

    const bookBtn = page.locator('.service-package .service-actions .btn-primary').first()
    await expect(bookBtn).toBeEnabled()
    await bookBtn.click()

    await page.waitForURL(/\/services\/\d+\?book=1$/)
    // 首页默认第一卡为 E2E 关闭预约服务（bookable=false）：入口收敛到详情页，
    // 且不打开下单弹窗而是显示不可预约原因（下单弹窗契约见 future-booking 正向用例）
    await expect(page.locator('.bookable-reason')).toBeVisible()
    await expect(page.locator('.order-modal')).toHaveCount(0)
  })

  test('F-NAV-004: 商家详情预约按钮 → /services/{id}?book=1', async ({ page }) => {
    await login(page)
    await page.goto(`${BASE}/merchants/1`)
    await page.getByText('Happy Pet Store').first().waitFor()

    const bookButton = page.locator('.service-action button').first()
    await expect(bookButton).toBeEnabled()
    await bookButton.click()

    await page.waitForURL(/\/services\/\d+\?book=1$/)
  })

  test('F-NAV-005: 看护人“选择服务”→ /merchants/{id}', async ({ page }) => {
    await login(page)
    await page.goto(`${BASE}/keepers`)

    const pickBtn = page.getByRole('button', { name: '选择服务' }).first()
    await expect(pickBtn).toBeVisible()
    await pickBtn.click()

    await page.waitForURL(/\/merchants\/\d+$/)
  })

  test('F-NAV-006: 订单页不再提供新建订单入口', async ({ page }) => {
    await login(page)
    await page.goto(`${BASE}/orders`)
    await page.locator('.orders-page').waitFor()

    await expect(page.getByRole('button', { name: /新建|创建订单/ })).toHaveCount(0)
  })
})
