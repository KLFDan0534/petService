import { mount } from '@vue/test-utils'
import DataTable from '@/components/common/DataTable.vue'

describe('DataTable.vue', () => {
  // 单元格内容可能是用户填的，必须当纯文本渲染，不能让它真的变成 img 标签
  it('renders HTML-like strings as text', () => {
    const wrapper = mount(DataTable, {
      props: {
        columns: [{ label: 'Name', key: 'name' }],
        data: [{ name: '<img src=x onerror=alert(1)>' }],
      },
    })

    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.text()).toContain('<img src=x onerror=alert(1)>')
  })

  // 带 badge 的对象走专门的分支画成徽章，同样不走 v-html
  it('renders structured badge cells without v-html', () => {
    const wrapper = mount(DataTable, {
      props: {
        columns: [{ label: 'Status', key: 'status' }],
        data: [{ status: { label: '已完成', badge: 'badge-success' } }],
      },
    })

    const badge = wrapper.find('.badge-success')
    expect(badge.exists()).toBe(true)
    expect(badge.text()).toBe('已完成')
  })
})
