import request from '@/utils/request'

export const PROFILE_ACTIONS = {
  ADD_PET: 'ADD_PET',
  CREATE_ORDER: 'CREATE_ORDER',
  APPLY_MERCHANT: 'APPLY_MERCHANT',
  APPLY_KEEPER: 'APPLY_KEEPER',
}

const ACTION_REQUIREMENTS = {
  [PROFILE_ACTIONS.ADD_PET]: {
    label: '上传宠物',
    requireRealName: true,
    requirePhone: false,
  },
  [PROFILE_ACTIONS.CREATE_ORDER]: {
    label: '下单',
    requireRealName: true,
    requirePhone: true,
  },
  [PROFILE_ACTIONS.APPLY_MERCHANT]: {
    label: '成为商家',
    requireRealName: true,
    requirePhone: true,
  },
  [PROFILE_ACTIONS.APPLY_KEEPER]: {
    label: '成为keeper',
    requireRealName: true,
    requirePhone: true,
  },
}

export function isRealNameVerified(user) {
  return Number(user?.real_name_status_wsh || 0) === 2
}

export function hasBoundPhone(user) {
  return Boolean(String(user?.phone_wsh || '').trim())
}

export async function ensureProfileRequirement(action, { authStore, appStore, router }) {
  const requirement = ACTION_REQUIREMENTS[action]
  if (!requirement) return true

  let user
  try {
    user = await refreshCurrentUser(authStore)
  } catch (error) {
    appStore?.addToast(error.message || '获取个人资料失败', 'error')
    return false
  }

  if (requirement.requireRealName && !isRealNameVerified(user)) {
    appStore?.addToast(`${requirement.label}前需要先完成实名认证`, 'warning')
    router?.push('/profile')
    return false
  }

  if (requirement.requirePhone && !hasBoundPhone(user)) {
    appStore?.addToast(`${requirement.label}前需要先绑定手机号`, 'warning')
    router?.push('/profile')
    return false
  }

  return true
}

async function refreshCurrentUser(authStore) {
  const r = await request.get('/users/me')
  if (r.data.code !== 200 || !r.data.data) {
    throw new Error(r.data.message || '获取个人资料失败')
  }
  const user = r.data.data
  if (authStore?.user) {
    authStore.user = { ...authStore.user, ...user }
  }
  localStorage.setItem('user', JSON.stringify({ ...(authStore?.user || {}), ...user }))
  return user
}
