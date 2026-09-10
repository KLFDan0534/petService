import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PopupNotice from '@/components/common/PopupNotice.vue'
import { useAuthStore } from '@/stores/auth'
import { dismissPopup, getActiveNotices, getPopupNotices } from '@/api/notice'

vi.mock('@/api/notice', () => ({
  getActiveNotices: vi.fn(),
  getPopupNotices: vi.fn(),
  dismissPopup: vi.fn(),
}))

describe('PopupNotice.vue', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('loads anonymous public popup notices from the active notice API only once', async () => {
    getActiveNotices.mockResolvedValue({
      code: 200,
      data: [
        notice(1, 'First popup', 'notice', 'popup'),
        notice(2, 'Banner popup', 'banner', 'popup'),
        notice(3, 'Notification only', 'notice', 'notification'),
        notice(4, 'Second popup', 'notice', 'popup,notification'),
      ],
    })

    const wrapper = mount(PopupNotice)
    await flushPromises()

    expect(getActiveNotices).toHaveBeenCalledWith(expect.objectContaining({ type: 'notice' }))
    expect(getPopupNotices).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('First popup')
    expect(wrapper.text()).not.toContain('Banner popup')
    expect(wrapper.text()).not.toContain('Notification only')

    await wrapper.find('.popup-footer button').trigger('click')
    await flushPromises()

    expect(localStorage.getItem('notice_read_public_1')).toBe('1')
    expect(wrapper.text()).toContain('Second popup')
  })

  it('loads logged-in popup notices and dismisses them through the popup API', async () => {
    const authStore = useAuthStore()
    authStore.token = 'token'
    getPopupNotices.mockResolvedValue({ code: 200, data: [notice(9, 'Private popup', 'notice', 'popup')] })
    dismissPopup.mockResolvedValue({ code: 200 })

    const wrapper = mount(PopupNotice)
    await flushPromises()

    expect(getPopupNotices).toHaveBeenCalledTimes(1)
    expect(getActiveNotices).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Private popup')

    await wrapper.find('.popup-close').trigger('click')
    await flushPromises()

    expect(dismissPopup).toHaveBeenCalledWith(9)
    expect(wrapper.find('.popup-card').exists()).toBe(false)
  })
})

function notice(id, title, type, deliveryType) {
  return {
    id_wsh: id,
    title_wsh: title,
    content_wsh: `${title} content`,
    type_wsh: type,
    delivery_type_wsh: deliveryType,
  }
}