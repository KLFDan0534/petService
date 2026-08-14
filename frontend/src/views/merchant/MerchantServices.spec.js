import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import MerchantServices from './MerchantServices.vue'
import { useAppStore } from '@/stores/app'

const mocks = vi.hoisted(() => ({
  getMy: vi.fn(),
  getServiceCategoryList: vi.fn(),
  getMerchantServices: vi.fn(),
  createService: vi.fn(),
  updateService: vi.fn(),
  deleteService: vi.fn(),
  toggleServiceStatus: vi.fn(),
  getServiceManageDetail: vi.fn(),
  uploadProductImage: vi.fn(),
}))

vi.mock('@/services/merchantService', () => ({
  getMy: mocks.getMy,
}))

vi.mock('@/api/serviceCategory', () => ({
  getServiceCategoryList: mocks.getServiceCategoryList,
}))

vi.mock('@/api/service', () => ({
  createService: mocks.createService,
  deleteService: mocks.deleteService,
  getMerchantServices: mocks.getMerchantServices,
  getServiceManageDetail: mocks.getServiceManageDetail,
  toggleServiceStatus: mocks.toggleServiceStatus,
  updateService: mocks.updateService,
}))

vi.mock('@/api/file', () => ({
  uploadProductImage: mocks.uploadProductImage,
}))

const categories = [
  { id_wsh: 1, parent_id_wsh: null, name_wsh: '寄养', sort_order_wsh: 1 },
  { id_wsh: 2, parent_id_wsh: 1, name_wsh: '标准寄养', sort_order_wsh: 1 },
]

const listServices = [
  {
    id_wsh: 5,
    name_wsh: 'VIP寄养',
    category_id_wsh: 2,
    price_wsh: 299,
    unit_wsh: '天',
    status_wsh: 1,
    description_wsh: '豪华单间',
    images_wsh: 'http://minio/service/legacy-a.png',
  },
]

const detail = {
  service_wsh: {
    id_wsh: 5,
    name_wsh: 'VIP寄养',
    category_id_wsh: 2,
    price_wsh: 299,
    unit_wsh: '天',
    status_wsh: 1,
    description_wsh: '豪华单间',
    images_wsh: 'http://minio/service/u1.png,http://minio/service/u2.png',
  },
  media_wsh: [
    { id_wsh: 11, file_id_wsh: 101, sort_order_wsh: 0, is_cover_wsh: 1, url_wsh: 'http://minio/service/u1.png' },
    { id_wsh: 12, file_id_wsh: 102, sort_order_wsh: 1, is_cover_wsh: 0, url_wsh: 'http://minio/service/u2.png' },
  ],
}

function imageFile(name = 'a.png', type = 'image/png', size = 1024) {
  const file = new File([new Uint8Array(size)], name, { type })
  return file
}

function setFiles(wrapper, files) {
  const input = wrapper.find('input[type="file"]')
  Object.defineProperty(input.element, 'files', { value: files, configurable: true })
  return input
}

function createWrapper() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const wrapper = mount(MerchantServices, {
    global: { plugins: [pinia] },
  })
  return { wrapper, appStore: useAppStore() }
}

async function openCreateModal(wrapper) {
  await flushPromises()
  await wrapper.find('.btn-primary').trigger('click')
  await flushPromises()
}

async function fillRequired(wrapper, overrides = {}) {
  const fields = {
    'input[placeholder="例如：标准寄养"]': '豪华寄养',
    'input[placeholder="0.00"]': '399',
  }
  for (const [selector, value] of Object.entries(fields)) {
    await wrapper.find(selector).setValue(value)
  }
  await wrapper.find('select').setValue('2')
  if (overrides.unit) {
    await wrapper.find('input[placeholder="天 / 次 / 小时"]').setValue(overrides.unit)
  }
}

describe('MerchantServices 服务产品编辑器', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    mocks.getMy.mockResolvedValue({ id_wsh: 10, name_wsh: '萌宠之家' })
    mocks.getServiceCategoryList.mockResolvedValue({ code: 200, data: categories })
    mocks.getMerchantServices.mockResolvedValue({ code: 200, data: listServices })
    mocks.getServiceManageDetail.mockResolvedValue({ code: 200, data: detail })
    mocks.createService.mockResolvedValue({ code: 200, data: { id_wsh: 99 } })
    mocks.updateService.mockResolvedValue({ code: 200, data: { id_wsh: 5 } })
    mocks.uploadProductImage.mockImplementation((merchantId, data) => {
      const name = data.get('file').name
      const id = name === 'a.png' ? 101 : 102
      return Promise.resolve({
        code: 200,
        data: { id_wsh: id, url_wsh: `http://minio/service/${name}` },
      })
    })
  })

  it('F-MGT-001 创建表单校验必填的名称/分类/价格', async () => {
    const { wrapper } = createWrapper()
    await openCreateModal(wrapper)
    await wrapper.find('form').trigger('submit')
    expect(mocks.createService).not.toHaveBeenCalled()
    await fillRequired(wrapper)
    await wrapper.find('form').trigger('submit')
    expect(mocks.createService).toHaveBeenCalledTimes(1)
  })

  it('F-MGT-002 编辑时管理详情回填标量字段与图册顺序/封面', async () => {
    const { wrapper } = createWrapper()
    await flushPromises()
    await wrapper.findAll('button').find(b => b.text() === '编辑').trigger('click')
    await flushPromises()
    expect(mocks.getServiceManageDetail).toHaveBeenCalledWith(5)
    expect(wrapper.find('input[placeholder="例如：标准寄养"]').element.value).toBe('VIP寄养')
    expect(wrapper.find('input[placeholder="0.00"]').element.value).toBe('299')
    const imgs = wrapper.findAll('.image-preview img')
    expect(imgs.map(img => img.attributes('src'))).toEqual([
      'http://minio/service/u1.png',
      'http://minio/service/u2.png',
    ])
    const badges = wrapper.findAll('.cover-badge')
    expect(badges).toHaveLength(1)
    const coverTile = badges[0].element.closest('.image-preview')
    expect(coverTile.querySelector('img').getAttribute('src')).toBe('http://minio/service/u1.png')
  })

  it('F-MGT-003 多图上传保存文件ID而非仅URL', async () => {
    const { wrapper } = createWrapper()
    await openCreateModal(wrapper)
    const input = setFiles(wrapper, [imageFile('a.png'), imageFile('b.png')])
    await input.trigger('change')
    await flushPromises()
    expect(mocks.uploadProductImage).toHaveBeenCalledTimes(2)
    const firstArg = mocks.uploadProductImage.mock.calls[0]
    expect(firstArg[0]).toBe(10)
    expect(firstArg[1]).toBeInstanceOf(FormData)
    await fillRequired(wrapper)
    await wrapper.find('form').trigger('submit')
    expect(mocks.createService).toHaveBeenCalledTimes(1)
    const payload = mocks.createService.mock.calls[0][0]
    expect(payload.merchant_id_wsh).toBeUndefined()
    expect(payload.media_wsh).toEqual([
      { file_id_wsh: 101, sort_order_wsh: 0, is_cover_wsh: 1 },
      { file_id_wsh: 102, sort_order_wsh: 1, is_cover_wsh: 0 },
    ])
  })

  it('F-MGT-004 部分上传失败保留成功图片且重试不产生重复', async () => {
    mocks.uploadProductImage.mockImplementation((merchantId, data) => {
      const name = data.get('file').name
      if (name === 'bad.png') return Promise.reject(new Error('上传失败'))
      const id = { 'a.png': 101, 'b.png': 102 }[name]
      return Promise.resolve({
        code: 200,
        data: { id_wsh: id, url_wsh: `http://minio/service/${name}` },
      })
    })
    const { wrapper } = createWrapper()
    await openCreateModal(wrapper)
    await setFiles(wrapper, [imageFile('a.png'), imageFile('bad.png')]).trigger('change')
    await flushPromises()
    expect(wrapper.findAll('.image-preview')).toHaveLength(1)
    mocks.uploadProductImage.mockResolvedValue({
      code: 200,
      data: { id_wsh: 103, url_wsh: 'http://minio/service/bad.png' },
    })
    await setFiles(wrapper, [imageFile('bad.png')]).trigger('change')
    await flushPromises()
    const srcs = wrapper.findAll('.image-preview img').map(img => img.attributes('src'))
    expect(srcs).toEqual([
      'http://minio/service/a.png',
      'http://minio/service/bad.png',
    ])
  })

  it('F-MGT-005 左移/右移确定性重排图册顺序', async () => {
    const { wrapper } = createWrapper()
    await openCreateModal(wrapper)
    await setFiles(wrapper, [imageFile('a.png'), imageFile('b.png')]).trigger('change')
    await flushPromises()
    const srcs = () => wrapper.findAll('.image-preview img').map(img => img.attributes('src'))
    expect(srcs()).toEqual([
      'http://minio/service/a.png',
      'http://minio/service/b.png',
    ])
    await wrapper.findAll('.media-actions button')[1].trigger('click')
    expect(srcs()).toEqual([
      'http://minio/service/b.png',
      'http://minio/service/a.png',
    ])
    await wrapper.findAll('.media-actions button')[4].trigger('click')
    expect(srcs()).toEqual([
      'http://minio/service/a.png',
      'http://minio/service/b.png',
    ])
  })

  it('F-MGT-006 封面唯一且移除封面后确定性替补', async () => {
    const { wrapper } = createWrapper()
    await openCreateModal(wrapper)
    await setFiles(wrapper, [imageFile('a.png'), imageFile('b.png')]).trigger('change')
    await flushPromises()
    const coverCount = () => wrapper.findAll('.cover-badge').length
    expect(coverCount()).toBe(1)
    const second = wrapper.findAll('.image-preview')[1]
    await second.findAll('button')[2].trigger('click')
    expect(coverCount()).toBe(1)
    expect(second.element.querySelector('img').getAttribute('src')).toContain('b.png')
    const tiles = wrapper.findAll('.image-preview')
    await tiles[1].findAll('button')[3].trigger('click')
    expect(coverCount()).toBe(1)
    expect(wrapper.findAll('.image-preview')[0].element.querySelector('img').getAttribute('src')).toContain('a.png')
  })

  it('F-MGT-007 拒绝错误MIME/超大文件/超过10张并给出可见错误', async () => {
    const { wrapper } = createWrapper()
    await openCreateModal(wrapper)
    const oversize = imageFile('big.png', 'image/png', 11 * 1024 * 1024)
    await setFiles(wrapper, [oversize]).trigger('change')
    await flushPromises()
    expect(mocks.uploadProductImage).not.toHaveBeenCalled()
    await setFiles(wrapper, [imageFile('movie.mp4', 'video/mp4')]).trigger('change')
    await flushPromises()
    expect(mocks.uploadProductImage).not.toHaveBeenCalled()
    await setFiles(wrapper, [imageFile('a.png')]).trigger('change')
    await flushPromises()
    expect(mocks.uploadProductImage).toHaveBeenCalledTimes(1)
    const before = mocks.uploadProductImage.mock.calls.length
    const many = Array.from({ length: 10 }, (_, i) => imageFile(`img${i}.png`, 'image/png', 100))
    await setFiles(wrapper, many).trigger('change')
    await flushPromises()
    expect(mocks.uploadProductImage.mock.calls.length).toBe(before)
    const toasts = wrapper.vm.appStore?.toasts ?? []
    expect(toasts.length).toBeGreaterThan(0)
  })

  it('F-MGT-008 保存失败保留已填数据并恢复控件', async () => {
    mocks.createService.mockResolvedValue({ code: 500, message: '保存服务失败' })
    const { wrapper } = createWrapper()
    await openCreateModal(wrapper)
    await fillRequired(wrapper)
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.find('input[placeholder="例如：标准寄养"]').element.value).toBe('豪华寄养')
    const submit = wrapper.find('button[type="submit"]')
    expect(submit.attributes('disabled')).toBeUndefined()
  })

  it('F-MGT-009 连续双击只发出一次变更请求', async () => {
    const { wrapper } = createWrapper()
    await openCreateModal(wrapper)
    await fillRequired(wrapper)
    const first = wrapper.vm.saveService()
    const second = wrapper.vm.saveService()
    await Promise.all([first, second])
    expect(mocks.createService).toHaveBeenCalledTimes(1)
  })

  it('F-MGT-010 未保存更改关闭前拦截', async () => {
    const { wrapper } = createWrapper()
    await openCreateModal(wrapper)
    await wrapper.find('input[placeholder="例如：标准寄养"]').setValue('临时名称')
    window.confirm.mockReturnValue(false)
    await wrapper.find('.btn-secondary').trigger('click')
    await flushPromises()
    expect(wrapper.find('.service-modal').exists()).toBe(true)
    window.confirm.mockReturnValue(true)
    await wrapper.find('.btn-secondary').trigger('click')
    await flushPromises()
    expect(wrapper.find('.service-modal').exists()).toBe(false)
  })

  it('F-MGT-002 旧版仅URL图册在编辑时以只读方式回显并在保存时提示重新上传', async () => {
    mocks.getServiceManageDetail.mockResolvedValue({
      code: 200,
      data: { service_wsh: detail.service_wsh, media_wsh: [] },
    })
    const { wrapper } = createWrapper()
    await flushPromises()
    await wrapper.findAll('button').find(b => b.text() === '编辑').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('.image-preview img')[0].attributes('src')).toBe('http://minio/service/u1.png')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(mocks.updateService).not.toHaveBeenCalled()
  })
})
