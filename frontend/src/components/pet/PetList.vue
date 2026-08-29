<template>
  <ul class="pet-grid">
    <li v-for="pet in pets" :key="pet.id_wsh" class="pet-card-wrap">
      <article class="pet-card" @click="$emit('view', pet.id_wsh)">
        <!-- Header: Avatar + Name -->
        <header class="pet-card-header">
          <div class="pet-avatar-wrap">
            <img
              v-if="pet.avatar_wsh"
              class="pet-avatar-img"
              :src="pet.avatar_wsh"
              :alt="pet.name_wsh"
              loading="lazy"
            >
            <span v-else class="pet-avatar-placeholder" :aria-label="`${pet.name_wsh}（${petTypeLabel(pet.type_wsh)}）暂无头像`">
              <svg v-if="normalizeType(pet.type_wsh) === 'dog'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10 5.172C10 3.782 8.423 2.679 6.5 3c-2.823.47-4.113 6.006-4 7 .08.703 1.725 1.722 3.656 1 1.261-.472 1.96-1.45 2.344-2.5M14.267 5.172c0-1.39 1.577-2.493 3.5-2.172 2.823.47 4.113 6.006 4 7-.08.703-1.725 1.722-3.656 1-1.261-.472-1.855-1.45-2.239-2.5M8 14v.5M15.5 14v.5M11.75 16.25c.75.5 2 .5 2.75 0M4.42 11.247A13.152 13.152 0 0 0 4 14.556C4 18.728 7.582 21 12 21s8-2.272 8-6.444c0-1.061-.162-2.2-.493-3.309m-9.243-6.082A8.801 8.801 0 0 1 12 5c.78 0 1.5.108 2.161.306"/></svg>
              <svg v-else-if="normalizeType(pet.type_wsh) === 'cat'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 5c-1.5-3-5-3-5-3s0 3.5 1.5 5M12 5c1.5-3 5-3 5-3s0 3.5-1.5 5M12 5v14M8 19c-2 0-4-1-4-3 0-2 2-3 4-3M16 19c2 0 4-1 4-3 0-2-2-3-4-3M8 19h8"/></svg>
              <svg v-else-if="normalizeType(pet.type_wsh) === 'rabbit'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M8 2c0 2-2 6-2 10 0 3 2 6 6 6s6-3 6-6c0-4-2-8-2-10M12 18v4M9 22h6"/></svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M11.5 9C11.5 9 9 7 7 9s-2 5 2 7 7-2 7-2-1.5-2-4.5-4z"/><circle cx="6.5" cy="6.5" r="1.5"/><circle cx="17.5" cy="6.5" r="1.5"/><circle cx="11.5" cy="5.5" r="1.5"/><path d="M12 18c-4 0-6-2-6-5 0-2 2-4 6-4s6 2 6 4c0 3-2 5-6 5z"/></svg>
            </span>
          </div>
          <div class="pet-title">
            <div class="pet-name-row">
              <button type="button" class="pet-name-btn" @click.stop="$emit('view', pet.id_wsh)">
                {{ pet.name_wsh }}
              </button>
              <svg class="pet-arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M7 17L17 7M17 7H7M17 7v10"/></svg>
            </div>
            <p class="pet-type-breed">
              {{ petTypeLabel(pet.type_wsh) }}
              <span class="pet-sep">·</span>
              {{ pet.breed_wsh || '未知品种' }}
            </p>
          </div>
          <span v-if="!profileComplete(pet)" class="pet-badge-warn">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            档案待完善
          </span>
        </header>

        <!-- Specs Grid -->
        <dl class="pet-specs">
          <div class="pet-spec">
            <dt>年龄</dt>
            <dd :class="{ 'pet-spec-empty': !formatAge(pet.age_wsh) }">
              {{ formatAge(pet.age_wsh) || '—' }}
            </dd>
          </div>
          <div class="pet-spec">
            <dt>体重</dt>
            <dd :class="{ 'pet-spec-empty': !formatWeight(pet.weight_wsh) }">
              {{ formatWeight(pet.weight_wsh) || '—' }}
            </dd>
          </div>
          <div class="pet-spec">
            <dt>性别</dt>
            <dd :class="{ 'pet-spec-empty': pet.gender_wsh === 0 }">
              {{ genderLabel(pet.gender_wsh) }}
            </dd>
          </div>
        </dl>

        <!-- Health Flags -->
        <ul class="pet-health">
          <li class="pet-health-item" :class="{ active: pet.vaccinated_wsh === 1 }">
            <span class="pet-dot" :class="pet.vaccinated_wsh === 1 ? 'dot-ok' : 'dot-muted'"></span>
            {{ pet.vaccinated_wsh === 1 ? '已免疫' : '未免疫' }}
          </li>
          <li class="pet-health-item" :class="{ active: pet.sterilized_wsh === 1 }">
            <span class="pet-dot" :class="pet.sterilized_wsh === 1 ? 'dot-info' : 'dot-muted'"></span>
            {{ pet.sterilized_wsh === 1 ? '已绝育' : '未绝育' }}
          </li>
        </ul>

        <!-- Notes -->
        <div v-if="pet.description_wsh || pet.allergies_wsh || pet.habits_wsh" class="pet-notes">
          <p v-if="pet.description_wsh" class="pet-note-desc">{{ pet.description_wsh }}</p>
          <p v-if="pet.habits_wsh" class="pet-note-habit">
            <span class="pet-note-label">习惯</span> {{ pet.habits_wsh }}
          </p>
          <p v-if="pet.allergies_wsh" class="pet-note-allergy">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            <span>
              <strong>过敏 / 禁忌</strong> {{ pet.allergies_wsh }}
            </span>
          </p>
        </div>

        <!-- Actions -->
        <div class="pet-actions" @click.stop>
          <button class="btn btn-primary btn-sm" @click="$emit('view', pet.id_wsh)">查看档案</button>
          <button class="btn btn-outline btn-sm" @click="$emit('edit', pet)">编辑</button>
          <button class="btn btn-quiet btn-sm pet-delete-btn" @click="$emit('delete', pet.id_wsh)" :aria-label="`删除 ${pet.name_wsh}`">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            删除
          </button>
        </div>
      </article>
    </li>
  </ul>
</template>

<script setup>
defineProps({
  pets: { type: Array, required: true },
})

defineEmits(['edit', 'delete', 'view'])

const typeMap = {
  dog: '狗', cat: '猫', rabbit: '兔子', bird: '鸟',
  fish: '鱼', hamster: '仓鼠', other: '其他',
}

function normalizeType(type) {
  return String(type || 'other').toLowerCase()
}

function petTypeLabel(type) {
  return typeMap[normalizeType(type)] || type || '其他'
}

function genderLabel(gender) {
  const value = Number(gender)
  return value === 1 ? '公' : value === 2 ? '母' : '未知'
}

function formatAge(age) {
  return age ? `${age} 个月` : null
}

function formatWeight(weight) {
  return weight ? `${weight} kg` : null
}

function profileComplete(pet) {
  return Boolean(pet.description_wsh)
}
</script>

<style scoped>
.pet-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}
@media (min-width: 768px) {
  .pet-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (min-width: 1200px) {
  .pet-grid { grid-template-columns: repeat(3, 1fr); }
}

.pet-card-wrap { height: 100%; }

.pet-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px;
  border-radius: 16px;
  border: 1px solid var(--color-border);
  background: var(--color-card);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}
.pet-card:hover {
  border-color: var(--color-border);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

/* Header */
.pet-card-header {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}
.pet-avatar-wrap { flex-shrink: 0; }
.pet-avatar-img {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--color-border);
}
.pet-avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-muted);
  color: var(--color-muted-foreground);
  border: 1px solid var(--color-border);
}
.pet-avatar-placeholder svg { width: 24px; height: 24px; }

.pet-title { min-width: 0; flex: 1; }
.pet-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.pet-name-btn {
  background: none;
  border: none;
  padding: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-foreground);
  text-align: left;
  cursor: pointer;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  border-radius: 4px;
}
.pet-name-btn:hover { text-decoration: underline; }
.pet-name-btn:focus-visible {
  outline: 2px solid var(--color-ring);
  outline-offset: 2px;
}
.pet-arrow-icon {
  width: 14px;
  height: 14px;
  color: var(--color-muted-foreground);
  opacity: 0;
  transition: opacity 0.2s ease;
  flex-shrink: 0;
}
.pet-card:hover .pet-arrow-icon { opacity: 1; }
.pet-type-breed {
  margin-top: 2px;
  font-size: 14px;
  color: var(--color-muted-foreground);
}
.pet-sep { margin: 0 6px; color: var(--color-border); }

.pet-badge-warn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(249, 115, 22, 0.08);
  font-size: 12px;
  font-weight: 500;
  color: var(--color-primary);
}
.pet-badge-warn svg { width: 12px; height: 12px; }

/* Specs */
.pet-specs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  margin: 16px 0;
  padding: 14px 0;
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
}
.pet-spec {
  padding: 0 16px;
  border-right: 1px solid var(--color-border);
}
.pet-spec:first-child { padding-left: 0; }
.pet-spec:last-child { border-right: none; padding-right: 0; }
.pet-spec dt {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.05em;
  color: var(--color-muted-foreground);
  text-transform: uppercase;
}
.pet-spec dd {
  margin-top: 4px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-foreground);
  font-variant-numeric: tabular-nums;
}
.pet-spec dd.pet-spec-empty { color: var(--color-muted-foreground); }

/* Health Flags */
.pet-health {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  list-style: none;
  margin-bottom: 0;
}
.pet-health-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--color-muted-foreground);
}
.pet-health-item.active { color: var(--color-foreground); }
.pet-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.dot-ok { background: var(--color-success); }
.dot-info { background: var(--color-info); }
.dot-muted { background: var(--color-border); }

/* Notes */
.pet-notes { margin-top: 12px; flex: 1; }
.pet-note-desc {
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-muted-foreground);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.pet-note-habit {
  margin-top: 6px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-muted-foreground);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pet-note-label { color: var(--color-muted-foreground); }
.pet-note-allergy {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 6px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-danger);
}
.pet-note-allergy svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  margin-top: 3px;
}
.pet-note-allergy strong { font-weight: 500; }

/* Actions */
.pet-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
}
.pet-delete-btn {
  margin-left: auto;
  padding: 8px 10px;
}
.pet-delete-btn svg { width: 14px; height: 14px; }

/* Quiet Button */
.btn-quiet {
  background: transparent;
  border: none;
  color: var(--color-muted-foreground);
  cursor: pointer;
}
.btn-quiet:hover {
  background: rgba(220, 38, 38, 0.06);
  color: var(--color-danger);
}
</style>
