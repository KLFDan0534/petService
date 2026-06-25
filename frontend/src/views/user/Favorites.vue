<template>
  <div>
    <PageHero title="我的收藏" subtitle="查看您收藏的商家和服务" />
    <EmptyState v-if="!loading && favorites.length === 0" title="暂无收藏" icon="⭐" description="去浏览并收藏您喜欢的服务吧">
      <router-link to="/dashboard" class="btn btn-primary">浏览服务</router-link>
    </EmptyState>
    <div v-else class="pet-grid">
      <div v-for="f in favorites" :key="f.id_wsh" class="pet-card">
        <div class="pet-avatar">⭐</div>
        <div class="pet-name">{{ f.name_wsh }}</div>
        <div class="pet-info">{{ f.description_wsh || '' }}</div>
        <button class="btn btn-sm btn-danger" style="margin-top:12px" @click="removeFav(f.id_wsh)">取消收藏</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import PageHero from '@/components/common/PageHero.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const authStore = useAuthStore()
const appStore = useAppStore()
const favorites = ref([])
const loading = ref(true)

onMounted(async () => {
  try { const r = await authStore.apiGet('/api/favorites'); if (r.code === 200) favorites.value = r.data }
  catch (e) {}
  finally { loading.value = false }
})

async function removeFav(id) {
  try {
    // Find the item to get targetType and targetId
    const item = favorites.value.find(f => f.id_wsh === id);
    const targetType = item?.targetType || 'MERCHANT';
    const targetId = item?.targetId || id;
    const r = await authStore.apiPost('/api/favorites/toggle', { target_type_wsh: targetType, target_id_wsh: targetId });
    if (r.code === 200) { favorites.value = favorites.value.filter(f => f.id_wsh !== id); appStore.addToast('已取消收藏', 'info') }
  }
  catch (e) { appStore.addToast('操作失败', 'error') }
}
</script>
