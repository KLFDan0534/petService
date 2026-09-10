import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import BannerCarousel from '@/components/dashboard/BannerCarousel.vue'

const banners = [
  {
    id_wsh: 1,
    title_wsh: '夏日寄养优惠',
    image_url_wsh: '/minio/banner-one.webp',
    link_url_wsh: 'https://example.com/summer',
    status_wsh: 1,
  },
  {
    id_wsh: 2,
    title_wsh: '新客美容体验',
    image_url_wsh: '/minio/banner-two.webp',
    link_url_wsh: 'javascript:alert(1)',
    status_wsh: 1,
  },
]

function mountBanner() {
  return mount(BannerCarousel, {
    props: { banners },
    global: {
      stubs: {
        'el-icon': { template: '<span><slot /></span>' },
        ArrowLeft: true,
        ArrowRight: true,
        MediaWithFallback: {
          props: ['src', 'alt'],
          template: '<img :src="src" :alt="alt">',
        },
      },
    },
  })
}

describe('BannerCarousel.vue', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      value: vi.fn(() => ({ matches: false })),
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders active administrator banners and switches slides', async () => {
    const wrapper = mountBanner()

    expect(wrapper.text()).toContain('夏日寄养优惠')
    expect(wrapper.get('.campaign-slide').attributes('href')).toBe('https://example.com/summer')

    await wrapper.get('button[aria-label="下一条广告"]').trigger('click')

    expect(wrapper.text()).toContain('新客美容体验')
    expect(wrapper.get('.campaign-slide').attributes('href')).toBeUndefined()
  })
})
