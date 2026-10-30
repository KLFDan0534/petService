import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import { useAppStore } from '@/stores/app'

// 主题状态放在 store 里，按钮自己只负责点一下把它翻过去
describe('ThemeToggle.vue', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
    document.documentElement.classList.remove('dark')
    document.documentElement.removeAttribute('style')
    setActivePinia(createPinia())
  })

  // 按钮上的提示语要跟着当前主题走，不然读屏用户听到的意思正好反了
  it('toggles the app theme when clicked', async () => {
    const store = useAppStore()
    const wrapper = mount(ThemeToggle, {
      global: {
        stubs: {
          'el-icon': { template: '<span><slot /></span>' },
          Sunny: true,
          Moon: true,
        },
      },
    })

    expect(wrapper.attributes('aria-label')).toBe('切换到夜间主题')

    await wrapper.trigger('click')
    expect(store.theme).toBe('dark')
    expect(wrapper.attributes('aria-label')).toBe('切换到日间主题')

    await wrapper.trigger('click')
    expect(store.theme).toBe('light')
    expect(wrapper.attributes('aria-label')).toBe('切换到夜间主题')
  })
})
