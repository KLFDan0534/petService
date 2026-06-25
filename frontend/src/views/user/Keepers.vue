<template>
  <div>
    <PageHero title="寄养员" subtitle="找到最适合的宠物照护人" />
    <div class="form-group" style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap;margin-bottom:20px">
      <div>
        <label>纬度</label>
        <input v-model="lat" placeholder="纬度" class="input" style="width:120px" />
      </div>
      <div>
        <label>经度</label>
        <input v-model="lng" placeholder="经度" class="input" style="width:120px" />
      </div>
      <div>
        <label>半径(km)</label>
        <input v-model="radius" placeholder="半径" class="input" style="width:100px" />
      </div>
      <button class="btn btn-primary" @click="nearbySearch">附近搜索</button>
      <button class="btn btn-outline" @click="resetSearch">重置</button>
    </div>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="pet-grid">
      <div v-for="k in keepers" :key="k.id_wsh" class="pet-card">
        <div class="pet-avatar">👤</div>
        <div class="pet-name">{{ k.name_wsh || '寄养员' }}</div>
        <div class="pet-info">⭐ {{ k.rating_wsh || '暂无' }} · 经验: {{ k.experience_years_wsh || 0 }} 年</div>
        <div style="margin-top:12px;display:flex;gap:8px">
          <button class="btn btn-sm btn-primary" @click="$router.push(`/chat?userId=${k.id_wsh}`)">联系</button>
          <button class="btn btn-sm btn-outline" @click="$router.push('/dashboard')">查看服务</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import PageHero from '@/components/common/PageHero.vue'

const authStore = useAuthStore()
const keepers = ref([])
const loading = ref(true)
const lat = ref('')
const lng = ref('')
const radius = ref('')

async function loadKeepers() {
  loading.value = true
  try { const r = await authStore.apiGet('/api/keepers'); if (r.code === 200) keepers.value = r.data }
  catch (e) {}
  finally { loading.value = false }
}

async function nearbySearch() {
  if (!lat.value || !lng.value) return
  loading.value = true
  try {
    const r = await authStore.apiGet('/api/keepers/nearby', { lat: lat.value, lng: lng.value, radius: radius.value || undefined })
    if (r.code === 200) keepers.value = r.data
  } catch (e) {}
  finally { loading.value = false }
}

function resetSearch() {
  lat.value = ''
  lng.value = ''
  radius.value = ''
  loadKeepers()
}

onMounted(loadKeepers)
</script>
