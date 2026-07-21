import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getOrderDetail,
  getOrderTimeline,
  getDailyStatus,
  getOrderMessages,
  markOrderConversationRead,
  getOrderReport,
  regenerateReport,
  createPayment,
  executePayment,
  cancelOrder as cancelOrderApi
} from '@/api/orderDetail'

export const useOrderDetailStore = defineStore('orderDetail', () => {
  const order = ref(null)
  const timeline = ref([])
  const dailyStatus = ref(null)
  const messages = ref([])
  const reports = ref([])
  const loading = ref(true)
  const timelineLoading = ref(false)
  const messageLoading = ref(false)
  const reportLoading = ref(false)
  const error = ref(null)
  const timelinePage = ref(1)
  const timelineHasMore = ref(true)

  async function fetchOrderDetail(id) {
    loading.value = true
    error.value = null
    try {
      const response = await getOrderDetail(id)
      if (response.code === 200) {
        order.value = response.data
      } else {
        error.value = response.message || '加载失败'
      }
    } catch (e) {
      error.value = '网络错误，请稍后重试'
    } finally {
      loading.value = false
    }
  }

  async function fetchTimeline(orderId, append = false) {
    if (!append) {
      timelineLoading.value = true
      timelinePage.value = 1
      timelineHasMore.value = true
    }
    try {
      const [timelineResponse, dailyResponse] = await Promise.all([
        getOrderTimeline(orderId, timelinePage.value),
        getDailyStatus(orderId)
      ])
      if (timelineResponse.data.code === 200) {
        const items = timelineResponse.data.data || []
        timeline.value = append ? [...timeline.value, ...items] : items
        if (items.length < 20) timelineHasMore.value = false
      }
      if (dailyResponse.data.code === 200) dailyStatus.value = dailyResponse.data.data
    } catch (e) {
      if (!append) timeline.value = []
    } finally {
      timelineLoading.value = false
    }
  }

  async function loadMoreTimeline(orderId) {
    if (!timelineHasMore.value || timelineLoading.value) return
    timelinePage.value += 1
    await fetchTimeline(orderId, true)
  }

  async function fetchMessages(orderId) {
    messageLoading.value = true
    try {
      const response = await getOrderMessages(orderId)
      if (response.code === 200) {
        messages.value = response.data || []
        await markConversationRead(orderId)
      }
    } catch (e) {
      messages.value = []
    } finally {
      messageLoading.value = false
    }
  }

  async function markConversationRead(orderId) {
    try {
      const response = await markOrderConversationRead(orderId)
      if (response.code === 200) {
        messages.value = messages.value.map(message => ({
          ...message,
          is_read_wsh: true,
          read_wsh: message.read_wsh === 0 ? 1 : message.read_wsh,
        }))
      }
    } catch (e) {
      // Read state is opportunistic; loading messages should not fail because of it.
    }
  }

  async function fetchReports(orderId) {
    reportLoading.value = true
    try {
      const response = await getOrderReport(orderId)
      if (response.code === 200) reports.value = response.data || []
    } catch (e) {
      reports.value = []
    } finally {
      reportLoading.value = false
    }
  }

  async function regenerate(orderId) {
    if (!order.value) return null
    const response = await regenerateReport({ order_id_wsh: order.value.id_wsh })
    if (response.code === 200) await fetchReports(orderId)
    return response
  }

  async function pay(id, method = 'balance') {
    const createRes = await createPayment({ order_id_wsh: id, method_wsh: method })
    if (createRes.code !== 200) return { success: false, message: createRes.message }
    const payNo = createRes.data?.pay_no_wsh
    if (!payNo) return { success: false, message: '支付单创建失败' }
    const payRes = await executePayment({ pay_no_wsh: payNo })
    if (payRes.code === 200) {
      await fetchOrderDetail(id)
      return { success: true }
    }
    return { success: false, message: payRes.message }
  }

  async function cancel(id) {
    const response = await cancelOrderApi({ order_id_wsh: id })
    if (response.code === 200) {
      await fetchOrderDetail(id)
      return { success: true }
    }
    return { success: false, message: response.message }
  }

  function $reset() {
    order.value = null
    timeline.value = []
    dailyStatus.value = null
    messages.value = []
    reports.value = []
    loading.value = true
    timelineLoading.value = false
    messageLoading.value = false
    reportLoading.value = false
    error.value = null
    timelinePage.value = 1
    timelineHasMore.value = true
  }

  return {
    order,
    timeline,
    dailyStatus,
    messages,
    reports,
    loading,
    timelineLoading,
    messageLoading,
    reportLoading,
    error,
    timelineHasMore,
    fetchOrderDetail,
    fetchTimeline,
    loadMoreTimeline,
    fetchMessages,
    markConversationRead,
    fetchReports,
    regenerate,
    pay,
    cancel,
    $reset
  }
})
