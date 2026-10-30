import request from '@/utils/request'

// 后台用户列表，按手机号、状态这些条件筛着看
export async function getUsers(params) {
  const res = await request.get('/api/users', { params })
  return res.data
}

// 封号或解封，封了这人就登不上了
export async function updateUserStatus(id, status) {
  const res = await request.put(`/api/users/${id}/status`, { status_wsh: status })
  return res.data
}

export async function getUserRoles(userId) {
  const res = await request.get(`/api/users/${userId}/roles`)
  return res.data
}

// 整批换掉这人的角色，传什么就是什么，不是往里加
export async function setUserRoles(userId, roleIds) {
  const res = await request.put(`/api/users/${userId}/roles`, { role_ids_wsh: roleIds })
  return res.data
}

// 系统里所有角色，分配权限时选人用
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

// 操作日志，谁在什么时候改了啥，出问题时靠它查
export async function getOperationLogs(params) {
  const res = await request.get('/api/operation-logs', { params })
  return res.data
}

export async function getOperationLog(id) {
  const res = await request.get(`/api/operation-logs/${id}`)
  return res.data
}

// 看回收站里有哪几张表能翻，先选表再查数据
export async function getRecycleBinTables() {
  const res = await request.get('/api/recycle-bin/tables')
  return res.data
}

export async function getRecycleBin(params) {
  const res = await request.get('/api/recycle-bin', { params })
  return res.data
}

// 把删掉的那条数据恢复回来，误删就靠这个救
export async function restoreFromRecycleBin(table, id) {
  const res = await request.post(`/api/recycle-bin/restore?table=${table}&id=${id}`, {})
  return res.data
}

// 彻底清掉，这一步做完就再也恢复不了了
export async function permanentDeleteFromRecycleBin(table, id) {
  const res = await request.post(`/api/recycle-bin/delete?table=${table}&id=${id}`, {})
  return res.data
}



