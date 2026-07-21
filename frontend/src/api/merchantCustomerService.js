import request from '@/utils/request'

export async function applyMerchantCustomerService(data) {
  const res = await request.post('/api/merchant-customer-service/applications', data)
  return res.data
}

export async function getMyCustomerServiceApplications() {
  const res = await request.get('/api/merchant-customer-service/applications/me')
  return res.data
}

export async function getPendingCustomerServiceApplications() {
  const res = await request.get('/api/merchant-customer-service/merchant/applications/pending')
  return res.data
}

export async function getMerchantCustomerServiceStaff() {
  const res = await request.get('/api/merchant-customer-service/merchant/staff')
  return res.data
}

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
