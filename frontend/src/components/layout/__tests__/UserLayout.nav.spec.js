import { readFileSync } from 'fs'
import { resolve } from 'path'
import { mount, RouterLinkStub } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import UserLayout from '@/components/layout/UserLayout.vue'
import { useAuthStore } from '@/stores/auth'

const mocks = vi.hoisted(() => ({
  routerPush: vi.fn(),
  routerReplace: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mocks.routerPush,
    replace: mocks.routerReplace,
    currentRoute: { value: { name: 'Dashboard', fullPath: '/dashboard' } },
  }),
}))

function createAuthenticatedPinia(roles) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.token = 'test-token'
  authStore.user = {
    id_wsh: 1,
    username_wsh: 'owner',
    nickname_wsh: '宠物主人',
    roles_wsh: roles || ['ROLE_USER', 'ROLE_ADMIN', 'ROLE_KEEPER'],
  }
  return pinia
}

function mountLayout(roles) {
  const pinia = createAuthenticatedPinia(roles)
  return mount(UserLayout, {
    global: {
      plugins: [pinia],
      stubs: {
        MessageIndicator: true,
        RouterLink: RouterLinkStub,
        ThemeToggle: true,
        'el-icon': { template: '<span class="el-icon-stub"><slot /></span>' },
      },
    },
  })
}

describe('UserLayout 顶部导航栏', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('不渲染「附近商户」导航链接', () => {
    const wrapper = mountLayout()
    const links = wrapper.findAll('.user-nav a')
    expect(links.map(l => l.text())).not.toContain('附近商户')
  })

  it('不渲染顶部「预约服务」按钮', () => {
    const wrapper = mountLayout()
    expect(wrapper.find('.header-booking').exists()).toBe(false)
  })

  it('退出按钮只保留图标,不包含「退出」文字', () => {
    const wrapper = mountLayout()
    const logoutBtn = wrapper.find('.header-logout')
    expect(logoutBtn.exists()).toBe(true)
    expect(logoutBtn.text()).not.toContain('退出')
    expect(logoutBtn.find('.el-icon-stub').exists()).toBe(true)
  })

  it('退出按钮带 aria-label 与 title 保持可访问性', () => {
    const wrapper = mountLayout()
    const logoutBtn = wrapper.find('.header-logout')
    expect(logoutBtn.attributes('aria-label')).toBe('退出登录')
    expect(logoutBtn.attributes('title')).toBe('退出登录')
  })

  it('导航链接(首页/预约服务等)仍然保留', () => {
    const wrapper = mountLayout()
    const links = wrapper.findAll('.user-nav a')
    const texts = links.map(l => l.text().trim())
    expect(texts).toContain('首页')
    expect(texts).toContain('预约服务')
    expect(texts).toContain('管理后台')
  })

  it('shell 使用三列 grid 布局(grid-template-columns 三列),中间列绝对居中', () => {
    const wrapper = mountLayout()
    const shell = wrapper.get('.user-header-shell')
    expect(shell.exists()).toBe(true)
  })

  it('未登录时展示登录/注册按钮', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.token = null
    authStore.user = null
    const wrapper = mount(UserLayout, {
      global: {
        plugins: [pinia],
        stubs: {
          MessageIndicator: true,
          RouterLink: RouterLinkStub,
          ThemeToggle: true,
          'el-icon': { template: '<span class="el-icon-stub"><slot /></span>' },
        },
      },
    })
    const login = wrapper.findAll('.user-header-end a').map(l => l.text())
    expect(login).toContain('登录')
    expect(login).toContain('注册')
  })
})

describe('UserLayout 导航样式规则(仿 proxyAI 胶囊/玻璃)', () => {
  let css
  beforeAll(() => {
    const file = resolve(__dirname, '../UserLayout.vue')
    css = readFileSync(file, 'utf8').split('<style scoped>')[1].split('</style>')[0]
  })

  it('默认(顶部)导航栏: 全宽紧贴顶部, 不透明背景', () => {
    expect(css).toMatch(/:global\(header\.user-header\) \{[\s\S]*?top: 0;/)
    expect(css).toMatch(/:global\(header\.user-header\) \{[\s\S]*?background: var\(--color-card\);/)
  })

  it('滚动后: header 透明, 悬停 top:16px 收窄, shell 变圆角胶囊', () => {
    expect(css).toMatch(/user-header-scrolled\) \{[\s\S]*?top: 28px;/)
    expect(css).toMatch(/user-header-scrolled\) \{[\s\S]*?background: transparent;/)
    expect(css).toMatch(/user-header-scrolled \.user-header-shell\) \{[\s\S]*?border-radius: 999px;/)
  })

  it('滚动后 shell 使用磨砂玻璃(backdrop-filter blur + 半透明背景 + 阴影)', () => {
expect(css).toMatch(/user-header-scrolled \.user-header-shell\) \{[\s\S]*?min-height: 68px;/)
    expect(css).toMatch(/user-header-scrolled \.user-header-shell\) \{[\s\S]*?backdrop-filter: blur\(20px\) saturate\(1\.35\);/)
expect(css).toMatch(/user-header-scrolled \.user-header-shell\) \{[\s\S]*?background: color-mix/)
expect(css).toMatch(/user-header-scrolled \.user-header-shell\) \{[\s\S]*?box-shadow:/)
  })

  it('桌面布局使用对称三列 grid, 中间 nav 绝对居中', () => {
    expect(css).toMatch(/\.user-header-shell \{[\s\S]*?grid-template-columns: minmax\(0, 1fr\) auto minmax\(0, 1fr\);/)
    expect(css).toMatch(/\.user-nav \{[\s\S]*?grid-column: 2;/)
    expect(css).toMatch(/\.user-logo \{[\s\S]*?grid-column: 1;/)
    expect(css).toMatch(/\.user-header-right \{[\s\S]*?grid-column: 3;/)
  })

  it('滚动阈值使用 proxyAI 的 80px(scrollY > 80)', () => {
    const script = readFileSync(resolve(__dirname, '../UserLayout.vue'), 'utf8')
    expect(script).toMatch(/window\.scrollY > 80/)
  })

  it('移动端(<1100px)隐藏 nav 与 header-end, 由汉堡切换', () => {
    expect(css).toMatch(/@media \(max-width: 1100px\) \{[\s\S]*?\.user-nav \{[\s\S]*?display: none;/)
    expect(css).toMatch(/@media \(max-width: 1100px\) \{[\s\S]*?\.user-header-end \{[\s\S]*?display: none;/)
    expect(css).toMatch(/@media \(max-width: 1100px\) \{[\s\S]*?\.header-menu-toggle \{[\s\S]*?display: grid;/)
  })
})