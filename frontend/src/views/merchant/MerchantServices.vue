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
          <span>¥{{ service.price_wsh }} / {{ service.unit_wsh || '次' }}</span>
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
              <input v-model.trim="form.unit_wsh" maxlength="20" placeholder="天 / 次 / 小时">
            </div>
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
              <span class="upload-hint">支持多张图片，上传后自动保存 MinIO 地址。</span>
            </div>
            <div v-if="imageList.length" class="image-preview-grid">
              <div v-for="url in imageList" :key="url" class="image-preview">
                <img :src="url" alt="服务图片">
                <button type="button" class="remove-image" :disabled="imageUploading" @click="removeImage(url)">移除</button>
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
            <button type="button" class="btn btn-secondary btn-sm" :disabled="saving || imageUploading" @click="closeForm">取消</button>
            <button type="submit" class="btn btn-primary btn-sm" :disabled="saving || imageUploading">
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
  toggleServiceStatus,
  updateService,
} from '@/api/service'
import { getServiceCategoryList } from '@/api/serviceCategory'
import { uploadFileToDirectory } from '@/api/file'

const appStore = useAppStore()
const loading = ref(true)
const saving = ref(false)
const imageUploading = ref(false)
const imageInput = ref(null)
const merchant = ref(null)
const services = ref([])
const categories = ref([])
const showForm = ref(false)
const editingService = ref(null)
const selectedCategoryId = ref(0)

const form = reactive({
  name_wsh: '',
  category_id_wsh: null,
  description_wsh: '',
  price_wsh: null,
  unit_wsh: '天',
  images_wsh: '',
  status_wsh: 1,
})

const imageList = computed(() => parseImages(form.images_wsh))

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

function setImages(urls) {
  form.images_wsh = [...new Set(urls)].join(',')
}

function firstImage(value) {
  return parseImages(value)[0] || ''
}

function resetForm() {
  Object.assign(form, {
    name_wsh: '',
    category_id_wsh: null,
    description_wsh: '',
    price_wsh: null,
    unit_wsh: '天',
    images_wsh: '',
    status_wsh: 1,
  })
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
  showForm.value = true
}

function openEdit(service) {
  editingService.value = service
  Object.assign(form, {
    name_wsh: service.name_wsh || '',
    category_id_wsh: service.category_id_wsh ?? null,
    description_wsh: service.description_wsh || '',
    price_wsh: Number(service.price_wsh ?? 0),
    unit_wsh: service.unit_wsh || '天',
    images_wsh: service.images_wsh || '',
    status_wsh: Number(service.status_wsh ?? 1),
  })
  showForm.value = true
}

function closeForm() {
  if (saving.value || imageUploading.value) return
  showForm.value = false
  editingService.value = null
}

async function uploadServiceImages(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  if (!files.length) return

  const invalid = files.find(file => !file.type.startsWith('image/'))
  if (invalid) {
    appStore.addToast('只能上传图片文件', 'error')
    return
  }

  imageUploading.value = true
  try {
    const uploadedUrls = []
    for (const file of files) {
      const data = new FormData()
      data.append('file', file)
      const res = await uploadFileToDirectory('services', data)
      if (res.code === 200 && res.data?.url_wsh) {
        uploadedUrls.push(res.data.url_wsh)
      } else {
        throw new Error(res.message || '上传图片失败')
      }
    }
    setImages([...imageList.value, ...uploadedUrls])
    appStore.addToast('图片上传成功', 'success')
  } catch (error) {
    appStore.addToast(error.message || '上传图片失败', 'error')
  } finally {
    imageUploading.value = false
  }
}

function removeImage(url) {
  setImages(imageList.value.filter(item => item !== url))
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

  const payload = {
    name_wsh: form.name_wsh,
    category_id_wsh: form.category_id_wsh,
    description_wsh: form.description_wsh,
    price_wsh: form.price_wsh,
    unit_wsh: form.unit_wsh || '天',
    images_wsh: form.images_wsh,
  }

  if (editingService.value) {
    payload.status_wsh = form.status_wsh
  } else {
    payload.merchant_id_wsh = merchant.value.id_wsh
  }

  return payload
}

async function saveService() {
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

.remove-image {
  position: absolute;
  right: 6px;
  bottom: 6px;
  border: 0;
  border-radius: 4px;
  padding: 4px 8px;
  background: rgba(0, 0, 0, .68);
  color: #fff;
  font-size: 12px;
  cursor: pointer;
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
