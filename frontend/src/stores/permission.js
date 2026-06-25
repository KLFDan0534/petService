import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const ROLE_PERMISSIONS = {
  ADMIN: [
    'user:create', 'user:read', 'user:update', 'user:delete',
    'user:ban', 'user:unban',
    'merchant:create', 'merchant:read', 'merchant:update', 'merchant:delete',
    'merchant:approve', 'merchant:reject',
    'order:create', 'order:read', 'order:update', 'order:delete',
    'order:cancel', 'order:approve',
    'pet:create', 'pet:read', 'pet:update', 'pet:delete',
    'pet:approve',
    'role:create', 'role:read', 'role:update', 'role:delete',
    'notice:create', 'notice:read', 'notice:update', 'notice:delete',
    'category:create', 'category:read', 'category:update', 'category:delete',
    'banner:create', 'banner:read', 'banner:update', 'banner:delete',
    'review:read', 'review:approve', 'review:reject',
    'refund:read', 'refund:approve', 'refund:reject',
    'withdrawal:read', 'withdrawal:approve', 'withdrawal:reject',
    'ticket:read', 'ticket:assign', 'ticket:resolve', 'ticket:close',
    'complaint:read', 'complaint:resolve', 'complaint:reject',
    'statistics:read',
    'system:config', 'system:log',
    'keeper:read', 'keeper:approve', 'keeper:reject',
    'transaction:read',
  ],
  MERCHANT: [
    'pet:create', 'pet:read', 'pet:update', 'pet:delete',
    'order:read', 'order:update', 'order:accept', 'order:reject',
    'order:complete', 'order:start',
    'merchant:read', 'merchant:update',
    'service:create', 'service:read', 'service:update', 'service:delete',
    'statistics:read',
    'keeper:read',
  ],
  CUSTOMER_SERVICE: [
    'ticket:read', 'ticket:assign', 'ticket:resolve', 'ticket:close',
    'complaint:read', 'complaint:resolve', 'complaint:reject',
    'review:read', 'review:approve', 'review:reject',
  ],
  USER: [
    'pet:create', 'pet:read', 'pet:update', 'pet:delete',
    'order:create', 'order:read', 'order:cancel',
    'address:create', 'address:read', 'address:update', 'address:delete',
    'profile:read', 'profile:update',
    'ticket:create', 'ticket:read',
    'complaint:create', 'complaint:read',
    'refund:create', 'refund:read',
    'favorite:toggle',
    'rating:create',
    'chat:send',
  ],
}

export const usePermissionStore = defineStore('permission', () => {
  const customPermissions = ref([])

  const userRoles = computed(() => {
    try {
      const user = JSON.parse(localStorage.getItem('user') || 'null')
      if (!user) return []
      const roleMap = { 'OWNER': 'USER', 'KEEPER': 'USER' }
      return (user.roles_wsh || []).map(r => roleMap[r.replace('ROLE_', '')] || r.replace('ROLE_', ''))
    } catch {
      return []
    }
  })

  const allPermissions = computed(() => {
    const perms = new Set()
    userRoles.value.forEach(role => {
      const rolePerms = ROLE_PERMISSIONS[role]
      if (rolePerms) {
        rolePerms.forEach(p => perms.add(p))
      }
    })
    customPermissions.value.forEach(p => perms.add(p))
    return perms
  })

  function can(permission) {
    return allPermissions.value.has(permission)
  }

  function hasRole(role) {
    const cleanRole = role.replace('ROLE_', '')
    const roleMap = { 'OWNER': 'USER', 'KEEPER': 'USER' }
    return userRoles.value.some(r => (roleMap[r] || r) === cleanRole)
  }

  function addCustomPermissions(perms) {
    perms.forEach(p => {
      if (!customPermissions.value.includes(p)) {
        customPermissions.value.push(p)
      }
    })
  }

  return { can, hasRole, userRoles, allPermissions, addCustomPermissions }
})
