<template>
  <div class="pet-grid">
    <div v-for="pet in pets" :key="pet.id_wsh" class="pet-card">
      <div class="pet-card-header">
        <img
          v-if="pet.avatar_wsh"
          class="pet-avatar-img"
          :src="pet.avatar_wsh"
          :alt="pet.name_wsh"
        >
        <div v-else class="pet-avatar">{{ petTypeIcon(pet.type_wsh) }}</div>
        <div class="pet-title">
          <div class="pet-name">{{ pet.name_wsh }}</div>
          <div class="pet-info">{{ petTypeLabel(pet.type_wsh) }} / {{ pet.breed_wsh || '未知品种' }}</div>
        </div>
      </div>

      <div class="pet-detail-grid">
        <div>
          <span>年龄</span>
          <strong>{{ formatAge(pet.age_wsh) }}</strong>
        </div>
        <div>
          <span>体重</span>
          <strong>{{ formatWeight(pet.weight_wsh) }}</strong>
        </div>
        <div>
          <span>性别</span>
          <strong>{{ genderLabel(pet.gender_wsh) }}</strong>
        </div>
        <div>
          <span>档案</span>
          <strong>{{ pet.description_wsh ? '已补充' : '待完善' }}</strong>
        </div>
      </div>

      <div class="pet-badges">
        <span :class="['badge', pet.vaccinated_wsh === 1 ? 'badge-success' : 'badge-disabled']">
          {{ pet.vaccinated_wsh === 1 ? '已免疫' : '未免疫' }}
        </span>
        <span :class="['badge', pet.sterilized_wsh === 1 ? 'badge-info' : 'badge-disabled']">
          {{ pet.sterilized_wsh === 1 ? '已绝育' : '未绝育' }}
        </span>
      </div>

      <p v-if="pet.description_wsh" class="pet-note">{{ pet.description_wsh }}</p>
      <p v-if="pet.allergies_wsh" class="pet-warning">过敏/禁忌：{{ pet.allergies_wsh }}</p>
      <p v-if="pet.habits_wsh" class="pet-note">习惯：{{ pet.habits_wsh }}</p>

      <div class="pet-actions">
        <button class="btn btn-sm btn-primary" @click="$emit('view', pet.id_wsh)">查看</button>
        <button class="btn btn-sm btn-outline" @click="$emit('edit', pet)">编辑</button>
        <button class="btn btn-sm btn-danger" @click="$emit('delete', pet.id_wsh)">删除</button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  pets: {
    type: Array,
    required: true,
  },
})

defineEmits(['edit', 'delete', 'view'])

const typeMap = {
  dog: '狗',
  cat: '猫',
  rabbit: '兔子',
  bird: '鸟',
  fish: '鱼',
  hamster: '仓鼠',
  other: '其他',
}

const iconMap = {
  dog: '犬',
  cat: '猫',
  rabbit: '兔',
  bird: '鸟',
  fish: '鱼',
  hamster: '鼠',
  other: '宠',
}

function normalizeType(type) {
  return String(type || 'other').toLowerCase()
}

function petTypeLabel(type) {
  return typeMap[normalizeType(type)] || type || '其他'
}

function petTypeIcon(type) {
  return iconMap[normalizeType(type)] || '宠'
}

function genderLabel(gender) {
  const value = Number(gender)
  if (value === 1) return '公'
  if (value === 2) return '母'
  return '未知'
}

function formatAge(age) {
  return age ? `${age}个月` : '-'
}

function formatWeight(weight) {
  return weight ? `${weight}kg` : '-'
}
</script>

<style scoped>
.pet-card-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.pet-title {
  min-width: 0;
}

.pet-avatar-img {
  width: 72px;
  height: 72px;
  border-radius: 999px;
  object-fit: cover;
  background: var(--color-muted);
  flex: 0 0 auto;
}

.pet-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 14px 0;
}

.pet-detail-grid div {
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-muted);
}

.pet-detail-grid span {
  display: block;
  font-size: 12px;
  color: var(--color-muted-foreground);
}

.pet-detail-grid strong {
  display: block;
  margin-top: 2px;
  font-size: 14px;
  color: var(--color-foreground);
}

.pet-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.pet-note,
.pet-warning {
  margin-top: 8px;
  font-size: 13px;
  color: var(--color-muted-foreground);
}

.pet-warning {
  color: var(--color-destructive);
}

.pet-actions {
  margin-top: 14px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
