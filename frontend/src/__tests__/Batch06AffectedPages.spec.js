import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Pets from '@/views/user/Pets.vue'
import PetFormDialog from '@/components/pet/PetFormDialog.vue'
import KeeperApply from '@/views/user/KeeperApply.vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

/**
 * 批次 06 其余受影响页面：只验证「被迁移的那几个调用」，不重复测页面其它逻辑。
 *
 * 覆盖：
 *  - Pets.vue：加载 / 新增 / 编辑 / 删除 走 pet api
 *  - PetFormDialog.vue：头像上传走 file.uploadFileToDirectory('pets', fd)
 *  - KeeperApply.vue：商家列表 / 当前用户 / 看护者信息 / 提交申请走具名 api
 *  - AdminLayout.vue：进后台用 auth.getCurrentUser 刷新头像
 */
const mocks = vi.hoisted(() => ({
  // pet
  getPets: vi.fn(),
  createPet: vi.fn(),
  updatePet: vi.fn(),
  deletePet: vi.fn(),
  // file
  uploadFileToDirectory: vi.fn(),
  // keeper / merchant / auth
  getMyKeeperInfo: vi.fn(),
  createKeeper: vi.fn(),
  getMerchants: vi.fn(),
  getCurrentUser: vi.fn(),
  // 其它
  ensureProfileRequirement: vi.fn(),
  routerPush: vi.fn(),
  resetDynamicRoutes: vi.fn(),
}))

vi.mock('@/api/pet', () => ({
  getPets: mocks.getPets,
  createPet: mocks.createPet,
  updatePet: mocks.updatePet,
  deletePet: mocks.deletePet,
}))

vi.mock('@/api/file', () => ({
  uploadFileToDirectory: mocks.uploadFileToDirectory,
}))

vi.mock('@/api/keeper', () => ({
  getMyKeeperInfo: mocks.getMyKeeperInfo,
  createKeeper: mocks.createKeeper,
}))

vi.mock('@/api/merchant', () => ({
  getMerchants: mocks.getMerchants,
}))

vi.mock('@/api/auth', () => ({
  getCurrentUser: mocks.getCurrentUser,
}))

vi.mock('@/utils/profileRequirements', () => ({
  ensureProfileRequirement: mocks.ensureProfileRequirement,
  PROFILE_ACTIONS: { ADD_PET: 'addPet', APPLY_KEEPER: 'applyKeeper' },
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mocks.routerPush }),
  useRoute: () => ({ query: {} }),
  RouterLink: { template: '<a><slot /></a>' },
}))

vi.mock('@/router', () => ({
  resetDynamicRoutes: mocks.resetDynamicRoutes,
  addDynamicRoutes: vi.fn(),
}))

function freshPinia() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return pinia
}

describe('Pets.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.ensureProfileRequirement.mockResolvedValue(true)
    mocks.getPets.mockResolvedValue({ code: 200, data: [{ id_wsh: 1, name_wsh: '豆豆' }] })
  })

  it('加载列表走 pet.getPets', async () => {
    const pinia = freshPinia()
    mount(Pets, {
      global: { plugins: [pinia], stubs: { PetList: true, PetFormDialog: true } },
    })
    await flushPromises()

    expect(mocks.getPets).toHaveBeenCalledTimes(1)
  })

  it('保存（新增/编辑）走 pet.createPet / pet.updatePet', async () => {
    mocks.createPet.mockResolvedValue({ code: 200, data: { id_wsh: 2 } })
    mocks.updatePet.mockResolvedValue({ code: 200, data: { id_wsh: 1 } })
    const pinia = freshPinia()

    const wrapper = mount(Pets, {
      global: {
        plugins: [pinia],
        stubs: {
          PetList: true,
          PetFormDialog: {
            name: 'PetFormDialog',
            emits: ['save'],
            template: `<div><button class="t-save-new" @click="$emit('save', { name_wsh: '新宠物' })">new</button></div>`,
          },
        },
      },
    })
    await flushPromises()

    await wrapper.get('.t-save-new').trigger('click')
    await flushPromises()

    expect(mocks.createPet).toHaveBeenCalledWith({ name_wsh: '新宠物' })
    expect(mocks.updatePet).not.toHaveBeenCalled()
  })
})

describe('PetFormDialog.vue 头像上传', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('选择图片后调用 file.uploadFileToDirectory("pets", FormData)', async () => {
    mocks.uploadFileToDirectory.mockResolvedValue({ code: 200, data: { url_wsh: '/img/p.png' } })
    const pinia = freshPinia()
    const appStore = useAppStore(pinia)
    const addToast = vi.spyOn(appStore, 'addToast').mockImplementation(() => {})

    const wrapper = mount(PetFormDialog, {
      global: { plugins: [pinia], stubs: { AppDialog: { template: '<div><slot /></div>' } } },
      props: { modelValue: true },
    })
    await flushPromises()

    const fileInput = wrapper.get('input[type="file"]')
    const file = { name: 'a.png', type: 'image/png', size: 1024 }
    Object.defineProperty(fileInput.element, 'files', { value: [file], configurable: true })
    await fileInput.trigger('change')
    await flushPromises()

    expect(mocks.uploadFileToDirectory).toHaveBeenCalledTimes(1)
    const [dir, fd] = mocks.uploadFileToDirectory.mock.calls[0]
    expect(dir).toBe('pets')
    expect(fd).toBeInstanceOf(FormData)
    expect(addToast).not.toHaveBeenCalledWith('上传失败', 'error')
  })
})

describe('KeeperApply.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.getMerchants.mockResolvedValue({ code: 200, data: [{ id_wsh: 1, name_wsh: 'PetCare' }] })
    mocks.getCurrentUser.mockResolvedValue({ code: 200, data: { id_wsh: 3, nickname_wsh: '小王' } })
    mocks.getMyKeeperInfo.mockRejectedValue(new Error('404'))
  })

  it('进页面并行拉商家列表 + 当前用户 + 看护者信息（失败也不阻断）', async () => {
    const pinia = freshPinia()
    mount(KeeperApply, {
      global: { plugins: [pinia], stubs: { 'el-icon': { template: '<span><slot /></span>' } } },
    })
    await flushPromises()

    expect(mocks.getMerchants).toHaveBeenCalledTimes(1)
    expect(mocks.getCurrentUser).toHaveBeenCalledTimes(1)
    expect(mocks.getMyKeeperInfo).toHaveBeenCalledTimes(1)
  })
})

describe('AdminLayout.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('进后台用 auth.getCurrentUser 刷新头像并写回本地', async () => {
    mocks.getCurrentUser.mockResolvedValue({ code: 200, data: { avatar_wsh: '/img/new.png' } })
    const pinia = freshPinia()
    const authStore = useAuthStore(pinia)
    authStore.user = { id_wsh: 1, username_wsh: 'admin', avatar_wsh: '/img/old.png' }

    mount(AdminLayout, {
      global: {
        plugins: [pinia],
        stubs: {
          MessageIndicator: true,
          ThemeToggle: true,
          RouterLink: { template: '<a><slot /></a>' },
          RouterView: true,
        },
      },
    })
    await flushPromises()

    expect(mocks.getCurrentUser).toHaveBeenCalledTimes(1)
    expect(authStore.user.avatar_wsh).toBe('/img/new.png')
    expect(JSON.parse(localStorage.getItem('user')).avatar_wsh).toBe('/img/new.png')
  })

  it('接口失败时不破坏本地用户信息', async () => {
    mocks.getCurrentUser.mockRejectedValue(new Error('boom'))
    const pinia = freshPinia()
    const authStore = useAuthStore(pinia)
    authStore.user = { id_wsh: 1, username_wsh: 'admin', avatar_wsh: '/img/old.png' }

    mount(AdminLayout, {
      global: {
        plugins: [pinia],
        stubs: {
          MessageIndicator: true,
          ThemeToggle: true,
          RouterLink: { template: '<a><slot /></a>' },
          RouterView: true,
        },
      },
    })
    await flushPromises()

    expect(authStore.user.avatar_wsh).toBe('/img/old.png')
  })
})
