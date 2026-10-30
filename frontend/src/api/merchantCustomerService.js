import request from '@/utils/request'

// 用户申请当某商家的客服，提交后进待审核队列，这时还不能接待
export async function applyMerchantCustomerService(data) {
  const res = await request.post('/api/merchant-customer-service/applications', data)
  return res.data
}

export async function getMyCustomerServiceApplications() {
  const res = await request.get('/api/merchant-customer-service/applications/me')
  return res.data
}

// 商家后台拉待审的申请；这步只是看列表，通过/拒绝要调下面两个接口
export async function getPendingCustomerServiceApplications() {
  const res = await request.get('/api/merchant-customer-service/merchant/applications/pending')
  return res.data
}

export async function getMerchantCustomerServiceStaff() {
  const res = await request.get('/api/merchant-customer-service/merchant/staff')
  return res.data
}

// 审核通过后人直接进商家客服名单；拒绝走下面的 reject，两个接口别混着用
export async function approveCustomerServiceApplication(id, reviewNote = '') {
  const res = await request.post(`/api/merchant-customer-service/merchant/applications/${id}/approve`, {
    review_note_wsh: reviewNote,
  })
  return res.data
}

export async function rejectCustomerServiceApplication(id, reviewNote = '') {
  const res = await request.post(`/api/merchant-customer-service/merchant/applications/${id}/reject`, {
    review_note_wsh: reviewNote,
  })
  return res.data
}
