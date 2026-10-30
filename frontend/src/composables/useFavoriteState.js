import { computed, ref, unref, watch } from 'vue'
import { check as checkFavorite, toggle as toggleFavorite } from '@/services/favoriteService'
import { normalizeFavoriteTargetType } from '@/constants/favorite'

export function useFavoriteState(targetId, targetType, options = {}) {
  const isFavorited = ref(false)
  const loading = ref(true)
  const toggling = ref(false)
  // enabled 是外面传进来的开关，一般登录了才打开；没传就当开着
  const enabled = computed(() => {
    if (typeof options.enabled === 'undefined') {
      return true
    }
    return !!unref(options.enabled)
  })

  // 没登录、没 id 或类型不对，就直接当没收藏，也不去调接口
  // 为什么？因为未登录调收藏接口只会拿到 401，不如省掉这次请求
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

  // 点一下收藏：接口成功后再把本地状态翻过来，不是先翻再请求
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

  // 目标一变就重新查一次；immediate 保证组件一挂载就开始查
  watch(() => [Number(unref(targetId)), normalizeFavoriteTargetType(unref(targetType)), enabled.value], () => {
    refresh()
  }, { immediate: true })

  return { isFavorited, loading, toggling, refresh, toggle }
}
