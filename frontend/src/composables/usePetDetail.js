import { ref, computed } from 'vue'
import * as PetDomain from '@/domain/PetDomain'
import { OrderStatus, getStatusLabel, getStatusBadge } from '@/constants/statusMaps'
import { formatDate } from '@/utils/format'

export function usePetDetail(id) {
  const loading = ref(false)
  const error = ref(null)
  const data = ref(null)

  const pet = computed(() => data.value?.pet ?? null)
  const orders = computed(() => data.value?.orders ?? [])
  const reports = computed(() => data.value?.reports ?? [])
  const canGenerate = computed(() => data.value?.canGenerateReport ?? false)

  function formatAge(a) {
    return PetDomain.formatAge(a)
  }

  function genderLabel(g) {
    return PetDomain.genderLabel(g)
  }

  function orderLabel(status) {
    return getStatusLabel(OrderStatus, status)
  }

  function orderBadge(status) {
    return getStatusBadge(OrderStatus, status)
  }

  async function load() {
    if (!id) {
      error.value = '缺少宠物ID'
      loading.value = false
      return
    }
    loading.value = true
    error.value = null
    try {
      data.value = await PetDomain.getFullProfile(id)
    } catch (e) {
      error.value = e.message || '加载宠物信息失败'
      data.value = null
    } finally {
      loading.value = false
    }
  }

  async function retry() {
    await load()
  }

  async function handleUpdateAvatar(file) {
    if (!pet.value) return
    const avatarUrl = await PetDomain.updateAvatar(pet.value.id_wsh, file)
    if (data.value) {
      data.value = { ...data.value, pet: { ...data.value.pet, avatar_wsh: avatarUrl } }
    }
    return avatarUrl
  }

  async function handleUpdateProfile(payload) {
    if (!pet.value) return
    const updated = await PetDomain.updateProfile(pet.value.id_wsh, payload)
    if (data.value) {
      data.value = { ...data.value, pet: updated }
    }
    return updated
  }

  return {
    loading, error, data,
    pet, orders, reports, canGenerate,
    formatAge, genderLabel, orderLabel, orderBadge, formatDate,
    load, retry,
    updateAvatar: handleUpdateAvatar,
    updateProfile: handleUpdateProfile,
  }
}
