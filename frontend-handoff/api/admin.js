import request from '@/utils/request'

export async function getUsers(params) {
  const res = await request.get('/api/users', { params })
  return res.data
}

export async function updateUserStatus(id, status) {
  const res = await request.put(`/api/users/${id}/status`, { status_wsh: status })
  return res.data
}

export async function getUserRoles(userId) {
  const res = await request.get(`/api/users/${userId}/roles`)
  return res.data
}

export async function setUserRoles(userId, roleIds) {
  const res = await request.put(`/api/users/${userId}/roles`, { role_ids_wsh: roleIds })
  return res.data
}

export async function getRoles() {
  const res = await request.get('/api/roles')
  return res.data
}

export async function createRole(data) {
  const res = await request.post('/api/roles', data)
  return res.data
}

export async function updateRole(id, data) {
  const res = await request.put(`/api/roles/${id}`, data)
  return res.data
}

export async function deleteRole(id) {
  const res = await request.delete(`/api/roles/${id}`)
  return res.data
}

export async function getOperationLogs(params) {
  const res = await request.get('/api/operation-logs', { params })
  return res.data
}

export async function getRecycleBinTables() {
  const res = await request.get('/api/recycle-bin/tables')
  return res.data
}

export async function getRecycleBin(params) {
  const res = await request.get('/api/recycle-bin', { params })
  return res.data
}

export async function restoreFromRecycleBin(table, id) {
  const res = await request.post(`/api/recycle-bin/restore?table=${table}&id=${id}`, {})
  return res.data
}



