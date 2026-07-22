import { mount } from '@vue/test-utils'
import MediaWithFallback from './MediaWithFallback.vue'

function mountMedia(props = {}) {
  return mount(MediaWithFallback, {
    props: {
      alt: '宠物服务现场',
      placeholder: '图片暂不可用',
      ...props,
    },
    global: {
      stubs: {
        'el-icon': { template: '<span class="icon-stub"><slot /></span>' },
        Picture: true,
      },
    },
  })
}

describe('MediaWithFallback.vue', () => {
  it('normalizes local MinIO URLs through the frontend proxy', () => {
    const wrapper = mountMedia({ src: 'http://127.0.0.1:9000/minio/pets/photo.jpg?size=large' })

    expect(wrapper.get('img').attributes('src')).toBe('/minio/pets/photo.jpg?size=large')
  })

  it('shows a fallback after an image error and retries when src changes', async () => {
    const wrapper = mountMedia({ src: '/images/first.jpg' })

    await wrapper.get('img').trigger('error')
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.get('.media-placeholder').text()).toContain('图片暂不可用')

    await wrapper.setProps({ src: '/images/second.jpg' })
    expect(wrapper.get('img').attributes('src')).toBe('/images/second.jpg')
  })
})
