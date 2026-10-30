import { ref, computed } from 'vue'
import * as KeeperDomain from '@/domain/KeeperDomain'
import { KeeperOnlineStatus, getStatusLabel, getStatusBadge } from '@/constants/statusMaps'

export function useKeeperDetail(id) {
  const loading = ref(false)
  const error = ref(null)
  const data = ref(null)

  // 详情接口一次返回好几块，这里拆开给页面用
  const keeper = computed(() => data.value?.keeper ?? null)
  const ratings = computed(() => data.value?.ratings ?? [])
  const qualifications = computed(() => data.value?.qualifications ?? [])
  const stats = computed(() => data.value?.stats ?? {})
  const avgScore = computed(() => stats.value.avgScore ?? '-')

  // 在线状态查不到时给个占位，别让徽章空着
  const onlineLabel = computed(() => {
    const k = keeper.value
    return k ? getStatusLabel(KeeperOnlineStatus, k.status_wsh) : '-'
  })

  const onlineBadge = computed(() => {
    const k = keeper.value
    return k ? getStatusBadge(KeeperOnlineStatus, k.status_wsh) : 'badge-info'
  })

  // 拉看护者的基本信息和他的评价列表
  async function load() {
    if (!id) {
      error.value = '缺少看护者ID'
      loading.value = false
      return
    }
    loading.value = true
    error.value = null
    try {
      data.value = await KeeperDomain.getFullProfile(id)
    } catch (e) {
      error.value = e.message || '加载看护者信息失败'
      data.value = null
    } finally {
      loading.value = false
    }
  }

  async function retry() {
    await load()
  }

  return {
    loading, error, data,
    keeper, ratings, qualifications, stats, avgScore,
    onlineLabel, onlineBadge,
    load, retry,
  }
}
