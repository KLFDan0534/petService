import { flushPromises, mount } from '@vue/test-utils'
import ReviewDialog from '@/components/order/ReviewDialog.vue'

// 一条订单可以分别给商家、看护人、服务打分，这组测的就是三个页签怎么切、怎么提交
describe('ReviewDialog.vue 三维评价', () => {
  const order = {
    id_wsh: 100,
    merchant_id_wsh: 1,
    merchant_name_wsh: '商家甲',
    keeper_id_wsh: 2,
    keeper_name_wsh: '看护人乙',
    service_id_wsh: 3,
    service_name_wsh: '寄养服务',
  }

  function mountDialog(props = {}) {
    return mount(ReviewDialog, {
      props: {
        visible: true,
        order,
        doneTypes: props.doneTypes || [],
        ...props,
      },
    })
  }

  it('renders three independent dimensions, each showing its target name', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    // 页面改版后三个维度平铺成三行，不再是页签；每行各自带目标名
    const rows = wrapper.findAll('.review-row')
    expect(rows).toHaveLength(3)
    expect(rows[0].find('.review-row__label').text()).toContain('商家')
    expect(rows[1].find('.review-row__label').text()).toContain('看护人')
    expect(rows[2].find('.review-row__label').text()).toContain('服务')

    const names = ['商家甲', '看护人乙', '寄养服务']
    names.forEach((name, i) => {
      expect(rows[i].find('.review-row__hint').text()).toContain(name)
    })
  })

  // 商家评过了就该锁住并打标记，剩下没评的维度照样能打分、按维度提交
  it('defaults to the first undone dimension and emits reviewed with dimension payload', async () => {
    const wrapper = mountDialog({ doneTypes: ['merchant'] })
    await flushPromises()

    const rows = wrapper.findAll('.review-row')
    expect(rows[0].find('.review-row__done').exists()).toBe(true)
    expect(rows[0].findAll('.star')[0].attributes('disabled')).toBeDefined()
    expect(rows[1].find('.review-row__done').exists()).toBe(false)
    expect(rows[1].find('.review-row__hint').text()).toContain('看护人乙')

    // 给看护人打 4 星、写留言，再点提交评价
    await rows[1].findAll('.star')[3].trigger('click')
    await wrapper.find('textarea').setValue('很细心')
    await wrapper.find('.app-btn--primary').trigger('click')
    await flushPromises()

    const emitted = wrapper.emitted('reviewed')
    expect(emitted).toHaveLength(1)
    expect(emitted[0][0]).toEqual({
      orderId: 100,
      targetType: 'keeper',
      targetId: 2,
      score: 4,
      content: '很细心',
    })
  })

  // 三个维度都评过了，行上打标记、星星禁掉，点提交也不该再抛出 reviewed
  it('marks already-rated dimensions and blocks resubmission', async () => {
    const wrapper = mountDialog({ doneTypes: ['merchant', 'keeper', 'service'] })
    await flushPromises()

    const rows = wrapper.findAll('.review-row')
    expect(wrapper.findAll('.review-row__done')).toHaveLength(3)
    rows.forEach((row) => {
      expect(row.findAll('.star')[0].attributes('disabled')).toBeDefined()
    })

    // 没有可评维度时提交只给提示，不会重复评价
    await wrapper.find('.app-btn--primary').trigger('click')
    await flushPromises()
    expect(wrapper.emitted('reviewed')).toBeUndefined()
    expect(wrapper.find('.review-error').text()).toContain('请为可评价的维度评分后再提交')
  })

  // 订单没带服务 id，这一行就打不了分，行上写明原因，提交也不会带上它
  it('disables submit when active dimension has no target id', async () => {
    const wrapper = mount(ReviewDialog, {
      props: {
        visible: true,
        order: { id_wsh: 100, merchant_id_wsh: 1, merchant_name_wsh: '商家甲', keeper_id_wsh: null },
        doneTypes: [],
      },
    })
    await flushPromises()

    const serviceRow = wrapper.findAll('.review-row')[2]
    expect(serviceRow.find('.review-row__note').text()).toContain('该订单缺少信息')
    expect(serviceRow.findAll('.star')[0].attributes('disabled')).toBeDefined()

    // 只给商家打分并提交，缺 id 的服务维度不该出现在 reviewed 事件里
    await wrapper.findAll('.review-row')[0].findAll('.star')[3].trigger('click')
    await wrapper.find('.app-btn--primary').trigger('click')
    await flushPromises()

    const emitted = wrapper.emitted('reviewed') || []
    expect(emitted).toHaveLength(1)
    expect(emitted[0][0].targetType).toBe('merchant')
    expect(emitted.map(call => call[0].targetType)).not.toContain('service')
  })
})
