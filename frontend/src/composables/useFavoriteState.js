import { computed, ref, unref, watch } from 'vue'
import { check as checkFavorite, toggle as toggleFavorite } from '@/services/favoriteService'
import { normalizeFavoriteTargetType } from '@/constants/favorite'

export function useFavoriteState(targetId, targetType, options = {}) {
  const isFavorited = ref(false)
  const loading = ref(true)
  const toggling = ref(false)
  const enabled = computed(() => {
    if (typeof options.enabled === 'undefined') {
      return true
    }
    return !!unref(options.enabled)
  })

  async function refresh() {
    const id = Number(unref(targetId))
    const type = normalizeFavoriteTargetType(unref(targetType))
    if (!enabled.value || !id || !type) {
      isFavorited.value = false
      loading.value = false
      return
    }
    loading.value = true
    try {
      isFavorited.value = !!(await checkFavorite(id, type))
    } catch {
      isFavorited.value = false
    } finally {
      loading.value = false
    }
  }

  async function toggle() {
    const id = Number(unref(targetId))
    const type = normalizeFavoriteTargetType(unref(targetType))
    if (!enabled.value || !id || !type) return false
    toggling.value = true
    try {
      await toggleFavorite(id, type)
      isFavorited.value = !isFavorited.value
      return true
    } finally {
      toggling.value = false
    }
  }

  watch(() => [Number(unref(targetId)), normalizeFavoriteTargetType(unref(targetType)), enabled.value], () => {
    refresh()
  }, { immediate: true })

  return { isFavorited, loading, toggling, refresh, toggle }
}
