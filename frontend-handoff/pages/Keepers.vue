<template>
  <div>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="pet-grid">
      <div v-for="k in keepers" :key="k.id_wsh" class="pet-card" @click="$router.push(`/keepers/${k.id_wsh}`)">
        <img v-if="k.avatar_wsh" :src="k.avatar_wsh" class="pet-avatar-img">
        <div v-else class="pet-avatar">👤</div>
        <div class="pet-name">{{ k.name_wsh || '寄养员' }}</div>
        <div class="pet-info">⭐ {{ k.rating_wsh || '暂无' }} · 经验: {{ k.experience_years_wsh || 0 }} 年</div>
        <div class="pet-info">每日价格: ¥{{ money(k.price_per_day_wsh) }} | 在照护: {{ k.current_pets_wsh || 0 }}/{{ k.max_pets_wsh || '-' }}</div>
        <div style="margin-top:12px;display:flex;gap:8px" @click.stop>
          <button class="btn btn-sm btn-primary" @click="goToMerchant(k)">选择服务</button>
          <button class="btn btn-sm btn-outline" @click="$router.push(`/keepers/${k.id_wsh}`)">查看主页</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getKeepers } from '@/api/keeper'

const router = useRouter()
const keepers = ref([])
const loading = ref(true)

function money(v) { return Number(v || 0).toFixed(2) }

function goToMerchant(keeper) {
  if (!keeper?.merchant_id_wsh) return
  router.push(`/merchants/${keeper.merchant_id_wsh}`)
}

async function loadKeepers() {
  loading.value = true
  try { const r = await getKeepers(); if (r.code === 200) keepers.value = r.data }
  catch (e) {}
  finally { loading.value = false }
}

onMounted(loadKeepers)
</script>

<style scoped>
.pet-card { cursor: pointer; }
.pet-avatar-img { width: 72px; height: 72px; border-radius: 50%; object-fit: cover; margin: 0 auto 10px; display: block; }
</style>
