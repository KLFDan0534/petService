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

// 弹窗公告分两种人：没登录的只能看公开公告，登录了才走带身份的接口
describe('PopupNotice.vue', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  // 没登录时只请求公开公告，而且只请求一次，不能每次渲染都去打接口
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
    // banner 类型和只走站内信的都不算弹窗，不该出现在这里
    expect(wrapper.text()).not.toContain('Banner popup')
    expect(wrapper.text()).not.toContain('Notification only')

    // 点“我知道了”后要把这条记进本地，关掉之后下一条公告才会接着弹出来
    await wrapper.find('.popup-footer button').trigger('click')
    await flushPromises()

    expect(localStorage.getItem('notice_read_public_1')).toBe('1')
    expect(wrapper.text()).toContain('Second popup')
  })

  // 登录之后换成另一个接口，关弹窗也要通知后端，不然下次进来还会再弹
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