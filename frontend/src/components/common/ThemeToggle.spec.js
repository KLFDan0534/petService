import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ThemeToggle from './ThemeToggle.vue'
import { useAppStore } from '@/stores/app'

describe('ThemeToggle.vue', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
    document.documentElement.classList.remove('dark')
    document.documentElement.removeAttribute('style')
    setActivePinia(createPinia())
  })

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
