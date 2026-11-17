<template>
  <div class="merchant-pets-page">
    <div class="page-head">
      <div>
        <h2>宠物管理</h2>
        <p>宠物归主人所有，这里只展示在本店下过单的宠物档案，仅供查阅。</p>
      </div>
    </div>

    <div v-if="loading" class="state">加载中...</div>

    <div v-else-if="pets.length === 0" class="state">
      本店暂时还没有服务过任何宠物。等有顾客下单后，宠物档案会自动出现在这里。
    </div>

    <div v-else class="pet-table">
      <div class="table-head">
        <span>宠物</span>
        <span>种类</span>
        <span>年龄</span>
        <span>体重</span>
        <span>主人</span>
      </div>

      <div v-for="pet in pets" :key="pet.id_wsh" class="table-row">
        <div class="pet-summary">
          <img v-if="pet.avatar_wsh" :src="pet.avatar_wsh" :alt="pet.name_wsh">
          <div v-else class="avatar-placeholder">{{ petTypeIcon(pet.type_wsh) }}</div>
          <div>
            <strong>{{ pet.name_wsh }}</strong>
            <p>{{ pet.breed_wsh || '未填写品种' }}</p>
          </div>
        </div>
        <span>{{ pet.type_wsh || '-' }}</span>
        <span>{{ pet.age_wsh ? pet.age_wsh + ' 岁' : '-' }}</span>
        <span>{{ pet.weight_wsh != null ? pet.weight_wsh + ' kg' : '-' }}</span>
        <span>{{ pet.owner_name_wsh || '-' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getMerchantPets } from '@/api/pet'
import { petTypeIcon } from '@/domain/PetDomain'

// 商家侧只读列表：接口按订单聚合本店服务过的宠物，商家不能新增或下架宠物
const pets = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const r = await getMerchantPets()
    if (r.code === 200) pets.value = r.data?.list || r.data || []
  } catch (e) {
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.merchant-pets-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head h2 {
  margin: 0;
  font-size: 22px;
}

.page-head p {
  margin: 6px 0 0;
  color: var(--color-muted-foreground);
}

.state {
  padding: 28px;
  color: var(--color-muted-foreground);
  text-align: center;
}

.pet-table {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--color-card);
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: minmax(200px, 1.5fr) minmax(80px, .6fr) minmax(80px, .6fr) minmax(90px, .7fr) minmax(110px, .9fr);
  gap: 12px;
  align-items: center;
  padding: 12px 16px;
}

.table-head {
  color: var(--color-muted-foreground);
  font-size: 13px;
  background: var(--color-muted);
  border-bottom: 1px solid var(--color-border);
}

.table-row {
  border-bottom: 1px solid var(--color-border);
}

.table-row:last-child {
  border-bottom: 0;
}

.pet-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.pet-summary img,
.avatar-placeholder {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-pill);
  object-fit: cover;
  border: 1px solid var(--color-border);
  background: var(--color-muted);
  flex-shrink: 0;
}

.avatar-placeholder {
  display: grid;
  place-items: center;
  color: var(--color-muted-foreground);
  font-size: 13px;
}

.pet-summary strong {
  display: block;
}

.pet-summary p {
  margin: 4px 0 0;
  color: var(--color-muted-foreground);
  font-size: 13px;
}

@media (max-width: 980px) {
  .table-head {
    display: none;
  }

  .table-row {
    grid-template-columns: 1fr;
    align-items: stretch;
  }
}
</style>
