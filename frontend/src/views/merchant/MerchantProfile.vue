<template>
  <div class="merchant-profile-page">
    <PageHero
      :title="merchant?.name_wsh || '商家资料'"
      subtitle="查看商家基础资料、审核状态和当前店铺状态。"
    />

    <LoadingSpinner v-if="loading" text="加载商家资料..." />

    <EmptyState
      v-else-if="!merchant"
      title="暂无商家资料"
      description="当前账号还没有绑定商家信息。"
    >
      <button class="btn btn-primary" @click="goBack">返回控制台</button>
    </EmptyState>

    <section v-else class="panel">
      <div class="panel-head">
        <div>
          <h2>{{ merchant.name_wsh }}</h2>
          <div class="badge-row">
            <span :class="['badge', statusBadge]">{{ statusLabel }}</span>
            <span :class="['badge', modeBadge]">{{ modeLabel }}</span>
            <span :class="['badge', storeStatusBadge]">{{ storeStatusLabel }}</span>
          </div>
        </div>
        <button class="btn btn-outline btn-sm" @click="goBusinessHours">
          管理营业时间
        </button>
      </div>

      <dl class="info-grid">
        <div>
          <dt>联系电话</dt>
          <dd>{{ merchant.phone_wsh || '-' }}</dd>
        </div>
        <div>
          <dt>地址</dt>
          <dd>{{ merchant.address_wsh || '-' }}</dd>
        </div>
        <div>
          <dt>评分</dt>
          <dd>{{ merchant.rating_wsh ?? '-' }}</dd>
        </div>
        <div v-if="merchant.created_at_wsh">
          <dt>入驻时间</dt>
          <dd>{{ formatDate(merchant.created_at_wsh) }}</dd>
        </div>
      </dl>

      <div v-if="merchant.description_wsh" class="desc-block">
        <h3>商家简介</h3>
        <p>{{ merchant.description_wsh }}</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHero from '@/components/common/PageHero.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getMy } from '@/services/merchantService'

const router = useRouter()
const loading = ref(true)
const merchant = ref(null)

const statusMap = {
  0: { label: '待审核', badge: 'badge-warning' },
  1: { label: '已通过', badge: 'badge-success' },
  2: { label: '已拒绝', badge: 'badge-danger' },
}

const modeMap = {
  0: { label: '自动营业', badge: 'badge-info' },
  1: { label: '手动开店', badge: 'badge-success' },
  2: { label: '手动关店', badge: 'badge-secondary' },
}

const storeStatusMap = {
  0: { label: '休息中', badge: 'badge-secondary' },
  1: { label: '营业中', badge: 'badge-success' },
}

const statusLabel = computed(() => statusMap[merchant.value?.status_wsh]?.label || '-')
const statusBadge = computed(() => statusMap[merchant.value?.status_wsh]?.badge || 'badge-info')
const modeLabel = computed(() => modeMap[merchant.value?.store_mode_wsh]?.label || '-')
const modeBadge = computed(() => modeMap[merchant.value?.store_mode_wsh]?.badge || 'badge-info')
const storeStatusLabel = computed(() => storeStatusMap[merchant.value?.store_status_wsh]?.label || '-')
const storeStatusBadge = computed(() => storeStatusMap[merchant.value?.store_status_wsh]?.badge || 'badge-info')

async function loadProfile() {
  loading.value = true
  try {
    merchant.value = await getMy()
  } catch (error) {
    merchant.value = null
  } finally {
    loading.value = false
  }
}

function formatDate(value) {
  return value ? String(value).slice(0, 10) : '-'
}

function goBack() {
  router.push('/merchant/dashboard')
}

function goBusinessHours() {
  router.push('/merchant/business-hours')
}

onMounted(loadProfile)
</script>

<style scoped>
.merchant-profile-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-card);
  padding: 20px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.panel h2,
.panel h3 {
  margin: 0;
}

.badge-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin: 0;
}

.info-grid dt {
  font-size: 12px;
  color: var(--color-muted-foreground);
}

.info-grid dd {
  margin: 4px 0 0;
}

.desc-block {
  margin-top: 18px;
}

.desc-block p {
  margin: 6px 0 0;
  color: var(--color-muted-foreground);
  line-height: 1.6;
}
</style>
