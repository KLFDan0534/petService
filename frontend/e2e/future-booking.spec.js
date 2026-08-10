import { test, expect } from '@playwright/test'

const BASE = 'http://localhost:5173'

function isoDate(offsetDays) {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
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

    await page.waitForURL('**/orders?create=true*')
    const dialog = page.locator('.order-modal')
    await expect(dialog).toBeVisible()

    await dialog.locator('select').nth(0).selectOption('1')
    const merchantSelect = dialog.locator('select').nth(1)
    await expect(merchantSelect).toBeEnabled({ timeout: 15000 })
    await expect(merchantSelect).toHaveValue('104')
    const keeperSelect = dialog.locator('select').nth(2)
    await expect(keeperSelect).toBeEnabled({ timeout: 15000 })
    await keeperSelect.selectOption('200')

    await dialog.getByRole('button', { name: '商家位置' }).click()

    const delivery = dialog.locator('input[type="datetime-local"]').nth(0)
    const pickup = dialog.locator('input[type="datetime-local"]').nth(1)
    await delivery.fill(`${isoDate(1)}T10:00`)
    await pickup.fill(`${isoDate(2)}T18:00`)

    await dialog.getByPlaceholder('联系人姓名').fill('Tom')
    await dialog.getByPlaceholder('联系人手机号').fill('13800000002')

    await dialog.getByRole('button', { name: '确认下单' }).click()

    await expect(dialog).toBeHidden()
    const orderCard = page.locator('.order-card', { hasText: 'Test Pet Shop' }).first()
    await expect(orderCard).toBeVisible()
    await orderCard.getByRole('button', { name: /余额支付/ }).click()
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