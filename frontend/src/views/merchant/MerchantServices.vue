<template>
  <div class="merchant-services-page">
    <div class="page-head">
      <div>
        <h2>服务管理</h2>
        <p>发布、编辑和上下架商家可提供的宠物服务。</p>
      </div>
      <button class="btn btn-primary btn-sm" :disabled="!merchant || categories.length === 0" @click="openCreate">
        新增服务
      </button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="!merchant" class="empty-state">
      当前账号尚未绑定商家资料，暂不能发布服务。
    </div>

    <div v-else-if="categories.length === 0" class="empty-state">
      暂无可用服务分类，请联系管理员先配置服务分类。
    </div>

    <div v-else>
      <div class="filter-bar">
        <button
          v-for="cat in filterCategories"
          :key="cat.id_wsh"
          :class="['btn btn-sm', selectedCategoryId === cat.id_wsh ? 'btn-primary' : 'btn-outline']"
          @click="selectedCategoryId = cat.id_wsh"
        >
          {{ cat.name_wsh }}
          <span v-if="cat.count != null" class="filter-count">{{ cat.count }}</span>
        </button>
      </div>

      <div class="service-table">
        <div class="table-head">
          <span>服务名称</span>
          <span>服务分类</span>
          <span>价格</span>
          <span>状态</span>
          <span>操作</span>
        </div>

        <div v-if="filteredServices.length === 0" class="empty-row">
          {{ selectedCategoryId ? '该分类下暂无服务' : '暂无服务，点击"新增服务"开始发布。' }}
        </div>

        <div v-for="service in filteredServices" :key="service.id_wsh" class="table-row">
          <div class="service-summary">
            <img v-if="firstImage(service.images_wsh)" :src="firstImage(service.images_wsh)" :alt="service.name_wsh">
            <div>
              <strong>{{ service.name_wsh }}</strong>
              <p>{{ service.description_wsh || '暂无描述' }}</p>
            </div>
          </div>
          <span>{{ getCategoryName(service.category_id_wsh) }}</span>
          <span>¥{{ service.price_wsh }} / {{ unitLabel(service.unit_wsh) || '次' }}</span>
          <span :class="['badge', service.status_wsh === 1 ? 'badge-success' : 'badge-secondary']">
            {{ service.status_wsh === 1 ? '已上架' : '已下架' }}
          </span>
          <div class="row-actions">
            <button class="btn btn-sm btn-outline" @click="openEdit(service)">编辑</button>
            <button class="btn btn-sm btn-outline" @click="toggleStatus(service)">
              {{ service.status_wsh === 1 ? '下架' : '上架' }}
            </button>
            <button class="btn btn-sm btn-danger" @click="removeService(service)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showForm" class="modal-overlay" @mousedown.self="closeForm">
      <div class="modal service-modal">
        <h2>{{ editingService ? '编辑服务' : '新增服务' }}</h2>
        <p v-if="detailLoading" class="detail-loading">正在加载服务详情...</p>
        <form @submit.prevent="saveService">
          <div class="form-group">
            <label>服务名称</label>
            <input v-model.trim="form.name_wsh" required maxlength="100" placeholder="例如：标准寄养">
          </div>

          <div class="form-group">
            <label>服务分类</label>
            <select v-model.number="form.category_id_wsh" required>
              <option :value="null" disabled>请选择服务分类</option>
              <option v-for="cat in categoryOptions" :key="cat.id_wsh" :value="cat.id_wsh">
                {{ cat.label }}
              </option>
            </select>
          </div>

          <div class="form-grid">
            <div class="form-group">
              <label>价格</label>
              <input v-model.number="form.price_wsh" required type="number" min="0" step="0.01" placeholder="0.00">
            </div>
            <div class="form-group">
              <label>计价单位</label>
              <select data-testid="unit-select" :value="form.unit_wsh" @change="onUnitChange">
                <option value="day">天</option>
                <option value="session">次</option>
                <option value="hour">小时</option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label>单次服务时长（分钟）</label>
            <input
              data-testid="duration-input"
              v-model.number="form.duration_minutes_wsh"
              type="number"
              min="15"
              max="1440"
              step="15"
              :disabled="form.unit_wsh === 'day'"
            >
            <span v-if="durationError" data-testid="duration-error" class="field-error">{{ durationError }}</span>
            <span v-else class="field-hint">{{ durationHint }}</span>
          </div>

          <div class="form-group">
            <label>服务图片</label>
            <div class="image-uploader">
              <input
                ref="imageInput"
                type="file"
                accept="image/*"
                multiple
                class="hidden-input"
                :disabled="imageUploading"
                @change="uploadServiceImages"
              >
              <button type="button" class="btn btn-outline btn-sm" :disabled="imageUploading" @click="imageInput?.click()">
                {{ imageUploading ? '上传中...' : '上传图片' }}
              </button>
              <span class="upload-hint">支持 PNG/JPEG/GIF/BMP，单张不超过 10MB，最多 {{ MAX_MEDIA }} 张。</span>
            </div>
            <div v-if="mediaItems.length" class="image-preview-grid">
              <div v-for="(item, index) in mediaItems" :key="item.file_id_wsh ?? item.url_wsh" class="image-preview">
                <img :src="item.url_wsh" :alt="`服务图片 ${index + 1}`">
                <span v-if="item.is_cover" class="cover-badge">封面</span>
                <div class="media-actions">
                  <button type="button" :disabled="imageUploading || index === 0" @click="moveMedia(index, -1)">左移</button>
                  <button type="button" :disabled="imageUploading || index === mediaItems.length - 1" @click="moveMedia(index, 1)">右移</button>
                  <button type="button" :disabled="imageUploading || item.is_cover" @click="setCover(index)">设为封面</button>
                  <button type="button" :disabled="imageUploading" @click="removeMedia(index)">移除</button>
                </div>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label>服务描述</label>
            <textarea v-model.trim="form.description_wsh" rows="4" placeholder="说明服务内容、适用宠物和注意事项"></textarea>
          </div>

          <div v-if="editingService" class="form-group">
            <label>状态</label>
            <select v-model.number="form.status_wsh">
              <option :value="1">上架</option>
              <option :value="0">下架</option>
            </select>
          </div>

          <div class="modal-actions">
            <button type="button" class="btn btn-secondary btn-sm" :disabled="saving || imageUploading || detailLoading" @click="closeForm">取消</button>
            <button type="submit" class="btn btn-primary btn-sm" :disabled="saving || imageUploading || detailLoading">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { getMy } from '@/services/merchantService'
import {
  createService,
  deleteService,
  getMerchantServices,
  getServiceManageDetail,
  toggleServiceStatus,
  updateService,
} from '@/api/service'
import { getServiceCategoryList } from '@/api/serviceCategory'
import { uploadProductImage } from '@/api/file'
import { normalizeUnit, defaultDurationMinutes, unitLabel } from '@/domain/BookingUnit'

const MAX_MEDIA = 10
const MAX_IMAGE_BYTES = 10 * 1024 * 1024

const appStore = useAppStore()
const loading = ref(true)
const saving = ref(false)
const imageUploading = ref(false)
const detailLoading = ref(false)
const imageInput = ref(null)
const merchant = ref(null)
const services = ref([])
const categories = ref([])
const showForm = ref(false)
const editingService = ref(null)
const selectedCategoryId = ref(0)
const mediaItems = ref([])
let formSnapshot = ''

const form = reactive({
  name_wsh: '',
  category_id_wsh: null,
  description_wsh: '',
  price_wsh: null,
  unit_wsh: 'day',
  duration_minutes_wsh: 1440,
  status_wsh: 1,
})

function onUnitChange(event) {
  const unit = event.target.value
  form.unit_wsh = unit
  form.duration_minutes_wsh = defaultDurationMinutes(unit)
}

const durationError = computed(() => {
  const unit = form.unit_wsh
  const minutes = Number(form.duration_minutes_wsh)
  if (!minutes || Number.isNaN(minutes)) return '请输入服务时长'
  if (minutes < 15) return '时长不能少于 15 分钟'
  if (minutes > 1440) return '时长不能超过 1440 分钟'
  if (unit === 'hour' && minutes % 60 !== 0) return '按小时服务时长必须是 60 的整数倍'
  return ''
})

const durationHint = computed(() => {
  const unit = form.unit_wsh
  if (unit === 'day') return '按天服务时长固定为 1440 分钟（1 天）'
  if (unit === 'hour') return '必须是 60 的整数倍，范围 60..1440 分钟'
  return '范围 15..1440 分钟'
})

const categoryOptions = computed(() => {
  const byParent = new Map()
  categories.value.forEach(cat => {
    const parentId = cat.parent_id_wsh || 0
    if (!byParent.has(parentId)) byParent.set(parentId, [])
    byParent.get(parentId).push(cat)
  })

  const walk = (parentId = 0, level = 0, rows = []) => {
    const siblings = [...(byParent.get(parentId) || [])].sort((a, b) => {
      const orderDiff = (a.sort_order_wsh || 0) - (b.sort_order_wsh || 0)
      if (orderDiff !== 0) return orderDiff
      return (a.id_wsh || 0) - (b.id_wsh || 0)
    })
    siblings.forEach(cat => {
      rows.push({
        ...cat,
        label: `${'  '.repeat(level)}${cat.name_wsh}`,
      })
      walk(cat.id_wsh, level + 1, rows)
    })
    return rows
  }

  return walk()
})

function parseImages(value) {
  return String(value || '')
    .split(',')
    .map(url => url.trim())
    .filter(Boolean)
}

function firstImage(value) {
  return parseImages(value)[0] || ''
}

function currentMediaSignature() {
  return JSON.stringify({
    ...form,
    media: mediaItems.value.map(item => [item.file_id_wsh ?? item.url_wsh, item.is_cover]),
  })
}

function takeSnapshot() {
  formSnapshot = currentMediaSignature()
}

function isDirty() {
  return formSnapshot !== currentMediaSignature()
}

function resetForm() {
  Object.assign(form, {
    name_wsh: '',
    category_id_wsh: null,
    description_wsh: '',
    price_wsh: null,
    unit_wsh: 'day',
    duration_minutes_wsh: 1440,
    status_wsh: 1,
  })
  mediaItems.value = []
}

function getCategoryIdsIncludingChildren(id, allCats) {
  const ids = [id]
  const children = allCats.filter(c => (c.parent_id_wsh || 0) === id)
  children.forEach(child => ids.push(...getCategoryIdsIncludingChildren(child.id_wsh, allCats)))
  return ids
}

const filterCategories = computed(() => {
  const topLevel = categories.value.filter(c => !c.parent_id_wsh)
  return [
    { id_wsh: 0, name_wsh: '全部' },
    ...topLevel,
  ]
})

const filteredServices = computed(() => {
  if (!selectedCategoryId.value) return services.value
  const ids = getCategoryIdsIncludingChildren(selectedCategoryId.value, categories.value)
  return services.value.filter(s => ids.includes(s.category_id_wsh))
})

function getCategoryName(categoryId) {
  const category = categories.value.find(item => item.id_wsh === categoryId)
  return category?.name_wsh || '未分类'
}

async function loadPage() {
  loading.value = true
  try {
    const [myMerchant, categoryRes] = await Promise.all([
      getMy().catch(() => null),
      getServiceCategoryList(),
    ])
    merchant.value = myMerchant
    categories.value = categoryRes.code === 200 ? (categoryRes.data || []) : []
    await loadServices()
  } catch (error) {
    appStore.addToast('加载服务管理数据失败', 'error')
  } finally {
    loading.value = false
  }
}

async function loadServices() {
  if (!merchant.value?.id_wsh) {
    services.value = []
    return
  }
  const res = await getMerchantServices(merchant.value.id_wsh)
  services.value = res.code === 200 ? (res.data || []) : []
}

function openCreate() {
  editingService.value = null
  resetForm()
  takeSnapshot()
  showForm.value = true
}

async function openEdit(service) {
  editingService.value = service
  resetForm()
  showForm.value = true
  detailLoading.value = true
  try {
    const res = await getServiceManageDetail(service.id_wsh)
    if (res.code !== 200 || !res.data) {
      appStore.addToast(res.message || '加载服务详情失败', 'error')
      showForm.value = false
      editingService.value = null
      return
    }
    const detail = res.data
    const rawUnit = normalizeUnit(detail.service_wsh?.unit_wsh) || 'day'
    Object.assign(form, {
      name_wsh: detail.service_wsh?.name_wsh || '',
      category_id_wsh: detail.service_wsh?.category_id_wsh ?? null,
      description_wsh: detail.service_wsh?.description_wsh || '',
      price_wsh: Number(detail.service_wsh?.price_wsh ?? 0),
      unit_wsh: rawUnit,
      duration_minutes_wsh: Number(detail.service_wsh?.duration_minutes_wsh) || defaultDurationMinutes(rawUnit),
      status_wsh: Number(detail.service_wsh?.status_wsh ?? 1),
    })
    const media = (detail.media_wsh || []).map(item => ({
      file_id_wsh: item.file_id_wsh ?? null,
      url_wsh: item.url_wsh || '',
      is_cover: Number(item.is_cover_wsh) === 1,
    }))
    if (!media.length) {
      parseImages(detail.service_wsh?.images_wsh).forEach((url, index) => {
        media.push({ file_id_wsh: null, url_wsh: url, is_cover: index === 0 })
      })
    }
    mediaItems.value = media
  } catch (error) {
    appStore.addToast('加载服务详情失败', 'error')
    showForm.value = false
    editingService.value = null
    return
  } finally {
    detailLoading.value = false
    takeSnapshot()
  }
}

function closeForm() {
  if (saving.value || imageUploading.value || detailLoading.value) return
  if (showForm.value && isDirty() && !window.confirm('有未保存的更改，确定关闭吗？')) return
  showForm.value = false
  editingService.value = null
  mediaItems.value = []
}

async function uploadServiceImages(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  if (!files.length) return

  const invalidType = files.find(file => !file.type.startsWith('image/'))
  if (invalidType) {
    appStore.addToast('只能上传图片文件', 'error')
    return
  }
  const oversize = files.find(file => file.size > MAX_IMAGE_BYTES)
  if (oversize) {
    appStore.addToast('单张图片不能超过 10MB', 'error')
    return
  }
  if (mediaItems.value.length + files.length > MAX_MEDIA) {
    appStore.addToast(`最多上传 ${MAX_MEDIA} 张图片`, 'error')
    return
  }

  imageUploading.value = true
  const uploaded = []
  let failedCount = 0
  try {
    for (const file of files) {
      const data = new FormData()
      data.append('file', file)
      try {
        const res = await uploadProductImage(merchant.value.id_wsh, data)
        if (res.code === 200 && res.data?.id_wsh && res.data?.url_wsh) {
          uploaded.push({ file_id_wsh: res.data.id_wsh, url_wsh: res.data.url_wsh })
        } else {
          failedCount++
        }
      } catch (error) {
        failedCount++
      }
    }
    if (uploaded.length) {
      const existing = new Set(mediaItems.value.map(item => item.file_id_wsh))
      const next = mediaItems.value.slice()
      uploaded.forEach(item => {
        if (!existing.has(item.file_id_wsh)) {
          next.push({ ...item, is_cover: false })
        }
      })
      if (next.length && !next.some(item => item.is_cover)) {
        next[0].is_cover = true
      }
      mediaItems.value = next
      if (failedCount > 0) {
        appStore.addToast(`成功上传 ${uploaded.length} 张，失败 ${failedCount} 张，可重新选择重试`, 'error')
      } else {
        appStore.addToast('图片上传成功', 'success')
      }
    } else {
      appStore.addToast('图片上传失败，请重试', 'error')
    }
  } finally {
    imageUploading.value = false
  }
}

function moveMedia(index, direction) {
  const target = index + direction
  if (target < 0 || target >= mediaItems.value.length) return
  const next = mediaItems.value.slice()
  const temp = next[index]
  next[index] = next[target]
  next[target] = temp
  mediaItems.value = next
}

function setCover(index) {
  const next = mediaItems.value.map(item => ({ ...item, is_cover: false }))
  next[index].is_cover = true
  mediaItems.value = next
}

function removeMedia(index) {
  const removed = mediaItems.value[index]
  const next = mediaItems.value.filter((_, i) => i !== index)
  if (removed?.is_cover && next.length && !next.some(item => item.is_cover)) {
    next[0].is_cover = true
  }
  mediaItems.value = next
}

function buildPayload() {
  if (!form.category_id_wsh) {
    appStore.addToast('请选择服务分类', 'error')
    return null
  }
  if (form.price_wsh === null || form.price_wsh === '' || Number(form.price_wsh) < 0) {
    appStore.addToast('请输入有效价格', 'error')
    return null
  }
  if (durationError.value) {
    appStore.addToast(durationError.value, 'error')
    return null
  }
  if (mediaItems.value.length && mediaItems.value.some(item => item.file_id_wsh == null)) {
    appStore.addToast('存在旧版图片，请移除后重新上传', 'error')
    return null
  }

  const payload = {
    name_wsh: form.name_wsh,
    category_id_wsh: form.category_id_wsh,
    description_wsh: form.description_wsh,
    price_wsh: form.price_wsh,
    unit_wsh: form.unit_wsh || 'day',
    duration_minutes_wsh: Number(form.duration_minutes_wsh),
    media_wsh: mediaItems.value.map((item, index) => ({
      file_id_wsh: item.file_id_wsh,
      sort_order_wsh: index,
      is_cover_wsh: item.is_cover ? 1 : 0,
    })),
  }

  if (editingService.value) {
    payload.status_wsh = form.status_wsh
  }

  return payload
}

async function saveService() {
  if (saving.value || imageUploading.value) return
  const payload = buildPayload()
  if (!payload) return

  saving.value = true
  try {
    const res = editingService.value
      ? await updateService(editingService.value.id_wsh, payload)
      : await createService(payload)
    if (res.code === 200) {
      appStore.addToast(editingService.value ? '服务已更新' : '服务已发布', 'success')
      showForm.value = false
      editingService.value = null
      await loadServices()
    } else {
      appStore.addToast(res.message || '保存服务失败', 'error')
    }
  } catch (error) {
    appStore.addToast('保存服务失败', 'error')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(service) {
  try {
    const res = await toggleServiceStatus(service.id_wsh)
    if (res.code === 200) {
      appStore.addToast(service.status_wsh === 1 ? '服务已下架' : '服务已上架', 'success')
      await loadServices()
    } else {
      appStore.addToast(res.message || '更新状态失败', 'error')
    }
  } catch (error) {
    appStore.addToast('更新状态失败', 'error')
  }
}

async function removeService(service) {
  if (!confirm(`确定删除服务“${service.name_wsh}”吗？`)) return
  try {
    const res = await deleteService(service.id_wsh)
    if (res.code === 200) {
      appStore.addToast('服务已删除', 'success')
      await loadServices()
    } else {
      appStore.addToast(res.message || '删除服务失败', 'error')
    }
  } catch (error) {
    appStore.addToast('删除服务失败', 'error')
  }
}

onMounted(loadPage)
</script>

<style scoped>
.merchant-services-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.page-head h2 {
  margin: 0;
  font-size: 22px;
}

.page-head p {
  margin: 6px 0 0;
  color: var(--color-muted-foreground);
}

.loading,
.empty-state,
.empty-row {
  padding: 28px;
  color: var(--color-muted-foreground);
  text-align: center;
}

.service-table {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--color-card);
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: minmax(220px, 1.6fr) minmax(130px, .9fr) minmax(120px, .8fr) 90px minmax(220px, 1fr);
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

.service-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.service-summary img {
  width: 52px;
  height: 52px;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid var(--color-border);
  background: var(--color-muted);
  flex-shrink: 0;
}

.table-row strong {
  display: block;
}

.table-row p {
  margin: 4px 0 0;
  color: var(--color-muted-foreground);
  font-size: 13px;
  line-height: 1.5;
}

.row-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.service-modal {
  max-width: 620px;
}

.detail-loading {
  margin: 0 0 12px;
  color: var(--color-muted-foreground);
  font-size: 13px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.image-uploader {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.hidden-input {
  display: none;
}

.upload-hint {
  color: var(--color-muted-foreground);
  font-size: 13px;
}

.image-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(96px, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.image-preview {
  position: relative;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--color-muted);
  aspect-ratio: 1;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-badge {
  position: absolute;
  left: 6px;
  top: 6px;
  border: 0;
  border-radius: 4px;
  padding: 2px 8px;
  background: rgba(24, 144, 255, .92);
  color: #fff;
  font-size: 12px;
  line-height: 18px;
}

.media-actions {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 4px;
  justify-content: center;
  padding: 4px;
  background: rgba(0, 0, 0, .68);
}

.media-actions button {
  border: 0;
  border-radius: 4px;
  padding: 3px 6px;
  background: rgba(255, 255, 255, .18);
  color: #fff;
  font-size: 11px;
  line-height: 16px;
  cursor: pointer;
}

.media-actions button:disabled {
  opacity: .4;
  cursor: not-allowed;
}

.media-actions button:hover:not(:disabled) {
  background: rgba(255, 255, 255, .32);
}

textarea {
  resize: vertical;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.field-hint {
  display: block;
  min-height: 18px;
  color: var(--color-muted-foreground);
  font-size: 12px;
  line-height: 1.5;
  margin-top: 4px;
}

.field-error {
  display: block;
  min-height: 18px;
  color: var(--color-danger, #e5484d);
  font-size: 12px;
  line-height: 1.5;
  margin-top: 4px;
}

.filter-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  font-size: 11px;
  line-height: 18px;
  background: rgba(0, 0, 0, 0.08);
}

.btn-primary .filter-count {
  background: rgba(255, 255, 255, 0.22);
}

@media (max-width: 980px) {
  .table-head {
    display: none;
  }

  .table-row {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .row-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 560px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
