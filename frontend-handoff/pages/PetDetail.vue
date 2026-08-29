<template>
  <div>
    <LoadingSpinner v-if="loading" text="加载宠物信息..." />

    <div v-else-if="pet" class="detail-container">
      <div class="detail-main">
        <section class="card">
          <div class="pet-hero">
            <div class="pet-avatar-wrapper">
              <img v-if="pet.avatar_wsh" :src="pet.avatar_wsh" class="pet-avatar">
              <div v-else class="pet-avatar-placeholder">{{ petTypeIcon(pet.type_wsh) }}</div>
              <label class="avatar-overlay" title="更换头像">
                <input type="file" accept="image/*" hidden @change="onAvatarChange">
                <span>更换</span>
              </label>
            </div>
            <div class="pet-hero-info">
              <h1>{{ pet.name_wsh }}</h1>
              <div class="pet-meta">
                <span class="badge badge-info">{{ petTypeLabel(pet.type_wsh) }}</span>
                <span>{{ pet.breed_wsh || '未知品种' }}</span>
                <span v-if="pet.owner_name_wsh" class="text-muted">主人：{{ pet.owner_name_wsh }}</span>
              </div>
            </div>
          </div>

          <dl class="info-grid">
            <div>
              <dt>年龄</dt>
              <dd>{{ formatAge(pet.age_wsh) }}</dd>
            </div>
            <div>
              <dt>体重</dt>
              <dd>{{ pet.weight_wsh ? pet.weight_wsh + 'kg' : '-' }}</dd>
            </div>
            <div>
              <dt>性别</dt>
              <dd>{{ genderLabel(pet.gender_wsh) }}</dd>
            </div>
            <div>
              <dt>免疫</dt>
              <dd>{{ pet.vaccinated_wsh === 1 ? '已完成' : '未完成' }}</dd>
            </div>
            <div>
              <dt>绝育</dt>
              <dd>{{ pet.sterilized_wsh === 1 ? '已绝育' : '未绝育' }}</dd>
            </div>
          </dl>

          <div v-if="pet.description_wsh" class="desc-block">
            <h4>性格与照护说明</h4>
            <p>{{ pet.description_wsh }}</p>
          </div>

          <div class="desc-row">
            <div v-if="pet.allergies_wsh" class="desc-block warn">
              <h4>过敏/禁忌</h4>
              <p>{{ pet.allergies_wsh }}</p>
            </div>
            <div v-if="pet.habits_wsh" class="desc-block">
              <h4>生活习惯</h4>
              <p>{{ pet.habits_wsh }}</p>
            </div>
          </div>

          <div class="pet-actions">
            <button class="btn btn-primary btn-sm" @click="showEditDialog = true">编辑档案</button>
          </div>
        </section>

        <section class="card">
          <h3 class="section-title">AI健康报告</h3>
          <div v-if="reports.length > 0" class="report-list">
            <div v-for="r in reports" :key="r.id_wsh" class="report-item">
              <div class="report-header">
                <strong>{{ r.type_wsh === 'daily' ? '日常报告' : r.type_wsh === 'final' ? '总结报告' : r.type_wsh || '报告' }}</strong>
                <span class="text-muted">{{ formatDate(r.created_at_wsh) }}</span>
                <button class="btn btn-sm btn-outline" @click="viewReport(r)">查看</button>
              </div>
              <p v-if="r.content_wsh" class="text-muted">{{ r.content_wsh.slice(0, 120) }}{{ r.content_wsh.length > 120 ? '...' : '' }}</p>
            </div>
          </div>
          <EmptyState v-else title="暂无报告" description="还没有AI健康报告">
            <button v-if="canGenerate" class="btn btn-sm btn-primary" @click="generateReport">生成报告</button>
          </EmptyState>
        </section>

        <section class="card">
          <h3 class="section-title">服务记录</h3>
          <div v-if="orders.length > 0" class="order-ref-list">
            <div v-for="o in orders" :key="o.id_wsh" class="order-ref-item">
              <div class="order-ref-info">
                <strong>{{ o.service_name_wsh || '寄养服务' }}</strong>
                <span class="text-muted">{{ formatDate(o.start_date_wsh) }} 至 {{ formatDate(o.end_date_wsh) }}</span>
                <span :class="['badge', orderBadge(o.status_wsh)]">{{ orderLabel(o.status_wsh) }}</span>
              </div>
              <button class="btn btn-sm btn-outline" @click="$router.push(`/orders/${o.id_wsh}`)">查看订单</button>
            </div>
          </div>
          <EmptyState v-else title="暂无服务记录" description="该宠物还没有服务记录" />
        </section>
      </div>

      <aside class="detail-sidebar">
        <div class="card">
          <h4>快捷操作</h4>
          <button class="btn btn-primary btn-block" @click="showEditDialog = true">编辑档案</button>
          <button class="btn btn-outline btn-block" @click="goBack">返回列表</button>
        </div>
      </aside>
    </div>

    <EmptyState v-else title="宠物不存在" description="找不到该宠物信息">
      <button class="btn btn-primary" @click="goBack">返回列表</button>
    </EmptyState>

    <PetFormDialog
      :visible="showEditDialog"
      :pet="pet"
      :saving="saving"
      :server-error="serverError"
      @save="handleSave"
      @close="showEditDialog = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { usePetDetail } from '@/composables/usePetDetail'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PetFormDialog from '@/components/pet/PetFormDialog.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const {
  loading, pet, orders, reports, canGenerate,
  formatAge, genderLabel, orderLabel, orderBadge, formatDate,
  load,
  updateAvatar, updateProfile,
} = usePetDetail(route.params.id)

const showEditDialog = ref(false)
const saving = ref(false)
const serverError = ref('')
const avatarUploading = ref(false)

function petTypeLabel(type) {
  const m = { dog: '狗', cat: '猫', rabbit: '兔子', bird: '鸟', fish: '鱼', hamster: '仓鼠', other: '其他' }
  return m[String(type || 'other').toLowerCase()] || type || '其他'
}

function petTypeIcon(type) {
  const m = { dog: '犬', cat: '猫', rabbit: '兔', bird: '鸟', fish: '鱼', hamster: '鼠', other: '宠' }
  return m[String(type || 'other').toLowerCase()] || '宠'
}

async function onAvatarChange(e) {
  const file = e.target?.files?.[0]
  if (!file) return
  avatarUploading.value = true
  try {
    await updateAvatar(file)
    appStore.addToast('头像更新成功', 'success')
  } catch (err) {
    appStore.addToast('上传失败: ' + (err.message || '未知错误'), 'error')
  } finally {
    avatarUploading.value = false
    e.target.value = ''
  }
}

async function handleSave(payload) {
  saving.value = true
  serverError.value = ''
  try {
    await updateProfile(payload)
    showEditDialog.value = false
    appStore.addToast('更新成功', 'success')
  } catch (e) {
    serverError.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

function viewReport(r) {
  appStore.addToast('报告内容：' + (r.content_wsh?.slice(0, 100) || '无内容'), 'info')
}

async function generateReport() {
  appStore.addToast('报告生成功能即将上线', 'info')
}

function goBack() {
  router.push('/pets')
}

onMounted(load)
</script>

<style scoped>
.detail-container {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 0;
}
.detail-main { display: flex; flex-direction: column; gap: 20px; }
.card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: 8px; padding: 20px; }
.pet-hero { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
.pet-avatar-wrapper { position: relative; width: 96px; height: 96px; flex-shrink: 0; }
.pet-avatar { width: 96px; height: 96px; border-radius: 50%; object-fit: cover; }
.pet-avatar-placeholder { width: 96px; height: 96px; border-radius: 50%; background: var(--color-muted); display: flex; align-items: center; justify-content: center; font-size: 36px; color: var(--color-primary); }
.avatar-overlay {
  position: absolute; inset: 0; border-radius: 50%;
  background: rgba(0,0,0,0.4); color: #fff;
  display: flex; align-items: center; justify-content: center;
  opacity: 0; cursor: pointer; transition: opacity 0.2s;
  font-size: 13px;
}
.avatar-overlay:hover { opacity: 1; }
.pet-hero-info { flex: 1; min-width: 0; }
.pet-hero-info h1 { margin: 0; font-size: 22px; }
.pet-meta { display: flex; align-items: center; gap: 10px; margin-top: 6px; flex-wrap: wrap; }
.info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 12px; }
.info-grid dt { color: var(--color-muted-foreground); font-size: 12px; }
.info-grid dd { margin: 2px 0 0; font-weight: 500; }
.desc-block { margin-top: 16px; }
.desc-block.warn { border-left: 3px solid var(--color-danger); padding-left: 12px; }
.desc-block h4 { margin: 0 0 8px; font-size: 14px; }
.desc-block p { color: var(--color-muted-foreground); line-height: 1.6; margin: 0; }
.desc-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.section-title { margin: 0 0 14px; font-size: 16px; }
.text-muted { color: var(--color-muted-foreground); font-size: 13px; }
.pet-actions { margin-top: 16px; display: flex; gap: 8px; }
.report-list { display: flex; flex-direction: column; gap: 12px; }
.report-item { border: 1px solid var(--color-border); border-radius: 8px; padding: 14px; }
.report-header { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.order-ref-list { display: flex; flex-direction: column; gap: 10px; }
.order-ref-item { display: flex; justify-content: space-between; align-items: center; padding: 12px; border: 1px solid var(--color-border); border-radius: 8px; }
.order-ref-info { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.order-ref-info strong { font-size: 14px; }
.detail-sidebar { display: flex; flex-direction: column; gap: 16px; }
.btn-block { width: 100%; }
@media (max-width: 760px) { .detail-container { grid-template-columns: 1fr; } .desc-row { grid-template-columns: 1fr; } }
</style>
