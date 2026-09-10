import { flushPromises, mount } from '@vue/test-utils'
import ReviewDialog from '@/components/order/ReviewDialog.vue'

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

    const tabs = wrapper.findAll('.dimension-tab').map(tab => tab.text())
    expect(tabs).toContain('商家')
    expect(tabs).toContain('看护人')
    expect(tabs).toContain('服务')

    const names = ['商家甲', '看护人乙', '寄养服务']
    for (let i = 0; i < names.length; i += 1) {
      await wrapper.findAll('.dimension-tab').at(i).trigger('click')
      await flushPromises()
      expect(wrapper.find('.dimension-target').text()).toContain(names[i])
    }
  })

  it('defaults to the first undone dimension and emits reviewed with dimension payload', async () => {
    const wrapper = mountDialog({ doneTypes: ['merchant'] })
    await flushPromises()

    expect(wrapper.find('.dimension-tab.active').text()).toContain('看护人')
    expect(wrapper.find('.dimension-target').text()).toContain('看护人乙')

    await wrapper.find('select').setValue('4')
    await wrapper.find('textarea').setValue('很细心')
    await wrapper.find('.btn-primary').trigger('click')
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

  it('marks already-rated dimensions and blocks resubmission', async () => {
    const wrapper = mountDialog({ doneTypes: ['merchant', 'keeper', 'service'] })
    await flushPromises()

    const doneCount = wrapper.findAll('.dimension-done')
    expect(doneCount).toHaveLength(3)
    const submit = wrapper.find('.btn-primary')
    expect(submit.attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('该维度已评价')
  })

  it('disables submit when active dimension has no target id', async () => {
    const wrapper = mount(ReviewDialog, {
      props: {
        visible: true,
        order: { id_wsh: 100, merchant_id_wsh: 1, merchant_name_wsh: '商家甲', keeper_id_wsh: null },
        doneTypes: [],
      },
    })
    await flushPromises()

    const serviceTab = wrapper.findAll('.dimension-tab').at(2)
    await serviceTab.trigger('click')
    await flushPromises()

    expect(wrapper.find('.dimension-note').text()).toContain('缺少服务信息，无法评价')
    expect(wrapper.find('.btn-primary').attributes('disabled')).toBeDefined()
  })
})
