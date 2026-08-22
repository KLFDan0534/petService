import { test, expect } from '@playwright/test'

const BASE = 'http://localhost:5173'

async function login(page, username, password) {
  await page.goto(`${BASE}/login`)
  await page.getByPlaceholder('请输入用户名').fill(username)
  await page.getByPlaceholder('请输入密码').fill(password)
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL(url => !url.pathname.includes('/login'))
}

// F-SUP 客服体系（智能客服 / 投诉选择对象 / 管理端 RAG 文档 CRUD）
test.describe('客服体系浏览器契约', () => {
  test('F-SUP-001: 智能客服输入“转人工”触发人工转接面板', async ({ page }) => {
    await login(page, 'owner', 'pet123456')
    await page.goto(`${BASE}/ai/chat`)
    await page.locator('.ai-chat-page').waitFor()

    await page.getByPlaceholder('请输入您的问题，例如：退款规则是什么？').fill('我要转人工')
    await page.getByRole('button', { name: '发送', exact: true }).click()

    await expect(page.locator('.handoff-panel')).toBeVisible()
    await expect(page.getByRole('button', { name: '转人工客服' })).toBeVisible()
  })

  test('F-SUP-002: 投诉表单用“选择对象”替代手填 ID', async ({ page }) => {
    await login(page, 'owner', 'pet123456')
    await page.goto(`${BASE}/complaints`)
    await page.locator('.complaints-page, .page-container').first().waitFor()

    await page.getByRole('button', { name: '+ 提交投诉' }).click()

    // 表单通过下拉选择对象，而非手填 ID
    await expect(page.getByText('投诉对象')).toBeVisible()
    await expect(page.locator('.modal select').first()).toBeVisible()
    // 页面任何位置都不应出现要求用户输入 ID 的输入框
    await expect(page.locator('input[placeholder*="ID" i]')).toHaveCount(0)
    await expect(page.getByText(/请输入.*ID/i)).toHaveCount(0)
  })

  test('F-SUP-003: 管理端 RAG 文档新增→编辑→删除', async ({ page }) => {
    const unique = `E2E文档${Date.now()}`
    await login(page, 'admin', '123456')
    await page.goto(`${BASE}/admin/rag`)
    await page.getByRole('button', { name: '+ 新增文档' }).waitFor()

    // 新增
    await page.getByRole('button', { name: '+ 新增文档' }).click()
    await page.locator('.modal').waitFor()
    await page.locator('.modal input:not([type="radio"]):not([type="file"])').fill(unique)
    await page.locator('.modal select').selectOption({ label: '护理' })
    await page.locator('.modal textarea').fill('这是 Playwright 自动生成的文档内容，用于验证 RAG 管理端增删改查。')
    await page.getByRole('button', { name: '创建', exact: true }).click()

    const row = page.locator('tbody tr', { hasText: unique })
    await expect(row).toBeVisible()

    // 编辑
    const edited = `${unique}-已编辑`
    await row.getByRole('button', { name: '编辑' }).click()
    await page.locator('.modal input:not([type="radio"]):not([type="file"])').fill(edited)
    await page.getByRole('button', { name: '保存', exact: true }).click()
    await expect(page.locator('tbody tr', { hasText: edited })).toBeVisible()

    // 删除
    page.once('dialog', dialog => dialog.accept())
    await page.locator('tbody tr', { hasText: edited }).getByRole('button', { name: '删除' }).click()
    await expect(page.locator('tbody tr', { hasText: edited })).toHaveCount(0)
  })
})
