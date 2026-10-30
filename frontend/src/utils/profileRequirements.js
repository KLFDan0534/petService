import request from '@/utils/request'

export const PROFILE_ACTIONS = {
  ADD_PET: 'ADD_PET',
  CREATE_ORDER: 'CREATE_ORDER',
  APPLY_MERCHANT: 'APPLY_MERCHANT',
  APPLY_KEEPER: 'APPLY_KEEPER',
}

// 每件事干之前需要用户满足哪些资料条件，写死在这张表里，后端规则变了这里也要跟着改
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

// 实名状态 2 表示审核通过，其他值（0 未提交、1 审核中、3 驳回）都算没通过
export function isRealNameVerified(user) {
  return Number(user?.real_name_status_wsh || 0) === 2
}

export function hasBoundPhone(user) {
  return Boolean(String(user?.phone_wsh || '').trim())
}

// 动手之前先查一遍资料，够了返回 true 放行，不够就弹提示并把人带去个人中心
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

// 为什么要重新拉一次？因为本地存的用户信息可能是几天前的，实名刚通过时还没更新
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
