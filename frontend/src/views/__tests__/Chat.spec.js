import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import Chat from '../user/Chat.vue'

vi.mock('@/stores/design', () => ({
  useDesignStore: () => ({ activeDesign: null }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

function createMockSetup(initialUnread, conversation) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.setAuth({
    userId: 1, username: 'owner', nickname: '宠物主',
    roles: ['OWNER'], accessToken: 't', refreshToken: 'r',
  })
  authStore.apiGet = vi.fn()
    .mockResolvedValueOnce({ code: 200, data: initialUnread })
    .mockResolvedValueOnce({ code: 200, data: conversation })
  authStore.apiPost = vi.fn().mockResolvedValue({ code: 200, data: {} })
  return pinia
}

describe('Chat.vue', () => {
  it('renders chat interface', async () => {
    const pinia = createMockSetup([{ id_wsh: 1, from_user_id_wsh: 2, content_wsh: '你好', type_wsh: 'text', is_read_wsh: 0 }], [])
    const wrapper = mount(Chat, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('消息中心')
    expect(wrapper.text()).toContain('选择一个会话开始聊天')
  })

  it('renders conversation sidebar', async () => {
    const pinia = createMockSetup([], [])
    const wrapper = mount(Chat, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('会话列表')
  })

  it('loads messages when selecting conversation', async () => {
    const initialUnread = [
      { id_wsh: 1, name_wsh: '宠物主', last_message_wsh: '你好', type_wsh: 'text' },
    ]
    const messages = [
      { id_wsh: 1, from_user_id_wsh: 1, to_user_id_wsh: 2, content_wsh: '你好', type_wsh: 'text', created_at_wsh: '2026-01-01T00:00:00' },
      { id_wsh: 2, from_user_id_wsh: 2, to_user_id_wsh: 1, content_wsh: '您好，宠物很好', type_wsh: 'text', created_at_wsh: '2026-01-01T00:01:00' },
    ]
    const pinia = createMockSetup(initialUnread, messages)
    const wrapper = mount(Chat, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const sidebarCards = wrapper.findAll('.chat-sidebar').at(0).findAll('.card')
    await sidebarCards.at(0).trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('你好')
    expect(wrapper.text()).toContain('您好，宠物很好')
  })

  it('shows message input area', async () => {
    const initialUnread = [
      { id_wsh: 1, name_wsh: '宠物主', last_message_wsh: '你好', type_wsh: 'text' },
    ]
    const messages = [
      { id_wsh: 1, from_user_id_wsh: 1, to_user_id_wsh: 2, content_wsh: '你好', type_wsh: 'text', created_at_wsh: '2026-01-01T00:00:00' },
    ]
    const pinia = createMockSetup(initialUnread, messages)
    const wrapper = mount(Chat, { global: { plugins: [pinia] } })
    await new Promise(r => setTimeout(r, 50))
    const sidebarCards = wrapper.findAll('.chat-sidebar').at(0).findAll('.card')
    await sidebarCards.at(0).trigger('click')
    await flushPromises()
    expect(wrapper.find('input').exists()).toBe(true)
    expect(wrapper.text()).toContain('发送')
  })
})
