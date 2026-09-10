import { ref } from 'vue'

/**
 * 统一的加载状态管理（Design System Phase B）。
 * 将 Loading / Empty / Error / Retry 收敛为一份实现，取代各页面的空 catch {} 与“错误当空状态”。
 *
 * 做法：把异步加载器交给 useLoadState，组件只读 loading / error / data，
 * 并在失败时通过 retry() 重新执行，避免页面里裸 catch 吞掉错误。
 *
 * 用法：
 *   const { loading, error, data, run, retry } = useLoadState(fetchFn)
 *   onMounted(run)
 *   button @click="retry"
 */
export function useLoadState(loader, { successField = 'data' } = {}) {
  const loading = ref(false)
  const error = ref(null)
  const data = ref(null)
  const loaded = ref(false)

  async function run(...args) {
    loading.value = true
    error.value = null
    try {
      const result = await loader(...args)
      const payload =
        result && typeof result === 'object' && successField in result ? result[successField] : result
      data.value = payload
      loaded.value = true
      return result
    } catch (e) {
      error.value = e || new Error('请求失败')
      return null
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    error,
    data,
    loaded,
    run,
    load: run,
    retry: run,
  }
}