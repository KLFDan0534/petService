import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import LoginPromptDialog from '@/components/common/LoginPromptDialog.vue'

const state = vi.hoisted(() => ({
  router: { push: vi.fn() },
  app: { loginRedirectPath: '' },
}))

vi.mock('vue-router', () => ({
  useRouter: () => state.router,
}))

vi.mock('@/stores/app', () => ({
  useAppStore: () => state.app,
}))

function mountDialog() {
  return mount(LoginPromptDialog, {
    props: { visible: true },
    global: {
      stubs: {
        'el-icon': { template: '<span class="el-icon-stub"><slot /></span>' },
        teleport: true,
      },
    },
  })
}

describe('LoginPromptDialog redirect (F-NAV-006)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    state.app.loginRedirectPath = ''
  })

  it('redirects to the stored path encoded in the login query', async () => {
    state.app.loginRedirectPath = '/services/5?book=1'
    const wrapper = mountDialog()
    await wrapper.find('.btn-primary').trigger('click')

    expect(state.router.push).toHaveBeenCalledWith('/login?redirect=' + encodeURIComponent('/services/5?book=1'))
  })

  it('falls back to /dashboard when no redirect is stored', async () => {
    const wrapper = mountDialog()
    await wrapper.find('.btn-primary').trigger('click')

    expect(state.router.push).toHaveBeenCalledWith('/login?redirect=' + encodeURIComponent('/dashboard'))
  })

  it('rejects non-relative redirect values', async () => {
    for (const evil of ['https://evil.example.com', '//evil.example.com', 'javascript:alert(1)']) {
      state.app.loginRedirectPath = evil
      const wrapper = mountDialog()
      await wrapper.find('.btn-primary').trigger('click')
      expect(state.router.push).toHaveBeenLastCalledWith('/login?redirect=' + encodeURIComponent('/dashboard'))
    }
  })
})
